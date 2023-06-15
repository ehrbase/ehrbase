/*
 * Copyright (c) 2019-2022 vitasystems GmbH and Hannover Medical School.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.dao.access.jooq;

import static org.ehrbase.jooq.pg.Tables.EVENT_CONTEXT;
import static org.ehrbase.jooq.pg.Tables.EVENT_CONTEXT_HISTORY;
import static org.ehrbase.jooq.pg.Tables.IDENTIFIER;
import static org.ehrbase.jooq.pg.Tables.PARTICIPATION;
import static org.ehrbase.jooq.pg.Tables.PARTICIPATION_HISTORY;
import static org.ehrbase.jooq.pg.Tables.PARTY_IDENTIFIED;

import com.nedap.archie.rm.composition.EventContext;
import com.nedap.archie.rm.datastructures.ItemStructure;
import com.nedap.archie.rm.datavalues.DvCodedText;
import com.nedap.archie.rm.datavalues.DvIdentifier;
import com.nedap.archie.rm.datavalues.DvText;
import com.nedap.archie.rm.datavalues.quantity.DvInterval;
import com.nedap.archie.rm.datavalues.quantity.datetime.DvDateTime;
import com.nedap.archie.rm.generic.Participation;
import com.nedap.archie.rm.generic.PartyIdentified;
import com.nedap.archie.rm.generic.PartyProxy;
import com.nedap.archie.rm.support.identification.ObjectId;
import com.nedap.archie.rm.support.identification.PartyRef;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.commons.collections4.CollectionUtils;
import org.ehrbase.api.definitions.ServerConfig;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.dao.access.interfaces.I_CompositionAccess;
import org.ehrbase.dao.access.interfaces.I_ContextAccess;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.dao.access.jooq.party.PersistedObjectId;
import org.ehrbase.dao.access.jooq.party.PersistedPartyProxy;
import org.ehrbase.dao.access.support.DataAccess;
import org.ehrbase.dao.access.util.TransactionTime;
import org.ehrbase.jooq.dbencoding.RawJson;
import org.ehrbase.jooq.pg.tables.records.EventContextHistoryRecord;
import org.ehrbase.jooq.pg.tables.records.EventContextRecord;
import org.ehrbase.jooq.pg.tables.records.ParticipationHistoryRecord;
import org.ehrbase.jooq.pg.tables.records.ParticipationRecord;
import org.ehrbase.jooq.pg.tables.records.PartyIdentifiedRecord;
import org.ehrbase.service.RecordedDvCodedText;
import org.ehrbase.service.RecordedDvDateTime;
import org.ehrbase.service.RecordedDvText;
import org.ehrbase.util.UuidGenerator;
import org.jooq.DSLContext;
import org.jooq.InsertQuery;
import org.jooq.JSONB;
import org.jooq.Result;
import org.jooq.UpdateQuery;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Christian Chevalley
 * @author Jake Smolka
 * @author Luis Marco-Ruiz
 * @since 1.0
 */
public class ContextAccess extends DataAccess implements I_ContextAccess {
    private static final String DB_INCONSISTENCY = "DB inconsistency";

    private final Logger log = LoggerFactory.getLogger(ContextAccess.class);
    private final List<ParticipationRecord> participations = new ArrayList<>();
    private EventContextRecord eventContextRecord;

    public ContextAccess(DSLContext context, ServerConfig serverConfig, EventContext eventContext, Short sysTenant) {
        super(context, null, null, serverConfig);
        if (eventContext == null) return;
        eventContextRecord = context.newRecord(EVENT_CONTEXT);
        setRecordFields(UuidGenerator.randomUUID(), eventContext, sysTenant);
    }

    private ContextAccess(I_DomainAccess domainAccess) {
        super(domainAccess);
    }

    public static I_ContextAccess retrieveInstance(I_DomainAccess domainAccess, UUID id) {
        ContextAccess contextAccess = new ContextAccess(domainAccess);
        // We explicitly do not fetch the participations, since we do not update them. They are replaced in case of an
        // update
        contextAccess.eventContextRecord = domainAccess.getContext().fetchOne(EVENT_CONTEXT, EVENT_CONTEXT.ID.eq(id));
        return contextAccess;
    }

    public static I_ContextAccess retrieveInstance(I_DomainAccess domainAccess, Result<?> records) {
        ContextAccess contextAccess = new ContextAccess(domainAccess);
        EventContextRecord eventContextRecord = domainAccess.getContext().newRecord(EVENT_CONTEXT);
        eventContextRecord.setStartTime((Timestamp) records.getValue(0, I_CompositionAccess.F_CONTEXT_START_TIME));
        eventContextRecord.setStartTimeTzid(
                (String) records.getValue(0, I_CompositionAccess.F_CONTEXT_START_TIME_TZID));
        eventContextRecord.setEndTime((Timestamp) records.getValue(0, I_CompositionAccess.F_CONTEXT_END_TIME));
        eventContextRecord.setEndTimeTzid((String) records.getValue(0, I_CompositionAccess.F_CONTEXT_END_TIME_TZID));
        eventContextRecord.setLocation((String) records.getValue(0, I_CompositionAccess.F_CONTEXT_LOCATION));
        eventContextRecord.setOtherContext((JSONB) records.getValue(0, I_CompositionAccess.F_CONTEXT_OTHER_CONTEXT));
        return contextAccess;
    }

    /**
     * @throws InternalServerException on failure of decoding DvText or DvDateTime
     */
    public static EventContext retrieveHistoricalEventContext(
            I_DomainAccess domainAccess, UUID compositionId, Timestamp transactionTime) {

        // use fetch any since duplicates are possible during tests...
        EventContextHistoryRecord eventContextHistoryRecord = domainAccess
                .getContext()
                .fetchAny(
                        EVENT_CONTEXT_HISTORY,
                        EVENT_CONTEXT_HISTORY
                                .COMPOSITION_ID
                                .eq(compositionId)
                                .and(EVENT_CONTEXT_HISTORY.SYS_TRANSACTION.eq(transactionTime)));

        if (eventContextHistoryRecord == null) return null; // no matching version for this composition

        // get the facility entry
        PartyIdentified healthCareFacility = null;

        if (eventContextHistoryRecord.getFacility() != null) {
            PartyIdentifiedRecord partyIdentifiedRecord = domainAccess
                    .getContext()
                    .fetchOne(PARTY_IDENTIFIED, PARTY_IDENTIFIED.ID.eq(eventContextHistoryRecord.getFacility()));
            // facility identifiers

            if (partyIdentifiedRecord != null) {
                List<DvIdentifier> identifiers = new ArrayList<>();

                domainAccess
                        .getContext()
                        .fetch(IDENTIFIER, IDENTIFIER.PARTY.eq(partyIdentifiedRecord.getId()))
                        .forEach(record -> {
                            DvIdentifier dvIdentifier = new DvIdentifier();
                            dvIdentifier.setIssuer(record.getIssuer());
                            dvIdentifier.setAssigner(record.getAssigner());
                            dvIdentifier.setId(record.getIdValue());
                            dvIdentifier.setType(record.getTypeName());
                            identifiers.add(dvIdentifier);
                        });

                // get PartyRef values from record
                healthCareFacility = getPartyIdentifiedFromRecord(partyIdentifiedRecord, identifiers);
            }
        }

        List<Participation> participationList = new ArrayList<>();
        // get the participations
        domainAccess
                .getContext()
                .fetch(
                        PARTICIPATION_HISTORY,
                        PARTICIPATION_HISTORY
                                .EVENT_CONTEXT
                                .eq(eventContextHistoryRecord.getId())
                                .and(PARTICIPATION_HISTORY.SYS_TRANSACTION.eq(transactionTime)))
                .forEach(historyRecord -> {

                    // retrieve performer
                    PartyProxy performer = new PersistedPartyProxy(domainAccess).retrieve(historyRecord.getPerformer());

                    DvInterval<DvDateTime> startTime = getStartTimeInterval(historyRecord);
                    DvCodedText mode;
                    if (historyRecord.getMode() != null) {
                        mode = (DvCodedText)
                                new RecordedDvCodedText().fromDB(historyRecord, PARTICIPATION_HISTORY.MODE);
                    } else {
                        mode = null;
                    }

                    Participation participation = new Participation(
                            performer,
                            (DvText) new RecordedDvCodedText().fromDB(historyRecord, PARTICIPATION_HISTORY.FUNCTION),
                            mode,
                            startTime);

                    participationList.add(participation);
                });

        DvCodedText setting = (DvCodedText)
                new RecordedDvCodedText().fromDB(eventContextHistoryRecord, EVENT_CONTEXT_HISTORY.SETTING);

        return new EventContext(
                healthCareFacility,
                new RecordedDvDateTime()
                        .decodeDvDateTime(
                                eventContextHistoryRecord.getStartTime(), eventContextHistoryRecord.getStartTimeTzid()),
                new RecordedDvDateTime()
                        .decodeDvDateTime(
                                eventContextHistoryRecord.getEndTime(), eventContextHistoryRecord.getEndTimeTzid()),
                participationList.isEmpty() ? null : participationList,
                eventContextHistoryRecord.getLocation(),
                setting,
                null);
    }

    private static PartyIdentified getPartyIdentifiedFromRecord(
            PartyIdentifiedRecord partyIdentifiedRecord, List<DvIdentifier> identifiers) {
        PartyIdentified healthCareFacility;
        PartyRef partyRef = null;
        if (partyIdentifiedRecord.getPartyRefValue() != null && partyIdentifiedRecord.getPartyRefScheme() != null) {
            ObjectId objectID = new PersistedObjectId().fromDB(partyIdentifiedRecord);
            partyRef = new PartyRef(
                    objectID, partyIdentifiedRecord.getPartyRefNamespace(), partyIdentifiedRecord.getPartyRefType());
        }
        healthCareFacility = new PartyIdentified(
                partyRef, partyIdentifiedRecord.getName(), identifiers.isEmpty() ? null : identifiers);
        return healthCareFacility;
    }

    /**
     * setup an EventContextRecord instance based on values from an EventContext instance
     *
     * @param id
     * @param eventContext
     */
    @Override
    public void setRecordFields(UUID id, EventContext eventContext, Short sysTenant) {
        RecordedDvDateTime recordedDvDateTime = new RecordedDvDateTime(eventContext.getStartTime());
        eventContextRecord.setStartTime(recordedDvDateTime.toTimestamp());
        eventContextRecord.setSysTenant(sysTenant);
        recordedDvDateTime.zoneId().ifPresent(eventContextRecord::setStartTimeTzid);
        if (eventContext.getEndTime() != null) {
            recordedDvDateTime = new RecordedDvDateTime(eventContext.getEndTime());
            eventContextRecord.setEndTime(recordedDvDateTime.toTimestamp());
            recordedDvDateTime.zoneId().ifPresent(eventContextRecord::setEndTimeTzid);
        }
        eventContextRecord.setId(id != null ? id : UuidGenerator.randomUUID());

        // Health care facility
        if (eventContext.getHealthCareFacility() != null) {
            UUID healthcareFacilityId =
                    new PersistedPartyProxy(this).getOrCreate(eventContext.getHealthCareFacility(), sysTenant);

            eventContextRecord.setFacility(healthcareFacilityId);
        }

        // location
        if (eventContext.getLocation() != null) eventContextRecord.setLocation(eventContext.getLocation());

        new RecordedDvCodedText().toDB(eventContextRecord, EVENT_CONTEXT.SETTING, eventContext.getSetting());

        // We always replace participations -> remove the old ones if any
        participations.clear();
        if (eventContext.getParticipations() != null) {
            for (Participation participation : eventContext.getParticipations()) {
                ParticipationRecord participationRecord = getContext().newRecord(PARTICIPATION);
                participationRecord.setEventContext(eventContextRecord.getId());
                new RecordedDvText().toDB(participationRecord, PARTICIPATION.FUNCTION, participation.getFunction());
                if (participation.getMode() != null)
                    new RecordedDvCodedText().toDB(participationRecord, PARTICIPATION.MODE, participation.getMode());
                if (participation.getTime() != null) {
                    DvDateTime lower = participation.getTime().getLower();
                    if (lower != null) {
                        recordedDvDateTime = new RecordedDvDateTime(lower);
                        participationRecord.setTimeLower(recordedDvDateTime.toTimestamp());
                        recordedDvDateTime.zoneId().ifPresent(participationRecord::setTimeLowerTz);
                    }
                    DvDateTime upper = participation.getTime().getUpper();
                    if (upper != null) {
                        recordedDvDateTime = new RecordedDvDateTime(upper);
                        participationRecord.setTimeUpper(recordedDvDateTime.toTimestamp());
                        recordedDvDateTime.zoneId().ifPresent(participationRecord::setTimeUpperTz);
                    }
                }

                PartyIdentified performer; // only PartyIdentified performer is supported now

                PartyProxy setPerformer = participation.getPerformer();

                if (!(setPerformer instanceof PartyIdentified)) {
                    log.warn("Set performer is using unsupported type: {}", setPerformer);
                    break;
                }

                performer = (PartyIdentified) setPerformer;
                UUID performerUuid = new PersistedPartyProxy(this).getOrCreate(performer, sysTenant);
                // set the performer
                participationRecord.setPerformer(performerUuid);
                participationRecord.setSysTenant(sysTenant);
                participations.add(participationRecord);
            }
        }

        // other context
        if (eventContext.getOtherContext() != null
                && CollectionUtils.isNotEmpty(eventContext.getOtherContext().getItems())) {
            // set up the JSONB field other_context
            eventContextRecord.setOtherContext(JSONB.valueOf(new RawJson().marshal(eventContext.getOtherContext())));
        }
    }

    /**
     * @throws InternalServerException  when database operation or
     * @throws IllegalArgumentException when context commit failed
     */
    @Override
    public UUID commit(Timestamp transactionTime) {
        eventContextRecord.setSysTransaction(transactionTime);
        InsertQuery<?> insertQuery = getContext().insertQuery(EVENT_CONTEXT);
        insertQuery.addValue(EVENT_CONTEXT.ID, eventContextRecord.getId());
        insertQuery.addValue(EVENT_CONTEXT.COMPOSITION_ID, eventContextRecord.getCompositionId());
        insertQuery.addValue(EVENT_CONTEXT.START_TIME, eventContextRecord.getStartTime());
        insertQuery.addValue(EVENT_CONTEXT.START_TIME_TZID, eventContextRecord.getStartTimeTzid());
        insertQuery.addValue(EVENT_CONTEXT.END_TIME, eventContextRecord.getEndTime());
        insertQuery.addValue(EVENT_CONTEXT.END_TIME_TZID, eventContextRecord.getEndTimeTzid());
        insertQuery.addValue(EVENT_CONTEXT.FACILITY, eventContextRecord.getFacility());
        insertQuery.addValue(EVENT_CONTEXT.LOCATION, eventContextRecord.getLocation());
        insertQuery.addValue(EVENT_CONTEXT.SYS_TENANT, eventContextRecord.getSysTenant());
        if (eventContextRecord.getOtherContext() != null)
            insertQuery.addValue(EVENT_CONTEXT.OTHER_CONTEXT, eventContextRecord.getOtherContext());
        insertQuery.addValue(EVENT_CONTEXT.SETTING, eventContextRecord.getSetting());
        insertQuery.addValue(EVENT_CONTEXT.SYS_TRANSACTION, eventContextRecord.getSysTransaction());

        int result;
        try {
            result = insertQuery.execute();
        } catch (DataAccessException e) {
            throw new InternalServerException("Problem executing database operation", e);
        }

        if (result < 1) throw new IllegalArgumentException("Context commit failed");

        if (!participations.isEmpty()) {
            participations.forEach(participation -> {
                participation.setEventContext(eventContextRecord.getId());
                participation.setSysTransaction(transactionTime);
                participation.store();
            });
        }

        return eventContextRecord.getId();
    }

    /**
     * @throws InternalServerException  when database operation or
     * @throws IllegalArgumentException when context commit failed
     */
    @Override
    public UUID commit() {
        return commit(TransactionTime.millis());
    }

    /**
     * @throws InternalServerException if DB inconsistency or other problem with updating DB entry
     */
    @Override
    public Boolean update(Timestamp transactionTime) {
        // updateComposition participations
        // We replace participations, instead of updating them, because we have no concrete criteria which
        // participations should be updated
        getContext()
                .deleteFrom(PARTICIPATION)
                .where(PARTICIPATION.EVENT_CONTEXT.eq(getId()))
                .execute();
        for (ParticipationRecord participationRecord : participations) {
            participationRecord.setSysTransaction(transactionTime);
            // check if commit or updateComposition (exists or not...)
            try {
                participationRecord.setId(UuidGenerator.randomUUID());
                participationRecord.store();
            } catch (DataAccessException e) { // generalize DB exceptions
                throw new InternalServerException(DB_INCONSISTENCY, e);
            }
        }
        // ignore the temporal field since it is maintained by an external trigger!
        eventContextRecord.changed(EVENT_CONTEXT.SYS_PERIOD, false);

        eventContextRecord.setSysTransaction(transactionTime);

        UpdateQuery<?> updateQuery = getContext().updateQuery(EVENT_CONTEXT);

        updateQuery.addValue(EVENT_CONTEXT.COMPOSITION_ID, eventContextRecord.getCompositionId());
        updateQuery.addValue(EVENT_CONTEXT.START_TIME, eventContextRecord.getStartTime());
        updateQuery.addValue(EVENT_CONTEXT.START_TIME_TZID, eventContextRecord.getStartTimeTzid());
        updateQuery.addValue(EVENT_CONTEXT.END_TIME, eventContextRecord.getEndTime());
        updateQuery.addValue(EVENT_CONTEXT.END_TIME_TZID, eventContextRecord.getEndTimeTzid());
        updateQuery.addValue(EVENT_CONTEXT.FACILITY, eventContextRecord.getFacility());
        updateQuery.addValue(EVENT_CONTEXT.LOCATION, eventContextRecord.getLocation());
        if (eventContextRecord.getOtherContext() != null)
            updateQuery.addValue(EVENT_CONTEXT.OTHER_CONTEXT, eventContextRecord.getOtherContext());
        updateQuery.addValue(EVENT_CONTEXT.SETTING, eventContextRecord.getSetting());
        updateQuery.addValue(EVENT_CONTEXT.SYS_TRANSACTION, eventContextRecord.getSysTransaction());
        updateQuery.addConditions(EVENT_CONTEXT.ID.eq(getId()));

        boolean result;
        try {
            result = updateQuery.execute() > 0;
        } catch (DataAccessException e) { // generalize DB exceptions
            throw new InternalServerException("Problem when updating DB entry", e);
        }

        return result;
    }

    /**
     * @throws InternalServerException when update failed
     */
    @Override
    public Boolean update(Timestamp transactionTime, boolean force) {
        if (force) {
            eventContextRecord.changed(true);
            // jOOQ limited support of TSTZRANGE, exclude sys_period from updateComposition!
            eventContextRecord.changed(EVENT_CONTEXT.SYS_PERIOD, false);

            for (ParticipationRecord participationRecord : participations) {
                participationRecord.changed(true);
                // jOOQ limited support of TSTZRANGE, exclude sys_period from updateComposition!
                participationRecord.changed(PARTICIPATION.SYS_PERIOD, false);
            }
        }
        return update(transactionTime);
    }

    /**
     * @throws InternalServerException because inherited interface function isn't implemented in this class
     * @deprecated
     */
    @Deprecated
    @Override
    public Boolean update() {
        throw new InternalServerException(
                "INTERNAL: Invalid updateComposition call to updateComposition without Transaction time and/or force flag arguments");
    }

    /**
     * @throws InternalServerException because inherited interface function isn't implemented in this class
     * @deprecated
     */
    @Deprecated
    @Override
    public Boolean update(Boolean force) {
        throw new InternalServerException(
                "INTERNAL: Invalid updateComposition call to updateComposition without Transaction time and/or force flag arguments");
    }

    @Override
    public Integer delete() {
        int count = 0;
        // delete any cross reference participants if any
        // delete the participation record
        count += getContext()
                .delete(PARTICIPATION)
                .where(PARTICIPATION.EVENT_CONTEXT.eq(eventContextRecord.getId()))
                .execute();

        count += eventContextRecord.delete();
        return count;
    }

    /**
     * @throws InternalServerException on failure of decoding DvText or DvDateTime
     */
    @Override
    public EventContext mapRmEventContext() {

        // get the facility entry
        PartyIdentifiedRecord partyIdentifiedRecord =
                getContext().fetchOne(PARTY_IDENTIFIED, PARTY_IDENTIFIED.ID.eq(eventContextRecord.getFacility()));
        // facility identifiers
        PartyIdentified healthCareFacility = null;

        if (partyIdentifiedRecord != null) {
            List<DvIdentifier> identifiers = new ArrayList<>();

            getContext()
                    .fetch(IDENTIFIER, IDENTIFIER.PARTY.eq(partyIdentifiedRecord.getId()))
                    .forEach(record -> {
                        DvIdentifier dvIdentifier = new DvIdentifier();
                        dvIdentifier.setIssuer(record.getIssuer());
                        dvIdentifier.setAssigner(record.getAssigner());
                        dvIdentifier.setId(record.getIdValue());
                        dvIdentifier.setType(record.getTypeName());
                        identifiers.add(dvIdentifier);
                    });

            // get PartyRef values from record
            healthCareFacility = getPartyIdentifiedFromRecord(partyIdentifiedRecord, identifiers);
        }

        List<Participation> participationList = new ArrayList<>();
        // get the participations
        getContext()
                .fetch(PARTICIPATION, PARTICIPATION.EVENT_CONTEXT.eq(eventContextRecord.getId()))
                .forEach(participationRecord -> {
                    // retrieve performer
                    PartyProxy performer = new PersistedPartyProxy(this).retrieve(participationRecord.getPerformer());

                    DvInterval<DvDateTime> startTime = getStartTimeInterval(participationRecord);
                    DvCodedText mode;
                    if (participationRecord.getMode() != null) {
                        mode = (DvCodedText) new RecordedDvCodedText().fromDB(participationRecord, PARTICIPATION.MODE);
                    } else {
                        mode = null;
                    }

                    Participation participation = new Participation(
                            performer,
                            (DvText) new RecordedDvCodedText().fromDB(participationRecord, PARTICIPATION.FUNCTION),
                            mode,
                            startTime);

                    participationList.add(participation);
                });

        DvCodedText concept = (DvCodedText) new RecordedDvCodedText().fromDB(eventContextRecord, EVENT_CONTEXT.SETTING);

        ItemStructure otherContext = null;

        if (eventContextRecord.getOtherContext() != null) {
            otherContext = new RawJson()
                    .unmarshal((eventContextRecord.getOtherContext().data()), ItemStructure.class);
        }

        return new EventContext(
                healthCareFacility,
                new RecordedDvDateTime()
                        .decodeDvDateTime(eventContextRecord.getStartTime(), eventContextRecord.getStartTimeTzid()),
                new RecordedDvDateTime()
                        .decodeDvDateTime(eventContextRecord.getEndTime(), eventContextRecord.getEndTimeTzid()),
                participationList.isEmpty() ? null : participationList,
                eventContextRecord.getLocation(),
                concept,
                otherContext);
    }

    private static DvInterval<DvDateTime> getStartTimeInterval(ParticipationHistoryRecord historyRecord) {
        if (historyRecord.getTimeLower() != null) { // start time null value is allowed for participation
            return new DvInterval<>(
                    new RecordedDvDateTime()
                            .decodeDvDateTime(historyRecord.getTimeLower(), historyRecord.getTimeLowerTz()),
                    new RecordedDvDateTime()
                            .decodeDvDateTime(historyRecord.getTimeUpper(), historyRecord.getTimeUpperTz()));
        } else {
            return null;
        }
    }

    private static DvInterval<DvDateTime> getStartTimeInterval(ParticipationRecord participationRecord) {
        if (participationRecord.getTimeLower() != null) {
            // start time null value is allowed for participation
            return new DvInterval<>(
                    new RecordedDvDateTime()
                            .decodeDvDateTime(participationRecord.getTimeLower(), participationRecord.getTimeLowerTz()),
                    new RecordedDvDateTime()
                            .decodeDvDateTime(
                                    participationRecord.getTimeUpper(), participationRecord.getTimeUpperTz()));
        } else {
            return null;
        }
    }

    @Override
    public String getOtherContextJson() {
        if (eventContextRecord.getOtherContext() == null) return null;
        return (eventContextRecord.getOtherContext().data());
    }

    @Override
    public void setCompositionId(UUID compositionId) {
        eventContextRecord.setCompositionId(compositionId);
    }

    @Override
    public UUID getId() {
        return eventContextRecord.getId();
    }

    @Override
    public boolean isVoid() {
        return eventContextRecord == null;
    }

    @Override
    public DataAccess getDataAccess() {
        return this;
    }
}
