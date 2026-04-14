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

import com.nedap.archie.rm.archetyped.Archetyped;
import com.nedap.archie.rm.changecontrol.OriginalVersion;
import com.nedap.archie.rm.datavalues.DvText;
import com.nedap.archie.rm.datavalues.quantity.datetime.DvDateTime;
import com.nedap.archie.rm.ehr.EhrStatus;
import com.nedap.archie.rm.ehr.VersionedEhrStatus;
import com.nedap.archie.rm.generic.PartySelf;
import com.nedap.archie.rm.generic.RevisionHistory;
import com.nedap.archie.rm.support.identification.ArchetypeID;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import com.nedap.archie.rm.support.identification.PartyRef;
import java.time.OffsetDateTime;
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
import org.ehrbase.api.util.LocatableUtils;
import org.ehrbase.openehr.sdk.util.rmconstants.RmConstants;
import org.ehrbase.repository.CompositionRepository;
import org.ehrbase.repository.EhrFolderRepository;
import org.ehrbase.repository.EhrRepository;
import org.ehrbase.repository.experimental.ItemTagRepository;
import org.ehrbase.util.UuidGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.NoTransactionException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

@Service(value = "ehrService")
@Transactional()
public class EhrServiceImp implements EhrService {

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
    public UUID create(UUID ehrId, EhrStatus statusToCreate) {
        if (ehrId == null) {
            ehrId = UuidGenerator.randomUUID();
        }
        // status always gets a new UUID
        ObjectVersionId statusVersionId = buildObjectVersionId(UuidGenerator.randomUUID(), 1, systemService);

        EhrStatus status;
        if (statusToCreate == null) { // in case of new status with default values
            status = new EhrStatus(
                    statusVersionId,
                    "openEHR-EHR-EHR_STATUS.generic.v1",
                    new DvText("EHR Status"),
                    new Archetyped(new ArchetypeID("openEHR-EHR-EHR_STATUS.generic.v1"), RmConstants.RM_VERSION_1_0_4),
                    null,
                    null,
                    null,
                    null,
                    new PartySelf(),
                    true,
                    true,
                    null);
        } else {
            status = statusToCreate;
            status.setUid(statusVersionId);
            // pre-step: validate
            validateEhrStatus(status);
        }
        try {
            ehrRepository.commit(ehrId, status, null, null);
        } catch (DuplicateKeyException e) {
            checkEhrExistForParty(e, status);
            if (hasEhr(ehrId)) {
                throw new StateConflictException("EHR with id %s already exists.".formatted(ehrId));
            } else {
                throw e;
            }
        }

        return ehrId;
    }

    @Override
    public EhrStatus updateStatus(
            UUID ehrId, EhrStatus ehrStatus, ObjectVersionId ifMatch, UUID contributionId, UUID audit) {

        UUID ehrStatusId = UUID.fromString(ifMatch.getObjectId().getValue());
        int version = LocatableUtils.getUidVersion(ifMatch);

        // set correct next id with incremented version
        ObjectVersionId statusVersionId = buildObjectVersionId(ehrStatusId, version + 1, systemService);
        ehrStatus.setUid(statusVersionId);

        validateEhrStatus(ehrStatus);

        try {
            ehrRepository.update(ehrId, ehrStatus, contributionId, audit);
        } catch (DuplicateKeyException e) {
            checkEhrExistForParty(e, ehrStatus);
            throw e;
        }

        return getEhrStatus(ehrId);
    }

    @Override
    public EhrStatus getEhrStatus(UUID ehrId) {
        return ehrRepository.findHead(ehrId).orElseThrow(() -> ehrNotFoundException(ehrId));
    }

    @Override
    public Optional<OriginalVersion<EhrStatus>> getEhrStatusAtVersion(
            UUID ehrId, UUID versionedObjectUid, int version) {
        // pre-step: check for valid ehrId
        ensureEhrExist(ehrId);

        return ehrRepository.getOriginalVersionStatus(ehrId, versionedObjectUid, version);
    }

    private void checkEhrExistForParty(DuplicateKeyException e, EhrStatus status) throws StateConflictException {
        if (e.getMessage().contains("\"ehr_status_subject_idx\"")) {
            PartyRef pRef = status.getSubject().getExternalRef();
            throw new StateConflictException(
                    "Supplied partyId[%s] is used by a different EHR in the same partyNamespace[%s]."
                            .formatted(pRef.getId().getValue(), pRef.getNamespace()));
        }
    }

    private void validateEhrStatus(EhrStatus status) {
        try {
            validationService.check(status);
        } catch (UnprocessableEntityException | ValidationException | InternalServerException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new InternalServerException(e.getMessage(), e);
        }
    }

    @Override
    public Optional<UUID> findBySubject(String subjectId, String nameSpace) {

        return ehrRepository.findBySubject(subjectId, nameSpace);
    }

    @Override
    public ObjectVersionId getEhrStatusVersionByTimestamp(UUID ehrId, OffsetDateTime timestamp) {
        // pre-step: check for valid ehrId
        ensureEhrExist(ehrId);

        return ehrRepository
                .findVersionByTime(ehrId, timestamp)
                .orElseThrow(() -> new ObjectNotFoundException(
                        "ehr_status", "No EHR_STATUS with given timestamp: %s".formatted(timestamp)));
    }

    public ObjectVersionId getLatestVersionUidOfStatus(UUID ehrId) {
        Optional<ObjectVersionId> latestVersion = ehrRepository.findLatestVersion(ehrId);
        if (latestVersion.isEmpty()) {
            ensureEhrExist(ehrId);
        }

        return latestVersion.orElseThrow();
    }

    public DvDateTime getCreationTime(UUID ehrId) {
        // pre-step: check for valid ehrId
        ensureEhrExist(ehrId);

        return new DvDateTime(ehrRepository.findEhrCreationTime(ehrId));
    }

    @Override
    public boolean hasEhr(UUID ehrId) {
        if (isIsRollbackOnly()) {
            return ehrRepository.hasEhrNewTransaction(ehrId);
        } else {
            return ehrRepository.hasEhr(ehrId);
        }
    }

    private static boolean isIsRollbackOnly() {
        try {
            return TransactionAspectSupport.currentTransactionStatus().isRollbackOnly();
        } catch (NoTransactionException _) {
            return false;
        }
    }

    @Override
    public VersionedEhrStatus getVersionedEhrStatus(UUID ehrId) {
        return ehrRepository.getVersionedEhrStatus(ehrId).orElseThrow(() -> ehrNotFoundException(ehrId));
    }

    @Override
    public RevisionHistory getRevisionHistoryOfVersionedEhrStatus(UUID ehrUid) {

        RevisionHistory revisionHistory = ehrRepository.getRevisionHistory(ehrUid);

        if (revisionHistory.getItems().isEmpty()) {
            throw ehrNotFoundException(ehrUid);
        }
        return revisionHistory;
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
    public String getSubjectExtRef(UUID ehrId) {
        return ehrRepository.getSubjectExternalRef(ehrId);
    }

    @Override
    public void checkEhrExists(UUID ehrId) {
        if (ehrId == null || !hasEhr(ehrId)) {
            throw new ObjectNotFoundException("EHR", String.format("EHR with id %s not found", ehrId));
        }
    }

    @Override
    public void checkEhrExistsAndIsModifiable(UUID ehrId) {

        boolean modifiable = Optional.ofNullable(ehrRepository.fetchIsModifiable(ehrId))
                .orElseThrow(
                        () -> new ObjectNotFoundException("EHR", String.format("EHR with id %s not found", ehrId)));

        if (!modifiable) {
            throw new StateConflictException(String.format("EHR with id %s does not allow modification", ehrId));
        }
    }

    private void ensureEhrExist(UUID ehrId) {
        // pre-step: check for valid ehrId
        if (!hasEhr(ehrId)) {
            throw ehrNotFoundException(ehrId);
        }
    }

    private static ObjectNotFoundException ehrNotFoundException(UUID ehrId) {
        return new ObjectNotFoundException("ehr", String.format("No EHR found with given ID: %s", ehrId));
    }
}
