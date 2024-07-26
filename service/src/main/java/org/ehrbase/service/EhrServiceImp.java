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
import static org.ehrbase.util.OriginalVersionUtil.originalVersionCopyWithData;

import com.nedap.archie.rm.changecontrol.OriginalVersion;
import com.nedap.archie.rm.changecontrol.VersionedObject;
import com.nedap.archie.rm.datavalues.DvText;
import com.nedap.archie.rm.datavalues.quantity.datetime.DvDateTime;
import com.nedap.archie.rm.ehr.EhrStatus;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.ehrbase.api.dto.EhrStatusDto;
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
import org.ehrbase.service.maping.EhrStatusMapper;
import org.ehrbase.util.UuidGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service(value = "ehrService")
@Transactional()
public class EhrServiceImp implements EhrService {

    private final ValidationService validationService;

    private final EhrFolderRepository ehrFolderRepository;
    private final CompositionRepository compositionRepository;

    private final EhrRepository ehrRepository;
    private final SystemService systemService;

    @Autowired
    public EhrServiceImp(
            ValidationService validationService,
            SystemService systemService,
            EhrFolderRepository ehrFolderRepository,
            CompositionRepository compositionRepository,
            EhrRepository ehrRepository) {

        this.validationService = validationService;

        this.ehrFolderRepository = ehrFolderRepository;
        this.compositionRepository = compositionRepository;
        this.ehrRepository = ehrRepository;

        this.systemService = systemService;
    }

    @Override
    public EhrResult create(@Nullable UUID ehrId, @Nullable EhrStatusDto status) {

        // pre-step: use or create UUID
        ehrId = Optional.ofNullable(ehrId).orElseGet(UuidGenerator::randomUUID);

        if (hasEhr(ehrId)) {
            throw new StateConflictException("EHR with this ID already exists");
        }

        if (status == null) { // in case of new status with default values
            status = new EhrStatusDto(
                    null,
                    "openEHR-EHR-EHR_STATUS.generic.v1",
                    new DvText("EHR Status"),
                    null,
                    null,
                    new PartySelf(null),
                    true,
                    true,
                    null);
        } else {
            // pre-step: validate
            check(status);
            checkEhrExistForParty(ehrId, status);
        }

        // server sets own new UUID in both cases (new or given status)
        ObjectVersionId statusVersionId = buildObjectVersionId(UuidGenerator.randomUUID(), 1, systemService);
        status = ehrStatusDtoWithId(status, statusVersionId);

        ehrRepository.commit(ehrId, EhrStatusMapper.fromDto(status), null, null);

        return new EhrResult(ehrId, statusVersionId, status);
    }

    @Override
    public EhrResult updateStatus(
            UUID ehrId, EhrStatusDto status, ObjectVersionId ifMatch, UUID contributionId, UUID audit) {

        // pre-step: validate + check for valid ehrId
        check(status);
        ensureEhrExist(ehrId);

        // set uuid to validate it
        status = ehrStatusDtoWithId(status, ifMatch);
        checkEhrExistForParty(ehrId, status);

        UUID compId = UUID.fromString(ifMatch.getObjectId().getValue());
        int version = Integer.parseInt(ifMatch.getVersionTreeId().getValue());

        // set correct uuid with changed version
        ObjectVersionId statusVersionId = buildObjectVersionId(compId, version + 1, systemService);
        status = ehrStatusDtoWithId(status, statusVersionId);

        ehrRepository.update(ehrId, EhrStatusMapper.fromDto(status), contributionId, audit);

        return new EhrResult(ehrId, statusVersionId, status);
    }

    @Override
    public EhrResult getEhrStatus(UUID ehrId) {

        // pre-step: check for valid ehrId
        checkEhrExists(ehrId);

        Optional<EhrStatus> head = ehrRepository.findHead(ehrId);

        // post-step: check for valid head
        if (head.isEmpty()) {
            raiseEhrNotFoundException(ehrId);
        }

        return head.map(EhrStatusMapper::toDto)
                .map(dto -> new EhrResult(ehrId, ((ObjectVersionId) dto.uid()), dto))
                .orElseThrow();
    }

    @Override
    public Optional<OriginalVersion<EhrStatusDto>> getEhrStatusAtVersion(
            UUID ehrId, UUID versionedObjectUid, int version) {
        // pre-step: check for valid ehrId
        ensureEhrExist(ehrId);

        return ehrRepository
                .getOriginalVersionStatus(ehrId, versionedObjectUid, version)
                .map(ov -> originalVersionCopyWithData(ov, EhrStatusMapper.toDto(ov.getData())));
    }

    private void checkEhrExistForParty(UUID ehrId, EhrStatusDto status) {
        Optional<PartyRef> partyRef =
                Optional.ofNullable(status).map(EhrStatusDto::subject).map(PartyProxy::getExternalRef);

        if (partyRef.isPresent()) {
            String subjectId = partyRef.get().getId().getValue();
            String namespace = partyRef.get().getNamespace();
            Optional<UUID> ehrIdOpt = findBySubject(subjectId, namespace);
            if (ehrIdOpt.isPresent() && !ehrIdOpt.get().equals(ehrId)) {
                throw new StateConflictException(String.format(
                        "Supplied partyId[%s] is used by a different EHR in the same partyNamespace[%s].",
                        subjectId, namespace));
            }
        }
    }

    private void check(@Nonnull EhrStatusDto status) {
        try {
            validationService.check(status);
        } catch (Exception ex) {
            // rethrow if this class, but wrap all others in InternalServerException
            switch (ex) {
                case UnprocessableEntityException e -> throw e;
                case ValidationException e -> throw e;
                default -> throw new InternalServerException(ex.getMessage(), ex);
            }
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
                .orElseThrow(() ->
                        new ObjectNotFoundException("ehr_status", "No EHR_STATUS with given timestamp: " + timestamp));
    }

    public ObjectVersionId getLatestVersionUidOfStatus(UUID ehrId) {
        // pre-step: check for valid ehrId
        ensureEhrExist(ehrId);

        return ehrRepository.findLatestVersion(ehrId).orElseThrow();
    }

    public DvDateTime getCreationTime(UUID ehrId) {
        // pre-step: check for valid ehrId
        ensureEhrExist(ehrId);

        return new DvDateTime(ehrRepository.findEhrCreationTime(ehrId));
    }

    @Override
    public boolean hasEhr(UUID ehrId) {
        return ehrRepository.hasEhr(ehrId);
    }

    @Override
    public VersionedObject<EhrStatusDto> getVersionedEhrStatus(UUID ehrId) {
        // pre-step: check for valid ehrId
        ensureEhrExist(ehrId);

        return ehrRepository
                .getVersionedEhrStatus(ehrId)
                .map(versionedEhrStatus -> new VersionedObject<EhrStatusDto>(
                        versionedEhrStatus.getUid(),
                        versionedEhrStatus.getOwnerId(),
                        versionedEhrStatus.getTimeCreated()))
                .orElseThrow();
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
            Optional<OriginalVersion<EhrStatusDto>> ehrStatus = getEhrStatusAtVersion(ehrUid, versionedObjectUid, i);

            // create RevisionHistoryItem for each version and append it to RevisionHistory
            if (ehrStatus.isPresent()) revisionHistory.addItem(revisionHistoryItemFromEhrStatus(ehrStatus.get(), i));
        }

        if (revisionHistory.getItems().isEmpty()) {
            throw new InternalServerException("Problem creating RevisionHistory"); // never should be empty; not valid
        }
        return revisionHistory;
    }

    private RevisionHistoryItem revisionHistoryItemFromEhrStatus(OriginalVersion<EhrStatusDto> ehrStatus, int version) {

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
        ehrRepository.adminDelete(ehrId);
    }

    @Override
    public String getSubjectExtRef(String ehrId) {
        EhrStatusDto ehrStatus = getEhrStatus(UUID.fromString(ehrId)).status();
        return Optional.of(ehrStatus.subject())
                .map(PartyProxy::getExternalRef)
                .map(ObjectRef::getId)
                .map(ObjectId::getValue)
                .orElse(null);
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
            raiseEhrNotFoundException(ehrId);
        }
    }

    private EhrStatusDto ehrStatusDtoWithId(EhrStatusDto ehrStatusDto, ObjectVersionId versionId) {
        return new EhrStatusDto(
                versionId,
                ehrStatusDto.archetypeNodeId(),
                ehrStatusDto.name(),
                ehrStatusDto.archetypeDetails(),
                ehrStatusDto.feederAudit(),
                ehrStatusDto.subject(),
                ehrStatusDto.isQueryable(),
                ehrStatusDto.isModifiable(),
                ehrStatusDto.otherDetails());
    }

    private static void raiseEhrNotFoundException(UUID ehrId) {
        throw new ObjectNotFoundException("ehr", String.format("No EHR found with given ID: %s", ehrId));
    }
}
