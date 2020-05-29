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

import com.nedap.archie.rm.composition.Composition;
import com.nedap.archie.rm.composition.EventContext;
import com.nedap.archie.rm.generic.PartyIdentified;
import com.nedap.archie.rm.generic.PartyProxy;
import com.nedap.archie.rm.generic.PartySelf;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ehrbase.api.definitions.ServerConfig;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.dao.access.interfaces.*;
import org.ehrbase.dao.access.jooq.party.PersistedPartyProxy;
import org.ehrbase.dao.access.support.DataAccess;
import org.ehrbase.dao.access.util.ContributionDef;
import org.ehrbase.ehr.knowledge.I_KnowledgeCache;
import org.ehrbase.jooq.pg.enums.ContributionChangeType;
import org.ehrbase.jooq.pg.enums.ContributionDataType;
import org.ehrbase.jooq.pg.tables.records.AuditDetailsRecord;
import org.ehrbase.jooq.pg.tables.records.CompositionHistoryRecord;
import org.ehrbase.jooq.pg.tables.records.CompositionRecord;
import org.ehrbase.jooq.pg.tables.records.EventContextRecord;
import org.ehrbase.service.IntrospectService;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

import static org.ehrbase.jooq.pg.Tables.*;
import static org.jooq.impl.DSL.count;
import static org.jooq.impl.DSL.max;

/**
 * operations on the static part of Compositions (eg. non archetyped attributes)
 * <p>
 * Created by Christian Chevalley on 4/2/2015.
 */
public class CompositionAccess extends DataAccess implements I_CompositionAccess {

    private static final Logger log = LogManager.getLogger(CompositionAccess.class);
    // List of Entry DAOs and therefore provides access to all entries of the composition
    private List<I_EntryAccess> content = new ArrayList<>();
    private CompositionRecord compositionRecord;
    private I_ContributionAccess contributionAccess = null; //locally referenced contribution associated to this composition
    private I_AuditDetailsAccess auditDetailsAccess;  // audit associated with this composition
    private Composition composition;

    /**
     * Basic constructor for composition.
     * @param context DB context object of current server context
     * @param knowledgeManager Knowledge cache object of current server context
     * @param introspectCache Introspect cache object of current server context
     * @param serverConfig Server config object of current server context
     * @param composition Object representation of given new composition
     * @param ehrId Given ID of EHR this composition will be created for
     * @throws IllegalArgumentException when seeking language code, territory code or composer ID failed
     */
    public CompositionAccess(DSLContext context, I_KnowledgeCache knowledgeManager, IntrospectService introspectCache, ServerConfig serverConfig, Composition composition, UUID ehrId) {
        super(context, knowledgeManager, introspectCache, serverConfig);

        this.composition = composition;

        String territoryCode = composition.getTerritory().getCodeString();
        String languageCode = composition.getLanguage().getCodeString();

        UUID composerId = seekComposerId(composition.getComposer());

        compositionRecord = context.newRecord(COMPOSITION);
        compositionRecord.setId(UUID.randomUUID());

        compositionRecord.setTerritory(seekTerritoryCode(territoryCode));

        compositionRecord.setLanguage(seekLanguageCode(languageCode));
        compositionRecord.setActive(true);
//        compositionRecord.setContext(eventContextId);     // TODO: is context handled somewhere else (so remove here)? or is this a TODO?
        compositionRecord.setComposer(composerId);
        compositionRecord.setEhrId(ehrId);

        //associate a contribution with this composition
        contributionAccess = I_ContributionAccess.getInstance(this, compositionRecord.getEhrId());
        contributionAccess.setState(ContributionDef.ContributionState.COMPLETE);

        // associate composition's own audit with this composition access instance
        auditDetailsAccess = I_AuditDetailsAccess.getInstance(getDataAccess());

    }

    /**
     * Constructor with convenient {@link I_DomainAccess} parameter, for better readability.
     * @param domainAccess Current domain access object
     * @param composition Object representation of given new composition
     * @param ehrId Given ID of EHR this composition will be created for
     * @throws IllegalArgumentException when seeking language code, territory code or composer ID failed
     */
    public CompositionAccess(I_DomainAccess domainAccess, Composition composition, UUID ehrId) {
        super(domainAccess);

        this.composition = composition;

        String territoryCode = composition.getTerritory().getCodeString();
        String languageCode = composition.getLanguage().getCodeString();

        UUID composerId = seekComposerId(composition.getComposer());

        compositionRecord = domainAccess.getContext().newRecord(COMPOSITION);
        compositionRecord.setId(UUID.randomUUID());

        compositionRecord.setTerritory(seekTerritoryCode(territoryCode));

        compositionRecord.setLanguage(seekLanguageCode(languageCode));
        compositionRecord.setActive(true);
//        compositionRecord.setContext(eventContextId);     // TODO: is context handled somewhere else (so remove here)? or is this a TODO?
        compositionRecord.setComposer(composerId);
        compositionRecord.setEhrId(ehrId);

        //associate a contribution with this composition
        contributionAccess = I_ContributionAccess.getInstance(this, compositionRecord.getEhrId());
        contributionAccess.setState(ContributionDef.ContributionState.COMPLETE);

        // associate composition's own audit with this composition access instance
        auditDetailsAccess = I_AuditDetailsAccess.getInstance(getDataAccess());

    }

    /**
     * constructor used to perform non static operation on instance
     * creates new instance with values from record and new empty contribution and audit
     *
     * @param domainAccess      SQL context
     * @param compositionRecord record representation of composition
     */
    private CompositionAccess(I_DomainAccess domainAccess, CompositionRecord compositionRecord) {
        super(domainAccess);

        this.compositionRecord = compositionRecord;
        contributionAccess = I_ContributionAccess.getInstance(this, compositionRecord.getEhrId());
        contributionAccess.setState(ContributionDef.ContributionState.COMPLETE);

        // associate composition's own audit with this composition access instance
        auditDetailsAccess = I_AuditDetailsAccess.getInstance(this.getDataAccess());
    }

    CompositionAccess(I_DomainAccess domainAccess) {
        super(domainAccess);
    }

    /**
     * @throws IllegalArgumentException when version number is not greater 0
     * @throws ObjectNotFoundException  when no composition could be found with given input
     */
    public static I_CompositionAccess retrieveCompositionVersion(I_DomainAccess domainAccess, UUID id, int version) {

        if (version < 1)
            throw new IllegalArgumentException("Version number must be > 0  please check your code");

        //check if this version number matches the current version
        if (getLastVersionNumber(domainAccess, id) == version) { //current version
            return retrieveInstance(domainAccess, id);
        }

        // FIXME make jooq compliant
        String versionQuery =
                "select row_id, in_contribution, ehr_id, language, territory, composer, sys_transaction, has_audit from \n" +
                        "  (select ROW_NUMBER() OVER (ORDER BY sys_transaction ASC ) AS row_id, * from ehr.composition_history " +
                        "WHERE id = ?) \n" +
                        "    AS Version WHERE row_id = ?;";

        Connection connection = domainAccess.getConnection();

        I_CompositionAccess compositionHistoryAccess = null;
        try (PreparedStatement preparedStatement = connection.prepareStatement(versionQuery)) {
            preparedStatement.setObject(1, id);
            preparedStatement.setInt(2, version);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {

                while (resultSet.next()) {
                    CompositionRecord compositionRecord1 = domainAccess.getContext().newRecord(COMPOSITION);
                    compositionRecord1.setId(id);
                    compositionRecord1.setInContribution(UUID.fromString(resultSet.getString("in_contribution")));
                    compositionRecord1.setEhrId(UUID.fromString(resultSet.getString("ehr_id")));
                    compositionRecord1.setLanguage(resultSet.getString("language"));
                    compositionRecord1.setTerritory(resultSet.getInt("territory"));
                    compositionRecord1.setComposer(UUID.fromString(resultSet.getString("composer")));
                    compositionRecord1.setSysTransaction(resultSet.getTimestamp("sys_transaction"));
                    compositionRecord1.setHasAudit(UUID.fromString(resultSet.getString("has_audit")));
                    compositionHistoryAccess = new CompositionAccess(domainAccess, compositionRecord1);
                }
            }
        } catch (SQLException e) {
            throw new ObjectNotFoundException("composition", "Composition not found or or invalid DB content", e);
        }

        if (compositionHistoryAccess != null) {
            compositionHistoryAccess.setContent(I_EntryAccess.retrieveInstanceInCompositionVersion(domainAccess, compositionHistoryAccess, version));

            //retrieve the corresponding contribution
            I_ContributionAccess contributionAccess = I_ContributionAccess.retrieveVersionedInstance(domainAccess, compositionHistoryAccess.getContributionId(), compositionHistoryAccess.getSysTransaction());
            compositionHistoryAccess.setContributionAccess(contributionAccess);

            I_AuditDetailsAccess auditDetailsAccess = new AuditDetailsAccess(domainAccess.getDataAccess()).retrieveInstance(domainAccess.getDataAccess(), compositionHistoryAccess.getAuditDetailsId());
            compositionHistoryAccess.setAuditDetailsAccess(auditDetailsAccess);

            //retrieve versioned context
            EventContext historicalEventContext = I_ContextAccess.retrieveHistoricalEventContext(domainAccess, id, compositionHistoryAccess.getSysTransaction());
            //adjust context for entries
            if (historicalEventContext != null) {
                for (I_EntryAccess entryAccess : compositionHistoryAccess.getContent()) {
                    entryAccess.getComposition().setContext(historicalEventContext);
                }
            }

        }

        domainAccess.releaseConnection(connection);

        return compositionHistoryAccess;
    }

    public static Integer getLastVersionNumber(I_DomainAccess domainAccess, UUID compositionId) {
        // check if compositionId is valid (version count = 1) and add number of existing older versions
        if (domainAccess.getContext().fetchExists(COMPOSITION, COMPOSITION.ID.eq(compositionId))) {
            return 1 + domainAccess.getContext().fetchCount(COMPOSITION_HISTORY, COMPOSITION_HISTORY.ID.eq(compositionId));
        } else
            return domainAccess.getContext().fetchCount(COMPOSITION_HISTORY, COMPOSITION_HISTORY.ID.eq(compositionId));
    }

    public static boolean hasPreviousVersion(I_DomainAccess domainAccess, UUID compositionId) {
        return domainAccess.getContext().fetchExists(COMPOSITION_HISTORY, COMPOSITION_HISTORY.ID.eq(compositionId));
    }

    public static I_CompositionAccess retrieveInstance(I_DomainAccess domainAccess, UUID id) {
        I_CompositionAccess compositionAccess = new CompositionAccess(domainAccess);

        CompositionRecord compositionRecord = domainAccess.getContext().selectFrom(COMPOSITION).where(COMPOSITION.ID.eq(id)).fetchOne();

        if (compositionRecord == null)
            return null;

        compositionAccess.setCompositionRecord(compositionRecord);
        compositionAccess.setContent(I_EntryAccess.retrieveInstanceInComposition(domainAccess, compositionAccess));
        //retrieve the corresponding contribution
        I_ContributionAccess contributionAccess = I_ContributionAccess.retrieveInstance(domainAccess, compositionAccess.getContributionId());
        compositionAccess.setContributionAccess(contributionAccess);
        // retrieve corresponding audit
        I_AuditDetailsAccess auditAccess = new AuditDetailsAccess(domainAccess.getDataAccess()).retrieveInstance(domainAccess.getDataAccess(), compositionAccess.getAuditDetailsId());
        compositionAccess.setAuditDetailsAccess(auditAccess);

        return compositionAccess;
    }

    /**
     * @throws IllegalArgumentException when no version in compliance with timestamp is available
     * @throws InternalServerException  on problem with SQL statement or input
     */
    public static int getVersionFromTimeStamp(I_DomainAccess domainAccess, UUID vCompositionUid, Timestamp timeCommitted) {

        if (timeCommitted == null) {
            return getLastVersionNumber(domainAccess, vCompositionUid);
        }
        //get the latest composition time (available in ehr.composition) table
        Record result;
        try {
            result = domainAccess.getContext().select(max(COMPOSITION.SYS_TRANSACTION).as("mostRecentInTable")).from(COMPOSITION).where(COMPOSITION.ID.eq(vCompositionUid)).fetchOne();
        } catch (RuntimeException e) {  // generalize SQL exceptions
            throw new InternalServerException("Problem with SQL statement or input", e);
        }
        Timestamp latestCompoTime = (Timestamp) result.get("mostRecentInTable");

        //get the latest version (if more than one) time (available in ehr.composition_history) table
        Record result2;
        try {
            result2 = domainAccess.getContext().select(count().as("countVersionInTable")).from(COMPOSITION_HISTORY).where(COMPOSITION_HISTORY.SYS_TRANSACTION.lessOrEqual(timeCommitted).and(COMPOSITION_HISTORY.ID.eq(vCompositionUid))).fetchOne();
        } catch (RuntimeException e) { // generalize SQL exceptions
            throw new InternalServerException("Problem with SQL statement or input", e);
        }
        int versionComHist = (int) result2.get("countVersionInTable");
        if (timeCommitted.compareTo(latestCompoTime) >= 0) {//if the timestamp is after or equal to the sys_transaction of the latest composition available, add one since its version has not been counted for being the one stored in the ehr.composition table
            versionComHist++;
        }
        if (versionComHist == 0) {
            throw new IllegalArgumentException("There are no versions available prior to date " + timeCommitted + " for the the composition with id: " + vCompositionUid);
        }
        return versionComHist;
    }

    /**
     * @throws IllegalArgumentException when no version in compliance with timestamp is available or when calculated version number is not greater 0
     * @throws InternalServerException  on problem with SQL statement or input
     * @throws ObjectNotFoundException  when no composition could be found with given input
     */
    public static I_CompositionAccess retrieveInstanceByTimestamp(I_DomainAccess domainAccess, UUID compositionUid, Timestamp timeCommitted) {

        int version = getVersionFromTimeStamp(domainAccess, compositionUid, timeCommitted);

        if (getLastVersionNumber(domainAccess, compositionUid) == version) { //current version
            return retrieveInstance(domainAccess, compositionUid);
        }

        return retrieveCompositionVersion(domainAccess, compositionUid, version);
    }

    public static Map<I_CompositionAccess, Integer> retrieveCompositionsInContribution(I_DomainAccess domainAccess, UUID contribution) {
        Set<UUID> compositions = new HashSet<>();   // Set, because of unique values
        // add all compositions having a link to given contribution
        domainAccess.getContext().select(COMPOSITION.ID).from(COMPOSITION).where(COMPOSITION.IN_CONTRIBUTION.eq(contribution)).fetch()
                .forEach(rec -> compositions.add(rec.value1()));
        // and older versions or deleted ones, too
        domainAccess.getContext().select(COMPOSITION_HISTORY.ID).from(COMPOSITION_HISTORY).where(COMPOSITION_HISTORY.IN_CONTRIBUTION.eq(contribution)).fetch()
                .forEach(rec -> compositions.add(rec.value1()));

        // get whole "version map" of each matching composition and do fine-grain check for matching contribution
        // precondition: each UUID in `compositions` set is unique, so for each the "version map" is only created once below
        // (meta: can't do that as jooq query, because the specific version number isn't stored in DB)
        Map<I_CompositionAccess, Integer> resultMap = new HashMap<>();
        for (UUID compositionId : compositions) {
            Map<I_CompositionAccess, Integer> map = getVersionMapOfComposition(domainAccess, compositionId);
            // fine-grained contribution ID check
            for (Map.Entry<I_CompositionAccess, Integer> entry : map.entrySet()) {
                if (entry.getKey().getContributionId().equals(contribution))
                    resultMap.put(entry.getKey(), entry.getValue());
            }
        }

        return resultMap;
    }

    public static Map<I_CompositionAccess, Integer> getVersionMapOfComposition(I_DomainAccess domainAccess, UUID compositionId) {
        // TODO check like hasComposition or doesExist

        Map<I_CompositionAccess, Integer> versionMap = new HashMap<>();

        // create counter with highest version, to keep track of version number and allow check in the end
        Integer versionCounter = getLastVersionNumber(domainAccess, compositionId);

        // fetch matching entry
        CompositionRecord record = domainAccess.getContext().fetchOne(COMPOSITION, COMPOSITION.ID.eq(compositionId));
        if (record != null) {
            I_CompositionAccess compositionAccess = new CompositionAccess(domainAccess);
            compositionAccess.setCompositionRecord(record);
            compositionAccess.setContent(I_EntryAccess.retrieveInstanceInComposition(domainAccess, compositionAccess));
            versionMap.put(compositionAccess, versionCounter);

            versionCounter--;
        }

        // if composition was removed (i.e. from "COMPOSITION" table) *or* other versions are existing
        Result<CompositionHistoryRecord> historyRecords = domainAccess.getContext()
                .selectFrom(COMPOSITION_HISTORY)
                .where(COMPOSITION_HISTORY.ID.eq(compositionId))
                .orderBy(COMPOSITION_HISTORY.SYS_TRANSACTION.desc())
                .fetch();

        for (CompositionHistoryRecord historyRecord : historyRecords) {
            I_CompositionAccess historyAccess = new CompositionAccess(domainAccess);
            historyAccess.setCompositionRecord(historyRecord);
            historyAccess.setContent(I_EntryAccess.retrieveInstanceInComposition(domainAccess, historyAccess));
            versionMap.put(historyAccess, versionCounter);
            versionCounter--;
        }

        if (versionCounter != 0)
            throw new InternalServerException("Version Map generation failed");

        return versionMap;
    }

    /**
     * Decode composer ID
     *
     * @param composer given {@link PartyProxy}
     * @return ID of composer as {@link UUID}
     * @throws IllegalArgumentException when composer in composition is not supported
     */
    private UUID seekComposerId(PartyProxy composer) {
        return new PersistedPartyProxy(this).getOrCreate(composer);
    }

    /**
     * Decode territory code
     *
     * @param territoryCode String representation of territory code
     * @return territory code as Integer
     * @throws IllegalArgumentException when Invalid two letter territory code
     */
    private Integer seekTerritoryCode(String territoryCode) {
        Integer foundTerritoryCode = I_CompositionAccess.fetchTerritoryCode(this, territoryCode);

        if (foundTerritoryCode < 0)
            throw new IllegalArgumentException("Invalid two letter territory code");

        return foundTerritoryCode;
    }

    /**
     * Decode language code
     *
     * @param languageCode String representation of language code
     * @return language code as String
     * @throws IllegalArgumentException when Invalid language code
     */
    private String seekLanguageCode(String languageCode) {
        if (languageCode == null) //defaulted to english
            return "en";
        else if (!(I_CompositionAccess.isValidLanguageCode(this, languageCode)))
            throw new IllegalArgumentException("Invalid language code");

        return languageCode;
    }

    @Override
    public UUID getEhrid() {
        return compositionRecord.getEhrId();
    }

    @Override
    public void setEhrid(UUID ehrId) {
        compositionRecord.setEhrId(ehrId);
    }

    @Override
    public UUID getComposerId() {
        return compositionRecord.getComposer();
    }

    @Override
    public void setComposerId(UUID composerId) {
        compositionRecord.setComposer(composerId);
    }

    @Override
    public Optional<UUID> getContextId() {
        if (compositionRecord == null)
            return Optional.empty();
        if (compositionRecord.getId() == null)
            return Optional.empty();
        // conditional handling for persistent composition that do not have a event context
        EventContextRecord eventContext = getContext().fetchOne(EVENT_CONTEXT, EVENT_CONTEXT.COMPOSITION_ID.eq(compositionRecord.getId()));
        if (eventContext == null) {
            return Optional.empty();
        }
        return Optional.of(eventContext.getId());
    }

    @Override
    public UUID getContributionId() {
        return compositionRecord.getInContribution();
    }

    @Override
    public String getLanguageCode() {
        return compositionRecord.getLanguage();
    }

    @Override
    public void setLanguageCode(String code) {
        compositionRecord.setLanguage(code);
    }

    @Override
    public Integer getTerritoryCode() {
        return compositionRecord.getTerritory();
    }

    @Override
    public void setTerritoryCode(Integer code) {
        compositionRecord.setTerritory(code);
    }

    @Override
    public List<I_EntryAccess> getContent() {
        return this.content;
    }

    @Override
    public void setContent(List<I_EntryAccess> content) {
        this.content = content;
    }

    @Override
    public UUID getId() {
        return compositionRecord.getId();
    }

    /**
     * @throws InternalServerException on problem updating context
     */
    @Override
    public void setContextCompositionId(UUID contextId) {
        I_ContextAccess contextAccess = I_ContextAccess.retrieveInstance(this, contextId);
        contextAccess.setCompositionId(compositionRecord.getId());
        contextAccess.update(Timestamp.valueOf(LocalDateTime.now()));
    }

    @Override
    public void setContributionId(UUID contributionId) {
        compositionRecord.setInContribution(contributionId);
    }

    @Override
    public void setCompositionRecord(CompositionRecord record) {
        this.compositionRecord = record;
    }

    @Override
    public void setCompositionRecord(CompositionHistoryRecord historyRecord) {
        CompositionRecord compositionRecord = new CompositionRecord(
                historyRecord.getId(),
                historyRecord.getEhrId(),
                historyRecord.getInContribution(),
                historyRecord.getActive(),
                historyRecord.getIsPersistent(),
                historyRecord.getLanguage(),
                historyRecord.getTerritory(),
                historyRecord.getComposer(),
                null,
                null,
                historyRecord.getHasAudit(),
                null
        );
        this.compositionRecord = compositionRecord;
    }

    /**
     * @throws IllegalArgumentException when handling of record failed
     */
    @Override
    public void setCompositionRecord(Result<?> records) {
        compositionRecord = getContext().newRecord(compositionRef);
        try {
            compositionRecord.setId((UUID) records.getValue(0, F_COMPOSITION_ID));
            compositionRecord.setLanguage((String) records.getValue(0, F_LANGUAGE));
        } catch (IndexOutOfBoundsException e) {     // generalize DB exceptions
            throw new IllegalArgumentException("Handling of records failed", e);
        }
//        compositionRecord.setTerritory((Integer)records.getValue(0, F_TERRITORY_CODE));
    }

    @Override
    public void setComposition(Composition composition) {
        this.composition = composition;
    }

    @Override
    public int addContent(I_EntryAccess entry) {
        entry.setCompositionId(compositionRecord.getId());
        content.add(entry);

        if (entry.getComposition() != null)
            composition = entry.getComposition();

        return content.size();
    }

    @Override
    public List<UUID> getContentIds() {
        List<UUID> entryList = new ArrayList<>();

        for (I_EntryAccess entryAccess : content) {
            entryList.add(entryAccess.getId());
        }

        return entryList;
    }

    /**
     * Commit composition (incl embedded audit).
     * The composition' contribution (incl. its audit) and the composition's own audit are assumed to be prepared correctly before executing this commitment.
     *
     * @throws IllegalArgumentException when content couldn't be committed or requirements aren't met
     */
    @Override   // root commit
    public UUID commit(Timestamp transactionTime) {

        // create DB entry of prepared auditDetails so it can get referenced in this composition
        auditDetailsAccess.setChangeType(I_ConceptAccess.fetchContributionChangeType(this, I_ConceptAccess.ContributionChangeType.CREATION));
        UUID auditId = this.auditDetailsAccess.commit();
        compositionRecord.setHasAudit(auditId);

        // check if custom contribution is already set, because changing it would yield updating in DB which is not desired (creates wrong new "version")
        if (getContributionId() != null) {
            // check if set contribution is sane
            Optional.ofNullable(I_ContributionAccess.retrieveInstance(this, getContributionId())).orElseThrow(IllegalArgumentException::new);
        } else {
            // if not set, create DB entry of contribution so it can get referenced in this composition
            contributionAccess.setAuditDetailsChangeType(I_ConceptAccess.fetchContributionChangeType(this, I_ConceptAccess.ContributionChangeType.CREATION));
            UUID contributionId = this.contributionAccess.commit();
            setContributionId(contributionId);
        }

        compositionRecord.setSysTransaction(transactionTime);
        compositionRecord.store();

        if (content.isEmpty())
            log.warn("Composition has no content:");

        try {
            for (I_EntryAccess entryAccess : content)
                entryAccess.commit(transactionTime);
        } catch (Exception exception) {
            log.error("Problem in committing content, rolling back, exception:" + exception);
            throw new IllegalArgumentException("Could not commit content:" + exception);
        }

        if (!composition.getCategory().getDefiningCode().getCodeString().equals("431")) {
            EventContext eventContext = composition.getContext();
            I_ContextAccess contextAccess = I_ContextAccess.getInstance(this, eventContext);
            contextAccess.setCompositionId(compositionRecord.getId());
            contextAccess.commit(transactionTime);
        }
        return compositionRecord.getId();
    }

    /**
     * @deprecated
     */
    @Deprecated
    @Override
    public UUID commit() {
        // audit details need some data like systemId and committerId, so this won't do
        throw new UnsupportedOperationException("this commit() signature is not supported");
    }

    @Override
    public UUID commit(UUID committerId, UUID systemId, String description) {
        Timestamp timestamp = Timestamp.valueOf(LocalDateTime.now());
        // prepare contribution with given values
        contributionAccess.setDataType(ContributionDataType.composition);
        contributionAccess.setState(ContributionDef.ContributionState.COMPLETE);
        contributionAccess.setAuditDetailsValues(committerId, systemId, description);
        // prepare composition audit with given values
        auditDetailsAccess.setSystemId(systemId);
        auditDetailsAccess.setCommitter(committerId);
        auditDetailsAccess.setDescription(description);

        return commit(timestamp);
    }

    // commit with composition that has contribution already set with setContributionId(...) beforehand
    @Override
    public UUID commitWithCustomContribution(UUID committerId, UUID systemId, String description) {
        // prepare composition audit with given values
        auditDetailsAccess.setSystemId(systemId);
        auditDetailsAccess.setCommitter(committerId);
        auditDetailsAccess.setDescription(description);

        Timestamp timestamp = Timestamp.valueOf(LocalDateTime.now());
        return commit(timestamp);
    }

    @Override
    public Boolean update(Timestamp transactionTime) {
        return update(transactionTime, false);
    }

    @Override       // root update
    public Boolean update(Timestamp transactionTime, boolean force) {
        boolean result = false;

        if (force || compositionRecord.changed()) {
            //we assume the composition has been amended locally

            if (!compositionRecord.changed()) {
                compositionRecord.changed(true);
                //jOOQ limited support of TSTZRANGE, exclude sys_period from updateComposition!
                compositionRecord.changed(COMPOSITION.SYS_PERIOD, false);
            }

            compositionRecord.setSysTransaction(transactionTime);

            //update attributes
            updateCompositionData(composition);

            result = compositionRecord.update() > 0;

            //updateComposition each entry if required
            for (I_EntryAccess entryAccess : content) {
                entryAccess.setCompositionData(composition);
                entryAccess.update(transactionTime, true);
            }

            //update context
            //context
            Optional<UUID> contextId = getContextId();
            I_ContextAccess contextAccess;

            if (contextId.isEmpty()){
                EventContext context = new EventContextFactory().makeNull();
                contextAccess = I_ContextAccess.getInstance(this, context);
                contextAccess.commit(transactionTime);
            }
            else
                contextAccess = I_ContextAccess.retrieveInstance(this, contextId.get());

            EventContext newEventContext = composition.getContext();

            if (contextId.isPresent()) {
                contextAccess.setRecordFields(contextId.get(), newEventContext);
                contextAccess.update(transactionTime, true);
            }
        }

        return result;
    }

    @Override
    public Boolean update() {
        Timestamp timestamp = Timestamp.valueOf(LocalDateTime.now());
        // update both contribution (incl its audit) and the composition's own audit
        contributionAccess.update(timestamp, null, null, null, null, I_ConceptAccess.ContributionChangeType.MODIFICATION, null);
        auditDetailsAccess.update(null, null, I_ConceptAccess.ContributionChangeType.MODIFICATION, null);
        return update(timestamp);
    }

    @Override
    public Boolean update(Boolean force) {
        Timestamp timestamp = Timestamp.valueOf(LocalDateTime.now());
        // update both contribution (incl its audit) and the composition's own audit
        contributionAccess.update(timestamp, null, null, null, null, I_ConceptAccess.ContributionChangeType.MODIFICATION, null);
        auditDetailsAccess.update(null, null, I_ConceptAccess.ContributionChangeType.MODIFICATION, null);
        return update(timestamp, force);
    }

    @Override
    public Boolean update(UUID committerId, UUID systemId, ContributionDef.ContributionState state, I_ConceptAccess.ContributionChangeType contributionChangeType, String description) {
        Timestamp timestamp = Timestamp.valueOf(LocalDateTime.now());
        // update both contribution (incl its audit) and the composition's own audit
        contributionAccess.update(timestamp, committerId, systemId, null, state, contributionChangeType, description);
        auditDetailsAccess.update(systemId, committerId, contributionChangeType, description);
        return update(timestamp, true);    // TODO is forcing necessary and if so, is it also okay?
    }

    @Override
    public Boolean updateWithCustomContribution(UUID committerId, UUID systemId, I_ConceptAccess.ContributionChangeType contributionChangeType, String description) {
        Timestamp timestamp = Timestamp.valueOf(LocalDateTime.now());

        // update only the audit, so it shows the modification change type. a new custom contribution is set beforehand.
        // TODO: db-wise, this way a new audit "version" will be created which is (openEHR-)semantically wrong. but safe and processable anyway. so need to change that or is it okay?
        auditDetailsAccess.update(systemId, committerId, contributionChangeType, description);
        return update(timestamp);
    }

    @Override
    public Integer delete() {

        return delete(null, null, null);

    }

    @Override
    public Integer delete(UUID committerId, UUID systemId, String description) {
        // .delete() moves the old version to _history table.
        int delRows = compositionRecord.delete();

        // create new deletion audit
        I_AuditDetailsAccess delAudit = I_AuditDetailsAccess.getInstance(this, systemId, committerId, I_ConceptAccess.ContributionChangeType.DELETED, description);
        UUID delAuditId = delAudit.commit();

        // create new contribution for this deletion action (with embedded contribution.audit handling)
        contributionAccess = I_ContributionAccess.getInstance(getDataAccess(), contributionAccess.getEhrId()); // overwrite old contribution with new one
        UUID contrib = contributionAccess.commit(Timestamp.valueOf(LocalDateTime.now()), committerId, systemId, null, ContributionDef.ContributionState.COMPLETE, I_ConceptAccess.ContributionChangeType.DELETED, description);

        // create new, BUT already moved to _history, version documenting the deletion
        createAndCommitNewDeletedVersionAsHistory(delAuditId, contrib);

        return delRows;
    }

    @Override
    public Integer deleteWithCustomContribution(UUID committerId, UUID systemId, String description) {
        // .delete() moves the old version to _history table.
        int delRows = compositionRecord.delete();

        // create new deletion audit
        I_AuditDetailsAccess delAudit = I_AuditDetailsAccess.getInstance(this, systemId, committerId, I_ConceptAccess.ContributionChangeType.DELETED, description);
        UUID delAuditId = delAudit.commit();

        // create new, BUT already moved to _history, version documenting the deletion
        createAndCommitNewDeletedVersionAsHistory(delAuditId, compositionRecord.getInContribution());

        return delRows;
    }

    private void createAndCommitNewDeletedVersionAsHistory(UUID delAuditId, UUID contrib) {
        // a bit hacky: create new, BUT already moved to _history, version documenting the deletion
        // (Normal approach of first .update() then .delete() won't work, because postgres' transaction optimizer will
        // just skip the update if it will get deleted anyway.)
        // so copy values, but add deletion meta data
        CompositionHistoryAccess newDeletedVersionAsHistoryAccess = new CompositionHistoryAccess(getDataAccess());
        CompositionHistoryRecord newRecord = getDataAccess().getContext().newRecord(COMPOSITION_HISTORY);
        newRecord.setId(compositionRecord.getId());
        newRecord.setEhrId(compositionRecord.getEhrId());
        newRecord.setInContribution(contrib);
        newRecord.setActive(compositionRecord.getActive());
        newRecord.setIsPersistent(compositionRecord.getIsPersistent());
        newRecord.setLanguage(compositionRecord.getLanguage());
        newRecord.setTerritory(compositionRecord.getTerritory());
        newRecord.setComposer(compositionRecord.getComposer());
        newRecord.setHasAudit(delAuditId);
        newDeletedVersionAsHistoryAccess.setRecord(newRecord);
        if (newDeletedVersionAsHistoryAccess.commit() == null) // commit and throw error if nothing was inserted into DB
            throw new InternalServerException("DB inconsistency");
    }

    @Override
    public Timestamp getSysTransaction() {
        return compositionRecord.getSysTransaction();
    }

    public Timestamp getTimeCommitted() {
        return auditDetailsAccess.getTimeCommitted();
    }

    @Override
    public void setContributionAccess(I_ContributionAccess contributionAccess) {
        this.contributionAccess = contributionAccess;
    }

    @Override
    public void setAuditDetailsAccess(I_AuditDetailsAccess auditDetailsAccess) {
        this.auditDetailsAccess = auditDetailsAccess;
    }

    @Override
    public Integer getVersion() {
        //default current version, no history   // FIXME
        Integer version = 1;
        return version;
    }

    /**
     * @throws IllegalArgumentException when seeking language code, territory code or composer ID failed
     */
    @Override
    public void updateCompositionData(Composition newComposition) {
        //update the mutable attributes
        setLanguageCode(seekLanguageCode(newComposition.getLanguage().getCodeString()));
        setTerritoryCode(seekTerritoryCode(newComposition.getTerritory().getCodeString()));
        setComposerId(seekComposerId(newComposition.getComposer()));
    }

    @Override
    public void setContext(EventContext eventContext) {
        composition.setContext(eventContext);
    }


    @Override
    public DataAccess getDataAccess() {
        return this;
    }

    @Override
    public UUID getAuditDetailsId() {
        return compositionRecord.getHasAudit();
    }

    @Override
    public void setAuditDetailsId(UUID auditId) {
        compositionRecord.setHasAudit(auditId);
    }

    public static boolean exists(I_DomainAccess domainAccess, UUID versionedObjectId) {
        if (domainAccess.getContext().fetchExists(COMPOSITION, COMPOSITION.ID.eq(versionedObjectId)) ||
            domainAccess.getContext().fetchExists(COMPOSITION_HISTORY, COMPOSITION_HISTORY.ID.eq(versionedObjectId))) {
            return true;
        } else {
            throw new ObjectNotFoundException("composition", "No composition with given ID found");
        }
    }

    public static boolean isDeleted(I_DomainAccess domainAccess, UUID versionedObjectId) {
        // meta: logically deleted means that of this ID only entries in the history table are available

        // if available in normal table -> not deleted
        if (domainAccess.getContext().fetchExists(COMPOSITION, COMPOSITION.ID.eq(versionedObjectId)))
            return false;

        // if only in history table
        if (domainAccess.getContext().fetchExists(COMPOSITION_HISTORY, COMPOSITION_HISTORY.ID.eq(versionedObjectId))) {
            // retrieve the record
            Result<CompositionHistoryRecord> historyRecordsRes = domainAccess.getContext()
                    .selectFrom(COMPOSITION_HISTORY)
                    .where(COMPOSITION_HISTORY.ID.eq(versionedObjectId))
                    .orderBy(COMPOSITION_HISTORY.SYS_TRANSACTION.desc())    // latest at top, i.e. [0]
                    .fetch();
            // assumed not empty, because fetchExists was successful

            // retrieve matching audit
            AuditDetailsRecord audit = domainAccess.getContext().fetchOne(AUDIT_DETAILS, AUDIT_DETAILS.ID.eq(historyRecordsRes.get(0).getHasAudit()));
            if (audit == null)
                throw new InternalServerException("DB inconsistency: couldn't retrieve referenced audit");
            // and check for correct change type -> is deleted
            if (audit.getChangeType().equals(ContributionChangeType.deleted))
                return true;
        } else {
            throw new ObjectNotFoundException("composition", "No composition with given ID found");
        }
        // TODO: why is this line necessary? won't compile without return or throw. but if/else above is always reaching a return/throw anyway!?
        throw new InternalServerException("Problem processing CompositionAccess.isDeleted(..)");
    }
}
