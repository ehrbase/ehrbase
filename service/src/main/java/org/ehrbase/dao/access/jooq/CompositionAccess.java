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

import static org.ehrbase.jooq.pg.Tables.AUDIT_DETAILS;
import static org.ehrbase.jooq.pg.Tables.COMPOSITION;
import static org.ehrbase.jooq.pg.Tables.COMPOSITION_HISTORY;
import static org.ehrbase.jooq.pg.Tables.EVENT_CONTEXT;
import static org.jooq.impl.DSL.count;
import static org.jooq.impl.DSL.max;

import com.nedap.archie.rm.archetyped.FeederAudit;
import com.nedap.archie.rm.archetyped.Link;
import com.nedap.archie.rm.composition.Composition;
import com.nedap.archie.rm.composition.EventContext;
import com.nedap.archie.rm.generic.PartyProxy;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.ehrbase.api.definitions.ServerConfig;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.dao.access.interfaces.I_AuditDetailsAccess;
import org.ehrbase.dao.access.interfaces.I_CompositionAccess;
import org.ehrbase.dao.access.interfaces.I_ConceptAccess;
import org.ehrbase.dao.access.interfaces.I_ContextAccess;
import org.ehrbase.dao.access.interfaces.I_ContributionAccess;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.dao.access.interfaces.I_EntryAccess;
import org.ehrbase.dao.access.jooq.party.PersistedPartyProxy;
import org.ehrbase.dao.access.support.DataAccess;
import org.ehrbase.dao.access.support.TenantSupport;
import org.ehrbase.dao.access.util.ContributionDef;
import org.ehrbase.dao.access.util.TransactionTime;
import org.ehrbase.ehr.knowledge.I_KnowledgeCache;
import org.ehrbase.jooq.dbencoding.rmobject.FeederAuditEncoding;
import org.ehrbase.jooq.dbencoding.rmobject.LinksEncoding;
import org.ehrbase.jooq.pg.enums.ContributionChangeType;
import org.ehrbase.jooq.pg.enums.ContributionDataType;
import org.ehrbase.jooq.pg.tables.records.AuditDetailsRecord;
import org.ehrbase.jooq.pg.tables.records.CompositionHistoryRecord;
import org.ehrbase.jooq.pg.tables.records.CompositionRecord;
import org.ehrbase.jooq.pg.tables.records.EventContextRecord;
import org.ehrbase.service.IntrospectService;
import org.ehrbase.util.PartyUtils;
import org.ehrbase.util.UuidGenerator;
import org.jooq.AggregateFunction;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.Param;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Result;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Operations on the static part of Compositions (eg. non archetyped
 * attributes).
 *
 * @author Christian Chevalley
 * @author Jake Smolka
 * @author Luis Marco-Ruiz
 * @since 1.0
 */
public class CompositionAccess extends DataAccess implements I_CompositionAccess {

    public static final String COMPOSITION_LITERAL = "composition";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private Composition composition;
    private CompositionRecord compositionRecord;

    private I_EntryAccess entryAccess; // Entry linked to this composition
    private I_ContributionAccess contributionAccess = null; // locally referenced contribution associated to this
    // composition
    private I_AuditDetailsAccess auditDetailsAccess; // audit associated with this composition

    /**
     * Basic constructor for composition.
     *
     * @param context          DB context object of current server context
     * @param knowledgeManager Knowledge cache object of current server context
     * @param introspectCache  Introspect cache object of current server context
     * @param serverConfig     Server config object of current server context
     * @param composition      Object representation of given new composition
     * @param ehrId            Given ID of EHR this composition will be created for
     * @throws IllegalArgumentException when seeking language code, territory code
     *                                  or composer ID failed
     */
    public CompositionAccess(
            DSLContext context,
            I_KnowledgeCache knowledgeManager,
            IntrospectService introspectCache,
            ServerConfig serverConfig,
            Composition composition,
            UUID ehrId,
            Short sysTenant) {
        super(context, knowledgeManager, introspectCache, serverConfig);

        initRecord(
                composition,
                context.newRecord(COMPOSITION),
                composition.getTerritory().getCodeString(),
                composition.getLanguage().getCodeString(),
                ehrId,
                sysTenant);
    }

    /**
     * Constructor with convenient {@link I_DomainAccess} parameter, for better
     * readability.
     *
     * @param domainAccess Current domain access object
     * @param composition  Object representation of given new composition
     * @param ehrId        Given ID of EHR this composition will be created for
     * @throws IllegalArgumentException when seeking language code, territory code
     *                                  or composer ID failed
     */
    public CompositionAccess(I_DomainAccess domainAccess, Composition composition, UUID ehrId, Short sysTenant) {
        super(domainAccess);

        initRecord(
                composition,
                domainAccess.getContext().newRecord(COMPOSITION),
                composition.getTerritory().getCodeString(),
                composition.getLanguage().getCodeString(),
                ehrId,
                sysTenant);
    }

    private void initRecord(
            Composition composition,
            CompositionRecord compositionRecord,
            String territoryCode,
            String languageCode,
            UUID ehrId,
            Short sysTenant) {
        this.compositionRecord = compositionRecord;
        this.composition = composition;

        if (composition.getUid() != null) {

            compositionRecord.setId(
                    UUID.fromString(composition.getUid().getRoot().getValue()));
        } else {
            compositionRecord.setId(UuidGenerator.randomUUID());
        }
        compositionRecord.setTerritory(seekTerritoryCode(territoryCode));
        compositionRecord.setLanguage(seekLanguageCode(languageCode));
        compositionRecord.setActive(true);
        compositionRecord.setEhrId(ehrId);
        compositionRecord.setComposer(seekComposerId(composition.getComposer(), sysTenant));
        compositionRecord.setSysTenant(sysTenant);

        setFeederAudit(composition.getFeederAudit());
        setLinks(composition.getLinks());

        // associate a contribution with this composition
        contributionAccess = I_ContributionAccess.getInstance(this, compositionRecord.getEhrId(), sysTenant);
        contributionAccess.setState(ContributionDef.ContributionState.COMPLETE);

        // associate composition's own audit with this composition access instance
        auditDetailsAccess = I_AuditDetailsAccess.getInstance(getDataAccess(), sysTenant);
    }

    /**
     * constructor used to perform non static operation on instance creates new
     * instance with values from record and new empty contribution and audit
     *
     * @param domainAccess      SQL context
     * @param compositionRecord record representation of composition
     */
    private CompositionAccess(I_DomainAccess domainAccess, CompositionRecord compositionRecord) {
        super(domainAccess);

        this.compositionRecord = compositionRecord;
        contributionAccess =
                I_ContributionAccess.getInstance(this, compositionRecord.getEhrId(), compositionRecord.getSysTenant());
        contributionAccess.setState(ContributionDef.ContributionState.COMPLETE);

        // associate composition's own audit with this composition access instance
        auditDetailsAccess =
                I_AuditDetailsAccess.getInstance(this.getDataAccess(), this.compositionRecord.getSysTenant());
    }

    CompositionAccess(I_DomainAccess domainAccess) {
        super(domainAccess);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UUID commit(LocalDateTime timestamp, UUID committerId, UUID systemId, String description) {
        return internalCreate(timestamp, committerId, systemId, description, null, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UUID commit(LocalDateTime timestamp, UUID contribution, UUID audit) {
        return internalCreate(timestamp, null, null, null, contribution, audit);
    }

    private UUID internalCreate(
            LocalDateTime timestamp,
            UUID committerId,
            UUID systemId,
            String description,
            UUID contribution,
            UUID audit) {

        // check if custom contribution is already set, because changing it would yield
        // updating in DB which is not
        // desired (creates wrong new "version")
        if (contribution != null) {
            // Retrieve audit metadata from given contribution
            var newContributionAccess = I_ContributionAccess.retrieveInstance(this.getDataAccess(), contribution);
            systemId = newContributionAccess.getAuditsSystemId();
            committerId = newContributionAccess.getAuditsCommitter();
            description = newContributionAccess.getAuditsDescription();
        } else {
            // if not set, create DB entry of contribution so it can get referenced in this
            // composition
            // prepare contribution with given values
            contributionAccess.setDataType(ContributionDataType.composition);
            contributionAccess.setState(ContributionDef.ContributionState.COMPLETE);
            contributionAccess.setAuditDetailsValues(
                    committerId, systemId, description, I_ConceptAccess.ContributionChangeType.CREATION);

            UUID contributionId = this.contributionAccess.commit();
            setContributionId(contributionId);
        }

        if (audit == null) {
            // create DB entry of prepared auditDetails so it can get referenced in this
            // composition
            auditDetailsAccess.setChangeType(
                    I_ConceptAccess.fetchContributionChangeType(this, I_ConceptAccess.ContributionChangeType.CREATION));
            // prepare composition audit with given values
            auditDetailsAccess.setSystemId(systemId);
            auditDetailsAccess.setCommitter(committerId);
            auditDetailsAccess.setDescription(description);
            UUID auditId = this.auditDetailsAccess.commit();
            compositionRecord.setHasAudit(auditId);
        } else {
            compositionRecord.setHasAudit(audit);
        }

        compositionRecord.setSysTransaction(Timestamp.valueOf(timestamp));
        compositionRecord.store();

        if (entryAccess != null) {
            try {
                entryAccess.commit(Timestamp.valueOf(timestamp));
            } catch (Exception exception) {
                throw new IllegalArgumentException("Could not commit content:" + exception);
            }
        } else {
            logger.warn("Composition has no entry");
        }

        if (!composition.getCategory().getDefiningCode().getCodeString().equals("431")) {
            EventContext eventContext = composition.getContext();
            I_ContextAccess contextAccess =
                    I_ContextAccess.getInstance(this, eventContext, compositionRecord.getSysTenant());
            if (!contextAccess.isVoid()) {
                contextAccess.setCompositionId(compositionRecord.getId());
                contextAccess.commit(Timestamp.valueOf(timestamp));
            }
        }
        return compositionRecord.getId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean update(
            LocalDateTime timestamp,
            UUID committerId,
            UUID systemId,
            String description,
            I_ConceptAccess.ContributionChangeType changeType) {
        // create new contribution (and its audit) for this operation
        contributionAccess = new ContributionAccess(this, getEhrid(), compositionRecord.getSysTenant());
        contributionAccess.setDataType(ContributionDataType.composition);
        contributionAccess.setAuditDetailsValues(committerId, systemId, description, changeType);
        contributionAccess.setAuditDetailsChangeType(I_ConceptAccess.fetchContributionChangeType(this, changeType));
        UUID contributionId = this.contributionAccess.commit();
        setContributionId(contributionId);
        // create new composition audit with given values
        auditDetailsAccess = new AuditDetailsAccess(this, this.compositionRecord.getSysTenant());
        auditDetailsAccess.setSystemId(systemId);
        auditDetailsAccess.setCommitter(committerId);
        auditDetailsAccess.setDescription(description);
        auditDetailsAccess.setChangeType(I_ConceptAccess.fetchContributionChangeType(this, changeType));
        UUID auditID = this.auditDetailsAccess.commit();
        setAuditDetailsId(auditID);

        return internalUpdate(Timestamp.valueOf(timestamp));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean update(LocalDateTime timestamp, UUID contribution, UUID audit) {

        if (audit == null) {
            // Retrieve audit metadata from given contribution
            var newContributionAccess = I_ContributionAccess.retrieveInstance(this.getDataAccess(), contribution);
            UUID systemId = newContributionAccess.getAuditsSystemId();
            UUID committerId = newContributionAccess.getAuditsCommitter();
            String description = newContributionAccess.getAuditsDescription();
            I_ConceptAccess.ContributionChangeType changeType = newContributionAccess.getAuditsChangeType();

            // update only the audit (i.e. commit new one), so it shows the modification
            // change type. a new custom
            // contribution is set beforehand.
            auditDetailsAccess.update(systemId, committerId, changeType, description);
        } else {
            compositionRecord.setHasAudit(audit);
        }
        return internalUpdate(Timestamp.valueOf(timestamp));
    }

    // root update
    boolean internalUpdate(Timestamp transactionTime) {
        var result = false;

        // we assume the composition has been amended locally

        if (!compositionRecord.changed()) {
            compositionRecord.changed(true);
            // jOOQ limited support of TSTZRANGE, exclude sys_period from updateComposition!
            compositionRecord.changed(COMPOSITION.SYS_PERIOD, false);
        }

        compositionRecord.setSysTransaction(transactionTime);

        // update attributes
        updateCompositionData(composition);

        result = compositionRecord.update() > 0;

        // Update entry
        if (entryAccess != null) {
            entryAccess.setCompositionData(composition);
            entryAccess.update(transactionTime, true);
        }

        // update context
        // context
        Optional<UUID> contextId = getContextId();
        I_ContextAccess contextAccess;

        if (contextId.isEmpty()) {
            EventContext context = new EventContextFactory().makeNull();
            contextAccess = I_ContextAccess.getInstance(this, context, compositionRecord.getSysTenant());
            contextAccess.commit(transactionTime);
        } else {
            contextAccess = I_ContextAccess.retrieveInstance(this, contextId.get());
        }

        var newEventContext = composition.getContext();

        if (contextId.isPresent()) {
            contextAccess.setRecordFields(contextId.get(), newEventContext, compositionRecord.getSysTenant());
            contextAccess.update(transactionTime, true);
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int delete(LocalDateTime timestamp, UUID committerId, UUID systemId, String description) {
        // .delete() moves the old version to _history table.
        int delRows = compositionRecord.delete();

        // create new deletion audit
        var delAudit = I_AuditDetailsAccess.getInstance(
                this,
                systemId,
                committerId,
                I_ConceptAccess.ContributionChangeType.DELETED,
                description,
                compositionRecord.getSysTenant());
        UUID delAuditId = delAudit.commit();

        // create new contribution for this deletion action (with embedded
        // contribution.audit handling),  overwrite old contribution with new one
        contributionAccess = I_ContributionAccess.getInstance(
                getDataAccess(), contributionAccess.getEhrId(), compositionRecord.getSysTenant());
        UUID contrib = contributionAccess.commit(
                TransactionTime.millis(),
                committerId,
                systemId,
                null,
                ContributionDef.ContributionState.COMPLETE,
                I_ConceptAccess.ContributionChangeType.DELETED,
                description);

        // create new, BUT already moved to _history, version documenting the deletion
        createAndCommitNewDeletedVersionAsHistory(delAuditId, contrib);

        return delRows;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int delete(LocalDateTime timestamp, UUID contribution, UUID audit) {
        // Retrieve audit metadata from given contribution
        var newContributionAccess = I_ContributionAccess.retrieveInstance(this.getDataAccess(), contribution);
        UUID systemId = newContributionAccess.getAuditsSystemId();
        UUID committerId = newContributionAccess.getAuditsCommitter();
        String description = newContributionAccess.getAuditsDescription();

        // .delete() moves the old version to _history table.
        int delRows = compositionRecord.delete();

        if (audit == null) {
            // create new deletion audit
            var delAudit = I_AuditDetailsAccess.getInstance(
                    this,
                    systemId,
                    committerId,
                    I_ConceptAccess.ContributionChangeType.DELETED,
                    description,
                    this.compositionRecord.getSysTenant());
            audit = delAudit.commit();
        }

        // create new, BUT already moved to _history, version documenting the deletion
        createAndCommitNewDeletedVersionAsHistory(audit, compositionRecord.getInContribution());

        return delRows;
    }

    private void createAndCommitNewDeletedVersionAsHistory(UUID delAuditId, UUID contrib) {
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
        newRecord.setSysTenant(compositionRecord.getSysTenant());

        // a bit hacky: create new, BUT already moved to _history, version documenting
        // the deletion
        // (Normal approach of first .update() then .delete() won't work, because
        // postgres' transaction optimizer will
        // just skip the update if it will get deleted anyway.)
        // so copy values, but add deletion meta data
        var newDeletedVersionAsHistoryAccess =
                new CompositionHistoryAccess(getDataAccess(), compositionRecord.getSysTenant());
        newDeletedVersionAsHistoryAccess.setRecord(newRecord);
        if (newDeletedVersionAsHistoryAccess.commit() == null) // commit and throw error if nothing was inserted into DB
        {
            throw new InternalServerException("DB inconsistency");
        }
    }

    private static final String VERSION_QUERY =
            "SELECT row_id, in_contribution, ehr_id, language, territory, composer, sys_transaction, has_audit, attestation_ref, feeder_audit, links, sys_tenant from \n"
                    + "(SELECT ROW_NUMBER() OVER (ORDER BY sys_transaction ASC ) AS row_id, * FROM ehr.composition_history  WHERE id = ?) AS Version WHERE row_id = ?;";

    /**
     * @throws IllegalArgumentException when version number is not greater 0
     * @throws ObjectNotFoundException  when no composition could be found with
     *                                  given input
     */
    public static I_CompositionAccess retrieveCompositionVersion(I_DomainAccess domainAccess, UUID id, int version) {

        if (version < 1) {
            throw new IllegalArgumentException("Version number must be > 0  please check your code");
        }

        // check if this version number matches the current version
        if (getLastVersionNumber(domainAccess, id) == version) { // current version
            return retrieveInstance(domainAccess, id);
        }

        Connection connection = domainAccess.getConnection();

        I_CompositionAccess compositionHistoryAccess = null;
        try (PreparedStatement preparedStatement = connection.prepareStatement(VERSION_QUERY)) {
            preparedStatement.setObject(1, id);
            preparedStatement.setInt(2, version);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {

                while (resultSet.next()) {
                    CompositionRecord compositionRecord1 =
                            domainAccess.getContext().newRecord(COMPOSITION);
                    compositionRecord1.setId(id);
                    compositionRecord1.setInContribution(UUID.fromString(resultSet.getString("in_contribution")));
                    compositionRecord1.setEhrId(UUID.fromString(resultSet.getString("ehr_id")));
                    compositionRecord1.setLanguage(resultSet.getString("language"));
                    compositionRecord1.setTerritory(resultSet.getInt("territory"));
                    compositionRecord1.setComposer(UUID.fromString(resultSet.getString("composer")));
                    compositionRecord1.setSysTenant(resultSet.getShort("sys_tenant"));
                    compositionRecord1.setSysTransaction(resultSet.getTimestamp("sys_transaction"));
                    compositionRecord1.setHasAudit(UUID.fromString(resultSet.getString("has_audit")));
                    compositionRecord1.setFeederAudit(JSONB.valueOf(resultSet.getString("feeder_audit")));

                    /*
                     * TODO: uncomment when links encode/decode is fully implemented
                     * compositionRecord1.setLinks(JSONB.valueOf(resultSet.getString("links")));
                     */
                    compositionHistoryAccess = new CompositionAccess(domainAccess, compositionRecord1);
                }
            }
        } catch (SQLException e) {
            throw new ObjectNotFoundException(COMPOSITION_LITERAL, "Composition not found or or invalid DB content", e);
        }

        if (compositionHistoryAccess != null) {
            compositionHistoryAccess.setContent(I_EntryAccess.retrieveInstanceInCompositionVersion(
                    domainAccess, compositionHistoryAccess, version));

            // retrieve the corresponding contribution
            I_ContributionAccess contributionAccess =
                    I_ContributionAccess.retrieveInstance(domainAccess, compositionHistoryAccess.getContributionId());
            compositionHistoryAccess.setContributionAccess(contributionAccess);

            I_AuditDetailsAccess auditDetailsAccess = new AuditDetailsAccess(
                            domainAccess.getDataAccess(), TenantSupport.currentSysTenant())
                    .retrieveInstance(domainAccess.getDataAccess(), compositionHistoryAccess.getAuditDetailsId());
            compositionHistoryAccess.setAuditDetailsAccess(auditDetailsAccess);

            // retrieve versioned context
            EventContext historicalEventContext = I_ContextAccess.retrieveHistoricalEventContext(
                    domainAccess, id, compositionHistoryAccess.getSysTransaction());
            // adjust context for entries
            if (historicalEventContext != null) {
                I_EntryAccess entryAccess = compositionHistoryAccess.getContent();
                entryAccess.getComposition().setContext(historicalEventContext);
            }
        }

        domainAccess.releaseConnection(connection);

        return compositionHistoryAccess;
    }

    public static Integer getLastVersionNumber(I_DomainAccess domainAccess, UUID compositionId) {
        // check if compositionId is valid (version count = 1) and add number of existing older versions

        DSLContext ctx = domainAccess.getContext();
        Param<UUID> uuidParam = DSL.param("id", compositionId);
        Table<Record1<Integer>> unionAll = ctx.select(count(COMPOSITION.ID))
                .from(COMPOSITION)
                .where(COMPOSITION.ID.eq(uuidParam))
                .unionAll(ctx.select(count(COMPOSITION_HISTORY.ID))
                        .from(COMPOSITION_HISTORY)
                        .where(COMPOSITION_HISTORY.ID.eq(uuidParam)))
                .asTable("version_counts");

        AggregateFunction<BigDecimal> sum = DSL.sum(unionAll.field(0, Integer.class));

        int version = ctx.select(sum).from(unionAll).fetchOne(sum).intValue();

        return version;
    }

    public static boolean hasPreviousVersion(I_DomainAccess domainAccess, UUID compositionId) {
        return domainAccess.getContext().fetchExists(COMPOSITION_HISTORY, COMPOSITION_HISTORY.ID.eq(compositionId));
    }

    public static I_CompositionAccess retrieveInstance(I_DomainAccess domainAccess, UUID id) {
        I_CompositionAccess compositionAccess = new CompositionAccess(domainAccess);

        CompositionRecord compositionRecord = domainAccess
                .getContext()
                .selectFrom(COMPOSITION)
                .where(COMPOSITION.ID.eq(id))
                .fetchOne();

        if (compositionRecord == null) {
            return null;
        }

        compositionAccess.setCompositionRecord(compositionRecord);
        compositionAccess.setContent(I_EntryAccess.retrieveInstanceInComposition(domainAccess, compositionAccess));
        // retrieve the corresponding contribution
        I_ContributionAccess contributionAccess =
                I_ContributionAccess.retrieveInstance(domainAccess, compositionAccess.getContributionId());
        compositionAccess.setContributionAccess(contributionAccess);
        // retrieve corresponding audit
        I_AuditDetailsAccess auditAccess = new AuditDetailsAccess(
                        domainAccess.getDataAccess(), TenantSupport.currentSysTenant())
                .retrieveInstance(domainAccess.getDataAccess(), compositionAccess.getAuditDetailsId());
        compositionAccess.setAuditDetailsAccess(auditAccess);

        return compositionAccess;
    }

    /**
     * @throws ObjectNotFoundException when no version in compliance with timestamp
     *                                 is available
     * @throws InternalServerException on problem with SQL statement or input
     */
    public static int getVersionFromTimeStamp(
            I_DomainAccess domainAccess, UUID vCompositionUid, Timestamp timeCommitted) {

        if (timeCommitted == null) {
            return getLastVersionNumber(domainAccess, vCompositionUid);
        }
        // get the latest composition time (available in ehr.composition) table
        Record result;
        try {
            result = domainAccess
                    .getContext()
                    .select(max(COMPOSITION.SYS_TRANSACTION).as("mostRecentInTable"))
                    .from(COMPOSITION)
                    .where(COMPOSITION.ID.eq(vCompositionUid))
                    .fetchOne();
        } catch (RuntimeException e) { // generalize SQL exceptions
            throw new InternalServerException("Problem with SQL statement or input", e);
        }
        Timestamp latestCompoTime = (Timestamp) result.get("mostRecentInTable");

        // get the latest version (if more than one) time (available in
        // ehr.composition_history) table
        Record result2;
        try {
            result2 = domainAccess
                    .getContext()
                    .select(count().as("countVersionInTable"))
                    .from(COMPOSITION_HISTORY)
                    .where(COMPOSITION_HISTORY
                            .SYS_TRANSACTION
                            .lessOrEqual(timeCommitted)
                            .and(COMPOSITION_HISTORY.ID.eq(vCompositionUid)))
                    .fetchOne();
        } catch (RuntimeException e) { // generalize SQL exceptions
            throw new InternalServerException("Problem with SQL statement or input", e);
        }
        int versionComHist = (int) result2.get("countVersionInTable");
        if (timeCommitted.compareTo(latestCompoTime)
                >= 0) { // if the timestamp is after or equal to the sys_transaction of
            // the latest composition
            // available, add one since its version has not been counted for being the one
            // stored in the
            // ehr.composition table
            versionComHist++;
        }
        if (versionComHist == 0) {
            throw new ObjectNotFoundException(
                    "composition",
                    "There are no versions available prior to date " + timeCommitted
                            + " for the the composition with id: " + vCompositionUid);
        }
        return versionComHist;
    }

    /**
     * @throws IllegalArgumentException when no version in compliance with timestamp
     *                                  is available or when calculated version
     *                                  number is not greater 0
     * @throws InternalServerException  on problem with SQL statement or input
     * @throws ObjectNotFoundException  when no composition could be found with
     *                                  given input
     */
    public static I_CompositionAccess retrieveInstanceByTimestamp(
            I_DomainAccess domainAccess, UUID compositionUid, Timestamp timeCommitted) {

        int version = getVersionFromTimeStamp(domainAccess, compositionUid, timeCommitted);

        if (getLastVersionNumber(domainAccess, compositionUid) == version) { // current version
            return retrieveInstance(domainAccess, compositionUid);
        }

        return retrieveCompositionVersion(domainAccess, compositionUid, version);
    }

    public static Map<ObjectVersionId, I_CompositionAccess> retrieveCompositionsInContribution(
            I_DomainAccess domainAccess, UUID contribution, String node) {
        Set<UUID> compositions = new HashSet<>(); // Set, because of unique values
        // add all compositions having a link to given contribution
        domainAccess
                .getContext()
                .select(COMPOSITION.ID)
                .from(COMPOSITION)
                .where(COMPOSITION.IN_CONTRIBUTION.eq(contribution))
                .fetch()
                .forEach(rec -> compositions.add(rec.value1()));
        // and older versions or deleted ones, too
        domainAccess
                .getContext()
                .select(COMPOSITION_HISTORY.ID)
                .from(COMPOSITION_HISTORY)
                .where(COMPOSITION_HISTORY.IN_CONTRIBUTION.eq(contribution))
                .fetch()
                .forEach(rec -> compositions.add(rec.value1()));

        // get whole "version map" of each matching composition and do fine-grain check
        // for matching contribution
        // precondition: each UUID in `compositions` set is unique, so for each the
        // "version map" is only created once
        // below
        // (meta: can't do that as jooq query, because the specific version number isn't
        // stored in DB)
        Map<ObjectVersionId, I_CompositionAccess> resultMap = new HashMap<>();
        for (UUID compositionId : compositions) {
            Map<Integer, I_CompositionAccess> map = getVersionMapOfComposition(domainAccess, compositionId);
            // fine-grained contribution ID check
            map.forEach((k, v) -> {
                if (v.getContributionId().equals(contribution)) {
                    resultMap.put(new ObjectVersionId(compositionId.toString(), node, k.toString()), v);
                }
            });
        }

        return resultMap;
    }

    public static Map<Integer, I_CompositionAccess> getVersionMapOfComposition(
            I_DomainAccess domainAccess, UUID compositionId) {
        Map<Integer, I_CompositionAccess> versionMap = new HashMap<>();

        // create counter with highest version, to keep track of version number and
        // allow check in the end
        Integer versionCounter = getLastVersionNumber(domainAccess, compositionId);

        // fetch matching entry
        CompositionRecord compositionRecord =
                domainAccess.getContext().fetchOne(COMPOSITION, COMPOSITION.ID.eq(compositionId));
        if (compositionRecord != null) {
            I_CompositionAccess compositionAccess = new CompositionAccess(domainAccess);
            compositionAccess.setCompositionRecord(compositionRecord);
            compositionAccess.setContent(I_EntryAccess.retrieveInstanceInComposition(domainAccess, compositionAccess));
            versionMap.put(versionCounter, compositionAccess);

            versionCounter--;
        }

        // if composition was removed (i.e. from "COMPOSITION" table) *or* other
        // versions are existing
        Result<CompositionHistoryRecord> historyRecords = domainAccess
                .getContext()
                .selectFrom(COMPOSITION_HISTORY)
                .where(COMPOSITION_HISTORY.ID.eq(compositionId))
                .orderBy(COMPOSITION_HISTORY.SYS_TRANSACTION.desc())
                .fetch();

        for (CompositionHistoryRecord historyRecord : historyRecords) {
            I_CompositionAccess historyAccess = new CompositionAccess(domainAccess);
            historyAccess.setCompositionRecord(historyRecord);
            historyAccess.setContent(I_EntryAccess.retrieveInstanceInComposition(domainAccess, historyAccess));
            versionMap.put(versionCounter, historyAccess);
            versionCounter--;
        }

        if (versionCounter != 0) {
            throw new InternalServerException("Version Map generation failed");
        }

        return versionMap;
    }

    /**
     * Decode composer ID
     *
     * @param composer         given {@link PartyProxy}
     * @param sysTenant
     * @return ID of composer as {@link UUID}
     * @throws IllegalArgumentException when composer in composition is not
     *                                  supported
     */
    private UUID seekComposerId(PartyProxy composer, Short sysTenant) {
        if (PartyUtils.isEmpty(composer)) {
            return new PersistedPartyProxy(this).create(composer, sysTenant);
        } else {
            return new PersistedPartyProxy(this).getOrCreate(composer, sysTenant);
        }
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

        if (foundTerritoryCode < 0) {
            throw new IllegalArgumentException("Invalid two letter territory code");
        }

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
        if (languageCode == null) // defaulted to english
        {
            return "en";
        } else if (!(I_CompositionAccess.isValidLanguageCode(this, languageCode))) {
            throw new IllegalArgumentException("Invalid language code");
        }

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
        if (compositionRecord == null) {
            return Optional.empty();
        }
        if (compositionRecord.getId() == null) {
            return Optional.empty();
        }
        // conditional handling for persistent composition that do not have a event
        // context
        EventContextRecord eventContext =
                getContext().fetchOne(EVENT_CONTEXT, EVENT_CONTEXT.COMPOSITION_ID.eq(compositionRecord.getId()));
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
    public I_EntryAccess getContent() {
        return this.entryAccess;
    }

    @Override
    public void setContent(I_EntryAccess content) {
        this.entryAccess = content;

        if (content != null) {
            content.setCompositionId(compositionRecord.getId());
            composition = content.getComposition();
        }
    }

    @Override
    public UUID getId() {
        return compositionRecord.getId();
    }

    @Override
    public String getFeederAudit() {
        return compositionRecord.getFeederAudit() == null
                ? null
                : compositionRecord.getFeederAudit().toString();
    }

    @Override
    public String getLinks() {
        return compositionRecord.getLinks() == null
                ? null
                : compositionRecord.getLinks().toString();
    }

    @Override
    public void setFeederAudit(FeederAudit feederAudit) {
        if (feederAudit == null) {
            compositionRecord.setFeederAudit(null);
        } else {
            compositionRecord.setFeederAudit(JSONB.valueOf(new FeederAuditEncoding().toDB(feederAudit)));
        }
    }

    @Override
    public void setLinks(List<Link> links) {
        if (links == null) {
            compositionRecord.setLinks(null);
        } else {
            compositionRecord.setLinks(JSONB.valueOf(new LinksEncoding().toDB(links)));
        }
    }

    /**
     * @throws InternalServerException on problem updating context
     */
    @Override
    public void setContextCompositionId(UUID contextId) {
        I_ContextAccess contextAccess = I_ContextAccess.retrieveInstance(this, contextId);
        contextAccess.setCompositionId(compositionRecord.getId());
        contextAccess.update(TransactionTime.millis());
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
        this.compositionRecord = new CompositionRecord(
                historyRecord.getId(),
                historyRecord.getEhrId(),
                historyRecord.getInContribution(),
                historyRecord.getActive(),
                historyRecord.getIsPersistent(),
                historyRecord.getLanguage(),
                historyRecord.getTerritory(),
                historyRecord.getComposer(),
                historyRecord.getSysTransaction(),
                historyRecord.getSysPeriod(),
                historyRecord.getHasAudit(),
                historyRecord.getAttestationRef(),
                historyRecord.getFeederAudit(),
                historyRecord.getLinks(),
                historyRecord.getSysTenant());
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
        } catch (IndexOutOfBoundsException e) { // generalize DB exceptions
            throw new IllegalArgumentException("Handling of records failed", e);
        }
    }

    @Override
    public void setComposition(Composition composition) {
        this.composition = composition;
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
        // default current version, no history // FIXME
        return 1;
    }

    /**
     * @throws IllegalArgumentException when seeking language code, territory code
     *                                  or composer ID failed
     */
    @Override
    public void updateCompositionData(Composition newComposition) {
        // update the mutable attributes
        setLanguageCode(seekLanguageCode(newComposition.getLanguage().getCodeString()));
        setTerritoryCode(seekTerritoryCode(newComposition.getTerritory().getCodeString()));
        setComposerId(seekComposerId(newComposition.getComposer(), compositionRecord.getSysTenant()));

        setFeederAudit(newComposition.getFeederAudit());
        setLinks(newComposition.getLinks());
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
    public I_AuditDetailsAccess getAuditDetailsAccess() {
        return this.auditDetailsAccess;
    }

    @Override
    public void setAuditDetailsId(UUID auditId) {
        compositionRecord.setHasAudit(auditId);
    }

    public static boolean exists(I_DomainAccess domainAccess, UUID versionedObjectId) {
        if (domainAccess.getContext().fetchExists(COMPOSITION, COMPOSITION.ID.eq(versionedObjectId))
                || domainAccess
                        .getContext()
                        .fetchExists(COMPOSITION_HISTORY, COMPOSITION_HISTORY.ID.eq(versionedObjectId))) {
            return true;
        } else {
            throw new ObjectNotFoundException(COMPOSITION_LITERAL, "No composition with given ID found");
        }
    }

    public static boolean isDeleted(I_DomainAccess domainAccess, UUID versionedObjectId) {
        // meta: logically deleted means that of this ID only entries in the history table are available
        // XXX performance...
        // if available in normal table -> not deleted
        if (domainAccess.getContext().fetchExists(COMPOSITION, COMPOSITION.ID.eq(versionedObjectId))) {
            return false;
        }

        // if only in history table
        if (domainAccess.getContext().fetchExists(COMPOSITION_HISTORY, COMPOSITION_HISTORY.ID.eq(versionedObjectId))) {
            // retrieve the record
            Result<CompositionHistoryRecord> historyRecordsRes = domainAccess
                    .getContext()
                    .selectFrom(COMPOSITION_HISTORY)
                    .where(COMPOSITION_HISTORY.ID.eq(versionedObjectId))
                    .orderBy(COMPOSITION_HISTORY.SYS_TRANSACTION.desc()) // latest
                    // at
                    // top,
                    // i.e.
                    // [0]
                    .fetch();
            // assumed not empty, because fetchExists was successful

            // retrieve matching audit
            AuditDetailsRecord audit = domainAccess
                    .getContext()
                    .fetchOne(
                            AUDIT_DETAILS,
                            AUDIT_DETAILS.ID.eq(historyRecordsRes.get(0).getHasAudit()));
            if (audit == null) {
                throw new InternalServerException("DB inconsistency: couldn't retrieve referenced audit");
            }
            // and check for correct change type -> is deleted
            if (audit.getChangeType().equals(ContributionChangeType.deleted)) {
                return true;
            }
        } else {
            throw new ObjectNotFoundException(COMPOSITION_LITERAL, "No composition with given ID found");
        }
        throw new InternalServerException("Problem processing CompositionAccess.isDeleted(..)");
    }

    @Override
    public void adminDelete() {
        AdminApiUtils adminApi = new AdminApiUtils(getContext());
        adminApi.deleteComposition(this.getId());
    }

    @Override
    public UUID getAttestationRef() {
        return this.compositionRecord.getAttestationRef();
    }
}
