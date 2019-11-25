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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ehrbase.api.definitions.ServerConfig;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.dao.access.interfaces.*;
import org.ehrbase.dao.access.support.DataAccess;
import org.ehrbase.dao.access.util.ContributionDef;
import org.ehrbase.ehr.knowledge.I_KnowledgeCache;
import org.ehrbase.jooq.pg.enums.ContributionDataType;
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
        super(domainAccess.getContext(), domainAccess.getKnowledgeManager(), domainAccess.getIntrospectService(), domainAccess.getServerConfig());

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

    public CompositionAccess(I_DomainAccess domainAccess) {
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
            I_ContributionAccess contributionAccess = I_ContributionAccess.retrieveVersionedInstance(domainAccess, compositionHistoryAccess.getContributionVersionId(), compositionHistoryAccess.getSysTransaction());
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

//        connection.close();

        return compositionHistoryAccess;
    }

    public static Integer getLastVersionNumber(I_DomainAccess domainAccess, UUID compositionId) {

        if (!hasPreviousVersion(domainAccess, compositionId))
            return 1;

        int versionCount = domainAccess.getContext().fetchCount(COMPOSITION_HISTORY, COMPOSITION_HISTORY.ID.eq(compositionId));

        return versionCount + 1;
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
        I_ContributionAccess contributionAccess = I_ContributionAccess.retrieveInstance(domainAccess, compositionAccess.getContributionVersionId());
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

    /**
     * @throws IllegalArgumentException on DB inconsistency
     */
    // TODO retrieve version without using the version parameter?! appears actually not to deal with versioning at all (contributions don't have versions in openEHR sense, but *_history table for audit)
    public static Map<UUID, I_CompositionAccess> retrieveCompositionsInContributionVersion(I_DomainAccess domainAccess, UUID contribution, Integer versionNumber) {
        Map<UUID, I_CompositionAccess> compositions = new HashMap<>();

        try {
            domainAccess.getContext()
                    .selectFrom(COMPOSITION)
                    .where(COMPOSITION.IN_CONTRIBUTION.eq(contribution))
                    .fetch()
                    .forEach(record -> {
                        I_CompositionAccess compositionAccess = new CompositionAccess(domainAccess);
                        compositionAccess.setCompositionRecord(record);
                        compositionAccess.setContent(I_EntryAccess.retrieveInstanceInComposition(domainAccess, compositionAccess));
//                        compositionAccess.setCommitted(true);
                        compositions.put(compositionAccess.getId(), compositionAccess);

                    });
        } catch (Exception e) {
            log.error("DB inconsistency:" + e);
            throw new IllegalArgumentException("DB inconsistency:" + e);
        }
        return compositions;
    }

    /**
     * Decode composer ID
     *
     * @param composer given {@link PartyProxy}
     * @return ID of composer as {@link UUID}
     * @throws IllegalArgumentException when composer in composition is not supported
     */
    private UUID seekComposerId(PartyProxy composer) {
        if (!(composer instanceof PartyIdentified))
            throw new IllegalArgumentException("Composer found in composition is not an IdenfiedParty and is not supported:" + composer.toString());

        return I_PartyIdentifiedAccess.getOrCreateParty(this, (PartyIdentified) composer);
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
    public UUID getContributionVersionId() {
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
        if (getContributionVersionId() != null) {
            // check if set contribution is sane
            Optional.ofNullable(I_ContributionAccess.retrieveInstance(this, getContributionVersionId())).orElseThrow(IllegalArgumentException::new);
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
            result = compositionRecord.update() > 0;

            //updateComposition each entry if required
            for (I_EntryAccess entryAccess : content) {
                entryAccess.update(transactionTime, true);
            }
        }

        //updateComposition event context accordingly, if composition is not persistent (i.e. has a context)
        getContextId().ifPresent(id -> I_ContextAccess.retrieveInstance(this, id).update(transactionTime, force));

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

        Timestamp timestamp = Timestamp.valueOf(LocalDateTime.now());
        // update contribution (with embedded contribution.audit handling)
        contributionAccess.update(timestamp, committerId, systemId, null, ContributionDef.ContributionState.DELETED, I_ConceptAccess.ContributionChangeType.DELETED, description);
        // update this composition's audit
        auditDetailsAccess.update(systemId, committerId, I_ConceptAccess.ContributionChangeType.DELETED, description);
        return compositionRecord.delete();
    }

    @Override
    public Integer deleteWithCustomContribution(UUID committerId, UUID systemId, String description) {
        // update only the audit, so it shows the modification change type. a new custom contribution is set beforehand.
        // TODO: db-wise, this way a new audit "version" will be created which is (openEHR-)semantically wrong. but safe and processable anyway. so need to change that or is it okay?
        auditDetailsAccess.update(systemId, committerId, I_ConceptAccess.ContributionChangeType.DELETED, description);

        return compositionRecord.delete();
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
}
