/*
 * Copyright (c) 2019 vitasystems GmbH and Hannover Medical School.
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

import static org.ehrbase.jooq.pg.Tables.ENTRY;
import static org.ehrbase.jooq.pg.Tables.ENTRY_HISTORY;
import static org.ehrbase.jooq.pg.Tables.TERRITORY;

import com.nedap.archie.rm.archetyped.Archetyped;
import com.nedap.archie.rm.archetyped.FeederAudit;
import com.nedap.archie.rm.archetyped.Link;
import com.nedap.archie.rm.archetyped.TemplateId;
import com.nedap.archie.rm.composition.Action;
import com.nedap.archie.rm.composition.AdminEntry;
import com.nedap.archie.rm.composition.Composition;
import com.nedap.archie.rm.composition.Evaluation;
import com.nedap.archie.rm.composition.EventContext;
import com.nedap.archie.rm.composition.Instruction;
import com.nedap.archie.rm.composition.Observation;
import com.nedap.archie.rm.composition.Section;
import com.nedap.archie.rm.datatypes.CodePhrase;
import com.nedap.archie.rm.datavalues.DvCodedText;
import com.nedap.archie.rm.datavalues.DvText;
import com.nedap.archie.rm.generic.PartyProxy;
import com.nedap.archie.rm.support.identification.ArchetypeID;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import com.nedap.archie.rm.support.identification.TerminologyId;
import com.nedap.archie.rm.support.identification.UIDBasedId;
import java.sql.Timestamp;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.dao.access.interfaces.I_CompositionAccess;
import org.ehrbase.dao.access.interfaces.I_ContextAccess;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.dao.access.interfaces.I_EntryAccess;
import org.ehrbase.dao.access.jooq.party.PersistedPartyProxy;
import org.ehrbase.dao.access.support.DataAccess;
import org.ehrbase.dao.access.support.SafeNav;
import org.ehrbase.jooq.dbencoding.RawJson;
import org.ehrbase.jooq.dbencoding.rmobject.FeederAuditEncoding;
import org.ehrbase.jooq.dbencoding.rmobject.LinksEncoding;
import org.ehrbase.jooq.pg.enums.EntryType;
import org.ehrbase.jooq.pg.tables.records.EntryHistoryRecord;
import org.ehrbase.jooq.pg.tables.records.EntryRecord;
import org.ehrbase.jooq.pg.udt.records.DvCodedTextRecord;
import org.ehrbase.service.RecordedDvCodedText;
import org.ehrbase.service.RecordedDvText;
import org.jooq.Condition;
import org.jooq.JSONB;
import org.jooq.Record;
import org.jooq.UpdateQuery;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Operations on the Entry part of a Composition (Entry is archetyped).
 *
 * @author Christian Chevalley
 * @author Jake Smolka
 * @author Luis Marco-Ruiz
 * @since 1.0.0
 */
public class EntryAccess extends DataAccess implements I_EntryAccess {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String DB_INCONSISTENCY = "DB inconsistency:";

    private EntryRecord entryRecord;

    private Composition composition;

    /**
     * Constructor with convenient {@link I_DomainAccess} parameter, for better readability.
     *
     * @param domainAccess  Current domain access object
     * @param templateId    Template ID of this entry
     * @param sequence      Sequence number of this entry
     * @param compositionId Linked composition ID
     * @param composition   Object representation of linked composition
     */
    public EntryAccess(
            I_DomainAccess domainAccess,
            String templateId,
            Integer sequence,
            UUID compositionId,
            Composition composition,
            Short sysTenant) {
        super(
                domainAccess.getContext(),
                domainAccess.getKnowledgeManager(),
                domainAccess.getIntrospectService(),
                domainAccess.getServerConfig());
        setFields(templateId, sequence, compositionId, composition, sysTenant);
    }

    private EntryAccess(I_DomainAccess domainAccess) {
        super(domainAccess);
    }

    public static String fetchTemplateIdByCompositionId(I_DomainAccess domainAccess, UUID compositionId) {
        return domainAccess
                .getContext()
                .select(ENTRY.TEMPLATE_ID)
                .from(ENTRY)
                .where(ENTRY.COMPOSITION_ID.equal(compositionId))
                .fetchOptional(ENTRY.TEMPLATE_ID)
                .orElse(null);
    }

    /**
     * @throws IllegalArgumentException if DB is inconsistent or operation fails
     */
    public static I_EntryAccess retrieveInstanceInComposition(
            I_DomainAccess domainAccess, I_CompositionAccess compositionAccess) {

        Optional<EntryRecord> existingEntry =
                domainAccess.getContext().fetchOptional(ENTRY, ENTRY.COMPOSITION_ID.eq(compositionAccess.getId()));
        if (existingEntry.isEmpty()) {
            return null;
        }

        // build the list of parameters to recreate the composition
        Map<SystemValue, Object> values = new EnumMap<>(SystemValue.class);
        values.put(
                SystemValue.COMPOSER,
                new PersistedPartyProxy(domainAccess).retrieve(compositionAccess.getComposerId()));

        // optional handling for persistent compositions that do not have a context
        Optional<I_ContextAccess> opContextAccess =
                compositionAccess.getContextId().map(id -> I_ContextAccess.retrieveInstance(domainAccess, id));
        opContextAccess.ifPresent(context -> values.put(SystemValue.CONTEXT, context.mapRmEventContext()));

        values.put(
                SystemValue.LANGUAGE,
                new CodePhrase(new TerminologyId("ISO_639-1"), compositionAccess.getLanguageCode()));
        String territory2letters = domainAccess
                .getContext()
                .fetchSingle(TERRITORY, TERRITORY.CODE.eq(compositionAccess.getTerritoryCode()))
                .getTwoletter();

        values.put(SystemValue.TERRITORY, new CodePhrase(new TerminologyId("ISO_3166-1"), territory2letters));

        if (compositionAccess.getFeederAudit() != null) {
            values.put(SystemValue.FEEDER_AUDIT, new FeederAuditEncoding().fromDB(compositionAccess.getFeederAudit()));
        }

        if (compositionAccess.getLinks() != null) {
            values.put(SystemValue.LINKS, new LinksEncoding().fromDB(compositionAccess.getLinks()));
        }

        try {
            EntryAccess entryAccess = new EntryAccess(domainAccess);

            // set the record UID in the composition with matching version number
            Integer version = I_CompositionAccess.getLastVersionNumber(domainAccess, compositionAccess.getId());
            values.put(
                    SystemValue.UID,
                    new ObjectVersionId(compositionAccess.getId().toString() + "::"
                            + domainAccess.getServerConfig().getNodename() + "::" + version));

            EntryRecord entryRecord = existingEntry.get();
            entryAccess.entryRecord = entryRecord;
            String value = entryRecord.getEntry().data();
            entryAccess.composition = new RawJson().unmarshal(value, Composition.class);

            // continuing optional handling for persistent compositions
            opContextAccess
                    .map(I_ContextAccess::mapRmEventContext)
                    .ifPresent(ec -> values.put(SystemValue.CONTEXT, ec));
            values.put(SystemValue.CATEGORY, new RecordedDvCodedText().fromDB(entryRecord, ENTRY.CATEGORY));
            setCompositionAttributes(entryAccess.composition, values);
            buildArchetypeDetails(entryAccess);

            return entryAccess;
        } catch (Exception e) {
            throw new IllegalArgumentException(DB_INCONSISTENCY + e);
        }
    }

    private static void buildArchetypeDetails(EntryAccess entryAccess) {
        Archetyped archetypeDetails = new Archetyped();
        TemplateId templateId = new TemplateId();
        templateId.setValue(entryAccess.getTemplateId());
        archetypeDetails.setTemplateId(templateId);
        archetypeDetails.setArchetypeId(new ArchetypeID(entryAccess.getArchetypeId()));
        archetypeDetails.setRmVersion(entryAccess.getRmVersion());
        entryAccess.composition.setArchetypeDetails(archetypeDetails);
    }

    public static I_EntryAccess retrieveInstanceInCompositionVersion(
            I_DomainAccess domainAccess, I_CompositionAccess compositionHistoryAccess, int version) {

        Condition condition = ENTRY_HISTORY
                .COMPOSITION_ID
                .eq(compositionHistoryAccess.getId())
                .and(ENTRY_HISTORY.SYS_TRANSACTION.eq(compositionHistoryAccess.getSysTransaction()));

        Optional<EntryHistoryRecord> existingEntryHistory =
                domainAccess.getContext().fetchOptional(ENTRY_HISTORY, condition);
        if (existingEntryHistory.isEmpty()) {
            return null;
        }

        // build the list of parameters to recreate the composition
        Map<SystemValue, Object> values = new EnumMap<>(SystemValue.class);
        values.put(
                SystemValue.COMPOSER,
                new PersistedPartyProxy(domainAccess).retrieve(compositionHistoryAccess.getComposerId()));

        EventContext context = I_ContextAccess.retrieveHistoricalEventContext(
                domainAccess, compositionHistoryAccess.getId(), compositionHistoryAccess.getSysTransaction());
        if (context == null) { // unchanged context use the current one!
            // also optional handling of context, because persistent compositions don't have a context
            compositionHistoryAccess.getContextId().ifPresent(uuid -> I_ContextAccess.retrieveInstance(
                            domainAccess, uuid)
                    .mapRmEventContext());
        }
        values.put(SystemValue.CONTEXT, context);

        values.put(
                SystemValue.LANGUAGE,
                new CodePhrase(new TerminologyId("ISO_639-1"), compositionHistoryAccess.getLanguageCode()));
        String territory2letters = domainAccess
                .getContext()
                .fetchSingle(TERRITORY, TERRITORY.CODE.eq(compositionHistoryAccess.getTerritoryCode()))
                .getTwoletter();
        values.put(SystemValue.TERRITORY, new CodePhrase(new TerminologyId("ISO_3166-1"), territory2letters));

        values.put(
                SystemValue.FEEDER_AUDIT, new FeederAuditEncoding().fromDB(compositionHistoryAccess.getFeederAudit()));

        try {
            EntryAccess entryAccess = new EntryAccess(domainAccess);

            // set the record UID in the composition
            UUID compositionId = compositionHistoryAccess.getId();
            values.put(
                    SystemValue.UID,
                    new ObjectVersionId(compositionId.toString() + "::"
                            + domainAccess.getServerConfig().getNodename() + "::" + version));

            EntryHistoryRecord entryHistoryRecord = existingEntryHistory.get();
            entryAccess.entryRecord = domainAccess.getContext().newRecord(ENTRY);
            entryAccess.entryRecord.setSysTenant(entryHistoryRecord.getSysTenant());
            entryAccess.entryRecord.from(entryHistoryRecord);
            entryAccess.composition =
                    new RawJson().unmarshal(entryHistoryRecord.getEntry().data(), Composition.class);

            DvCodedTextRecord category = entryHistoryRecord.getCategory();

            //            SafeNav<DvCodedText> safeDvCodedText = SafeNav
            //                .of(category)
            //                .get(c -> c.getValue())
            //                .get(s -> new DvCodedText(s, (CodePhrase) null))
            //                .use(SafeNav.of(category).get(c -> c.getDefiningCode()).get(d -> d.getCodeString()))
            //                .get((s, d) -> {d.setDefiningCode(new CodePhrase(s)); return d;})
            //            values.put(SystemValue.CATEGORY, safeDvCodedText.get())
            SafeNav<DvCodedText> safeDvCodedText = SafeNav.of(category)
                    .get(c -> new DvCodedText(c.getValue(), c.getDefiningCode().getCodeString()));
            values.put(SystemValue.CATEGORY, safeDvCodedText.get());

            setCompositionAttributes(entryAccess.composition, values);
            buildArchetypeDetails(entryAccess);

            return entryAccess;
        } catch (Exception e) {
            throw new IllegalArgumentException(DB_INCONSISTENCY + e);
        }
    }

    private static void setCompositionAttributes(Composition composition, Map<SystemValue, Object> values) {

        if (values == null) {
            return;
        }

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
                case NAME:
                    composition.setName((DvText) systemValue.getValue());
                    break;

                case RM_VERSION:
                    composition.getArchetypeDetails().setRmVersion((String) systemValue.getValue());
                    break;

                case FEEDER_AUDIT:
                    composition.setFeederAudit((FeederAudit) systemValue.getValue());
                    break;

                case LINKS:
                    composition.setLinks((List<Link>) systemValue.getValue());
                    break;

                default:
                    throw new IllegalArgumentException(
                            "Could not handle composition attribute:" + systemValue.getKey());
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
     *
     * @param entryRecord Target {@link EntryRecord}
     * @param composition input data in {@link Composition} representation
     */
    private void setCompositionFields(EntryRecord entryRecord, Composition composition) {

        entryRecord.setCategory(entryRecord.getCategory());

        if (composition.getContent() != null && !composition.getContent().isEmpty()) {
            Object node = composition.getContent().get(0);

            if (node instanceof Section) {
                entryRecord.setItemType(EntryType.valueOf("section"));
            } else if (node instanceof Evaluation
                    || node instanceof Observation
                    || node instanceof Instruction
                    || node instanceof Action) {
                entryRecord.setItemType(EntryType.valueOf("care_entry"));
            } else if (node instanceof AdminEntry) {
                entryRecord.setItemType(EntryType.valueOf("admin"));
            }
        } else {
            entryRecord.setItemType(EntryType.valueOf("admin"));
        }

        entryRecord.setArchetypeId(composition.getArchetypeNodeId());

        RawJson rawJson = new RawJson();
        entryRecord.setEntry(JSONB.valueOf(rawJson.marshal(composition)));
    }

    /**
     * Sets the field of a new Entry record (as part of the calling {@link EntryAccess} instance) with
     * given parameters. The Composition of the calling {@link EntryAccess} will be updated with the
     * given {@link Composition} as a result.
     *
     * @param templateId    ID of template
     * @param sequence      Sequence number
     * @param compositionId ID of composition
     * @param composition   {@link Composition} object with more information for the entry
     */
    private void setFields(
            String templateId, Integer sequence, UUID compositionId, Composition composition, Short sysTenant) {

        entryRecord = getContext().newRecord(ENTRY);

        entryRecord.setTemplateId(templateId);
        entryRecord.setSequence(sequence);
        entryRecord.setCompositionId(compositionId);
        entryRecord.setRmVersion(composition.getArchetypeDetails().getRmVersion());
        entryRecord.setSysTenant(sysTenant);
        new RecordedDvCodedText().toDB(entryRecord, ENTRY.CATEGORY, composition.getCategory());
        setCompositionFields(entryRecord, composition);

        setCompositionName(composition.getName());

        this.composition = composition;
    }

    @Override
    public Composition getComposition() {
        return composition;
    }

    @Override
    public UUID commit(Timestamp transactionTime) {

        // use jOOQ
        Record result = getContext()
                .insertInto(
                        ENTRY,
                        ENTRY.SEQUENCE,
                        ENTRY.COMPOSITION_ID,
                        ENTRY.TEMPLATE_ID,
                        ENTRY.ITEM_TYPE,
                        ENTRY.ARCHETYPE_ID,
                        ENTRY.CATEGORY,
                        ENTRY.ENTRY_,
                        ENTRY.SYS_TRANSACTION,
                        ENTRY.NAME,
                        ENTRY.RM_VERSION,
                        ENTRY.SYS_TENANT)
                .values(
                        DSL.val(getSequence()),
                        DSL.val(getCompositionId()),
                        DSL.val(getTemplateId()),
                        DSL.val(EntryType.valueOf(getItemType())),
                        DSL.val(getArchetypeId()),
                        DSL.val(getCategory()),
                        DSL.val(getEntryJson()),
                        DSL.val(transactionTime),
                        DSL.val(getCompositionName()),
                        DSL.val(getRmVersion()),
                        // we do not expose the namespace
                        DSL.val(entryRecord.getSysTenant()))
                .returning(ENTRY.ID)
                .fetchOne();

        return result.getValue(ENTRY.ID);
    }

    /**
     * @throws InternalServerException because inherited interface function isn't implemented in this
     *                                 class
     * @deprecated
     */
    @Deprecated(forRemoval = true)
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
        logger.debug("updating entry with force flag: {} and changed flag: {}", force, entryRecord.changed());

        if (!(force || entryRecord.changed())) {
            logger.debug("No updateComposition took place, returning...");
            return false;
        }

        // ignore the temporal field since it is maintained by an external trigger!
        entryRecord.changed(ENTRY.SYS_PERIOD, false);

        UpdateQuery<?> updateQuery = getContext().updateQuery(ENTRY);
        updateQuery.addValue(ENTRY.COMPOSITION_ID, getCompositionId());
        updateQuery.addValue(ENTRY.SEQUENCE, DSL.field(DSL.val(getSequence())));
        updateQuery.addValue(ENTRY.TEMPLATE_ID, DSL.field(DSL.val(getTemplateId())));

        updateQuery.addValue(ENTRY.ITEM_TYPE, DSL.field(DSL.val(EntryType.valueOf(getItemType()))));
        updateQuery.addValue(ENTRY.ARCHETYPE_ID, DSL.field(DSL.val(getArchetypeId())));
        updateQuery.addValue(ENTRY.CATEGORY, DSL.field(DSL.val(getCategory())));
        updateQuery.addValue(ENTRY.ENTRY_, DSL.field(DSL.val(getEntryJson())));
        updateQuery.addValue(ENTRY.SYS_TRANSACTION, DSL.field(DSL.val(transactionTime)));
        updateQuery.addValue(ENTRY.NAME, DSL.field(DSL.val(getCompositionName())));
        updateQuery.addValue(ENTRY.RM_VERSION, DSL.field(DSL.val(getRmVersion())));
        updateQuery.addConditions(ENTRY.ID.eq(getId()));

        logger.debug("Update done...");

        return updateQuery.execute() > 0;
    }

    /**
     * @throws InternalServerException because inherited interface function isn't implemented in this
     *                                 class
     * @deprecated
     */
    @Deprecated(forRemoval = true)
    @Override
    public Boolean update() {
        throw new InternalServerException(
                "INTERNAL: Invalid updateComposition call to updateComposition without Transaction time and/or force flag arguments");
    }

    /**
     * @throws InternalServerException because inherited interface function isn't implemented in this
     *                                 class
     * @deprecated
     */
    @Deprecated(forRemoval = true)
    @Override
    public Boolean update(Boolean force) {
        throw new InternalServerException(
                "INTERNAL: Invalid updateComposition call to updateComposition without Transaction time and/or force flag arguments");
    }

    @Override
    public Integer delete() {

        if (entryRecord != null) {
            return entryRecord.delete();
        }

        return 0;
    }

    @Override
    public UUID getId() {
        return entryRecord.getId();
    }

    @Override
    public JSONB getEntryJson() {
        return entryRecord.getEntry();
    }

    @Override
    public DvCodedTextRecord getCategory() {
        return entryRecord.getCategory();
    }

    public DvCodedTextRecord getCompositionName() {
        return entryRecord.getName();
    }

    public void setCompositionName(DvText compositionName) {
        new RecordedDvText().toDB(entryRecord, ENTRY.NAME, compositionName);
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
    public String getRmVersion() {
        return entryRecord.getRmVersion();
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
