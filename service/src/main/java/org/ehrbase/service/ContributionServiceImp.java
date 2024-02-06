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
package org.ehrbase.service;

import com.nedap.archie.rm.RMObject;
import com.nedap.archie.rm.archetyped.TemplateId;
import com.nedap.archie.rm.changecontrol.Version;
import com.nedap.archie.rm.composition.Composition;
import com.nedap.archie.rm.datatypes.CodePhrase;
import com.nedap.archie.rm.datavalues.DvCodedText;
import com.nedap.archie.rm.datavalues.DvText;
import com.nedap.archie.rm.datavalues.quantity.datetime.DvDateTime;
import com.nedap.archie.rm.directory.Folder;
import com.nedap.archie.rm.ehr.EhrStatus;
import com.nedap.archie.rm.generic.AuditDetails;
import com.nedap.archie.rm.generic.PartyProxy;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import com.nedap.archie.rm.support.identification.TerminologyId;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.ehrbase.api.definitions.ServerConfig;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.exception.PreconditionFailedException;
import org.ehrbase.api.exception.UnexpectedSwitchCaseException;
import org.ehrbase.api.exception.UnprocessableEntityException;
import org.ehrbase.api.exception.ValidationException;
import org.ehrbase.api.service.CompositionService;
import org.ehrbase.api.service.ContributionService;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.api.service.TenantService;
import org.ehrbase.dao.access.interfaces.I_AuditDetailsAccess;
import org.ehrbase.dao.access.interfaces.I_CompositionAccess;
import org.ehrbase.dao.access.interfaces.I_ConceptAccess;
import org.ehrbase.dao.access.interfaces.I_ContributionAccess;
import org.ehrbase.dao.access.interfaces.I_StatusAccess;
import org.ehrbase.dao.access.jooq.AuditDetailsAccess;
import org.ehrbase.dao.access.jooq.party.PersistedPartyProxy;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.CompositionFormat;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.ContributionDto;
import org.ehrbase.repository.ContributionRepository;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ContributionServiceImp extends BaseServiceImp implements ContributionService {
    // the version list in a contribution adds an type tag to each item, so the specific object is distinguishable
    public static final String TYPE_COMPOSITION = "COMPOSITION";
    public static final String TYPE_EHRSTATUS = "EHR_STATUS";
    public static final String TYPE_FOLDER = "FOLDER";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final CompositionService compositionService;
    private final EhrService ehrService;
    private final InternalDirectoryService folderService;
    private final TenantService tenantService;

    private final ContributionRepository contributionRepository;

    enum SupportedClasses {
        COMPOSITION,
        EHRSTATUS,
        FOLDER
    }

    @Autowired
    public ContributionServiceImp(
            KnowledgeCacheService knowledgeCacheService,
            CompositionService compositionService,
            EhrService ehrService,
            InternalDirectoryService folderService,
            DSLContext context,
            ServerConfig serverConfig,
            TenantService tenantService,
            ContributionRepository contributionRepository) {
        super(knowledgeCacheService, context, serverConfig);
        this.compositionService = compositionService;
        this.ehrService = ehrService;
        this.folderService = folderService;
        this.tenantService = tenantService;
        this.contributionRepository = contributionRepository;
    }

    @Override
    public boolean hasContribution(UUID ehrId, UUID contributionId) {
        // pre-step: check for valid ehrId
        if (!ehrService.hasEhr(ehrId)) {
            throw new ObjectNotFoundException("ehr", "No EHR found with given ID: " + ehrId.toString());
        }

        I_ContributionAccess contributionAccess;
        // doesn't exist on error
        try {
            contributionAccess = I_ContributionAccess.retrieveInstance(this.getDataAccess(), contributionId);
        } catch (InternalServerException e) {
            return false;
        }

        // doesn't exist on empty result, too
        if (contributionAccess == null) {
            return false;
        }

        // with both pre-checks about only checking of contribution is part of EHR is left
        return contributionAccess.getEhrId().equals(ehrId);
    }

    @Override
    public Optional<ContributionDto> getContribution(UUID ehrId, UUID contributionId) {
        // pre-step: check for valid ehr and contribution ID
        if (!hasContribution(ehrId, contributionId)) {
            throw new ObjectNotFoundException("contribution", "Contribution with given ID does not exist");
        }

        ContributionDto contribution = new ContributionDto(
                contributionId,
                retrieveUuidsOfContributionObjects(ehrId, contributionId),
                retrieveAuditDetails(contributionId));

        return Optional.of(contribution);
    }

    @Override
    public UUID commitContribution(UUID ehrId, String content, CompositionFormat format) {
        /*Note: we do not perform is_modifiable checks here since a contribution may contain a modification of the
        is_modifiable flag. The is_modifiable checks are performed per version in the service responsible for handling the
        versions content*/

        // pre-step: check for valid ehrId
        if (!ehrService.hasEhr(ehrId)) {
            throw new ObjectNotFoundException("ehr", "No EHR found with given ID: " + ehrId.toString());
        }

        Short sysTenant = tenantService.getCurrentSysTenant();

        // create new empty/standard-value contribution - will be updated later with full details
        I_ContributionAccess contributionAccess =
                I_ContributionAccess.getInstance(this.getDataAccess(), ehrId, sysTenant);
        // parse and set audit information from input
        AuditDetails audit = ContributionServiceHelper.parseAuditDetails(content, format);
        contributionAccess.setAuditDetailsValues(audit);
        // commits with all default values (but without audit handling as it is done above)
        UUID contributionId = contributionAccess.commit(null, null, null);
        List<Version> versions = ContributionServiceHelper.parseVersions(content, format);

        if (versions.isEmpty()) {
            throw new InvalidApiParameterException("Invalid Contribution, must have at least one Version object.");
        }

        // go through those RM objects and execute the action of it (as listed in its audit) and connect it to new
        // contribution
        for (Version version : versions) {

            Object versionData = version.getData();

            if (versionData
                    != null) { // the version contains the optional "data" attribute (i.e. payload), therefore has
                // specific object type
                // (composition, folder,...)
                RMObject versionRmObject;
                if (versionData instanceof LinkedHashMap) {
                    versionRmObject = ContributionServiceHelper.unmarshalMapContentToRmObject(
                            (LinkedHashMap) versionData, format);
                } else {
                    throw new IllegalArgumentException("Contribution input can't be processed");
                }

                // switch to allow acting depending on exact type
                SupportedClasses versionClass;
                try {
                    versionClass = SupportedClasses.valueOf(
                            versionRmObject.getClass().getSimpleName().toUpperCase());
                } catch (Exception e) {
                    throw new InvalidApiParameterException("Invalid version object in contribution. "
                            + versionRmObject.getClass().getSimpleName().toUpperCase() + " not supported.");
                }
                switch (versionClass) {
                    case COMPOSITION:
                        try {
                            processCompositionVersion(ehrId, contributionId, version, (Composition) versionRmObject);
                        } catch (UnprocessableEntityException e) {
                            throw new ValidationException(e.getMessage());
                        }
                        break;
                    case EHRSTATUS:
                        processEhrStatusVersion(ehrId, contributionId, version, (EhrStatus) versionRmObject);
                        break;
                    case FOLDER:
                        processFolderVersion(ehrId, contributionId, version, (Folder) versionRmObject);
                        break;
                    default:
                        throw new UnexpectedSwitchCaseException(versionClass);
                }
            } else { // version doesn't contain "data", so it is only a metadata one to, for instance, delete a specific
                // object
                // via ID regardless of type
                processMetadataVersion(ehrId, contributionId, version);
            }
        }

        return contributionId;
    }

    /**
     * Helper function to process a version of composition type
     *
     * @param ehrId           ID of given EHR scope
     * @param contributionId  Top level contribution this version is part of
     * @param version         The version wrapper object
     * @param versionRmObject The actual composition payload
     * @throws IllegalArgumentException when input is missing precedingVersionUid in case of modification
     */
    private void processCompositionVersion(
            UUID ehrId, UUID contributionId, Version version, Composition versionRmObject) {
        // access audit and extract method, e.g. CREATION
        I_ConceptAccess.ContributionChangeType changeType = I_ConceptAccess.ContributionChangeType.valueOf(
                version.getCommitAudit().getChangeType().getValue().toUpperCase());

        checkContributionRules(version, changeType); // evaluate and check contribution rules

        UUID audit = contributionRepository.createAudit(version.getCommitAudit());

        switch (changeType) {
            case CREATION:
                // call creation of a new composition with given input
                compositionService.create(ehrId, versionRmObject, contributionId, audit);
                break;
            case AMENDMENT: // triggers the same processing as modification // TODO-396: so far so good, but should use
                // the type
                // "AMENDMENT" for audit in access layer
            case MODIFICATION:
                String actualPreceding = getAndCheckActualPreceding(version);
                // call modification of the given composition
                compositionService.update(
                        ehrId, new ObjectVersionId(actualPreceding), versionRmObject, contributionId, audit);
                break;
            case DELETED: // case of deletion change type, but request also has payload (TODO: should that be even
                // allowed?
                // specification-wise it's not forbidden)
                String actualPreceding2 = getAndCheckActualPreceding(version);
                compositionService.delete(ehrId, new ObjectVersionId(actualPreceding2), contributionId, audit);
                break;
            case SYNTHESIS: // TODO
            case UNKNOWN: // TODO
            default: // TODO keep as long as above has TODOs. Check of valid change type is done in
                // checkContributionRules
                throw new UnexpectedSwitchCaseException(changeType);
        }
    }

    private String getAndCheckActualPreceding(Version version) {
        // preceding_version_uid check
        Integer latestVersion = compositionService.getLastVersionNumber(getVersionedUidFromVersion(version));
        var id = version.getPrecedingVersionUid().toString();
        // remove version number after "::" and add queried version number to compare with given one
        String actualPreceding = id.substring(0, id.lastIndexOf("::") + 2).concat(latestVersion.toString());
        if (!actualPreceding.equals(version.getPrecedingVersionUid().toString())) {
            throw new PreconditionFailedException(
                    "Given preceding_version_uid for COMPOSITION object does not match latest existing version");
        }
        return actualPreceding;
    }

    /**
     * Helper function to process a version of composition type
     *
     * @param ehrId           ID of given EHR scope
     * @param contributionId  Top level contribution this version is part of
     * @param version         The version wrapper object
     * @param versionRmObject The actual EhrStatus payload
     * @throws IllegalArgumentException when input is missing precedingVersionUid in case of modification
     */
    private void processEhrStatusVersion(UUID ehrId, UUID contributionId, Version version, EhrStatus versionRmObject) {
        // access audit and extract method, e.g. CREATION
        I_ConceptAccess.ContributionChangeType changeType = I_ConceptAccess.ContributionChangeType.valueOf(
                version.getCommitAudit().getChangeType().getValue().toUpperCase());

        checkContributionRules(version, changeType); // evaluate and check contribution rules
        UUID audit = contributionRepository.createAudit(version.getCommitAudit());

        switch (changeType) {
            case CREATION:
                // call creation of a new status with given input is not possible as it is linked to and created through
                // an EHR object
                throw new InvalidApiParameterException("Invalid change type. EHR_STATUS can't be manually created.");
            case AMENDMENT: // triggers the same processing as modification // TODO-396: so far so good, but should use
                // the type
                // "AMENDMENT" for audit in access layer
            case MODIFICATION:
                // preceding_version_uid check
                String latestVersionUid = ehrService.getLatestVersionUidOfStatus(ehrId);
                if (!latestVersionUid.equals(version.getPrecedingVersionUid().toString())) {
                    throw new PreconditionFailedException(
                            "Given preceding_version_uid for EHR_STATUS object does not match latest existing version");
                }
                // call modification of the given status
                ehrService.updateStatus(ehrId, versionRmObject, contributionId, audit);
                break;
            case DELETED:
                // deleting a STATUS versioned object is invalid
                throw new InvalidApiParameterException("Invalid change type. EHR_STATUS can't be deleted.");
            case SYNTHESIS: // TODO
            case UNKNOWN: // TODO
            default: // TODO keep as long as above has TODOs. Check of valid change type is done in
                // checkContributionRules
                throw new UnexpectedSwitchCaseException(changeType);
        }
    }

    private void processFolderVersion(UUID ehrId, UUID contributionId, Version version, Folder versionRmObject) {
        // access audit and extract method, e.g. CREATION
        I_ConceptAccess.ContributionChangeType changeType = I_ConceptAccess.ContributionChangeType.valueOf(
                version.getCommitAudit().getChangeType().getValue().toUpperCase());

        checkContributionRules(version, changeType); // evaluate and check contribution rules

        UUID audit = contributionRepository.createAudit(version.getCommitAudit());

        switch (changeType) {
            case CREATION:
                // call creation of a new folder version with given input
                folderService.create(ehrId, versionRmObject, contributionId, audit);
                break;
            case AMENDMENT: // triggers the same processing as modification // TODO-396: so far so good, but should use
                // the type
                // "AMENDMENT" for audit in access layer
            case MODIFICATION:
                // preceding_version_uid check

                // call modification of the given folder
                folderService.update(ehrId, versionRmObject, version.getPrecedingVersionUid(), contributionId, audit);
                break;
            case DELETED: // case of deletion change type, but request also has payload (TODO: should that be even
                // allowed?
                // specification-wise it's not forbidden)
                folderService.delete(ehrId, version.getPrecedingVersionUid(), contributionId, audit);
                break;
            case SYNTHESIS: // TODO
            case UNKNOWN: // TODO
            default: // TODO keep as long as above has TODOs. Check of valid change type is done in
                // checkContributionRules
                throw new UnexpectedSwitchCaseException(changeType);
        }
    }

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
    private void checkContributionRules(Version version, I_ConceptAccess.ContributionChangeType changeType) {

        switch (changeType) {
            case CREATION:
                // can't have change type CREATION and a given "preceding_version_uid"
                if (version.getPrecedingVersionUid() != null) {
                    throw new InvalidApiParameterException(
                            "Invalid version. Change type CREATION, but also set \"preceding_version_uid\" attribute");
                }
                break;
            case MODIFICATION:
            case AMENDMENT:
                // can't have change type MODIFICATION and without giving "preceding_version_uid"
                if (version.getPrecedingVersionUid() == null) {
                    throw new InvalidApiParameterException(
                            "Invalid version. Change type MODIFICATION, but without \"preceding_version_uid\" attribute");
                }
                break;
                // block of valid change types, without any rules to apply (yet)
            case DELETED:
            case SYNTHESIS:
            case UNKNOWN:
                break;
                // invalid change type
            default:
                throw new InvalidApiParameterException("Change type \"" + changeType + "\" not valid");
        }
    }

    /**
     * Helper to process versions from a contribution, which do not have the optional "data" attribute and therefore are
     * called metadata versions.
     *
     * @param ehrId          ID of given EHR scope
     * @param contributionId Top level contribution this version is part of
     * @param version        The version wrapper object
     */
    private void processMetadataVersion(UUID ehrId, UUID contributionId, Version version) {
        // access audit and extract method, e.g. CREATION
        I_ConceptAccess.ContributionChangeType changeType = I_ConceptAccess.ContributionChangeType.valueOf(
                version.getCommitAudit().getChangeType().getValue().toUpperCase());

        UUID audit = contributionRepository.createAudit(version.getCommitAudit());

        switch (changeType) {
            case DELETED:
                // deleting an object without knowing which type it is requires checking of type, here with nested
                // try-catch blocks
                UUID objectUid = getVersionedUidFromVersion(version);
                try {

                    // throw exception to signal no matching composition was found
                    if (compositionService.retrieve(ehrId, objectUid, null).isEmpty()) {
                        throw new RuntimeException();
                    }
                    String actualPreceding = getAndCheckActualPreceding(version);
                    compositionService.delete(ehrId, new ObjectVersionId(actualPreceding), contributionId, audit);
                } catch (Exception e) {
                    // given version ID is not of type composition - ignoring the exception because it is
                    // expected possible outcome
                    try {
                        // TODO: is this nested try-catch approach really a good way to handle this?!
                        // TODO-396: add folder handling
                    } catch (Exception ee) {
                        // given version ID is not of type folder - ignoring the exception because it is expected
                        // possible outcome
                        // current end of going through supported types - last step is checking for EHR_STATUS
                        // and throwing specific error
                        Optional.of(ehrService.getEhrStatus(ehrId)).ifPresent(st -> {
                            if (st.getUid().equals(version.getPrecedingVersionUid())) {
                                throw new InvalidApiParameterException(
                                        "Invalid change type. EHR_STATUS can't be deleted.");
                            }
                        });

                        // TODO add nested try-catchs for more supported types, for instance folder, when their
                        // contribution support gets
                        //  implemented
                        // TODO last "try catch" in line needs to rethrow for real, as then no matching object would
                        // have been found
                        throw new ObjectNotFoundException(
                                Composition.class.getName(),
                                "Couldn't find object matching id: "
                                        + objectUid); // TODO: type is technically wrong, if
                        // more than one type gets tested
                    }
                }
                break;
            case SYNTHESIS: // TODO
            case UNKNOWN: // TODO
            case CREATION: // not expected in a metadata version (i.e. without payload)
            case MODIFICATION: // not expected in a metadata version (i.e. without payload)
            case AMENDMENT: // not expected in a metadata version (i.e. without payload)
            default:
                throw new UnexpectedSwitchCaseException(changeType);
        }
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
    private UUID getVersionedUidFromVersion(Version version) {
        ObjectVersionId precedingVersionUid = version.getPrecedingVersionUid();
        if (precedingVersionUid == null) {
            throw new IllegalArgumentException(
                    "Input invalid. Composition can't be modified without pointer to precedingVersionUid in Version container.");
        }
        if (precedingVersionUid.toString().split("::").length != 3) {
            throw new IllegalArgumentException("Input invalid. Given precedingVersionUid is not a versionUid.");
        }

        String versionedUid = precedingVersionUid.toString().split("::")[0];
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
        Map<String, String> objRefs = new HashMap<>();

        // query for compositions   // TODO: refactor to use service layer only!?
        Map<ObjectVersionId, I_CompositionAccess> compositions = I_CompositionAccess.retrieveInstancesInContribution(
                this.getDataAccess(), contribution, getServerConfig().getNodename());
        // for each fetched composition: add it to the return map and add the composition type tag - ignoring the access
        // obj
        compositions.forEach((k, v) -> objRefs.put(k.getValue(), TYPE_COMPOSITION));

        // query for statuses       // TODO: refactor to use service layer only!?
        Map<ObjectVersionId, I_StatusAccess> statuses = I_StatusAccess.retrieveInstanceByContribution(
                this.getDataAccess(), contribution, getServerConfig().getNodename());
        statuses.forEach((k, v) -> objRefs.put(k.getValue(), TYPE_EHRSTATUS));

        // query for folders
        Set<ObjectVersionId> folders = new HashSet<>(folderService.findForContribution(ehrId, contribution));
        folders.forEach(f -> objRefs.put(f.toString(), TYPE_FOLDER));

        return objRefs;
    }

    /**
     * retrieval and building of AuditDetails object attached to the given contribution context
     *
     * @param contributionId ID of contribution
     * @return {@link AuditDetails} object from contribution
     */
    private AuditDetails retrieveAuditDetails(UUID contributionId) {
        UUID auditId = I_ContributionAccess.retrieveInstance(this.getDataAccess(), contributionId)
                .getHasAuditDetails();

        Short sysTenant = tenantService.getCurrentSysTenant();

        I_AuditDetailsAccess auditDetailsAccess =
                new AuditDetailsAccess(this.getDataAccess(), sysTenant).retrieveInstance(this.getDataAccess(), auditId);

        String systemId = auditDetailsAccess.getSystemId().toString();
        PartyProxy committer =
                new PersistedPartyProxy(this.getDataAccess()).retrieve(auditDetailsAccess.getCommitter());
        DvDateTime timeCommitted = new DvDateTime(LocalDateTime.ofInstant(
                auditDetailsAccess.getTimeCommitted().toInstant(),
                ZoneId.of(auditDetailsAccess.getTimeCommittedTzId())));
        int changeTypeCode = I_ConceptAccess.ContributionChangeType.valueOf(
                        auditDetailsAccess.getChangeType().getLiteral().toUpperCase())
                .getCode();
        // FIXME: what's the terminology ID of the official change type terminology?
        DvCodedText changeType = new DvCodedText(
                auditDetailsAccess.getChangeType().getLiteral(),
                new CodePhrase(new TerminologyId("audit change type"), String.valueOf(changeTypeCode)));
        DvText description = new DvText(auditDetailsAccess.getDescription());

        return new AuditDetails(systemId, committer, timeCommitted, changeType, description);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public void adminDelete(UUID contributionId) {
        I_ContributionAccess contributionAccess =
                I_ContributionAccess.retrieveInstance(getDataAccess(), contributionId);
        contributionAccess.adminDelete();
    }

    @Override
    public Set<String> getListOfTemplates(String contribution, CompositionFormat format) {
        List<Version> versions = ContributionServiceHelper.parseVersions(contribution, format);
        Set<String> templates = new HashSet<>();
        for (Version version : versions) {

            Object versionData = version.getData();

            // the version contains the optional "data" attribute (i.e. payload), therefore has specific object type
            // (composition,
            // folder,...)
            if (versionData != null) {
                RMObject versionRmObject;
                if (versionData instanceof LinkedHashMap) {
                    versionRmObject = ContributionServiceHelper.unmarshalMapContentToRmObject(
                            (LinkedHashMap) versionData, format);
                } else {
                    throw new IllegalArgumentException("Contribution input can't be processed");
                }

                // switch to allow acting depending on exact type
                SupportedClasses versionClass;
                try {
                    versionClass = SupportedClasses.valueOf(
                            versionRmObject.getClass().getSimpleName().toUpperCase());
                } catch (Exception e) {
                    throw new InvalidApiParameterException("Invalid version object in contribution. "
                            + versionRmObject.getClass().getSimpleName().toUpperCase() + " not supported.");
                }
                switch (versionClass) {
                    case COMPOSITION:
                        TemplateId templateId = ((Composition) versionRmObject)
                                .getArchetypeDetails()
                                .getTemplateId();
                        if (templateId != null) {
                            templates.add(templateId.getValue());
                        }
                        break;
                    case EHRSTATUS: // TODO: might add later, if other_details support templated content
                    case FOLDER:
                    default:
                        throw new IllegalArgumentException("Contribution input contains invalid version class");
                }
            }
        }
        return templates;
    }
}
