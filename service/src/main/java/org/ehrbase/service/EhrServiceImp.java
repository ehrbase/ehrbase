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

import com.nedap.archie.rm.datavalues.quantity.datetime.DvDateTime;
import com.nedap.archie.rm.ehr.EhrStatus;
import com.nedap.archie.rm.ehr.VersionedEhrStatus;
import com.nedap.archie.rm.generic.AuditDetails;
import com.nedap.archie.rm.generic.PartySelf;
import com.nedap.archie.rm.generic.RevisionHistory;
import com.nedap.archie.rm.generic.RevisionHistoryItem;
import com.nedap.archie.rm.support.identification.HierObjectId;
import com.nedap.archie.rm.support.identification.ObjectRef;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import com.nedap.archie.rm.support.identification.PartyRef;
import org.ehrbase.api.definitions.CompositionFormat;
import org.ehrbase.api.definitions.ServerConfig;
import org.ehrbase.api.definitions.StructuredString;
import org.ehrbase.api.definitions.StructuredStringFormat;
import org.ehrbase.api.dto.EhrStatusDto;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.exception.StateConflictException;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.dao.access.interfaces.I_ConceptAccess;
import org.ehrbase.dao.access.interfaces.I_EhrAccess;
import org.ehrbase.dao.access.interfaces.I_PartyIdentifiedAccess;
import org.ehrbase.dao.access.interfaces.I_StatusAccess;
import org.ehrbase.serialisation.CanonicalJson;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional()
public class EhrServiceImp extends BaseService implements EhrService {
    public static final String MODIFIABLE = "modifiable";
    public static final String QUERYABLE = "queryable";
    public static final String SUBJECT_ID = "subjectId";
    public static final String SUBJECT_NAMESPACE = "subjectNamespace";
    public static final String DESCRIPTION = "description";
    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    public EhrServiceImp(KnowledgeCacheService knowledgeCacheService, DSLContext context, ServerConfig serverConfig) {

        super(knowledgeCacheService, context, serverConfig);
    }

    @Override
    public UUID create(EhrStatus status, UUID ehrId) {

        if (status == null) {   // in case of new status with default values
            status = new EhrStatus();
            PartySelf partySelf = new PartySelf(new PartyRef(new HierObjectId(UUID.randomUUID().toString()), "default", null));
            status.setSubject(partySelf);
            status.setModifiable(true);
            status.setQueryable(true);
        }
        status.setUid(new HierObjectId(UUID.randomUUID().toString()));  // server sets own new UUID in both cases (new or given status)

        String subjectId = status.getSubject().getExternalRef().getId().getValue();
        String subjectNamespace = status.getSubject().getExternalRef().getNamespace();

        UUID subjectUuid = I_PartyIdentifiedAccess.getOrCreatePartyByExternalRef(getDataAccess(), null, subjectId, BaseService.DEMOGRAPHIC, subjectNamespace, BaseService.PARTY);
        UUID systemId = getSystemUuid();
        UUID committerId = getUserUuid();

        if (I_EhrAccess.checkExist(getDataAccess(), subjectUuid))
            throw new StateConflictException("Specified party has already an EHR set (partyId=" + subjectUuid + ")");

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

            I_PartyIdentifiedAccess party = I_PartyIdentifiedAccess.retrieveInstance(getDataAccess(), ehrAccess.getParty());

            statusDto.setSubjectId(party.getPartyRefValue());
            statusDto.setSubjectNamespace(party.getPartyRefNamespace());
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
    public Optional<EhrStatus> getEhrStatusAtVersion(UUID ehrUuid, UUID versionedObjectUid, int version) {
        //pre-step: check for valid ehrId
        if (hasEhr(ehrUuid).equals(Boolean.FALSE)) {
            throw new ObjectNotFoundException("ehr", "No EHR found with given ID: " + ehrUuid.toString());
        }

        I_EhrAccess ehrAccess = I_EhrAccess.retrieveInstanceByStatus(getDataAccess(), ehrUuid, versionedObjectUid, version);
        if (ehrAccess == null) {
            return Optional.empty();
        }
        return Optional.of(ehrAccess.getStatus());
    }

    @Override
    public Optional<EhrStatus> updateStatus(UUID ehrId, EhrStatus status) {
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
        UUID subjectUuid = I_PartyIdentifiedAccess.findReferencedParty(getDataAccess(), subjectId, BaseService.DEMOGRAPHIC, nameSpace, BaseService.PARTY);
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
        return ehrAccess.getEhrStatusVersionFromTimeStamp(timestamp);
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
            Integer version = ehrAccess.getLastVersionNumberOfStatus(getDataAccess(), statusId);

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
            versionedEhrStatus.setTimeCreated(new DvDateTime(ehrAccess.getInitialTimeOfVersionedEhrStatus().toLocalDateTime()));

        }

        return versionedEhrStatus;
    }

    @Override
    public RevisionHistory getRevisionHistoryOfVersionedEhrStatus(UUID ehrUid) {
        I_EhrAccess ehrAccess = I_EhrAccess.retrieveInstance(getDataAccess(), ehrUid);

        // get number of versions
        int versions = ehrAccess.getNumberOfEhrStatusVersions();
        // fetch each version
        UUID versionedObjectUid = getEhrStatusVersionedObjectUidByEhr(ehrUid);
        RevisionHistory revisionHistory = new RevisionHistory();
        for (int i = 1; i <= versions; i++) {
            Optional<EhrStatus> ehrStatus = getEhrStatusAtVersion(ehrUid, versionedObjectUid, i);

            // FIXME VERSIONED_OBJECT_POC: create RevisionHistoryItem for each version and append it to RevisionHistory
            if (ehrStatus.isPresent())
                revisionHistory.addItem(revisionHistoryItemfromEhrStatus(ehrStatus.get(), i));
        }

        if (revisionHistory.getItems().isEmpty()) {
            throw new InternalServerException("Problem creating RevisionHistory"); // never should be empty; not valid
        }
        return revisionHistory;
    }

    private RevisionHistoryItem revisionHistoryItemfromEhrStatus(EhrStatus ehrStatus, int version) {

        String statusId = ehrStatus.getUid().getRoot().getValue();
        ObjectVersionId objectVersionId = new ObjectVersionId( statusId + "::" + getServerConfig().getNodename() + "::" + version);

        // Note: is List but only has more than one item when there are contributions regarding this object of change type attestation
        List<AuditDetails> auditDetails = new ArrayList<>();
        // FIXME VERSIONED_OBJECT_POC: retrieving the audits
        I_StatusAccess statusAccess = I_StatusAccess.retrieveInstance(getDataAccess(), UUID.fromString(statusId));
        //statusAccess.getAuditDetailsAccess().

        return new RevisionHistoryItem(objectVersionId, auditDetails);
    }
}
