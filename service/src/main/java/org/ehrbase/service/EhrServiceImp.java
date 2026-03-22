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

import com.nedap.archie.rm.changecontrol.OriginalVersion;
import com.nedap.archie.rm.datavalues.DvText;
import com.nedap.archie.rm.datavalues.quantity.datetime.DvDateTime;
import com.nedap.archie.rm.ehr.EhrStatus;
import com.nedap.archie.rm.ehr.VersionedEhrStatus;
import com.nedap.archie.rm.generic.PartySelf;
import com.nedap.archie.rm.generic.RevisionHistory;
import com.nedap.archie.rm.support.identification.HierObjectId;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import com.nedap.archie.rm.support.identification.UIDBasedId;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.api.service.SystemService;
import org.ehrbase.api.service.ValidationService;
import org.ehrbase.repository.ContributionRepository;
import org.ehrbase.repository.EhrRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for EHR and EHR_STATUS operations.
 * Rewritten for the new normalized schema — no DbToRmFormat or JSONB dependencies.
 */
@Service
@Transactional
public class EhrServiceImp implements EhrService {

    private static final Logger log = LoggerFactory.getLogger(EhrServiceImp.class);

    private final EhrRepository ehrRepository;
    private final ContributionRepository contributionRepository;
    private final ValidationService validationService;
    private final SystemService systemService;

    public EhrServiceImp(
            EhrRepository ehrRepository,
            ContributionRepository contributionRepository,
            ValidationService validationService,
            SystemService systemService) {
        this.ehrRepository = ehrRepository;
        this.contributionRepository = contributionRepository;
        this.validationService = validationService;
        this.systemService = systemService;
    }

    @Override
    public UUID create(UUID ehrId, EhrStatus status) {
        if (status == null) {
            status = createDefaultEhrStatus();
        }

        validationService.check(status);

        // Insert EHR row first (so FK from contribution → ehr is satisfied)
        UUID createdEhrId = ehrRepository.insertEhrRow(ehrId, status);

        // Now create the contribution (EHR row exists, FK is valid)
        UUID contributionId = contributionRepository.createContribution(createdEhrId, "ehr", "creation");

        // Insert EHR_STATUS with the contribution reference
        ehrRepository.insertEhrStatus(createdEhrId, status, contributionId);

        log.debug("Created EHR: id={}", createdEhrId);
        return createdEhrId;
    }

    @Override
    public EhrStatus updateStatus(
            UUID ehrId, EhrStatus status, ObjectVersionId targetObjId, UUID contribution, UUID audit) {
        Objects.requireNonNull(ehrId, "ehrId must not be null");
        Objects.requireNonNull(status, "status must not be null");

        checkEhrExistsAndIsModifiable(ehrId);
        validationService.check(status);

        int expectedVersion = extractVersion(targetObjId);

        if (contribution == null) {
            contribution = contributionRepository.createContribution(ehrId, "ehr", "modification");
        }

        ehrRepository.updateEhrStatus(ehrId, status, expectedVersion, contribution);

        return ehrRepository
                .findCurrentStatus(ehrId)
                .orElseThrow(() -> new ObjectNotFoundException("ehr_status", ehrId.toString()));
    }

    @Override
    public EhrStatus getEhrStatus(UUID ehrUuid) {
        checkEhrExists(ehrUuid);
        return ehrRepository
                .findCurrentStatus(ehrUuid)
                .orElseThrow(() -> new ObjectNotFoundException("ehr_status", ehrUuid.toString()));
    }

    @Override
    public Optional<OriginalVersion<EhrStatus>> getEhrStatusAtVersion(
            UUID ehrUuid, UUID versionedObjectUid, int version) {
        checkEhrExists(ehrUuid);
        Optional<EhrStatus> status = ehrRepository.findStatusByVersion(ehrUuid, version);
        if (status.isEmpty()) {
            return Optional.empty();
        }
        OriginalVersion<EhrStatus> originalVersion = new OriginalVersion<>();
        originalVersion.setUid(new ObjectVersionId(
                versionedObjectUid.toString() + "::" + systemService.getSystemId() + "::" + version));
        originalVersion.setData(status.get());
        return Optional.of(originalVersion);
    }

    @Override
    public Optional<UUID> findBySubject(String subjectId, String nameSpace) {
        return ehrRepository.findBySubject(subjectId, nameSpace);
    }

    @Override
    public ObjectVersionId getLatestVersionUidOfStatus(UUID ehrId) {
        checkEhrExists(ehrId);
        int version = ehrRepository
                .getLatestStatusVersion(ehrId)
                .orElseThrow(() -> new ObjectNotFoundException("ehr_status", ehrId.toString()));
        return new ObjectVersionId(ehrId.toString() + "::" + systemService.getSystemId() + "::" + version);
    }

    @Override
    public ObjectVersionId getEhrStatusVersionByTimestamp(UUID ehrUid, OffsetDateTime timestamp) {
        checkEhrExists(ehrUid);
        Optional<EhrStatus> status = ehrRepository.findStatusAtTime(ehrUid, timestamp);
        if (status.isEmpty()) {
            throw new ObjectNotFoundException(
                    "ehr_status", "No EHR_STATUS found at timestamp %s for EHR %s".formatted(timestamp, ehrUid));
        }
        int version = extractVersion(status.get().getUid());
        return new ObjectVersionId(ehrUid.toString() + "::" + systemService.getSystemId() + "::" + version);
    }

    @Override
    public DvDateTime getCreationTime(UUID ehrId) {
        OffsetDateTime creationTime = ehrRepository
                .getCreationTime(ehrId)
                .orElseThrow(() -> new ObjectNotFoundException("ehr", ehrId.toString()));
        return new DvDateTime(creationTime);
    }

    @Override
    public boolean hasEhr(UUID ehrId) {
        return ehrRepository.ehrExists(ehrId);
    }

    @Override
    public VersionedEhrStatus getVersionedEhrStatus(UUID ehrId) {
        checkEhrExists(ehrId);
        VersionedEhrStatus vs = new VersionedEhrStatus();
        vs.setUid(new HierObjectId(ehrId.toString()));
        vs.setOwnerId(new com.nedap.archie.rm.support.identification.ObjectRef<>(
                new HierObjectId(ehrId.toString()), "local", "EHR"));
        return vs;
    }

    @Override
    public RevisionHistory getRevisionHistoryOfVersionedEhrStatus(UUID ehrId) {
        checkEhrExists(ehrId);
        return new RevisionHistory();
    }

    @Override
    public void adminDeleteEhr(UUID ehrId) {
        throw new UnsupportedOperationException("Admin delete not yet implemented");
    }

    @Override
    public String getSubjectExtRef(String ehrId) {
        EhrStatus status = getEhrStatus(UUID.fromString(ehrId));
        if (status.getSubject() instanceof PartySelf self
                && self.getExternalRef() != null
                && self.getExternalRef().getId() != null) {
            return self.getExternalRef().getId().getValue();
        }
        return null;
    }

    @Override
    public void checkEhrExists(UUID ehrId) {
        if (!ehrRepository.ehrExists(ehrId)) {
            throw new ObjectNotFoundException("ehr", ehrId.toString());
        }
    }

    @Override
    public void checkEhrExistsAndIsModifiable(UUID ehrId) {
        ehrRepository.checkEhrExistsAndIsModifiable(ehrId);
    }

    private EhrStatus createDefaultEhrStatus() {
        EhrStatus status = new EhrStatus();
        status.setArchetypeNodeId("openEHR-EHR-EHR_STATUS.generic.v1");
        status.setName(new DvText("EHR Status"));
        status.setSubject(new PartySelf());
        status.setQueryable(true);
        status.setModifiable(true);
        return status;
    }

    private static int extractVersion(Object versionId) {
        if (versionId == null) return 1;
        String id;
        if (versionId instanceof ObjectVersionId ovid) {
            id = ovid.getValue();
        } else if (versionId instanceof UIDBasedId uid) {
            id = uid.getValue();
        } else {
            return 1;
        }
        int lastSep = id.lastIndexOf("::");
        return lastSep > 0 ? Integer.parseInt(id.substring(lastSep + 2)) : 1;
    }
}
