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
import com.nedap.archie.rm.support.identification.*;
import org.apache.catalina.Server;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ehrbase.api.definitions.ServerConfig;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.dao.access.interfaces.I_CompositionAccess;
import org.ehrbase.dao.access.interfaces.I_ContextAccess;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.dao.access.interfaces.I_PartyIdentifiedAccess;
import org.ehrbase.dao.access.support.DataAccess;
import org.ehrbase.jooq.pg.tables.records.*;
import org.ehrbase.serialisation.RawJson;
import org.jooq.DSLContext;
import org.jooq.InsertQuery;
import org.jooq.Result;
import org.jooq.UpdateQuery;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.postgresql.util.PGobject;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.ehrbase.jooq.pg.Tables.*;

/**
 * Created by Christian Chevalley on 4/9/2015.
 */
public class ContextAccess extends DataAccess implements I_ContextAccess {

    public static final TerminologyId OPENEHR_TERMINOLOGY_ID = new TerminologyId("openehr");
    final static String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZZ";
    public static final String DB_INCONSISTENCY = "DB inconsistency";
    private static Logger log = LogManager.getLogger(ContextAccess.class);
    private EventContextRecord eventContextRecord;
    private PreparedStatement updateStatement;
    private List<ParticipationRecord> participations = new ArrayList<>();

    public ContextAccess(DSLContext context, ServerConfig serverConfig, EventContext eventContext) {
        super(context, null, null, serverConfig);
        eventContextRecord = context.newRecord(EVENT_CONTEXT);
        setRecordFields(UUID.randomUUID(), eventContext);
    }

    public ContextAccess(I_DomainAccess domainAccess) {
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
        eventContextRecord.setOtherContext(records.getValue(0, I_CompositionAccess.F_CONTEXT_OTHER_CONTEXT));

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
     * Decodes and creates RM object instance from given {@link Timestamp} representation
     *
     * @param timestamp input as {@link Timestamp}
     * @param timezone  TODO doc! what format is the timezone in?
     * @return RM object generated from input
     * @throws InternalServerException on failure
     */
    // TODO unit test - until this is done please note this method was refactored from joda to java time classes
    private static DvDateTime decodeDvDateTime(Timestamp timestamp, String timezone) {
        if (timestamp == null) return null;

        Optional<LocalDateTime> codedLocalDateTime = Optional.empty();
        Optional<ZonedDateTime> zonedDateTime = Optional.empty();

        if (timezone != null)
            zonedDateTime = Optional.of(timestamp.toLocalDateTime().atZone(ZoneId.of(timezone)));
        else
            codedLocalDateTime = Optional.of(timestamp.toLocalDateTime());

        Optional<String> convertedDateTime = codedLocalDateTime.map(i -> i.format(java.time.format.DateTimeFormatter.ofPattern(DATE_FORMAT)));
        if (!convertedDateTime.isPresent())
            convertedDateTime = zonedDateTime.map(i -> i.format(java.time.format.DateTimeFormatter.ofPattern(DATE_FORMAT)));

        return new DvDateTime(convertedDateTime.orElseThrow(() -> new InternalServerException("Decoding DvDateTime failed")));
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
                    PartyProxy performer = I_PartyIdentifiedAccess.retrievePartyIdentified(domainAccess, record.getPerformer());


                    DvInterval<DvDateTime> startTime = new DvInterval<>(decodeDvDateTime(record.getStartTime(), record.getStartTimeTzid()), null);
                    DvCodedText mode = null;
                    try {
                        mode = decodeDvCodedText(record.getMode());
                    } catch (IllegalArgumentException e) {
                        throw new InternalServerException(DB_INCONSISTENCY, e);
                    }
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
                decodeDvDateTime(eventContextHistoryRecord.getStartTime(), eventContextHistoryRecord.getStartTimeTzid()),
                decodeDvDateTime(eventContextHistoryRecord.getEndTime(), eventContextHistoryRecord.getEndTimeTzid()),
                participationList.isEmpty() ? null : participationList,
                eventContextHistoryRecord.getLocation(),
                concept,
                null);

    }

    private static PartyIdentified getPartyIdentifiedFromRecord(PartyIdentifiedRecord partyIdentifiedRecord, List<DvIdentifier> identifiers) {
        PartyIdentified healthCareFacility;
        PartyRef partyRef;
        if (partyIdentifiedRecord.getPartyRefValue() != null && partyIdentifiedRecord.getPartyRefScheme() != null) {
            GenericId genericID = new GenericId(partyIdentifiedRecord.getPartyRefValue(), partyIdentifiedRecord.getPartyRefScheme());
            partyRef = new PartyRef(genericID, partyIdentifiedRecord.getPartyRefNamespace(), partyIdentifiedRecord.getPartyRefType());
        } else {
            ObjectId objectID = new HierObjectId("ref");
            partyRef = new PartyRef(objectID, partyIdentifiedRecord.getPartyRefNamespace(), partyIdentifiedRecord.getPartyRefType());
        }
        healthCareFacility = new PartyIdentified(partyRef, partyIdentifiedRecord.getName(), identifiers.isEmpty() ? null : identifiers);
        return healthCareFacility;
    }

    // TODO: doc!
    private void setRecordFields(UUID id, EventContext eventContext) {
        //@TODO get from eventContext
        eventContextRecord.setStartTimeTzid(ZoneId.systemDefault().getId());
        eventContextRecord.setStartTime(new Timestamp(eventContext.getStartTime().getValue().get(ChronoField.MILLI_OF_SECOND)));
        if (eventContext.getEndTime() != null) {
            eventContextRecord.setEndTime(new Timestamp(eventContext.getEndTime().getValue().get(ChronoField.MILLI_OF_SECOND)));
            eventContextRecord.setEndTimeTzid(ZoneId.systemDefault().getId());
        }
        eventContextRecord.setId(id != null ? id : UUID.randomUUID());

        //Health care facility
        if (eventContext.getHealthCareFacility() != null) {
            UUID healthcareFacilityId = I_PartyIdentifiedAccess.getOrCreateParty(this, eventContext.getHealthCareFacility());

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
                participationRecord.setMode(participation.getMode().toString());
                if (participation.getTime() != null) {
                    DvDateTime lower = (DvDateTime) participation.getTime().getLower();
                    if (lower != null) {

                        participationRecord.setStartTime(new Timestamp(lower.getValue().get(ChronoField.MILLI_OF_SECOND)));
                        participationRecord.setStartTimeTzid(ZoneId.systemDefault().getId());
                    }
                }

                PartyIdentified performer; //only PartyIdentified performer is supported now

                PartyProxy setPerformer = participation.getPerformer();

                if (!(setPerformer instanceof PartyIdentified)) {
                    log.warn("Set performer is using unsupported type:" + setPerformer.toString());
                    break;
                }

                performer = (PartyIdentified) setPerformer;
                UUID performerUuid = I_PartyIdentifiedAccess.getOrCreateParty(this, performer);
                //set the performer
                participationRecord.setPerformer(performerUuid);
                participations.add(participationRecord);
            }
        }

        //other context
        if (eventContext.getOtherContext() != null && CollectionUtils.isNotEmpty(eventContext.getOtherContext().getItems())) {
            //set up the JSONB field other_context
            eventContextRecord.setOtherContext(new RawJson().marshal(eventContext.getOtherContext()));
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
            insertQuery.addValue(EVENT_CONTEXT.OTHER_CONTEXT, (Object) DSL.field(DSL.val(eventContextRecord.getOtherContext()) + "::jsonb"));
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
            updateQuery.addValue(EVENT_CONTEXT.OTHER_CONTEXT, (Object) DSL.field(DSL.val(eventContextRecord.getOtherContext().toString()) + "::jsonb"));
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
            PartyProxy performer = I_PartyIdentifiedAccess.retrievePartyIdentified(this, record.getPerformer());

            DvInterval<DvDateTime> startTime = null;
            if (record.getStartTime() != null) { //start time null value is allowed for participation
                startTime = new DvInterval<>(decodeDvDateTime(record.getStartTime(), record.getStartTimeTzid()), null);
            }

            DvCodedText mode = null;
            try {
                mode = decodeDvCodedText(record.getMode());
            } catch (IllegalArgumentException e) {
                throw new InternalServerException(DB_INCONSISTENCY, e);
            }
            Participation participation = new Participation(performer,
                    new DvText(record.getFunction()),
                    mode,
                    startTime);

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
            otherContext = new RawJson().unmarshal(((PGobject) eventContextRecord.getOtherContext()).getValue(), ItemStructure.class);
        }

        return new EventContext(healthCareFacility,
                decodeDvDateTime(eventContextRecord.getStartTime(), eventContextRecord.getStartTimeTzid()),
                decodeDvDateTime(eventContextRecord.getEndTime(), eventContextRecord.getEndTimeTzid()),
                participationList.isEmpty() ? null : participationList,
                eventContextRecord.getLocation(),
                concept,
                otherContext
        );

    }

    @Override
    public String getOtherContextJson() {
        if (eventContextRecord.getOtherContext() == null)
            return null;
        return ((PGobject) eventContextRecord.getOtherContext()).getValue();
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
