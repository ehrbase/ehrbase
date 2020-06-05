/*
 * Copyright (c) 2019 Stefan Spiska (Vitasystems GmbH),
 * Jake Smolka (Hannover Medical School).
 *
 * This file is part of project EHRbase
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

package org.ehrbase.service;

import com.nedap.archie.rm.changecontrol.OriginalVersion;
import com.nedap.archie.rm.datatypes.CodePhrase;
import com.nedap.archie.rm.datavalues.DvCodedText;
import com.nedap.archie.rm.datavalues.DvText;
import com.nedap.archie.rm.datavalues.quantity.datetime.DvDateTime;
import com.nedap.archie.rm.ehr.EhrStatus;
import com.nedap.archie.rm.ehr.VersionedEhrStatus;
import com.nedap.archie.rm.generic.*;
import com.nedap.archie.rm.support.identification.*;
import org.ehrbase.api.definitions.CompositionFormat;
import org.ehrbase.api.definitions.ServerConfig;
import org.ehrbase.api.definitions.StructuredString;
import org.ehrbase.api.definitions.StructuredStringFormat;
import org.ehrbase.api.dto.EhrStatusDto;
import org.ehrbase.api.exception.*;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.api.service.ValidationService;
import org.ehrbase.dao.access.interfaces.*;
import org.ehrbase.dao.access.jooq.AttestationAccess;
import org.ehrbase.dao.access.jooq.party.PersistedPartyProxy;
import org.ehrbase.dao.access.jooq.party.PersistedPartyRef;
import org.ehrbase.serialisation.CanonicalJson;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional()
public class EhrServiceImp extends BaseService implements EhrService {
    public static final String DESCRIPTION = "description";
    private Logger logger = LoggerFactory.getLogger(getClass());
    private final ValidationService validationService;

    @Autowired
    public EhrServiceImp(KnowledgeCacheService knowledgeCacheService, ValidationService validationService, DSLContext context, ServerConfig serverConfig) {
        super(knowledgeCacheService, context, serverConfig);
        this.validationService = validationService;
    }

    @Override
    public UUID create(EhrStatus status, UUID ehrId) {

        try {
            validationService.check(status);
        } catch (Exception e) {
            // rethrow if this class, but wrap all others in InternalServerException
            if (e.getClass().equals(UnprocessableEntityException.class))
                throw (UnprocessableEntityException) e;
            if (e.getClass().equals(IllegalArgumentException.class))
                throw new ValidationException(e);
            if (e.getClass().equals(ValidationException.class))
                throw e;
            else if (e.getClass().equals(org.ehrbase.validation.constraints.wrappers.ValidationException.class))
                throw new ValidationException(e);
            else
                throw new InternalServerException(e);
        }

        if (status == null) {   // in case of new status with default values
            status = new EhrStatus();
            status.setSubject(new PartySelf(null));
            status.setModifiable(true);
            status.setQueryable(true);
        }
        status.setUid(new HierObjectId(UUID.randomUUID().toString()));  // server sets own new UUID in both cases (new or given status)

        UUID subjectUuid = new PersistedPartyProxy(getDataAccess()).getOrCreate(status.getSubject());

        if (status.getSubject().getExternalRef() != null && I_EhrAccess.checkExist(getDataAccess(), subjectUuid))
            throw new StateConflictException("Specified party has already an EHR set (partyId=" + subjectUuid + ")");

        UUID systemId = getSystemUuid();
        UUID committerId = getUserUuid();

        try {   // this try block sums up a bunch of operations that can throw errors in the following
            I_EhrAccess ehrAccess = I_EhrAccess.getInstance(getDataAccess(), subjectUuid, systemId, null, null, ehrId);
            ehrAccess.setStatus(status);
            return ehrAccess.commit(committerId, systemId, DESCRIPTION);
        } catch (Exception e) {
            throw new InternalServerException("Could not create an EHR with given parameters.", e);
        }
    }

    @Override
    public Optional<EhrStatusDto> getEhrStatusEhrScape(UUID ehrUuid, CompositionFormat format) {
        EhrStatusDto statusDto = new EhrStatusDto();
        try {

            I_EhrAccess ehrAccess = I_EhrAccess.retrieveInstance(getDataAccess(), ehrUuid);
            if (ehrAccess == null) {
                return Optional.empty();
            }

            PartyProxy partyProxy = new PersistedPartyProxy(getDataAccess()).retrieve(ehrAccess.getParty());

            statusDto.setSubjectId(partyProxy.getExternalRef().getId().getValue());
            statusDto.setSubjectNamespace(partyProxy.getExternalRef().getNamespace());
            statusDto.setModifiable(ehrAccess.isModifiable());
            statusDto.setQueryable(ehrAccess.isQueryable());
            statusDto.setOtherDetails(new StructuredString(new CanonicalJson().marshal(ehrAccess.getOtherDetails()), StructuredStringFormat.JSON));

        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new InternalServerException(e);
        }
        return Optional.of(statusDto);
    }

    @Override
    public Optional<EhrStatus> getEhrStatus(UUID ehrUuid) {
        //pre-step: check for valid ehrId
        if (hasEhr(ehrUuid).equals(Boolean.FALSE)) {
            throw new ObjectNotFoundException("ehr", "No EHR found with given ID: " + ehrUuid.toString());
        }

        try {

            I_EhrAccess ehrAccess = I_EhrAccess.retrieveInstance(getDataAccess(), ehrUuid);
            if (ehrAccess == null) {
                return Optional.empty();
            }
            return Optional.of(ehrAccess.getStatus());

        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new InternalServerException(e);
        }
    }

    @Override
    public Optional<OriginalVersion<EhrStatus>> getEhrStatusAtVersion(UUID ehrUuid, UUID versionedObjectUid, int version) {
        //pre-step: check for valid ehrId
        if (hasEhr(ehrUuid).equals(Boolean.FALSE)) {
            throw new ObjectNotFoundException("ehr", "No EHR found with given ID: " + ehrUuid.toString());
        }

        I_EhrAccess ehrAccess = I_EhrAccess.retrieveInstanceByStatus(getDataAccess(), ehrUuid, versionedObjectUid, version);
        if (ehrAccess == null) {
            return Optional.empty();
        }

        ObjectVersionId versionId = new ObjectVersionId(versionedObjectUid + "::" + getServerConfig().getNodename() + "::" + version);
        DvCodedText lifecycleState = new DvCodedText("TODO", new CodePhrase("TODO"));   // FIXME VERSIONED_OBJECT_POC: needs meaningful values
        AuditDetails commitAudit = ehrAccess.getStatusAccess().getAuditDetailsAccess().getAsAuditDetails();
        ObjectRef<HierObjectId> contribution = new ObjectRef<>(new HierObjectId(ehrAccess.getStatusAccess().getStatusRecord().getInContribution().toString()), "openehr", "contribution");
        List<UUID> attestationIdList = I_AttestationAccess.retrieveListOfAttestationsByRef(getDataAccess(), ehrAccess.getStatusAccess().getStatusRecord().getAttestationRef());
        List<Attestation> attestations = null;  // as default, gets content if available in the following lines
        if (!attestationIdList.isEmpty()) {
            attestations = new ArrayList<>();
            for (UUID id : attestationIdList) {
                I_AttestationAccess a = new AttestationAccess(getDataAccess()).retrieveInstance(id);
                attestations.add(a.getAsAttestation());
            }
        }
        OriginalVersion<EhrStatus> versionStatus = new OriginalVersion<>(versionId, null, ehrAccess.getStatus(),
                lifecycleState, commitAudit, contribution, null, null, attestations);

        return Optional.of(versionStatus);
    }

    @Override
    public Optional<EhrStatus> updateStatus(UUID ehrId, EhrStatus status) {

        try {
            validationService.check(status);
        } catch (Exception e) {
            // rethrow if this class, but wrap all others in InternalServerException
            if (e.getClass().equals(UnprocessableEntityException.class))
                throw (UnprocessableEntityException) e;
            if (e.getClass().equals(IllegalArgumentException.class))
                throw new ValidationException(e);
            if (e.getClass().equals(ValidationException.class))
                throw e;
            else if (e.getClass().equals(org.ehrbase.validation.constraints.wrappers.ValidationException.class))
                throw new ValidationException(e);
            else
                throw new InternalServerException(e);
        }

        //pre-step: check for valid ehrId
        if (hasEhr(ehrId).equals(Boolean.FALSE)) {
            throw new ObjectNotFoundException("ehr", "No EHR found with given ID: " + ehrId.toString());
        }

        I_EhrAccess ehrAccess;
        try {
            ehrAccess = I_EhrAccess.retrieveInstance(getDataAccess(), ehrId);
        } catch (Exception e) {
            throw new InternalServerException(e);
        }
        if (ehrAccess == null) {
            return Optional.empty();
        }
        if (status != null) {
            ehrAccess.setStatus(status);
        }

        // execute actual update and check for success
        if (ehrAccess.update(getUserUuid(), getSystemUuid(), null, I_ConceptAccess.ContributionChangeType.MODIFICATION, DESCRIPTION).equals(false))
            throw new InternalServerException("Problem updating EHR_STATUS"); //unexpected problem. expected ones are thrown inside of update()

        return getEhrStatus(ehrId);
    }

    @Override
    public Optional<UUID> findBySubject(String subjectId, String nameSpace) {
        UUID subjectUuid = new PersistedPartyRef(getDataAccess()).findInDB(subjectId, nameSpace);
        return Optional.ofNullable(I_EhrAccess.retrieveInstanceBySubject(getDataAccess(), subjectUuid));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean doesEhrExist(UUID ehrId) {
        Optional<I_EhrAccess> ehrAccess = Optional.ofNullable(I_EhrAccess.retrieveInstance(getDataAccess(), ehrId));
        return ehrAccess.isPresent();
    }

    /**
     * Fetches time of creation of specific EHR record
     *
     * @param ehrId
     * @return LocalDateTime instance of timestamp from DB
     */
    public LocalDateTime getCreationTime(UUID ehrId) {
        //pre-step: check for valid ehrId
        if (hasEhr(ehrId).equals(Boolean.FALSE)) {
            throw new ObjectNotFoundException("ehr", "No EHR found with given ID: " + ehrId.toString());
        }

        try {
            I_EhrAccess ehrAccess = I_EhrAccess.retrieveInstance(getDataAccess(), ehrId);
            return ehrAccess.getEhrRecord().getDateCreated().toLocalDateTime();
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new InternalServerException(e);
        }
    }

    @Override
    public Integer getEhrStatusVersionByTimestamp(UUID ehrUid, Timestamp timestamp) {
        I_EhrAccess ehrAccess = I_EhrAccess.retrieveInstance(getDataAccess(), ehrUid);
        return ehrAccess.getStatusAccess().getEhrStatusVersionFromTimeStamp(timestamp);
    }

    /**
     * Get latest version Uid of an EHR_STATUS by given versioned object UID.
     * @param ehrStatusId given versioned object UID
     * @return latest version Uid
     */
    public String getLatestVersionUidOfStatus(UUID ehrStatusId) {
        try {
            I_EhrAccess ehrAccess = I_EhrAccess.retrieveInstance(getDataAccess(), ehrStatusId);
            UUID statusId = ehrAccess.getStatusId();
            Integer version = I_StatusAccess.getLatestVersionNumber(getDataAccess(), statusId);

            return statusId.toString() + "::" + getServerConfig().getNodename() + "::" + version;
        } catch (Exception e) {
            throw new InternalServerException(e);
        }
    }

    public UUID getEhrStatusVersionedObjectUidByEhr(UUID ehrUid) {
        I_EhrAccess ehrAccess = I_EhrAccess.retrieveInstance(getDataAccess(), ehrUid);
        return ehrAccess.getStatusId();
    }

    public Boolean hasEhr(UUID ehrId) {
        I_EhrAccess ehrAccess;
        try {
            ehrAccess = I_EhrAccess.retrieveInstance(getDataAccess(), ehrId);
        } catch (IllegalArgumentException e) {
            return false;
        }
        return ehrAccess != null;   // true if != null; false if == null
    }

    @Override
    public VersionedEhrStatus getVersionedEhrStatus(UUID ehrUid) {

        // FIXME VERSIONED_OBJECT_POC: Pre_has_ehr: has_ehr (an_ehr_id)
        // FIXME VERSIONED_OBJECT_POC: Pre_has_ehr_status_version: has_ehr_status_version (an_ehr_id, a_version_uid)

        Optional<EhrStatus> ehrStatus = getEhrStatus(ehrUid);

        VersionedEhrStatus versionedEhrStatus = new VersionedEhrStatus();
        if (ehrStatus.isPresent()) {
            versionedEhrStatus.setUid(new HierObjectId(ehrStatus.get().getUid().toString()));
            versionedEhrStatus.setOwnerId(new ObjectRef<>(new HierObjectId(ehrUid.toString()), "local", "EHR"));
            I_EhrAccess ehrAccess = I_EhrAccess.retrieveInstance(getDataAccess(), ehrUid);
            versionedEhrStatus.setTimeCreated(new DvDateTime(OffsetDateTime.of(ehrAccess.getStatusAccess().getInitialTimeOfVersionedEhrStatus().toLocalDateTime(),
                    OffsetDateTime.now().getOffset())));
        }

        return versionedEhrStatus;
    }

    @Override
    public RevisionHistory getRevisionHistoryOfVersionedEhrStatus(UUID ehrUid) {
        I_EhrAccess ehrAccess = I_EhrAccess.retrieveInstance(getDataAccess(), ehrUid);

        // get number of versions
        int versions = I_StatusAccess.getLatestVersionNumber(getDataAccess(), ehrAccess.getStatusId());
        // fetch each version
        UUID versionedObjectUid = getEhrStatusVersionedObjectUidByEhr(ehrUid);
        RevisionHistory revisionHistory = new RevisionHistory();
        for (int i = 1; i <= versions; i++) {
            Optional<OriginalVersion<EhrStatus>> ehrStatus = getEhrStatusAtVersion(ehrUid, versionedObjectUid, i);

            // create RevisionHistoryItem for each version and append it to RevisionHistory
            if (ehrStatus.isPresent())
                revisionHistory.addItem(revisionHistoryItemfromEhrStatus(ehrUid, ehrStatus.get(), i));
        }

        if (revisionHistory.getItems().isEmpty()) {
            throw new InternalServerException("Problem creating RevisionHistory"); // never should be empty; not valid
        }
        return revisionHistory;
    }

    private RevisionHistoryItem revisionHistoryItemfromEhrStatus(UUID ehrId, OriginalVersion<EhrStatus> ehrStatus, int version) {

        String statusId = ehrStatus.getUid().getValue().split("::")[0];
        ObjectVersionId objectVersionId = new ObjectVersionId( statusId + "::" + getServerConfig().getNodename() + "::" + version);

        // Note: is List but only has more than one item when there are contributions regarding this object of change type attestation
        List<AuditDetails> auditDetailsList = new ArrayList<>();
        // retrieving the audits
        I_StatusAccess statusAccess = I_StatusAccess.retrieveInstance(getDataAccess(), UUID.fromString(statusId));
        I_AuditDetailsAccess commitAuditAccess = statusAccess.getAuditDetailsAccess();

        String systemId = commitAuditAccess.getSystemId().toString();
        PartyProxy committer = new PersistedPartyProxy(getDataAccess()).retrieve(commitAuditAccess.getCommitter());
        DvDateTime timeCommitted = new DvDateTime(commitAuditAccess.getTimeCommitted().toLocalDateTime());
        DvCodedText changeType = new DvCodedText(commitAuditAccess.getChangeType().getLiteral(), new CodePhrase(new TerminologyId("openehr"), "String"));
        DvText description = new DvText(commitAuditAccess.getDescription());

        AuditDetails commitAudit = new AuditDetails(systemId, committer, timeCommitted, changeType, description);

        auditDetailsList.add(commitAudit);

        // add retrieval of attestations, if there are any
        if (ehrStatus.getAttestations() != null) {
            for (Attestation a : ehrStatus.getAttestations()) {
                AuditDetails newAudit = new AuditDetails(a.getSystemId(), a.getCommitter(), a.getTimeCommitted(), a.getChangeType(), a.getDescription());
                auditDetailsList.add(newAudit);
            }
        }

        return new RevisionHistoryItem(objectVersionId, auditDetailsList);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UUID getDirectoryId(UUID ehrId) {
        try{
            I_EhrAccess ehrAccess = I_EhrAccess.retrieveInstance(getDataAccess(), ehrId);
            return ehrAccess.getDirectoryId();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new InternalServerException(e.getMessage(), e);
        }
    }
}
