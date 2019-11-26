/*
 * Modifications copyright (C) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School,
 * Jake Smolka (Hannover Medical School), Luis Marco-Ruiz (Hannover Medical School).

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

import com.nedap.archie.rm.archetyped.Archetyped;
import com.nedap.archie.rm.archetyped.TemplateId;
import com.nedap.archie.rm.composition.*;
import com.nedap.archie.rm.datatypes.CodePhrase;
import com.nedap.archie.rm.datavalues.DvCodedText;
import com.nedap.archie.rm.generic.PartyProxy;
import com.nedap.archie.rm.support.identification.ArchetypeID;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import com.nedap.archie.rm.support.identification.TerminologyId;
import com.nedap.archie.rm.support.identification.UIDBasedId;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ehrbase.api.definitions.ServerConfig;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.dao.access.interfaces.*;
import org.ehrbase.dao.access.query.AsyncSqlQuery;
import org.ehrbase.dao.access.support.DataAccess;
import org.ehrbase.ehr.knowledge.I_KnowledgeCache;
import org.ehrbase.jooq.pg.enums.EntryType;
import org.ehrbase.jooq.pg.tables.records.EntryHistoryRecord;
import org.ehrbase.jooq.pg.tables.records.EntryRecord;
import org.ehrbase.serialisation.RawJson;
import org.ehrbase.service.IntrospectService;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.UpdateQuery;
import org.jooq.impl.DSL;
import org.postgresql.util.PGobject;

import java.sql.Timestamp;
import java.util.*;

import static org.ehrbase.jooq.pg.Tables.*;

/**
 * operations on the Entry part of a Composition (Entry is archetyped)
 * <p>
 * Created by Christian Chevalley on 4/9/2015.
 */
public class EntryAccess extends DataAccess implements I_EntryAccess {

    private static final Logger log = LogManager.getLogger(EntryAccess.class);
    public static final String DB_INCONSISTENCY = "DB inconsistency:";

    private EntryRecord entryRecord;
    private I_ContainmentAccess containmentAccess;

    private Composition composition;

    /**
     * Basic constructor for entry.
     * @param context DB context object of current server context
     * @param knowledge Knowledge cache object of current server context
     * @param introspectCache Introspect cache object of current server context
     * @param serverConfig Server config object of current server context
     * @param templateId Template ID of this entry
     * @param sequence Sequence number of this entry
     * @param compositionId Linked composition ID
     * @param composition Object representation of linked composition
     */
    public EntryAccess(DSLContext context, I_KnowledgeCache knowledge, IntrospectService introspectCache, ServerConfig serverConfig, String templateId, Integer sequence, UUID compositionId, Composition composition) {
        super(context, knowledge, introspectCache, serverConfig);
        setFields(templateId, sequence, compositionId, composition);
    }

    /**
     * Constructor with convenient {@link I_DomainAccess} parameter, for better readability.
     * @param domainAccess Current domain access object
     * @param templateId Template ID of this entry
     * @param sequence Sequence number of this entry
     * @param compositionId Linked composition ID
     * @param composition Object representation of linked composition
     */
    public EntryAccess(I_DomainAccess domainAccess, String templateId, Integer sequence, UUID compositionId, Composition composition) {
        super(domainAccess.getContext(), domainAccess.getKnowledgeManager(), domainAccess.getIntrospectService(), domainAccess.getServerConfig());
        setFields(templateId, sequence, compositionId, composition);
    }

    private EntryAccess(I_DomainAccess domainAccess) {
        super(domainAccess);

    }

    /**
     * @throws IllegalArgumentException if DB is inconsistent or operation fails
     */
    public static List<I_EntryAccess> retrieveInstanceInComposition(I_DomainAccess domainAccess, I_CompositionAccess compositionAccess) {

        Result<EntryRecord> entryRecords = domainAccess.getContext().selectFrom(ENTRY).where(ENTRY.COMPOSITION_ID.eq(compositionAccess.getId())).fetch();

        //build the list of parameters to recreate the composition
        Map<SystemValue, Object> values = new HashMap<>();
        values.put(SystemValue.COMPOSER, I_PartyIdentifiedAccess.retrievePartyIdentified(domainAccess, compositionAccess.getComposerId()));

        // optional handling for persistent compositions that do not have a context
        Optional<I_ContextAccess> opContextAccess = compositionAccess.getContextId().map(id -> I_ContextAccess.retrieveInstance(domainAccess, id));
        opContextAccess.ifPresent(context -> values.put(SystemValue.CONTEXT, context.mapRmEventContext()));

        values.put(SystemValue.LANGUAGE, new CodePhrase(new TerminologyId("ISO_639-1"), compositionAccess.getLanguageCode()));
        String territory2letters = domainAccess.getContext().fetchOne(TERRITORY, TERRITORY.CODE.eq(compositionAccess.getTerritoryCode())).getTwoletter();

        values.put(SystemValue.TERRITORY, new CodePhrase(new TerminologyId("ISO_3166-1"), territory2letters));


        List<I_EntryAccess> content = new ArrayList<>();

        try {
            EntryAccess entryAccess = new EntryAccess(domainAccess);

            for (EntryRecord record : entryRecords) {
                //set the record UID in the composition with matching version number
                Integer version = I_CompositionAccess.getLastVersionNumber(domainAccess, compositionAccess.getId());
                values.put(SystemValue.UID,
                        new ObjectVersionId(compositionAccess.getId().toString() + "::" + domainAccess.getServerConfig().getNodename() + "::" + version));

                entryAccess.entryRecord = record;
                String value = ((PGobject) record.getEntry()).getValue();
                entryAccess.composition = new RawJson().unmarshal(value, Composition.class);

                // continuing optional handling for persistent compositions
                opContextAccess.map(I_ContextAccess::mapRmEventContext).ifPresent(ec -> values.put(SystemValue.CONTEXT, ec));

                setCompositionAttributes(entryAccess.composition, values);
                buildArchetypeDetails(entryAccess);
                content.add(entryAccess);
            }
        } catch (Exception e) {
            log.error(DB_INCONSISTENCY + e);
            throw new IllegalArgumentException(DB_INCONSISTENCY + e);
        }
        return content;
    }

    private static void buildArchetypeDetails(EntryAccess entryAccess) {
        Archetyped archetypeDetails = new Archetyped();
        TemplateId templateId = new TemplateId();
        templateId.setValue(entryAccess.getTemplateId());
        archetypeDetails.setTemplateId(templateId);
        archetypeDetails.setArchetypeId(new ArchetypeID(entryAccess.getArchetypeId()));
        entryAccess.composition.setArchetypeDetails(archetypeDetails);
        entryAccess.composition.setCategory(I_ConceptAccess.fetchConceptText(entryAccess, entryAccess.getCategory()));
    }

    public static List<I_EntryAccess> retrieveInstanceInCompositionVersion(I_DomainAccess domainAccess, I_CompositionAccess compositionHistoryAccess, int version) {

        Result<EntryHistoryRecord> entryHistoryRecords = domainAccess.getContext().
                selectFrom(ENTRY_HISTORY)
                .where(ENTRY_HISTORY.COMPOSITION_ID.eq(compositionHistoryAccess.getId()))
                .and(ENTRY_HISTORY.SYS_TRANSACTION.eq(compositionHistoryAccess.getSysTransaction()))
                .fetch();

        //build the list of parameters to recreate the composition
        Map<SystemValue, Object> values = new HashMap<>();
        values.put(SystemValue.COMPOSER, I_PartyIdentifiedAccess.retrievePartyIdentified(domainAccess, compositionHistoryAccess.getComposerId()));

        EventContext context = I_ContextAccess.retrieveHistoricalEventContext(domainAccess, compositionHistoryAccess.getId(), compositionHistoryAccess.getSysTransaction());
        if (context == null) {//unchanged context use the current one!
            // also optional handling of context, because persistent compositions don't have a context
            compositionHistoryAccess.getContextId().ifPresent(uuid -> I_ContextAccess.retrieveInstance(domainAccess, uuid).mapRmEventContext());
        }
        values.put(SystemValue.CONTEXT, context);

        values.put(SystemValue.LANGUAGE, new CodePhrase(new TerminologyId("ISO_639-1"), compositionHistoryAccess.getLanguageCode()));
        String territory2letters = domainAccess.getContext().fetchOne(TERRITORY, TERRITORY.CODE.eq(compositionHistoryAccess.getTerritoryCode())).getTwoletter();
        values.put(SystemValue.TERRITORY, new CodePhrase(new TerminologyId("ISO_3166-1"), territory2letters));


        List<I_EntryAccess> content = new ArrayList<>();

        try {
            EntryAccess entryAccess = new EntryAccess(domainAccess);

            for (EntryHistoryRecord record : entryHistoryRecords) {
                //set the record UID in the composition
                UUID compositionId = compositionHistoryAccess.getId();
                values.put(SystemValue.UID, new ObjectVersionId(compositionId.toString() + "::" + domainAccess.getServerConfig().getNodename() + "::" + version));

//                EntryAccess entry = new EntryAccess();
                entryAccess.entryRecord = domainAccess.getContext().newRecord(ENTRY);
                entryAccess.entryRecord.from(record);
                entryAccess.composition = new RawJson().unmarshal(((PGobject) record.getEntry()).getValue(), Composition.class);

                setCompositionAttributes(entryAccess.composition, values);
                buildArchetypeDetails(entryAccess);

                content.add(entryAccess);
//                entry.committed = true;
            }
        } catch (Exception e) {
            log.error(DB_INCONSISTENCY + e);
            throw new IllegalArgumentException(DB_INCONSISTENCY + e);
        }
        return content;
    }

    /**
     * @throws InternalServerException when the query failed
     */
    public static Map<String, Object> queryJSON(I_DomainAccess domainAccess, String queryString) {
        return new AsyncSqlQuery(domainAccess, queryString).fetch();
    }

    private static void setCompositionAttributes(Composition composition, Map<SystemValue, Object> values) {

        if (values == null) return;


        for (Map.Entry<SystemValue, Object> systemValue : values.entrySet()) {
            switch (systemValue.getKey()) {
                case CATEGORY:
                    composition.setCategory((DvCodedText) systemValue.getValue());
                    break;
                case LANGUAGE:
                    composition.setLanguage((CodePhrase) systemValue.getValue());
                    break;
                case TERRITORY:
                    composition.setTerritory((CodePhrase) systemValue.getValue());
                    break;
                case COMPOSER:
                    composition.setComposer((PartyProxy) systemValue.getValue());
                    break;
                case UID:
                    composition.setUid((UIDBasedId) systemValue.getValue());
                    break;
                case CONTEXT:
                    composition.setContext((EventContext) systemValue.getValue());
                    break;

                default:
                    throw new IllegalArgumentException("Could not handle composition attribute:" + systemValue.getKey());
            }
        }
    }

    /**
     * set the EntryRecord with fields from composition:<br>
     * <ul>
     * <li>category</li>
     * <li>item type</li>
     * <li>archetype node Id</li>
     * <li>entry content (json)</li>
     * </ul>
     * TODO: memory only until committed, correct?
     *
     * @param record      Target {@link EntryRecord}
     * @param composition input data in {@link Composition} representation
     */
    private void setCompositionFields(EntryRecord record, Composition composition) {

        Integer categoryId = Integer.parseInt(composition.getCategory().getDefiningCode().getCodeString());
        record.setCategory(I_ConceptAccess.fetchConcept(this, categoryId, "en"));

        if (composition.getContent() != null) {
            Object node = composition.getContent().get(0);


            if (node instanceof Section)
                record.setItemType(EntryType.valueOf("section"));
            else if (node instanceof Evaluation || node instanceof Observation || node instanceof Instruction || node instanceof Action)
                record.setItemType(EntryType.valueOf("care_entry"));
            else if (node instanceof AdminEntry)
                record.setItemType(EntryType.valueOf("admin"));
        } else
            record.setItemType(EntryType.valueOf("admin"));

        record.setArchetypeId(composition.getArchetypeNodeId());


        RawJson rawJson = new RawJson();
        record.setEntry(rawJson.marshal(composition));
        containmentAccess = new ContainmentAccess(getDataAccess(), record.getId(), record.getArchetypeId(), rawJson.getLtreeMap(), true);
    }

    /**
     * Sets the field of a new Entry record (as part of the calling {@link EntryAccess} instance) with given parameters. The
     * Composition of the calling {@link EntryAccess} will be updated with the given {@link Composition} as a result.
     * TODO: memory only until committed, correct?
     *
     * @param templateId    ID of template
     * @param sequence      Sequence number
     * @param compositionId ID of composition
     * @param composition   {@link Composition} object with more information for the entry
     */
    private void setFields(String templateId, Integer sequence, UUID compositionId, Composition composition) {

        entryRecord = getContext().newRecord(ENTRY);

        entryRecord.setTemplateId(templateId);
        entryRecord.setSequence(sequence);
        entryRecord.setCompositionId(compositionId);

        setCompositionFields(entryRecord, composition);

        this.composition = composition;
    }

    @Override
    public Composition getComposition() {
        return composition;
    }

    @Override
    public UUID commit(Timestamp transactionTime) {

        //--------------- TODO: WIP refactoring it in jooq
        /*InsertQuery<?> insertQuery = context.insertQuery(ENTRY);
        insertQuery.addValue(ENTRY.SEQUENCE, entryRecord.getSequence());
        insertQuery.addValue(ENTRY.COMPOSITION_ID, entryRecord.getCompositionId());
        insertQuery.addValue(ENTRY.TEMPLATE_ID, entryRecord.getTemplateId());
        insertQuery.addValue(ENTRY.ITEM_TYPE, entryRecord.getItemType());
        insertQuery.addValue(ENTRY.ARCHETYPE_ID, entryRecord.getArchetypeId());
        insertQuery.addValue(ENTRY.CATEGORY, entryRecord.getCategory());
        insertQuery.addValue(ENTRY.ENTRY_, DSL.val(getEntryJson() + "::jsonb"));
        insertQuery.addValue(ENTRY.SYS_TRANSACTION, transactionTime);

        int result;
        try {
            result = insertQuery.execute();
        } catch (DataAccessException e) {
            throw new InternalServerException("Problem executing database operation", e);
        }

        if (result < 1) // TODO check result for successful execution -> is '< 1' correct as condition?
            throw new InternalServerException("Entry commit failed");*/
        //------------- END --------------------

        //use jOOQ!
        Record result = getContext()
                .insertInto(ENTRY, ENTRY.SEQUENCE, ENTRY.COMPOSITION_ID, ENTRY.TEMPLATE_ID, ENTRY.ITEM_TYPE, ENTRY.ARCHETYPE_ID, ENTRY.CATEGORY, ENTRY.ENTRY_, ENTRY.SYS_TRANSACTION)
                .values(DSL.val(getSequence()),
                        DSL.val(getCompositionId()),
                        DSL.val(getTemplateId()),
                        DSL.val(EntryType.valueOf(getItemType())),
                        DSL.val(getArchetypeId()),
                        DSL.val(getCategory()),
                        DSL.field(DSL.val(getEntryJson()) + "::jsonb"),
                        DSL.val(transactionTime))
                .returning(ENTRY.ID)
                .fetchOne();

        if (containmentAccess != null) {
            containmentAccess.setCompositionId(entryRecord.getCompositionId());
            containmentAccess.update();
        }

        return result.getValue(ENTRY.ID);
        //return entryRecord.getId(); // TODO: part of WIP refactoring from above
    }

    /**
     * @throws InternalServerException because inherited interface function isn't implemented in this class
     * @deprecated
     */
    @Deprecated
    @Override
    public UUID commit() {
        throw new InternalServerException("INTERNAL: commit without transaction time is not legal");
    }

    @Override
    public Boolean update(Timestamp transactionTime) {
        return update(transactionTime, false);
    }

    @Override
    public Boolean update(Timestamp transactionTime, boolean force) {

        log.debug("updating entry with force flag:" + force + " and changed flag:" + entryRecord.changed());
        if (!(force || entryRecord.changed())) {
            log.debug("No updateComposition took place, returning...");
            return false;
        }

        //ignore the temporal field since it is maintained by an external trigger!
        entryRecord.changed(ENTRY.SYS_PERIOD, false);

        UpdateQuery<?> updateQuery = getContext().updateQuery(ENTRY);
        updateQuery.addValue(ENTRY.COMPOSITION_ID, getCompositionId());
        updateQuery.addValue(ENTRY.SEQUENCE, DSL.field(DSL.val(getSequence())));
        updateQuery.addValue(ENTRY.TEMPLATE_ID, DSL.field(DSL.val(getTemplateId())));

        updateQuery.addValue(ENTRY.ITEM_TYPE, DSL.field(DSL.val(EntryType.valueOf(getItemType()))));
        updateQuery.addValue(ENTRY.ARCHETYPE_ID, DSL.field(DSL.val(getArchetypeId())));
        updateQuery.addValue(ENTRY.CATEGORY, DSL.field(DSL.val(getCategory())));
        updateQuery.addValue(ENTRY.ENTRY_, (Object) DSL.field(DSL.val(getEntryJson()) + "::jsonb"));
        updateQuery.addValue(ENTRY.SYS_TRANSACTION, DSL.field(DSL.val(transactionTime)));
        updateQuery.addConditions(ENTRY.ID.eq(getId()));


        log.debug("Update done...");

        if (containmentAccess != null) {
            containmentAccess.setCompositionId(entryRecord.getCompositionId());
            containmentAccess.update();
        }

        return updateQuery.execute() > 0;
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

        if (entryRecord != null)
            return entryRecord.delete();

        return 0;
    }

    @Override
    public UUID getId() {
        return entryRecord.getId();
    }

    @Override
    public String getEntryJson() {
        if (entryRecord.getEntry() instanceof String)
            return (String) entryRecord.getEntry();

        PGobject entryPGobject = (PGobject) entryRecord.getEntry();
        return entryPGobject.getValue();
    }

    @Override
    public UUID getCategory() {
        return entryRecord.getCategory();
    }

    @Override
    public UUID getCompositionId() {
        return entryRecord.getCompositionId();
    }

    @Override
    public void setCompositionId(UUID compositionId) {
        entryRecord.setCompositionId(compositionId);
    }

    @Override
    public String getTemplateId() {
        return entryRecord.getTemplateId();
    }

    @Override
    public void setTemplateId(String templateId) {
        entryRecord.setTemplateId(templateId);
    }

    @Override
    public Integer getSequence() {
        return entryRecord.getSequence();
    }

    @Override
    public void setSequence(Integer sequence) {
        entryRecord.setSequence(sequence);
    }

    @Override
    public String getArchetypeId() {
        return entryRecord.getArchetypeId();
    }

    @Override
    public String getItemType() {
        return entryRecord.getItemType().getLiteral();
    }

    @Override
    public void setCompositionData(Composition composition) {
        setCompositionFields(entryRecord, composition);
    }

    @Override
    public DataAccess getDataAccess() {
        return this;
    }

}
