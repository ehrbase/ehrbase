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

import static org.ehrbase.jooq.pg.Tables.COMPOSITION;
import static org.ehrbase.jooq.pg.Tables.EHR_;
import static org.ehrbase.jooq.pg.Tables.ENTRY;
import static org.ehrbase.jooq.pg.Tables.IDENTIFIER;
import static org.ehrbase.jooq.pg.Tables.PARTY_IDENTIFIED;
import static org.ehrbase.jooq.pg.Tables.STATUS;
import static org.ehrbase.jooq.pg.Tables.STATUS_HISTORY;

import com.nedap.archie.rm.datastructures.ItemStructure;
import com.nedap.archie.rm.datavalues.DvCodedText;
import com.nedap.archie.rm.datavalues.DvText;
import com.nedap.archie.rm.ehr.EhrStatus;
import com.nedap.archie.rm.generic.PartySelf;
import com.nedap.archie.rm.support.identification.HierObjectId;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.apache.commons.collections.map.MultiValueMap;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.dao.access.interfaces.I_AuditDetailsAccess;
import org.ehrbase.dao.access.interfaces.I_ConceptAccess;
import org.ehrbase.dao.access.interfaces.I_ConceptAccess.ContributionChangeType;
import org.ehrbase.dao.access.interfaces.I_ContributionAccess;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.dao.access.interfaces.I_EhrAccess;
import org.ehrbase.dao.access.interfaces.I_StatusAccess;
import org.ehrbase.dao.access.interfaces.I_SystemAccess;
import org.ehrbase.dao.access.jooq.party.PersistedPartyProxy;
import org.ehrbase.dao.access.support.DataAccess;
import org.ehrbase.dao.access.support.TenantSupport;
import org.ehrbase.dao.access.util.ContributionDef;
import org.ehrbase.dao.access.util.ContributionDef.ContributionState;
import org.ehrbase.dao.access.util.TransactionTime;
import org.ehrbase.jooq.pg.Routines;
import org.ehrbase.jooq.pg.enums.ContributionDataType;
import org.ehrbase.jooq.pg.tables.records.AdminDeleteEhrFullRecord;
import org.ehrbase.jooq.pg.tables.records.EhrRecord;
import org.ehrbase.jooq.pg.tables.records.IdentifierRecord;
import org.ehrbase.jooq.pg.tables.records.StatusHistoryRecord;
import org.ehrbase.jooq.pg.tables.records.StatusRecord;
import org.ehrbase.service.RecordedDvCodedText;
import org.ehrbase.service.RecordedDvText;
import org.ehrbase.util.UuidGenerator;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Persistence operations on EHR.
 *
 * @author Christian Chevalley
 * @author Jake Smolka
 * @author Luis Marco-Ruiz
 * @since 1.0.0
 */
@SuppressWarnings("java:S2589")
public class EhrAccess extends DataAccess implements I_EhrAccess {

    // Logger used in static methods
    private static final Logger logger = LoggerFactory.getLogger(EhrAccess.class);

    public static final String JSONB = "::jsonb";
    public static final String EXCEPTION = " exception:";
    public static final String COULD_NOT_RETRIEVE_EHR_FOR_ID = "Could not retrieve EHR for id:";
    public static final String COULD_NOT_RETRIEVE_EHR_FOR_PARTY = "Could not retrieve EHR for party:";
    private EhrRecord ehrRecord;
    private boolean isNew = false;
    private boolean hasStatusChanged = false;

    // holds the non serialized ItemStructure other_details structure
    private ItemStructure otherDetails = null;

    private I_ContributionAccess contributionAccess; // locally referenced contribution associated to ehr transactions

    private I_StatusAccess statusAccess; // associated EHR_STATUS. Each EHR has 1 EHR_STATUS

    // set this variable to change the identification  mode in status
    public enum PARTY_MODE {
        IDENTIFIER,
        EXTERNAL_REF
    }

    /**
     * @throws InternalServerException if creating or retrieving system failed
     */
    public EhrAccess(I_DomainAccess domain, UUID partyId, UUID systemId, UUID accessId, UUID ehrId, Short sysTenant) {
        super(domain);

        this.ehrRecord = domain.getContext().newRecord(EHR_);
        // checking for and executing case of custom ehr ID
        ehrRecord.setId(Objects.requireNonNullElseGet(ehrId, UuidGenerator::randomUUID));

        // init a new EHR_STATUS with default values to associate with this EHR
        this.statusAccess = new StatusAccess(this, ehrRecord.getId(), sysTenant);
        this.statusAccess.getStatusRecord().setId(UuidGenerator.randomUUID());
        this.statusAccess.getStatusRecord().setIsModifiable(true);
        this.statusAccess.getStatusRecord().setIsQueryable(true);
        this.statusAccess.getStatusRecord().setParty(partyId);
        this.statusAccess.getStatusRecord().setEhrId(ehrRecord.getId());

        ehrRecord.setSystemId(systemId);
        ehrRecord.setAccess(accessId);
        ehrRecord.setSysTenant(sysTenant);

        if (ehrRecord.getSystemId() == null) { // storeComposition a default entry for the current system
            ehrRecord.setSystemId(I_SystemAccess.createOrRetrieveLocalSystem(this));
        }

        this.isNew = true;

        // associate a contribution with this EHR
        contributionAccess = I_ContributionAccess.getInstance(this, ehrRecord.getId(), sysTenant);
        contributionAccess.setState(ContributionDef.ContributionState.COMPLETE);
    }

    /**
     * Internal constructor to create minimal instance to customize further before returning.
     *
     * @param domainAccess DB domain access object
     * @param ehrId        EHR ID, necessary to create an EHR status and contributions
     */
    private EhrAccess(I_DomainAccess domainAccess, UUID ehrId, Short sysTenant) {
        super(domainAccess);
        statusAccess = new StatusAccess(this, ehrId, sysTenant); // minimal association with STATUS
        // associate a contribution with this EHR
        contributionAccess = I_ContributionAccess.getInstance(this, ehrId, sysTenant);
        contributionAccess.setState(ContributionDef.ContributionState.COMPLETE);
    }

    /**
     * @throws IllegalArgumentException if retrieving failed for given input
     */
    public static UUID retrieveInstanceBySubject(I_DomainAccess domainAccess, UUID subjectUuid) {
        Record record;
        DSLContext context = domainAccess.getContext();

        try {
            record = context.select(STATUS.EHR_ID)
                    .from(STATUS)
                    .where(STATUS.PARTY.eq(context.select(PARTY_IDENTIFIED.ID)
                            .from(PARTY_IDENTIFIED)
                            .where(PARTY_IDENTIFIED.ID.eq(subjectUuid))))
                    .fetchOne();

        } catch (Exception e) { // possibly not unique for a party: this is not permitted!
            throw new IllegalArgumentException("Could not retrieve  EHR for party:" + subjectUuid + EXCEPTION + e);
        }

        if (record == null || record.size() == 0) {
            logger.warn(COULD_NOT_RETRIEVE_EHR_FOR_PARTY + subjectUuid);
            return null;
        }

        return (UUID) record.getValue(0);
    }

    /**
     * @throws IllegalArgumentException if retrieving failed for given input
     */
    public static UUID retrieveInstanceBySubject(I_DomainAccess domainAccess, String subjectId, String issuerSpace) {
        Record record;
        DSLContext context = domainAccess.getContext();

        // get the corresponding party Id from the codification space provided by an issuer
        IdentifierRecord identifierRecord =
                context.fetchOne(IDENTIFIER, IDENTIFIER.ID_VALUE.eq(subjectId).and(IDENTIFIER.ISSUER.eq(issuerSpace)));

        if (identifierRecord == null) {
            throw new IllegalArgumentException("Could not invalidateContent an identified party for code:" + subjectId
                    + " issued by:" + issuerSpace);
        }

        try {
            record = context.select(STATUS.EHR_ID)
                    .from(STATUS)
                    .where(STATUS.PARTY.eq(context.select(PARTY_IDENTIFIED.ID)
                            .from(PARTY_IDENTIFIED)
                            .where(PARTY_IDENTIFIED.ID.eq(identifierRecord.getParty()))))
                    .fetchOne();

        } catch (Exception e) { // possibly not unique for a party: this is not permitted!
            throw new IllegalArgumentException(COULD_NOT_RETRIEVE_EHR_FOR_PARTY + subjectId + EXCEPTION + e);
        }

        if (record == null || record.size() == 0) {
            logger.warn(COULD_NOT_RETRIEVE_EHR_FOR_PARTY + subjectId);
            return null;
        }

        return (UUID) record.getValue(0);
    }

    /**
     * @throws IllegalArgumentException if retrieving failed for given input
     */
    public static UUID retrieveInstanceBySubjectExternalRef(
            I_DomainAccess domainAccess, String subjectId, String issuerSpace) {
        Record record;
        DSLContext context = domainAccess.getContext();

        try {
            record = context.select(STATUS.EHR_ID)
                    .from(STATUS)
                    .where(STATUS.PARTY.eq(context.select(PARTY_IDENTIFIED.ID)
                            .from(PARTY_IDENTIFIED)
                            .where(PARTY_IDENTIFIED
                                    .PARTY_REF_VALUE
                                    .eq(subjectId)
                                    .and(PARTY_IDENTIFIED.PARTY_REF_NAMESPACE.eq(issuerSpace)))))
                    .fetchOne();

        } catch (Exception e) { // possibly not unique for a party: this is not permitted!
            throw new IllegalArgumentException(COULD_NOT_RETRIEVE_EHR_FOR_PARTY + subjectId + EXCEPTION + e);
        }

        if (record == null || record.size() == 0) {
            logger.warn("Could not retrieve ehr for party: {}", subjectId);
            return null;
        }

        return (UUID) record.getValue(0);
    }

    public static I_EhrAccess retrieveInstanceByStatus(
            I_DomainAccess domainAccess, UUID ehrId, UUID status, Integer version) {
        if (version < 1) {
            throw new IllegalArgumentException("Version number must be > 0");
        }

        // minimal access, needs attributes to be set before returning
        EhrAccess ehrAccess = new EhrAccess(domainAccess, ehrId, TenantSupport.currentSysTenant());
        EhrRecord record;

        // necessary anyway, but if no version is provided assume latest version (otherwise this one will be overwritten
        // with wanted one)
        I_StatusAccess statusAccess = I_StatusAccess.retrieveInstance(domainAccess, status);
        ehrAccess.setStatusAccess(statusAccess);

        // first step of retrieving a particular version is to query for the amount of versions, which depends on the
        // latest one above
        Integer versions = domainAccess
                        .getContext()
                        .fetchCount(
                                STATUS_HISTORY,
                                STATUS_HISTORY.EHR_ID.eq(ehrAccess
                                        .getStatusAccess()
                                        .getStatusRecord()
                                        .getEhrId()))
                + 1;
        // check if input version number fits into existing amount of versions, but is not the same (same equals latest
        // version)
        // when either there is only one version or the requested one is the latest, continue with record already set
        if (versions > version && !version.equals(versions)) { // or get the particular requested version
            // sonarlint says that the expression above is always true? tested it, it can be true and false!?
            // note: here version is > 1 and there has to be at least one history entry
            Result<StatusHistoryRecord> result = domainAccess
                    .getContext()
                    .selectFrom(STATUS_HISTORY)
                    .where(STATUS_HISTORY.EHR_ID.eq(ehrId))
                    .orderBy(STATUS_HISTORY.SYS_TRANSACTION.asc()) // oldest at top, i.e. [0]
                    .fetch();

            if (result.isEmpty()) {
                throw new InternalServerException("Error retrieving EHR_STATUS"); // should never be reached
            }

            // result set of history table is always version+1, because the latest is in non-history table
            StatusHistoryRecord statusHistoryRecord = result.get(version - 1);
            // FIXME EHR_STATUS: manually converting types. dirty, formally break jooq-style, right? the record would
            // considered to be updated when calling methods like .store()
            ehrAccess.getStatusAccess().getStatusRecord().setEhrId(statusHistoryRecord.getEhrId());
            ehrAccess.getStatusAccess().getStatusRecord().setIsQueryable(statusHistoryRecord.getIsQueryable());
            ehrAccess.getStatusAccess().getStatusRecord().setIsModifiable(statusHistoryRecord.getIsModifiable());
            ehrAccess.getStatusAccess().getStatusRecord().setParty(statusHistoryRecord.getParty());
            ehrAccess.getStatusAccess().getStatusRecord().setOtherDetails(statusHistoryRecord.getOtherDetails());

            ehrAccess.getStatusAccess().getStatusRecord().setSysTransaction(statusHistoryRecord.getSysTransaction());
            ehrAccess.getStatusAccess().getStatusRecord().setSysPeriod(statusHistoryRecord.getSysPeriod());

            ehrAccess.getStatusAccess().getStatusRecord().setHasAudit(statusHistoryRecord.getHasAudit());
            ehrAccess.getStatusAccess().getStatusRecord().setAttestationRef(statusHistoryRecord.getAttestationRef());
            ehrAccess.getStatusAccess().getStatusRecord().setInContribution(statusHistoryRecord.getInContribution());
            ehrAccess.getStatusAccess().getStatusRecord().setArchetypeNodeId(statusHistoryRecord.getArchetypeNodeId());
            ehrAccess.getStatusAccess().getStatusRecord().setName(statusHistoryRecord.getName());
        }

        try {
            record = domainAccess
                    .getContext()
                    .selectFrom(EHR_)
                    .where(EHR_.ID.eq(
                            ehrAccess.getStatusAccess().getStatusRecord().getEhrId()))
                    .fetchOne();
        } catch (Exception e) { // possibly not unique for a party: this is not permitted!
            throw new IllegalArgumentException(
                    "Could not retrieveInstanceByNamedSubject EHR for status:" + status + EXCEPTION + e);
        }

        if (record.size() == 0) {
            logger.warn("Could not retrieveInstanceByNamedSubject ehr for status:" + status);
            return null;
        }

        ehrAccess.ehrRecord = record;

        ehrAccess.isNew = false;

        return ehrAccess;
    }

    /**
     * @throws IllegalArgumentException when either no EHR for ID, or problem with data structure of
     *                                  EHR, or DB inconsistency
     */
    public static I_EhrAccess retrieveInstance(I_DomainAccess domainAccess, UUID ehrId) {
        DSLContext context = domainAccess.getContext();
        EhrAccess ehrAccess = new EhrAccess(domainAccess, ehrId, TenantSupport.currentSysTenant());

        EhrRecord record;

        try {
            record = context.selectFrom(EHR_).where(EHR_.ID.eq(ehrId)).fetchOne();
        } catch (Exception e) { // possibly not unique for a party: this is not permitted!
            throw new IllegalArgumentException(COULD_NOT_RETRIEVE_EHR_FOR_ID + ehrId + EXCEPTION + e);
        }

        if (record == null || record.size() == 0) {
            logger.warn(COULD_NOT_RETRIEVE_EHR_FOR_ID + ehrId);
            return null;
        }

        ehrAccess.ehrRecord = record;
        // retrieve the corresponding status
        I_StatusAccess statusAccess = I_StatusAccess.retrieveInstanceByEhrId(domainAccess, ehrAccess.ehrRecord.getId());
        ehrAccess.setStatusAccess(statusAccess);

        // set otherDetails if available
        if (ehrAccess.getStatusAccess().getStatusRecord().getOtherDetails() != null) {
            ehrAccess.otherDetails =
                    ehrAccess.getStatusAccess().getStatusRecord().getOtherDetails();
        }

        ehrAccess.isNew = false;

        ehrAccess.setContributionAccess(I_ContributionAccess.retrieveInstance(
                domainAccess, ehrAccess.getStatusAccess().getContributionId()));

        return ehrAccess;
    }

    /**
     * @throws IllegalArgumentException when no EHR found for ID
     */
    public static Map<String, Object> fetchSubjectIdentifiers(I_DomainAccess domainAccess, UUID ehrId) {
        EhrAccess ehrAccess = (EhrAccess) retrieveInstance(domainAccess, ehrId);
        DSLContext context = domainAccess.getContext();

        if (ehrAccess == null) {
            throw new IllegalArgumentException("No ehr found for id:" + ehrId);
        }

        Map<String, Object> idlist = new MultiValueMap();

        // getNewFolderAccessInstance the corresponding subject Identifiers

        context.selectFrom(IDENTIFIER)
                .where(IDENTIFIER.PARTY.eq(getParty(ehrAccess)))
                .fetch()
                .forEach(record -> {
                    idlist.put("identifier_issuer", record.getIssuer());
                    idlist.put("identifier_id_value", record.getIdValue());
                });

        // get the list of ref attributes
        context.selectFrom(PARTY_IDENTIFIED)
                .where(PARTY_IDENTIFIED.ID.eq(getParty(ehrAccess)))
                .fetch()
                .forEach(record -> {
                    idlist.put("ref_name_space", record.getPartyRefNamespace());
                    idlist.put("id_value", record.getPartyRefValue());
                    idlist.put("ref_name_scheme", record.getPartyRefScheme());
                    idlist.put("ref_party_type", record.getPartyRefType());
                });

        return idlist;
    }

    /**
     * FIXME: check this method. appears to be needed later on. problematic: it actually gets a list of entries, not compositions. why only with three attributes? what about the unique key problem below?
     *
     * @throws IllegalArgumentException when no EHR found for ID
     */
    public static Map<String, Map<String, String>> getCompositionList(I_DomainAccess domainAccess, UUID ehrId) {
        EhrAccess ehrAccess = (EhrAccess) retrieveInstance(domainAccess, ehrId);
        DSLContext context = domainAccess.getContext();

        if (ehrAccess == null) {
            throw new IllegalArgumentException("No ehr found for id:" + ehrId);
        }

        Map<String, Map<String, String>> compositionlist = new HashMap<>(); // unique keys

        context.selectFrom(ENTRY)
                .where(ENTRY.COMPOSITION_ID.eq(
                        context.select(COMPOSITION.ID).from(COMPOSITION).where(COMPOSITION.EHR_ID.eq(ehrId))))
                .fetch()
                .forEach(record -> {
                    Map<String, String> details = new HashMap<>();
                    details.put("composition_id", record.getCompositionId().toString());
                    details.put("templateId", record.getTemplateId());
                    details.put("date", record.getSysTransaction().toString());
                    compositionlist.put(
                            "details",
                            details); // FIXME: bug? gets overwritten if more than 1 put() with this static key
                });

        return compositionlist;
    }

    private static UUID getParty(EhrAccess ehrAccess) {
        return ehrAccess.getStatusRecord().getParty();
    }

    @Override
    public DataAccess getDataAccess() {
        return this;
    }

    @Override
    public void setAccess(UUID access) {
        ehrRecord.setAccess(access);
    }

    @Override
    public void setSystem(UUID system) {
        ehrRecord.setSystemId(system);
    }

    @Override
    public void setModifiable(Boolean modifiable) {
        getStatusAccess().getStatusRecord().setIsModifiable(modifiable);
    }

    @Override
    public void setArchetypeNodeId(String archetypeNodeId) {
        getStatusAccess().getStatusRecord().setArchetypeNodeId(archetypeNodeId);
    }

    @Override
    public String getArchetypeNodeId() {
        return getStatusAccess().getStatusRecord().getArchetypeNodeId();
    }

    @Override
    public void setName(DvText name) {
        new RecordedDvText().toDB(getStatusAccess().getStatusRecord(), STATUS.NAME, name);
    }

    @Override
    public void setName(DvCodedText name) {
        new RecordedDvCodedText().toDB(getStatusAccess().getStatusRecord(), STATUS.NAME, name);
    }

    @Override
    public void setQueryable(Boolean queryable) {
        getStatusAccess().getStatusRecord().setIsQueryable(queryable);
    }

    /**
     * @throws InvalidApiParameterException when input couldn't be processed, i.e. EHR not stored
     */
    @Override
    public UUID commit(Timestamp transactionTime) {

        ehrRecord.setDateCreated(transactionTime);
        ehrRecord.setDateCreatedTzid(
                OffsetDateTime.from(transactionTime.toLocalDateTime().atOffset(ZoneOffset.from(OffsetDateTime.now())))
                        .getOffset()
                        .getId()); // get zoneId independent of "transactionTime"
        ehrRecord.store();

        UUID contributionId = contributionAccess.commit(transactionTime);

        if (isNew && getStatusAccess().getStatusRecord() != null && hasStatusChanged) {

            // status is attached to EHR, so always same contribution when creating both together
            statusAccess.setContributionId(contributionId);
            statusAccess.setEhrId(ehrRecord.getId());
            statusAccess.setOtherDetails(otherDetails);
            statusAccess.commit(transactionTime.toLocalDateTime(), contributionId, null);

            // reset
            hasStatusChanged = false;
        }

        return ehrRecord.getId();
    }

    /**
     * @throws InternalServerException because inherited interface function isn't implemented in this
     *                                 class
     * @deprecated
     */
    @Deprecated
    @Override
    public UUID commit() {
        throw new InternalServerException("INTERNAL: this commit is not legal");
    }

    /**
     * @throws IllegalArgumentException when EHR couldn't be stored
     */
    @Override
    public UUID commit(UUID committerId, UUID systemId, String description) {
        Timestamp timestamp = TransactionTime.millis();
        // prepare EHR_STATUS audit with given values

        // prepare associated contribution (with contribution's audit embedded)
        contributionAccess.setAuditDetailsValues(committerId, systemId, description, ContributionChangeType.CREATION);
        contributionAccess.setDataType(ContributionDataType.ehr);
        contributionAccess.setState(ContributionDef.ContributionState.COMPLETE);

        statusAccess.setAuditAndContributionAuditValues(
                systemId, committerId, description, ContributionChangeType.CREATION);

        return commit(timestamp);
    }

    /**
     * {@inheritDoc}
     *
     * @throws InvalidApiParameterException when marshalling of EHR_STATUS / OTHER_DETAILS failed
     */
    @Override
    public Boolean update(Timestamp transactionTime) {
        return update(transactionTime, false);
    }

    /**
     * {@inheritDoc}
     *
     * @throws InvalidApiParameterException when marshalling of EHR_STATUS / OTHER_DETAILS failed
     */
    @Override
    public Boolean update(Timestamp transactionTime, boolean force) {
        boolean result = false;

        if (hasStatusChanged) {
            statusAccess.setContributionAccess(this.contributionAccess);

            statusAccess.setOtherDetails(otherDetails);
            I_AuditDetailsAccess auditDetailsAccess = statusAccess.getAuditDetailsAccess();
            result = statusAccess.update(
                    LocalDateTime.ofInstant(transactionTime.toInstant(), ZoneId.systemDefault()),
                    this.contributionAccess.getId(),
                    auditDetailsAccess.getId());

            // reset
            hasStatusChanged = false;
        }

        if (force || ehrRecord.changed()) {
            ehrRecord.setDateCreated(transactionTime);
            ehrRecord.setDateCreatedTzid(
                    ZonedDateTime.now().getZone().getId()); // get zoneId independent of "transactionTime"
            result |= ehrRecord.update() > 0;
        }

        return result;
    }

    /**
     * @throws InternalServerException because inherited interface function isn't implemented in this
     *                                 class
     * @deprecated
     */
    @Deprecated
    @Override
    public Boolean update() {
        throw new InternalServerException("INTERNAL: this update is not legal");
    }

    /**
     * @throws InternalServerException because inherited interface function isn't implemented in this
     *                                 class
     * @deprecated
     */
    @Deprecated
    @Override
    public Boolean update(Boolean force) {
        throw new InternalServerException("INTERNAL: this update is not legal");
    }

    @Override
    public Boolean update(
            UUID committerId,
            UUID systemId,
            UUID contributionId,
            ContributionState state,
            ContributionChangeType contributionChangeType,
            String description,
            UUID audit) {
        Timestamp timestamp = TransactionTime.millis();
        // If custom contribution ID is provided use it, otherwise reuse already linked one
        if (contributionId != null) {
            I_ContributionAccess access = I_ContributionAccess.retrieveInstance(this.getDataAccess(), contributionId);
            I_AuditDetailsAccess auditDetailsAccess = new AuditDetailsAccess(
                            this.getDataAccess(), ehrRecord.getSysTenant())
                    .retrieveInstance(this.getDataAccess(), audit);
            if (access != null && auditDetailsAccess != null) {
                this.contributionAccess = access;
                this.statusAccess.setAuditDetailsAccess(auditDetailsAccess);
            } else {
                throw new InternalServerException("Can't update status with invalid contribution ID.");
            }
            // Status check, because a contribution is needed only IF a versioned object is changed.
            // So the plain EHR object change, e.g. only with new directory reference, shall not have a separate
            // contribution.
        } else if (hasStatusChanged) {
            this.contributionAccess = new ContributionAccess(
                    this, getEhrRecord().getId(), getEhrRecord().getSysTenant());
            provisionContributionAccess(
                    contributionAccess, committerId, systemId, description, state, contributionChangeType);
            this.contributionAccess.commit();

            // create new audit for this update
            statusAccess.setAuditDetailsAccess(new AuditDetailsAccess(
                    this,
                    this.contributionAccess.getAuditsSystemId(),
                    this.contributionAccess.getAuditsCommitter(),
                    ContributionChangeType.MODIFICATION,
                    this.contributionAccess.getAuditsDescription(),
                    ehrRecord.getSysTenant()));
            statusAccess.getAuditDetailsAccess().commit();
        }

        return update(timestamp);
    }

    /**
     * Helper to provision a contribution access object with several data items.
     */
    private void provisionContributionAccess(
            I_ContributionAccess access,
            UUID committerId,
            UUID systemId,
            String description,
            ContributionDef.ContributionState state,
            I_ConceptAccess.ContributionChangeType contributionChangeType) {
        access.setAuditDetailsValues(committerId, systemId, description, contributionChangeType);
        access.setState(state);
        access.setDataType(ContributionDataType.ehr);
        access.setState(ContributionState.COMPLETE);
    }

    /**
     * @throws InternalServerException because inherited interface function isn't implemented in this
     *                                 class
     * @deprecated
     */
    @Deprecated
    @Override
    public Integer delete() {
        throw new InternalServerException("INTERNAL: this delete is not legal");
    }

    /**
     * @throws IllegalArgumentException when instance's EHR ID can't be matched to existing one
     */
    @Override
    public UUID reload() {
        EhrRecord record;

        try {
            record = getContext().selectFrom(EHR_).where(EHR_.ID.eq(getId())).fetchOne();
        } catch (Exception e) { // possibly not unique for a party: this is not permitted!
            throw new IllegalArgumentException(COULD_NOT_RETRIEVE_EHR_FOR_ID + getId() + EXCEPTION + e);
        }

        if (record == null || record.size() == 0) {
            logger.warn(COULD_NOT_RETRIEVE_EHR_FOR_ID + getId());
            return null;
        }

        ehrRecord = record;
        // retrieve the corresponding status
        I_StatusAccess retStatusAccess =
                I_StatusAccess.retrieveInstanceByEhrId(this.getDataAccess(), ehrRecord.getId());
        setStatusAccess(retStatusAccess);
        isNew = false;

        return getId();
    }

    public I_EhrAccess retrieve(UUID id) {
        return retrieveInstance(this, id);
    }

    public EhrRecord getEhrRecord() {
        return ehrRecord;
    }

    private StatusRecord getStatusRecord() {
        return getStatusAccess().getStatusRecord();
    }

    public boolean isNew() {
        return isNew;
    }

    @Override
    public UUID getParty() {
        return getStatusAccess().getStatusRecord().getParty();
    }

    @Override
    public void setParty(UUID partyId) {
        getStatusAccess().getStatusRecord().setParty(partyId);
    }

    @Override
    public UUID getId() {
        return ehrRecord.getId();
    }

    @Override
    public Boolean isModifiable() {
        return getStatusAccess().getStatusRecord().getIsModifiable();
    }

    @Override
    public Boolean isQueryable() {
        return getStatusAccess().getStatusRecord().getIsQueryable();
    }

    @Override
    public UUID getSystemId() {
        return ehrRecord.getSystemId();
    }

    @Override
    public UUID getStatusId() {
        return statusAccess.getId();
    }

    @Override
    public UUID getAccessId() {
        return ehrRecord.getAccess();
    }

    @Override
    public void setOtherDetails(ItemStructure otherDetails, String templateId) {
        this.otherDetails = otherDetails;
        //        this.otherDetailsTemplateId =
        // Optional.ofNullable(otherDetails).map(Locatable::getArchetypeDetails).map(Archetyped::getTemplateId).map(ObjectId::getValue).orElse(null);
    }

    @Override
    public ItemStructure getOtherDetails() {
        return otherDetails;
    }

    public I_ContributionAccess getContributionAccess() {
        return contributionAccess;
    }

    @Override
    public void setContributionAccess(I_ContributionAccess contributionAccess) {
        this.contributionAccess = contributionAccess;
    }

    @Override
    public I_StatusAccess getStatusAccess() {
        return this.statusAccess;
    }

    @Override
    public void setStatusAccess(I_StatusAccess statusAccess) {
        this.statusAccess = statusAccess;
    }

    @Override
    public void setStatus(EhrStatus status) {
        setModifiable(status.isModifiable());
        setQueryable(status.isQueryable());
        setOtherDetails(status.getOtherDetails(), null);

        // Locatable stuff if present
        if (status.getArchetypeNodeId() != null) {
            setArchetypeNodeId(status.getArchetypeNodeId());
        }

        if (status.getName() != null) {
            setName(status.getName());
        }

        UUID subjectUuid = new PersistedPartyProxy(getDataAccess())
                .getOrCreate(
                        status.getSubject(), getStatusAccess().getStatusRecord().getSysTenant());
        setParty(subjectUuid);

        hasStatusChanged = true;
    }

    @Override // get latest status
    public EhrStatus getStatus() {
        EhrStatus status = new EhrStatus();

        status.setModifiable(isModifiable());
        status.setQueryable(isQueryable());
        // set otherDetails if available
        if (getStatusAccess().getStatusRecord().getOtherDetails() != null) {
            status.setOtherDetails(getStatusAccess().getStatusRecord().getOtherDetails());
        }

        // Locatable attribute
        status.setArchetypeNodeId(getArchetypeNodeId());
        Object name = new RecordedDvCodedText().fromDB(getStatusAccess().getStatusRecord(), STATUS.NAME);
        status.setName(name instanceof DvText ? (DvText) name : (DvCodedText) name);

        UUID statusId = getStatusAccess().getStatusRecord().getId();
        status.setUid(new HierObjectId(statusId.toString() + "::"
                + getServerConfig().getNodename() + "::" + I_StatusAccess.getLatestVersionNumber(this, statusId)));

        PartySelf partySelf = (PartySelf) new PersistedPartyProxy(this).retrieve(getParty());
        status.setSubject(partySelf);

        return status;
    }

    @Override
    public void adminDeleteEhr() {
        Result<AdminDeleteEhrFullRecord> result =
                Routines.adminDeleteEhrFull(getContext().configuration(), this.getId());
        if (result.isEmpty() || !Boolean.TRUE.equals(result.get(0).getDeleted())) {
            throw new InternalServerException("Admin deletion of EHR failed!");
        }
    }

    public static boolean hasEhr(I_DomainAccess domainAccess, UUID ehrId) {
        return domainAccess.getContext().fetchExists(EHR_, EHR_.ID.eq(ehrId));
    }

    public static Boolean isModifiable(I_DomainAccess domainAccess, UUID ehrId) {
        return domainAccess
                .getContext()
                .select(STATUS.IS_MODIFIABLE)
                .from(STATUS)
                .where(STATUS.EHR_ID.eq(ehrId))
                .fetchOne(Record1::value1);
    }
}
