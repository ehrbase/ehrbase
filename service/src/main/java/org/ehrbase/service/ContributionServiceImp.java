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

import com.nedap.archie.rm.RMObject;
import com.nedap.archie.rm.changecontrol.Version;
import com.nedap.archie.rm.composition.Composition;
import com.nedap.archie.rm.directory.Folder;
import com.nedap.archie.rm.ehr.EhrStatus;
import com.nedap.archie.rm.generic.AuditDetails;
import com.nedap.archie.rm.support.identification.ObjectId;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import com.nedap.archie.rminfo.ArchieRMInfoLookup;
import com.nedap.archie.rminfo.RMTypeInfo;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.api.dto.EhrStatusDto;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.exception.UnprocessableEntityException;
import org.ehrbase.api.exception.ValidationException;
import org.ehrbase.api.service.CompositionService;
import org.ehrbase.api.service.ContributionService;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.api.service.ValidationService;
import org.ehrbase.jooq.pg.enums.ContributionDataType;
import org.ehrbase.jooq.pg.tables.records.ContributionRecord;
import org.ehrbase.openehr.sdk.response.dto.ContributionCreateDto;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.ContributionDto;
import org.ehrbase.openehr.sdk.util.rmconstants.RmConstants;
import org.ehrbase.repository.AuditDetailsTargetType;
import org.ehrbase.repository.CompositionRepository;
import org.ehrbase.repository.ContributionRepository;
import org.ehrbase.repository.EhrFolderRepository;
import org.ehrbase.repository.EhrRepository;
import org.ehrbase.service.contribution.ContributionServiceHelper;
import org.ehrbase.service.contribution.ContributionWrapper;
import org.ehrbase.util.UuidGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ContributionServiceImp implements ContributionService {

    // the version list in a contribution adds a type tag to each item, so the specific object is distinguishable
    private final CompositionService compositionService;
    private final EhrService ehrService;
    private final InternalDirectoryService folderService;

    private final ValidationService validationService;
    private final ContributionRepository contributionRepository;
    private final CompositionRepository compositionRepository;

    private final EhrFolderRepository ehrFolderRepository;

    private final EhrRepository ehrRepository;

    public enum SupportedVersionedObject {
        COMPOSITION,
        EHR_STATUS,
        FOLDER
    }

    @Autowired
    public ContributionServiceImp(
            CompositionService compositionService,
            EhrService ehrService,
            InternalDirectoryService folderService,
            ValidationService validationService,
            ContributionRepository contributionRepository,
            CompositionRepository compositionRepository,
            EhrFolderRepository ehrFolderRepository,
            EhrRepository ehrRepository) {

        this.compositionService = compositionService;
        this.ehrService = ehrService;
        this.folderService = folderService;
        this.validationService = validationService;
        this.contributionRepository = contributionRepository;
        this.compositionRepository = compositionRepository;
        this.ehrFolderRepository = ehrFolderRepository;
        this.ehrRepository = ehrRepository;
    }

    /**
     * @param ehrId          ID of EHR
     * @param contributionId ID of contribution
     * @return
     * @throws ObjectNotFoundException if EHR or CONTRIBUTION is not found
     */
    @Override
    @Nonnull
    public ContributionDto getContribution(UUID ehrId, UUID contributionId) {
        // also checks for valid ehr and contribution ID
        AuditDetails auditDetails = retrieveAuditDetails(ehrId, contributionId);
        Map<String, String> objectReferences = retrieveUuidsOfContributionObjects(ehrId, contributionId);
        return new ContributionDto(contributionId, objectReferences, auditDetails);
    }

    @Override
    public UUID commitContribution(UUID ehrId, String content) {
        /*Note: we do not perform is_modifiable checks here since a contribution may contain a modification of the
        is_modifiable flag. The is_modifiable checks are performed per version in the service responsible for handling the
        versions content. Otherwise, resetting is_modifiable would not be possible. */

        // XXX Performance: pre-check could be omitted
        if (!ehrService.hasEhr(ehrId)) {
            throw new ObjectNotFoundException(RmConstants.EHR, "No EHR found with given ID: " + ehrId.toString());
        }

        ContributionWrapper contributionWrapper = ContributionServiceHelper.unmarshalContribution(content);
        ContributionCreateDto contribution = contributionWrapper.getContributionCreateDto();

        validationService.check(contribution);

        UUID auditUuid =
                contributionRepository.createAudit(contribution.getAudit(), AuditDetailsTargetType.CONTRIBUTION);

        UUID contributionUuid = Optional.of(contribution)
                .map(ContributionCreateDto::getUid)
                .map(ObjectId::getValue)
                .map(UUID::fromString)
                .orElseGet(UuidGenerator::randomUUID);

        UUID contributionId = contributionRepository.createContribution(
                ehrId, contributionUuid, ContributionDataType.other, auditUuid);

        // go through those RM objects versions and execute the action of it (as listed in its audit) and connect it to
        // new
        // contribution. Prefer to use the DTOs objects instead of the RMObjects.
        contributionWrapper.forEachVersion((version, dto) -> {
            RMObject versionRmObject = version.getData();

            // the version contains the optional "data" attribute (i.e. payload),
            // therefore has specific object type (composition, folder,...)
            // This must be in sync with SupportedVersionedObject.

            switch (versionRmObject) {
                case Composition composition -> {
                    try {
                        processCompositionVersion(ehrId, contributionId, version, composition);
                    } catch (UnprocessableEntityException e) {
                        throw new ValidationException(e.getMessage());
                    }
                }
                case Folder folder -> processFolderVersion(ehrId, contributionId, version, folder);
                case EhrStatus ignored -> {
                    // Here we use the EHRStatusDto to be able to apply a better validation
                    EhrStatusDto ehrStatusDto = Optional.ofNullable(dto)
                            .filter(EhrStatusDto.class::isInstance)
                            .map(EhrStatusDto.class::cast)
                            .orElseThrow(() -> new InternalServerException(
                                    "Expected DTO to exist for Contribution of EHR_STATUS"));
                    processEhrStatusVersion(ehrId, contributionId, version, ehrStatusDto);
                }
                case null -> {
                    // version doesn't contain "data", so it is only a metadata one to, for
                    // instance, delete a specific object via ID regardless of type

                    // :FIXME according to the spec. a version must contain data

                    processMetadataVersion(ehrId, contributionId, version);
                }
                default -> throw new ValidationException(ERR_VER_INVALID.formatted(Optional.of(
                                versionRmObject.getClass())
                        .map(ArchieRMInfoLookup.getInstance()::getTypeInfo)
                        .map(RMTypeInfo::getRmName)
                        .orElseGet(
                                () -> versionRmObject.getClass().getSimpleName().toUpperCase())));
            }
        });

        return contributionId;
    }

    private static final String ERR_VER_INVALID = "Invalid version object in contribution: %s not supported.";

    private static final String ERR_UNSUP_CHANGE_TYPE = "ChangeType[%s] not Supported.";

    /**
     * Helper function to process a version of composition type
     *
     * @param ehrId           ID of given EHR scope
     * @param contributionId  Top level contribution this version is part of
     * @param version         The version wrapper object
     * @param ehrStatus       The actual EhrStatus payload
     * @throws IllegalArgumentException when input is missing precedingVersionUid in case of modification
     */
    private void processEhrStatusVersion(UUID ehrId, UUID contributionId, Version<?> version, EhrStatusDto ehrStatus) {
        // access audit and extract method, e.g. CREATION
        ContributionChangeType changeType =
                ContributionService.ContributionChangeType.fromAuditDetails(version.getCommitAudit());

        checkContributionRules(version, changeType); // evaluate and check contribution rules
        UUID audit = contributionRepository.createAudit(version.getCommitAudit(), AuditDetailsTargetType.EHR_STATUS);

        switch (changeType) {
            case CREATION ->
            // call creation of a new status with given input is not possible as it is linked to and created through
            // an EHR object
            throw new ValidationException("Invalid change type. EHR_STATUS cannot be manually created.");
                // triggers the same processing as modification
                // TODO-396: so far so good, but should use the type "AMENDMENT" for audit in access layer
            case AMENDMENT, MODIFICATION -> ehrService.updateStatus(
                    ehrId, ehrStatus, version.getPrecedingVersionUid(), contributionId, audit);
            case DELETED ->
            // deleting a STATUS versioned object is invalid
            throw new ValidationException("Invalid change type. EHR_STATUS cannot be deleted.");
            case SYNTHESIS, UNKNOWN ->
            // valid change type is done in checkContributionRules
            throw new ValidationException(ERR_UNSUP_CHANGE_TYPE.formatted(changeType));
        }
    }

    /**
     * Helper function to process a version of composition type
     *
     * @param ehrId           ID of given EHR scope
     * @param contributionId  Top level contribution this version is part of
     * @param version         The version wrapper object
     * @param composition     The actual composition payload
     * @throws IllegalArgumentException when input is missing precedingVersionUid in case of modification
     */
    private void processCompositionVersion(
            UUID ehrId, UUID contributionId, Version<?> version, Composition composition) {
        // access audit and extract method, e.g. CREATION
        ContributionChangeType changeType =
                ContributionService.ContributionChangeType.fromAuditDetails(version.getCommitAudit());

        checkContributionRules(version, changeType); // evaluate and check contribution rules

        UUID audit = contributionRepository.createAudit(version.getCommitAudit(), AuditDetailsTargetType.COMPOSITION);

        switch (changeType) {
            case CREATION ->
            // call creation of a new composition with given input
            compositionService.create(ehrId, composition, contributionId, audit);
            case AMENDMENT,
                    // triggers the same processing as modification
                    // :TODO-396: so far so good, but should use the type "AMENDMENT" for audit in access layer
                    MODIFICATION ->
            // call modification of the given composition
            compositionService.update(ehrId, version.getPrecedingVersionUid(), composition, contributionId, audit);
            case DELETED ->
            // case of deletion change type, but request also has payload
            // :TODO: should that be even allowed? specification-wise it's not forbidden)
            compositionService.delete(ehrId, version.getPrecedingVersionUid(), contributionId, audit);
            case SYNTHESIS, UNKNOWN -> throw new ValidationException(ERR_UNSUP_CHANGE_TYPE.formatted(changeType));
        }
    }

    private void processFolderVersion(UUID ehrId, UUID contributionId, Version<?> version, Folder folder) {
        // access audit and extract method, e.g. CREATION
        ContributionChangeType changeType =
                ContributionService.ContributionChangeType.fromAuditDetails(version.getCommitAudit());

        checkContributionRules(version, changeType); // evaluate and check contribution rules

        UUID audit = contributionRepository.createAudit(version.getCommitAudit(), AuditDetailsTargetType.EHR_FOLDER);

        switch (changeType) {
            case CREATION ->
            // call creation of a new folder version with given input
            folderService.create(ehrId, folder, contributionId, audit);

                // triggers the same processing as modification
                // :TODO-396: so far so good, but should use the type"AMENDMENT" for audit in access layer
            case AMENDMENT, MODIFICATION ->
            // preceding_version_uid check call
            // modification of the given folder
            folderService.update(ehrId, folder, version.getPrecedingVersionUid(), contributionId, audit);
            case DELETED ->
            // case of deletion change type, but request
            // also has payload
            // TODO: should that be even allowed? specification-wise it's not forbidden
            folderService.delete(ehrId, version.getPrecedingVersionUid(), contributionId, audit);
            case SYNTHESIS, UNKNOWN ->
            // of valid change type is done in checkContributionRules
            throw new ValidationException(ERR_UNSUP_CHANGE_TYPE.formatted(changeType));
        }
    }

    private static final String ERR_MISSING_PRECEDING_UID =
            "Invalid version. Change type %s, but also set \"preceding_version_uid\" attribute";

    /**
     * Checks contribution rules, i.e. context-aware checks of the content. For instance, a committed version can't be
     * of change type CREATION while containing a "preceding_version_uid".
     * <p>
     * Note: Those rules are checked here, because context of the contribution might be important.
     * Apart from that, most rules logically could be checked within the appropriate service as well.
     *
     * @param version    Input version object
     * @param changeType Change type of this version
     */
    private void checkContributionRules(Version<?> version, ContributionChangeType changeType) {

        switch (changeType) {
            case CREATION -> {
                // can't have change type CREATION and a given "preceding_version_uid"
                if (version.getPrecedingVersionUid() != null)
                    throw new ValidationException(ERR_MISSING_PRECEDING_UID.formatted(changeType));
            }
            case MODIFICATION, AMENDMENT -> {
                // can't have change type MODIFICATION and without giving "preceding_version_uid"
                if (version.getPrecedingVersionUid() == null)
                    throw new ValidationException(ERR_MISSING_PRECEDING_UID.formatted(changeType));
            }
                // block of valid change types, without any rules to apply (yet)
            case DELETED, SYNTHESIS, UNKNOWN -> {}
            default -> throw new ValidationException(ERR_UNSUP_CHANGE_TYPE.formatted(changeType));
        }
    }

    /**
     * Helper to process versions from a contribution, which do not have the optional "data" attribute and therefore are
     * called metadata versions.
     * Only DELETE is supported.
     *
     * @param ehrId          ID of given EHR scope
     * @param contributionId Top level contribution this version is part of
     * @param version        The version wrapper object
     */
    private void processMetadataVersion(UUID ehrId, UUID contributionId, Version<?> version) {
        ContributionChangeType changeType =
                ContributionService.ContributionChangeType.fromAuditDetails(version.getCommitAudit());

        if (changeType != ContributionChangeType.DELETED) {
            throw new ValidationException(ERR_UNSUP_CHANGE_TYPE.formatted(changeType));
        }

        UUID objectUid = getVersionedUidFromVersion(version);

        // COMPOSITION?
        if (compositionService.exists(objectUid)) {
            UUID audit =
                    contributionRepository.createAudit(version.getCommitAudit(), AuditDetailsTargetType.COMPOSITION);
            compositionService.delete(ehrId, version.getPrecedingVersionUid(), contributionId, audit);

            // FOLDER?
        } else if (isFolderPresent(ehrId, version.getPrecedingVersionUid())) {
            UUID audit =
                    contributionRepository.createAudit(version.getCommitAudit(), AuditDetailsTargetType.EHR_FOLDER);
            compositionService.delete(ehrId, version.getPrecedingVersionUid(), contributionId, audit);
        } else {
            throw new ObjectNotFoundException(
                    "COMPOSITION|FOLDER", "Could not find Object[id: %s]".formatted(objectUid));
        }
    }

    private boolean isFolderPresent(UUID ehrId, ObjectVersionId folderUid) {
        return folderService.get(ehrId, folderUid, null).isPresent();
    }

    /**
     * Create versionUid UUID from retrieved precedingVersionUid from payload (and do sanity checks before continuing), or
     * throw errors if ID is not present nor valid.
     * Note: The precedingVersionUid parameter technically is optional for contributions but necessary when invoking other
     * change types than creation.
     *
     * @param version RM object of Version type
     * @return versionedUid
     * @throws IllegalArgumentException Given {@link Version} has no or no valid precedingVersionUid
     */
    private static UUID getVersionedUidFromVersion(Version<?> version) {
        ObjectVersionId precedingVersionUid = version.getPrecedingVersionUid();
        if (precedingVersionUid == null) {
            throw new IllegalArgumentException(
                    "Input invalid. Composition can't be modified without pointer to precedingVersionUid in Version container.");
        }
        if (StringUtils.countMatches(precedingVersionUid.getValue(), "::") != 2) {
            throw new IllegalArgumentException("Input invalid. Given precedingVersionUid is not a versionUid.");
        }
        String versionedUid = precedingVersionUid
                .getValue()
                .substring(0, precedingVersionUid.getValue().indexOf("::"));
        return UUID.fromString(versionedUid);
    }

    /**
     * retrieval of IDs of all objects that are saved as part of the given contribution
     *
     * @param ehrId
     * @param contribution ID of source contribution
     * @return Map with ID of the object as key and type ("composition", "folder",...) as value
     * @throws IllegalArgumentException on error when retrieving compositions
     */
    private Map<String, String> retrieveUuidsOfContributionObjects(UUID ehrId, UUID contribution) {
        Map<String, String> objRefs = new LinkedHashMap<>();

        compositionRepository.findVersionIdsByContribution(ehrId, contribution).stream()
                .sorted(Comparator.comparing(ObjectVersionId::getValue))
                .forEach(k -> objRefs.put(k.getValue(), SupportedVersionedObject.COMPOSITION.name()));

        ehrRepository.findVersionIdsByContribution(ehrId, contribution).stream()
                .sorted(Comparator.comparing(ObjectVersionId::getValue))
                .forEach(k -> objRefs.put(k.getValue(), SupportedVersionedObject.EHR_STATUS.name()));

        ehrFolderRepository.findForContribution(ehrId, contribution).stream()
                .sorted(Comparator.comparing(ObjectVersionId::getValue))
                .forEach(f -> objRefs.put(f.toString(), SupportedVersionedObject.FOLDER.name()));

        return objRefs;
    }

    /**
     * retrieval and building of AuditDetails object attached to the given contribution context
     *
     * @param contributionId ID of contribution
     * @return {@link AuditDetails} object from contribution
     * @throws ObjectNotFoundException if EHR or CONTRIBUTION is not found
     */
    private AuditDetails retrieveAuditDetails(UUID ehrId, UUID contributionId) {

        ContributionRecord contributionRec = contributionRepository.findById(contributionId);

        if (contributionRec == null || !contributionRec.getEhrId().equals(ehrId)) {
            if (ehrService.hasEhr(ehrId)) {
                throw new ObjectNotFoundException("CONTRIBUTION", "Contribution with given ID does not exist");
            } else {
                throw new ObjectNotFoundException(RmConstants.EHR, "No EHR found with given ID: %s".formatted(ehrId));
            }
        }

        UUID hasAudit = contributionRec.getHasAudit();
        return contributionRepository.findAuditDetails(hasAudit);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public void adminDelete(UUID ehrId, UUID contributionId) {
        throw new UnsupportedOperationException();
    }
}
