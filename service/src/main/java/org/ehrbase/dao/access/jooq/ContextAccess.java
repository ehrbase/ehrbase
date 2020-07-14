/*
 * Modifications copyright (C) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School,
 * Jake Smolka (Hannover Medical School), Luis Marco-Ruiz (Hannover Medical School)..

 * This file is part of Project EHRbase

 * Copyright (c) 2015 Christian Chevalley
 * This file is part of Project Ethercis
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.dao.access.jooq;

import com.nedap.archie.rm.composition.EventContext;
import com.nedap.archie.rm.datastructures.ItemStructure;
import com.nedap.archie.rm.datatypes.CodePhrase;
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
import com.nedap.archie.rm.support.identification.TerminologyId;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ehrbase.api.definitions.ServerConfig;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.dao.access.interfaces.I_CompositionAccess;
import org.ehrbase.dao.access.interfaces.I_ContextAccess;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.dao.access.jooq.party.PersistedObjectId;
import org.ehrbase.dao.access.jooq.party.PersistedPartyProxy;
import org.ehrbase.dao.access.support.DataAccess;
import org.ehrbase.jooq.pg.tables.records.*;
import org.ehrbase.service.RecordedDvCodedText;
import org.ehrbase.service.RecordedDvDateTime;
import org.ehrbase.serialisation.dbencoding.RawJson;
import org.jooq.*;
import org.jooq.exception.DataAccessException;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.ehrbase.jooq.pg.Tables.*;

/**
 * Created by Christian Chevalley on 4/9/2015.
 */
public class ContextAccess extends DataAccess implements I_ContextAccess {

    private static final TerminologyId OPENEHR_TERMINOLOGY_ID = new TerminologyId("openehr");
    private static final String DB_INCONSISTENCY = "DB inconsistency";
    private static Logger log = LogManager.getLogger(ContextAccess.class);
    private EventContextRecord eventContextRecord;
    private PreparedStatement updateStatement;
    private List<ParticipationRecord> participations = new ArrayList<>();

    public ContextAccess(DSLContext context, ServerConfig serverConfig, EventContext eventContext) {
        super(context, null, null, serverConfig);
        eventContextRecord = context.newRecord(EVENT_CONTEXT);
        setRecordFields(UUID.randomUUID(), eventContext);
    }

    private ContextAccess(I_DomainAccess domainAccess) {
        super(domainAccess);
    }

    public static I_ContextAccess retrieveInstance(I_DomainAccess domainAccess, UUID id) {
        ContextAccess contextAccess = new ContextAccess(domainAccess);
        contextAccess.eventContextRecord = domainAccess.getContext().fetchOne(EVENT_CONTEXT, EVENT_CONTEXT.ID.eq(id));
        return contextAccess;
    }

    public static I_ContextAccess retrieveInstance(I_DomainAccess domainAccess, Result<?> records) {
        ContextAccess contextAccess = new ContextAccess(domainAccess);
        EventContextRecord eventContextRecord = domainAccess.getContext().newRecord(EVENT_CONTEXT);
        eventContextRecord.setStartTime((Timestamp) records.getValue(0, I_CompositionAccess.F_CONTEXT_START_TIME));
        eventContextRecord.setStartTimeTzid((String) records.getValue(0, I_CompositionAccess.F_CONTEXT_START_TIME_TZID));
        eventContextRecord.setEndTime((Timestamp) records.getValue(0, I_CompositionAccess.F_CONTEXT_END_TIME));
        eventContextRecord.setEndTimeTzid((String) records.getValue(0, I_CompositionAccess.F_CONTEXT_END_TIME_TZID));
        eventContextRecord.setLocation((String) records.getValue(0, I_CompositionAccess.F_CONTEXT_LOCATION));
        eventContextRecord.setOtherContext((JSONB) records.getValue(0, I_CompositionAccess.F_CONTEXT_OTHER_CONTEXT));

        return contextAccess;
    }

    /**
     * Decodes and creates RM object instance from given String representation
     *
     * @param codedDvCodedText input as String
     * @return RM object generated from input
     * @throws IllegalArgumentException when failed to parse the input
     */
    // TODO unit test
    private static DvCodedText decodeDvCodedText(String codedDvCodedText) {
        String[] tokens = codedDvCodedText.substring(codedDvCodedText.indexOf("{") + 1, codedDvCodedText.indexOf("}")).split(",");
        if (tokens.length != 3) {
            throw new IllegalArgumentException("failed to parse DvCodedText \'" + codedDvCodedText + "\', wrong number of tokens.");
        } else {
            String textValue = tokens[2].split("=")[1];
            String codeTerminology = tokens[1].split("=")[1];
            String codeString = tokens[0].split("=")[1];
            return new DvCodedText(textValue, new CodePhrase(new TerminologyId(codeTerminology), codeString));
        }
    }

    /**
     * @throws InternalServerException on failure of decoding DvText or DvDateTime
     */
    public static EventContext retrieveHistoricalEventContext(I_DomainAccess domainAccess, UUID compositionId, Timestamp transactionTime) {

        //use fetch any since duplicates are possible during tests...
        EventContextHistoryRecord eventContextHistoryRecord = domainAccess.getContext()
                .fetchAny(EVENT_CONTEXT_HISTORY, EVENT_CONTEXT_HISTORY.COMPOSITION_ID.eq(compositionId)
                        .and(EVENT_CONTEXT_HISTORY.SYS_TRANSACTION.eq(transactionTime)));

        if (eventContextHistoryRecord == null) return null; //no matching version for this composition

        //get the facility entry
        PartyIdentified healthCareFacility = null;

        if (eventContextHistoryRecord.getFacility() != null) {
            PartyIdentifiedRecord partyIdentifiedRecord = domainAccess.getContext()
                    .fetchOne(PARTY_IDENTIFIED, PARTY_IDENTIFIED.ID.eq(eventContextHistoryRecord.getFacility()));
            //facility identifiers

            if (partyIdentifiedRecord != null) {
                List<DvIdentifier> identifiers = new ArrayList<>();

                domainAccess.getContext().fetch(IDENTIFIER, IDENTIFIER.PARTY.eq(partyIdentifiedRecord.getId())).forEach(record -> {
                    DvIdentifier dvIdentifier = new DvIdentifier();
                    dvIdentifier.setIssuer(record.getIssuer());
                    dvIdentifier.setAssigner(record.getAssigner());
                    dvIdentifier.setId(record.getIdValue());
                    dvIdentifier.setType(record.getTypeName());
                    identifiers.add(dvIdentifier);
                });

                //get PartyRef values from record
                healthCareFacility = getPartyIdentifiedFromRecord(partyIdentifiedRecord, identifiers);
            }
        }

        List<Participation> participationList = new ArrayList<>();
        //get the participations
        domainAccess.getContext().fetch(PARTICIPATION_HISTORY,
                PARTICIPATION_HISTORY.EVENT_CONTEXT.eq(eventContextHistoryRecord.getId())
                        .and(PARTICIPATION_HISTORY.SYS_TRANSACTION.eq(transactionTime)))
                .forEach(record -> {
                    //retrieve performer
                    PartyProxy performer = new PersistedPartyProxy(domainAccess).retrieve(record.getPerformer());


                    DvInterval<DvDateTime> startTime = convertDvIntervalDvDateTimeFromRecord(eventContextHistoryRecord);
                    DvCodedText mode = convertModeFromRecord(eventContextHistoryRecord);

                    Participation participation = new Participation(performer,
                            new DvText(record.getFunction()),
                            mode,
                            startTime);

                    participationList.add(participation);
                });

        DvCodedText concept;

        //retrieve the setting
        UUID settingUuid = eventContextHistoryRecord.getSetting();

        ConceptRecord conceptRecord = domainAccess.getContext().fetchOne(CONCEPT, CONCEPT.ID.eq(settingUuid).and(CONCEPT.LANGUAGE.eq("en")));

        if (conceptRecord != null) {
            concept = new DvCodedText(conceptRecord.getDescription(), new CodePhrase(OPENEHR_TERMINOLOGY_ID, conceptRecord.getConceptid().toString()));
        } else {
            concept = new DvCodedText("event", new CodePhrase(OPENEHR_TERMINOLOGY_ID, "433"));
        }

        return new EventContext(healthCareFacility,
                new RecordedDvDateTime().decodeDvDateTime(eventContextHistoryRecord.getStartTime(), eventContextHistoryRecord.getStartTimeTzid()),
                new RecordedDvDateTime().decodeDvDateTime(eventContextHistoryRecord.getEndTime(), eventContextHistoryRecord.getEndTimeTzid()),
                participationList.isEmpty() ? null : participationList,
                eventContextHistoryRecord.getLocation(),
                concept,
                null);

    }

    private static PartyIdentified getPartyIdentifiedFromRecord(PartyIdentifiedRecord partyIdentifiedRecord, List<DvIdentifier> identifiers) {
        PartyIdentified healthCareFacility;
        PartyRef partyRef = null;
        if (partyIdentifiedRecord.getPartyRefValue() != null && partyIdentifiedRecord.getPartyRefScheme() != null) {
            ObjectId objectID = new PersistedObjectId().fromDB(partyIdentifiedRecord);
            partyRef = new PartyRef(objectID, partyIdentifiedRecord.getPartyRefNamespace(), partyIdentifiedRecord.getPartyRefType());
        }
        healthCareFacility = new PartyIdentified(partyRef, partyIdentifiedRecord.getName(), identifiers.isEmpty() ? null : identifiers);
        return healthCareFacility;
    }

    // TODO: doc!
    @Override
    public void setRecordFields(UUID id, EventContext eventContext) {
        //@TODO get from eventContext
        RecordedDvDateTime recordedDvDateTime = new RecordedDvDateTime(eventContext.getStartTime());
        eventContextRecord.setStartTime(recordedDvDateTime.toTimestamp());
        eventContextRecord.setStartTimeTzid(recordedDvDateTime.zoneId());
        if (eventContext.getEndTime() != null) {
            recordedDvDateTime = new RecordedDvDateTime(eventContext.getEndTime());
            eventContextRecord.setEndTime(recordedDvDateTime.toTimestamp());
            eventContextRecord.setEndTimeTzid(recordedDvDateTime.zoneId());
        }
        eventContextRecord.setId(id != null ? id : UUID.randomUUID());

        //Health care facility
        if (eventContext.getHealthCareFacility() != null) {
            UUID healthcareFacilityId = new PersistedPartyProxy(this).getOrCreate(eventContext.getHealthCareFacility());

            eventContextRecord.setFacility(healthcareFacilityId);
        }

        //location
        if (eventContext.getLocation() != null)
            eventContextRecord.setLocation(eventContext.getLocation());

        //TODO: retrieveInstanceByNamedSubject program details from other context if any
//        setting = eventContext.getSetting().getCode();
        Integer settingCode;
        try {
            settingCode = Integer.parseInt(eventContext.getSetting().getDefiningCode().getCodeString());
            // when not throwing exception continue with
            eventContextRecord.setSetting(ConceptAccess.fetchConceptUUID(this, settingCode, "en"));
        } catch (NumberFormatException e) {
            // do nothing   //TODO: is treating it as optional correct? or should it be a real error case?
        }

        if (eventContext.getParticipations() != null) {
            for (Participation participation : eventContext.getParticipations()) {
                ParticipationRecord participationRecord = getContext().newRecord(PARTICIPATION);
                participationRecord.setEventContext(eventContextRecord.getId());
                participationRecord.setFunction(participation.getFunction().getValue());
                if (participation.getMode() != null)
                    new RecordedDvCodedText().toDB(participationRecord, PARTICIPATION.MODE, participation.getMode());
                if (participation.getTime() != null) {
                    DvDateTime lower = (DvDateTime) participation.getTime().getLower();
                    if (lower != null) {
                        recordedDvDateTime = new RecordedDvDateTime(lower);
                        participationRecord.setTimeLower(recordedDvDateTime.toTimestamp());
                        participationRecord.setTimeLowerTz(recordedDvDateTime.zoneId());
                    }
                    DvDateTime upper = (DvDateTime) participation.getTime().getUpper();
                    if (upper != null) {
                        recordedDvDateTime = new RecordedDvDateTime(upper);
                        participationRecord.setTimeUpper(recordedDvDateTime.toTimestamp());
                        participationRecord.setTimeUpperTz(recordedDvDateTime.zoneId());
                    }

                }

                PartyIdentified performer; //only PartyIdentified performer is supported now

                PartyProxy setPerformer = participation.getPerformer();

                if (!(setPerformer instanceof PartyIdentified)) {
                    log.warn("Set performer is using unsupported type:" + setPerformer.toString());
                    break;
                }

                performer = (PartyIdentified) setPerformer;
                UUID performerUuid = new PersistedPartyProxy(this).getOrCreate(performer);
                //set the performer
                participationRecord.setPerformer(performerUuid);
                participations.add(participationRecord);
            }
        }

        //other context
        if (eventContext.getOtherContext() != null && CollectionUtils.isNotEmpty(eventContext.getOtherContext().getItems())) {
            //set up the JSONB field other_context
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
//        UUID uuid = UUID.randomUUID();
        InsertQuery<?> insertQuery = getContext().insertQuery(EVENT_CONTEXT);
        insertQuery.addValue(EVENT_CONTEXT.ID, eventContextRecord.getId());
        insertQuery.addValue(EVENT_CONTEXT.COMPOSITION_ID, eventContextRecord.getCompositionId());
        insertQuery.addValue(EVENT_CONTEXT.START_TIME, eventContextRecord.getStartTime());
        insertQuery.addValue(EVENT_CONTEXT.START_TIME_TZID, eventContextRecord.getStartTimeTzid());
        insertQuery.addValue(EVENT_CONTEXT.END_TIME, eventContextRecord.getEndTime());
        insertQuery.addValue(EVENT_CONTEXT.END_TIME_TZID, eventContextRecord.getEndTimeTzid());
        insertQuery.addValue(EVENT_CONTEXT.FACILITY, eventContextRecord.getFacility());
        insertQuery.addValue(EVENT_CONTEXT.LOCATION, eventContextRecord.getLocation());
//        Field jsonbOtherContext = DSL.field(EVENT_CONTEXT.OTHER_CONTEXT+"::jsonb");
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

        if (result < 1) // TODO check result for successful execution -> is '< 1' correct as condition?
            throw new IllegalArgumentException("Context commit failed");

//        eventContextRecord.store();

        if (!participations.isEmpty()) {
            participations.forEach(participation -> {
                        participation.setEventContext(eventContextRecord.getId());
                        participation.setSysTransaction(transactionTime);
                        participation.store();
                    }
            );
        }

        return eventContextRecord.getId();
    }

    /**
     * @throws InternalServerException  when database operation or
     * @throws IllegalArgumentException when context commit failed
     */
    @Override
    public UUID commit() {
        return commit(Timestamp.valueOf(LocalDateTime.now()));
    }

    /**
     * @throws InternalServerException if DB inconsistency or other problem with updating DB entry
     */
    @Override
    public Boolean update(Timestamp transactionTime) {
        //updateComposition participations
        for (ParticipationRecord participationRecord : participations) {
            participationRecord.setSysTransaction(transactionTime);
            if (participationRecord.changed()) {
                //check if commit or updateComposition (exists or not...)
                try {
                    if (getContext().fetchExists(PARTICIPATION, PARTICIPATION.ID.eq(participationRecord.getId()))) {
                        participationRecord.update();
                    } else {
                        participationRecord.setId(UUID.randomUUID());
                        participationRecord.store();
                    }
                } catch (DataAccessException e) {   // generalize DB exceptions
                    throw new InternalServerException(DB_INCONSISTENCY, e);
                }
            }
        }
        //ignore the temporal field since it is maintained by an external trigger!
        eventContextRecord.changed(EVENT_CONTEXT.SYS_PERIOD, false);

        //TODO: still correct? original comment: "ignore other_context for the time being..."
//        eventContextRecord.changed(EVENT_CONTEXT.OTHER_CONTEXT, false);
        eventContextRecord.setSysTransaction(transactionTime);

        UpdateQuery<?> updateQuery = getContext().updateQuery(EVENT_CONTEXT);

        updateQuery.addValue(EVENT_CONTEXT.COMPOSITION_ID, eventContextRecord.getCompositionId());
        updateQuery.addValue(EVENT_CONTEXT.START_TIME, eventContextRecord.getStartTime());
        updateQuery.addValue(EVENT_CONTEXT.START_TIME_TZID, eventContextRecord.getStartTimeTzid());
        updateQuery.addValue(EVENT_CONTEXT.END_TIME, eventContextRecord.getEndTime());
        updateQuery.addValue(EVENT_CONTEXT.END_TIME_TZID, eventContextRecord.getEndTimeTzid());
        updateQuery.addValue(EVENT_CONTEXT.FACILITY, eventContextRecord.getFacility());
        updateQuery.addValue(EVENT_CONTEXT.LOCATION, eventContextRecord.getLocation());
//        Field jsonbOtherContext = DSL.field(EVENT_CONTEXT.OTHER_CONTEXT+"::jsonb");
        if (eventContextRecord.getOtherContext() != null)
            updateQuery.addValue(EVENT_CONTEXT.OTHER_CONTEXT, eventContextRecord.getOtherContext());
        updateQuery.addValue(EVENT_CONTEXT.SETTING, eventContextRecord.getSetting());
        updateQuery.addValue(EVENT_CONTEXT.SYS_TRANSACTION, eventContextRecord.getSysTransaction());
        updateQuery.addConditions(EVENT_CONTEXT.ID.eq(getId()));

        Boolean result;
        try {
            result = updateQuery.execute() > 0;
        } catch (DataAccessException e) {   // generalize DB exceptions
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
            //jOOQ limited support of TSTZRANGE, exclude sys_period from updateComposition!
            eventContextRecord.changed(EVENT_CONTEXT.SYS_PERIOD, false);

            for (ParticipationRecord participationRecord : participations) {
                participationRecord.changed(true);
                //jOOQ limited support of TSTZRANGE, exclude sys_period from updateComposition!
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
        throw new InternalServerException("INTERNAL: Invalid updateComposition call to updateComposition without Transaction time and/or force flag arguments");
    }

    /**
     * @throws InternalServerException because inherited interface function isn't implemented in this class
     * @deprecated
     */
    @Deprecated
    @Override
    public Boolean update(Boolean force) {
        throw new InternalServerException("INTERNAL: Invalid updateComposition call to updateComposition without Transaction time and/or force flag arguments");
    }

    @Override
    public Integer delete() {
        int count = 0;
        //delete any cross reference participants if any
        //delete the participation record
        count += getContext().delete(PARTICIPATION).where(PARTICIPATION.EVENT_CONTEXT.eq(eventContextRecord.getId())).execute();

        count += eventContextRecord.delete();
        return count;
    }

    /**
     * @throws InternalServerException on failure of decoding DvText or DvDateTime
     */
    @Override
    public EventContext mapRmEventContext() {

        //get the facility entry
        PartyIdentifiedRecord partyIdentifiedRecord = getContext().fetchOne(PARTY_IDENTIFIED, PARTY_IDENTIFIED.ID.eq(eventContextRecord.getFacility()));
        //facility identifiers
        PartyIdentified healthCareFacility = null;

        if (partyIdentifiedRecord != null) {
            List<DvIdentifier> identifiers = new ArrayList<>();

            getContext().fetch(IDENTIFIER, IDENTIFIER.PARTY.eq(partyIdentifiedRecord.getId())).forEach(record -> {
                DvIdentifier dvIdentifier = new DvIdentifier();
                dvIdentifier.setIssuer(record.getIssuer());
                dvIdentifier.setAssigner(record.getAssigner());
                dvIdentifier.setId(record.getIdValue());
                dvIdentifier.setType(record.getTypeName());
                identifiers.add(dvIdentifier);
            });

            //get PartyRef values from record
            healthCareFacility = getPartyIdentifiedFromRecord(partyIdentifiedRecord, identifiers);
        }

        List<Participation> participationList = new ArrayList<>();
        //get the participations
        getContext().fetch(PARTICIPATION, PARTICIPATION.EVENT_CONTEXT.eq(eventContextRecord.getId())).forEach(record -> {
            //retrieve performer
            PartyProxy performer = new PersistedPartyProxy(this).retrieve(record.getPerformer());

            DvInterval<DvDateTime> dvInterval = convertDvIntervalDvDateTimeFromRecord(record);

            DvCodedText mode = convertModeFromRecord(record);

            Participation participation = new Participation(performer,
                    new DvText(record.getFunction()),
                    mode,
                    dvInterval);

            participationList.add(participation);
        });

        DvCodedText concept;

        //retrieve the setting
        UUID settingUuid = eventContextRecord.getSetting();

        ConceptRecord conceptRecord = getContext().fetchOne(CONCEPT, CONCEPT.ID.eq(settingUuid).and(CONCEPT.LANGUAGE.eq("en")));

        if (conceptRecord != null) {
            concept = new DvCodedText(conceptRecord.getDescription(), new CodePhrase(OPENEHR_TERMINOLOGY_ID, conceptRecord.getConceptid().toString()));
        } else {
            concept = new DvCodedText("event", new CodePhrase(OPENEHR_TERMINOLOGY_ID, "433"));
        }
        ItemStructure otherContext = null;

        if (eventContextRecord.getOtherContext() != null) {
            otherContext = new RawJson().unmarshal((eventContextRecord.getOtherContext().data()), ItemStructure.class);
        }

        return new EventContext(healthCareFacility,
                new RecordedDvDateTime().decodeDvDateTime(eventContextRecord.getStartTime(), eventContextRecord.getStartTimeTzid()),
                new RecordedDvDateTime().decodeDvDateTime(eventContextRecord.getEndTime(), eventContextRecord.getEndTimeTzid()),
                participationList.isEmpty() ? null : participationList,
                eventContextRecord.getLocation(),
                concept,
                otherContext
        );

    }

    private static DvInterval<DvDateTime> convertDvIntervalDvDateTimeFromRecord(Record record){
        DvInterval<DvDateTime> dvDateTimeDvInterval = null;
        if (record.get(PARTICIPATION.TIME_LOWER) != null) { //start time null value is allowed for participation
            dvDateTimeDvInterval = new DvInterval<>(
                    new RecordedDvDateTime().decodeDvDateTime(record.get(PARTICIPATION.TIME_LOWER), record.get(PARTICIPATION.TIME_LOWER_TZ)),
                    new RecordedDvDateTime().decodeDvDateTime(record.get(PARTICIPATION.TIME_UPPER), record.get(PARTICIPATION.TIME_UPPER_TZ))
            );
        }
        return dvDateTimeDvInterval;
    }

    private static DvCodedText convertModeFromRecord(Record record){
        DvCodedText mode;
        try {
            if (record.get(PARTICIPATION.MODE) != null) {
                mode = (DvCodedText)new RecordedDvCodedText().fromDB(record, PARTICIPATION.MODE);
            } else {
                mode = null;
            }
        } catch (IllegalArgumentException e) {
            throw new InternalServerException(DB_INCONSISTENCY, e);
        }
        return mode;
    }

    @Override
    public String getOtherContextJson() {
        if (eventContextRecord.getOtherContext() == null)
            return null;
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
    public DataAccess getDataAccess() {
        return this;
    }
}
