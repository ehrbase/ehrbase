/*
 * Copyright (c) 2024 vitasystems GmbH.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.service;

import static org.ehrbase.repository.AbstractVersionedObjectRepository.buildObjectVersionId;

import com.nedap.archie.rm.changecontrol.OriginalVersion;
import com.nedap.archie.rm.datavalues.DvText;
import com.nedap.archie.rm.datavalues.quantity.datetime.DvDateTime;
import com.nedap.archie.rm.ehr.EhrStatus;
import com.nedap.archie.rm.ehr.VersionedEhrStatus;
import com.nedap.archie.rm.generic.Attestation;
import com.nedap.archie.rm.generic.AuditDetails;
import com.nedap.archie.rm.generic.PartyProxy;
import com.nedap.archie.rm.generic.PartySelf;
import com.nedap.archie.rm.generic.RevisionHistory;
import com.nedap.archie.rm.generic.RevisionHistoryItem;
import com.nedap.archie.rm.support.identification.ObjectId;
import com.nedap.archie.rm.support.identification.ObjectRef;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import com.nedap.archie.rm.support.identification.PartyRef;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.exception.StateConflictException;
import org.ehrbase.api.exception.UnprocessableEntityException;
import org.ehrbase.api.exception.ValidationException;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.api.service.SystemService;
import org.ehrbase.api.service.ValidationService;
import org.ehrbase.repository.CompositionRepository;
import org.ehrbase.repository.EhrFolderRepository;
import org.ehrbase.repository.EhrRepository;
import org.ehrbase.repository.experimental.ItemTagRepository;
import org.ehrbase.util.UuidGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service(value = "ehrService")
@Transactional()
public class EhrServiceImp implements EhrService {
    public static final String DESCRIPTION = "description";
    public static final String PARTY_ID_ALREADY_USED =
            "Supplied partyId[%s] is used by a different EHR in the same partyNamespace[%s].";
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ValidationService validationService;

    private final EhrFolderRepository ehrFolderRepository;
    private final CompositionRepository compositionRepository;
    private final ItemTagRepository itemTagRepository;

    private final EhrRepository ehrRepository;
    private final SystemService systemService;

    @Autowired
    public EhrServiceImp(
            ValidationService validationService,
            SystemService systemService,
            EhrFolderRepository ehrFolderRepository,
            CompositionRepository compositionRepository,
            EhrRepository ehrRepository,
            ItemTagRepository itemTagRepository) {

        this.validationService = validationService;

        this.ehrFolderRepository = ehrFolderRepository;
        this.compositionRepository = compositionRepository;
        this.ehrRepository = ehrRepository;
        this.itemTagRepository = itemTagRepository;

        this.systemService = systemService;
    }

    @Override
    public UUID create(UUID ehrId, EhrStatus status) {

        check(status);

        ehrId = Optional.ofNullable(ehrId).orElseGet(UuidGenerator::randomUUID);

        if (hasEhr(ehrId)) {
            throw new StateConflictException("EHR with this ID already exists");
        }

        if (status == null) { // in case of new status with default values
            status = new EhrStatus();
            status.setArchetypeNodeId("openEHR-EHR-EHR_STATUS.generic.v1");
            status.setName(new DvText("EHR Status"));
            status.setSubject(new PartySelf(null));
            status.setModifiable(true);
            status.setQueryable(true);
        } else {
            checkEhrExistForParty(ehrId, status);
        }

        status.setUid(buildObjectVersionId(
                UuidGenerator.randomUUID(),
                1,
                systemService)); // server sets own new UUID in both cases (new or given status)

        ehrRepository.commit(ehrId, status, null, null);

        return ehrId;
    }

    @Override
    public EhrStatus getEhrStatus(UUID ehrUuid) {

        Optional<EhrStatus> head = ehrRepository.findHead(ehrUuid);

        if (head.isEmpty() && !hasEhr(ehrUuid)) {
            throw new ObjectNotFoundException("ehr", "No EHR found with given ID: " + ehrUuid.toString());
        }

        return head.orElseThrow();
    }

    @Override
    public Optional<OriginalVersion<EhrStatus>> getEhrStatusAtVersion(
            UUID ehrUuid, UUID versionedObjectUid, int version) {
        // pre-step: check for valid ehrId
        if (!hasEhr(ehrUuid)) {
            throw new ObjectNotFoundException("ehr", "No EHR found with given ID: " + ehrUuid.toString());
        }

        return ehrRepository.getOriginalVersionStatus(ehrUuid, versionedObjectUid, version);
    }

    @Override
    public ObjectVersionId updateStatus(
            UUID ehrId, EhrStatus status, ObjectVersionId ifMatch, UUID contributionId, UUID audit) {

        // pre-step: check for valid ehrId
        if (!hasEhr(ehrId)) {
            throw new ObjectNotFoundException("ehr", "No EHR found with given ID: " + ehrId.toString());
        }

        // set uuid to validate it
        status.setUid(ifMatch);
        check(status);
        checkEhrExistForParty(ehrId, status);

        UUID compId = UUID.fromString(ifMatch.getObjectId().getValue());
        int version = Integer.parseInt(ifMatch.getVersionTreeId().getValue());
        // set correct uuid with changed version
        status.setUid(buildObjectVersionId(compId, version + 1, systemService));

        // pre-step: check for valid ehrId
        if (!hasEhr(ehrId)) {
            throw new ObjectNotFoundException("ehr", "No EHR found with given ID: " + ehrId.toString());
        }

        ehrRepository.update(ehrId, status, contributionId, audit);

        return (ObjectVersionId) status.getUid();
    }

    private void checkEhrExistForParty(UUID ehrId, EhrStatus status) {
        Optional<PartyRef> partyRef =
                Optional.ofNullable(status).map(EhrStatus::getSubject).map(PartyProxy::getExternalRef);

        if (partyRef.isPresent()) {
            String subjectId = partyRef.get().getId().getValue();
            String namespace = partyRef.get().getNamespace();
            Optional<UUID> ehrIdOpt = findBySubject(subjectId, namespace);
            if (ehrIdOpt.isPresent() && !ehrIdOpt.get().equals(ehrId)) {
                throw new StateConflictException(String.format(PARTY_ID_ALREADY_USED, subjectId, namespace));
            }
        }
    }

    private void check(EhrStatus status) {
        try {
            validationService.check(status);
        } catch (Exception ex) {
            // rethrow if this class, but wrap all others in InternalServerException
            switch (ex) {
                case UnprocessableEntityException e -> throw e;
                case IllegalArgumentException e -> throw new ValidationException(e.getMessage(), e);
                case ValidationException e -> throw e;
                default -> throw new InternalServerException(ex.getMessage(), ex);
            }
        }
    }

    @Override
    public Optional<UUID> findBySubject(String subjectId, String nameSpace) {

        return ehrRepository.findBySubject(subjectId, nameSpace);
    }

    /**
     * Fetches time of creation of specific EHR record
     *
     * @param ehrId
     * @return LocalDateTime instance of timestamp from DB
     */
    public DvDateTime getCreationTime(UUID ehrId) {
        // pre-step: check for valid ehrId
        if (!hasEhr(ehrId)) {
            throw new ObjectNotFoundException("ehr", "No EHR found with given ID: " + ehrId.toString());
        }

        return new DvDateTime(ehrRepository.findEhrCreationTime(ehrId));
    }

    @Override
    public ObjectVersionId getEhrStatusVersionByTimestamp(UUID ehrUid, OffsetDateTime timestamp) {

        if (!hasEhr(ehrUid)) {
            throw new ObjectNotFoundException("ehr", "No EHR found with given ID: " + ehrUid.toString());
        }

        return ehrRepository
                .findVersionByTime(ehrUid, timestamp)
                .orElseThrow(() ->
                        new ObjectNotFoundException("ehr_status", "No EHR_STATUS with given timestamp: " + timestamp));
    }

    public ObjectVersionId getLatestVersionUidOfStatus(UUID ehrUid) {
        if (!hasEhr(ehrUid)) {
            throw new ObjectNotFoundException("ehr", "No EHR found with given ID: " + ehrUid.toString());
        }

        return ehrRepository.findLatestVersion(ehrUid).orElseThrow();
    }

    @Override
    public boolean hasEhr(UUID ehrId) {
        return ehrRepository.hasEhr(ehrId);
    }

    @Override
    /*TODO This method should be cached...
    For contributions it may be called n times where n is the number of versions in the contribution which will in turn mean
    n SQL queries are performed*/
    public Boolean isModifiable(UUID ehrId) {
        return ehrRepository.fetchIsModifiable(ehrId);
    }

    @Override
    public VersionedEhrStatus getVersionedEhrStatus(UUID ehrUid) {

        // FIXME VERSIONED_OBJECT_POC: Pre_has_ehr: has_ehr (an_ehr_id)
        // FIXME VERSIONED_OBJECT_POC: Pre_has_ehr_status_version: has_ehr_status_version (an_ehr_id,
        // a_version_uid)

        if (!hasEhr(ehrUid)) {
            throw new ObjectNotFoundException("ehr", "No EHR found with given ID: " + ehrUid.toString());
        }

        return ehrRepository.getVersionedEhrStatus(ehrUid).orElseThrow();
    }

    @Override
    public RevisionHistory getRevisionHistoryOfVersionedEhrStatus(UUID ehrUid) {

        // get number of versions
        int versions = Integer.parseInt(
                getLatestVersionUidOfStatus(ehrUid).getVersionTreeId().getValue());
        // fetch each version
        UUID versionedObjectUid = UUID.fromString(
                getLatestVersionUidOfStatus(ehrUid).getObjectId().getValue());
        RevisionHistory revisionHistory = new RevisionHistory();
        for (int i = 1; i <= versions; i++) {
            Optional<OriginalVersion<EhrStatus>> ehrStatus = getEhrStatusAtVersion(ehrUid, versionedObjectUid, i);

            // create RevisionHistoryItem for each version and append it to RevisionHistory
            if (ehrStatus.isPresent()) revisionHistory.addItem(revisionHistoryItemFromEhrStatus(ehrStatus.get(), i));
        }

        if (revisionHistory.getItems().isEmpty()) {
            throw new InternalServerException("Problem creating RevisionHistory"); // never should be empty; not valid
        }
        return revisionHistory;
    }

    private RevisionHistoryItem revisionHistoryItemFromEhrStatus(OriginalVersion<EhrStatus> ehrStatus, int version) {

        String statusId = ehrStatus.getUid().getValue().split("::")[0];
        ObjectVersionId objectVersionId =
                new ObjectVersionId(statusId + "::" + systemService.getSystemId() + "::" + version);

        // Note: is List but only has more than one item when there are contributions regarding this object of change
        // type attestation
        List<AuditDetails> auditDetailsList = new ArrayList<>();
        // retrieving the audits
        auditDetailsList.add(ehrStatus.getCommitAudit());

        // add retrieval of attestations, if there are any
        if (ehrStatus.getAttestations() != null) {
            for (Attestation a : ehrStatus.getAttestations()) {
                AuditDetails newAudit = new AuditDetails(
                        a.getSystemId(), a.getCommitter(), a.getTimeCommitted(), a.getChangeType(), a.getDescription());
                auditDetailsList.add(newAudit);
            }
        }

        return new RevisionHistoryItem(objectVersionId, auditDetailsList);
    }

    /**
     * {@inheritDoc}
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public void adminDeleteEhr(UUID ehrId) {

        ehrFolderRepository.adminDelete(ehrId, null);
        compositionRepository.adminDeleteAll(ehrId);
        itemTagRepository.adminDeleteAll(ehrId);
        ehrRepository.adminDelete(ehrId);
    }

    @Override
    public List<String> getSubjectExtRefs(Collection<String> ehrIds) {

        return ehrIds.stream().map(this::getSubjectExtRef).toList();
    }

    @Override
    public String getSubjectExtRef(String ehrId) {
        EhrStatus ehrStatus = getEhrStatus(UUID.fromString(ehrId));
        return Optional.of(ehrStatus)
                .map(EhrStatus::getSubject)
                .map(PartyProxy::getExternalRef)
                .map(ObjectRef::getId)
                .map(ObjectId::getValue)
                .orElse(null);
    }
}
