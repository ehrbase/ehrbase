/*
 * Copyright (c) 2019 Vitasystems GmbH and Jake Smolka (Hannover Medical School).
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

import com.nedap.archie.rm.RMObject;
import com.nedap.archie.rm.changecontrol.Version;
import com.nedap.archie.rm.composition.Composition;
import com.nedap.archie.rm.datatypes.CodePhrase;
import com.nedap.archie.rm.datavalues.DvCodedText;
import com.nedap.archie.rm.datavalues.DvText;
import com.nedap.archie.rm.datavalues.quantity.datetime.DvDateTime;
import com.nedap.archie.rm.generic.AuditDetails;
import com.nedap.archie.rm.generic.PartyProxy;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import com.nedap.archie.rm.support.identification.TerminologyId;
import org.ehrbase.api.definitions.CompositionFormat;
import org.ehrbase.api.definitions.ServerConfig;
import org.ehrbase.api.dto.CompositionDto;
import org.ehrbase.api.dto.ContributionDto;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.exception.UnexpectedSwitchCaseException;
import org.ehrbase.api.service.CompositionService;
import org.ehrbase.api.service.ContributionService;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.dao.access.interfaces.*;
import org.ehrbase.dao.access.jooq.AuditDetailsAccess;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@Transactional
public class ContributionServiceImp extends BaseService implements ContributionService {
    // the version list in a contribution adds an type tag to each item, so the specific object is distinguishable
    public static final String TYPE_COMPOSITION = "COMPOSITION";
    public static final String TYPE_FOLDER = "FOLDER"; // TODO use when implemented as contribution-able. also add more constants for other types when implemented

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final CompositionService compositionService;
    private final EhrService ehrService;

    enum SupportedClasses {
        COMPOSITION //, FOLDER, etc.  TODO: add more class names when supported
    }

    @Autowired
    public ContributionServiceImp(KnowledgeCacheService knowledgeCacheService, CompositionService compositionService, EhrService ehrService, DSLContext context, ServerConfig serverConfig) {
        super(knowledgeCacheService, context, serverConfig);
        this.compositionService = compositionService;
        this.ehrService = ehrService;
    }

    @Override
    public boolean hasContribution(UUID ehrId, UUID contributionId) {
        //pre-step: check for valid ehrId
        if (ehrService.hasEhr(ehrId).equals(Boolean.FALSE)) {
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
        if (contributionAccess == null)
            return false;
        
        // with both pre-checks about only checking of contribution is part of EHR is left
        return contributionAccess.getEhrId().equals(ehrId);
    }

    @Override
    public Optional<ContributionDto> getContribution(UUID ehrId, UUID contributionId) {
        //pre-step: check for valid ehr and contribution ID
        if (!hasContribution(ehrId, contributionId))
            throw new ObjectNotFoundException("contribution", "Contribution with given ID does not exist");

        ContributionDto contribution = new ContributionDto(contributionId, retrieveUuidsOfContributionObjects(contributionId), retrieveAuditDetails(contributionId));

        return Optional.of(contribution);
    }

    @Override   // TODO: this will need to be one transaction, as the contribution itself will be created before the object handling. revoking it if the objects are failing will be necessary - see EHR-259
    public UUID commitContribution(UUID ehrId, String content, CompositionFormat format) {
        //pre-step: check for valid ehrId
        if (ehrService.hasEhr(ehrId).equals(Boolean.FALSE)) {
            throw new ObjectNotFoundException("ehr", "No EHR found with given ID: " + ehrId.toString());
        }

        // create new empty/standard-value contribution - will be updated later with full details
        I_ContributionAccess contributionAccess = I_ContributionAccess.getInstance(this.getDataAccess(), ehrId);
        // commits with all default values
        UUID contributionId = contributionAccess.commit(null, null, null, null, null, null, null);
        List<Version> versions = ContributionServiceHelper.getVersions(content, format);

        if (versions.isEmpty())
            throw new InvalidApiParameterException("Invalid Contribution, must have at least one Version object.");

        // go through those RM objects and execute the action of it (as listed in its audit) and connect it to new contribution
        for (Version version : versions) {

            Object versionData = version.getData();

            if (versionData != null) {  // the version contains the optional "data" attribute (i.e. payload), therefore has specific object type (composition, folder,...)
                RMObject versionRmObject;
                if (versionData instanceof LinkedHashMap) {
                    versionRmObject = ContributionServiceHelper.unmarshalMapContentToRmObject((LinkedHashMap) versionData, format);
                } else {
                    throw new IllegalArgumentException("Contribution input can't be processed");
                }

                // switch to allow acting depending on exact type
                SupportedClasses versionClass = SupportedClasses.valueOf(versionRmObject.getClass().getSimpleName().toUpperCase());
                switch (versionClass) {
                    case COMPOSITION:
                        processCompositionVersion(ehrId, contributionId, version, (Composition) versionRmObject);
                        break;
                    // TODO: add other version types with their own case when needed
                    default:
                        throw new UnexpectedSwitchCaseException(versionClass);

                }
            } else {    // version doesn't contain "data", so it is only a metadata one to, for instance, delete a specific object via ID regardless of type
                processMetadataVersion(ehrId, contributionId, version);
            }
        }

        return contributionId;
    }

    /**
     * Helper function to process a version of composition type
     * @param ehrId ID of given EHR scope
     * @param contributionId Top level contribution this version is part of
     * @param version The version wrapper object
     * @param versionRmObject The actual composition payload
     * @throws IllegalArgumentException when input is missing precedingVersionUid in case of modification
     */
    private void processCompositionVersion(UUID ehrId, UUID contributionId, Version version, Composition versionRmObject) {
        // access audit and extract method, e.g. CREATION
        I_ConceptAccess.ContributionChangeType changeType = I_ConceptAccess.ContributionChangeType.valueOf(version.getCommitAudit().getChangeType().getValue().toUpperCase());
        switch (changeType) {
            case CREATION:
                // call creation of a new composition with given input
                /*UUID compositionId =*/ compositionService.create(ehrId, versionRmObject, contributionId);
                break;
            case AMENDMENT: // triggers the same processing as modification
            case MODIFICATION:
                // call modification of the given composition
                /*String versionUid =*/ compositionService.update(getVersionedUidFromVersion(version), versionRmObject, contributionId);
                break;
            case DELETED:   // case of deletion change type, but request also has payload (TODO: should that be even allowed? specification-wise it's not forbidden)
                /*LocalDateTime localDateTime =*/ compositionService.delete(getVersionedUidFromVersion(version));
                break;
            case SYNTHESIS:     // TODO
            case UNKNOWN:       // TODO
            default:
                throw new UnexpectedSwitchCaseException(changeType);
        }
    }

    /**
     * Helper to process versions from a contribution, which do not have the optional "data" attribute and therefore are called metadata versions.
     * @param ehrId ID of given EHR scope
     * @param contributionId Top level contribution this version is part of
     * @param version The version wrapper object
     */
    private void processMetadataVersion(UUID ehrId, UUID contributionId, Version version) {
        // access audit and extract method, e.g. CREATION
        I_ConceptAccess.ContributionChangeType changeType = I_ConceptAccess.ContributionChangeType.valueOf(version.getCommitAudit().getChangeType().getValue().toUpperCase());
        switch (changeType) {
            case DELETED:
                // deleting an object without knowing which type it is requires checking of type, here with nested try-catch blocks
                UUID objectUid = getVersionedUidFromVersion(version);
                try {
                    // throw exception to signal no matching composition was found
                    CompositionDto compo = compositionService.retrieve(objectUid, null).orElseThrow(Exception::new);
                    compositionService.delete(compo.getUuid());
                } catch (Exception e) { // given version ID is not of type composition - ignoring the exception because it is expected
                    // TODO add nested try-catchs for more supported types, for instance folder, when their contribution support gets implemented
                    // TODO last "try catch" in line needs to rethrow for real, as then no matching object would have been found
                    throw new ObjectNotFoundException(I_CompositionAccess.class.getName(), "Couldn't find object matching id: " + objectUid); // TODO: type is technically wrong, if more than one type gets tested
                }
                break;
            case SYNTHESIS:     // TODO
            case UNKNOWN:       // TODO
            case CREATION:      // not expected in a metadata version (i.e. without payload)
            case MODIFICATION:  // not expected in a metadata version (i.e. without payload)
            case AMENDMENT:     // not expected in a metadata version (i.e. without payload)
            default:
                throw new UnexpectedSwitchCaseException(changeType);
        }
    }

    /**
     * Create versionUid UUID from retrieved precedingVersionUid from payload (and do sanity checks before continuing), or throw errors if ID is not present nor valid.
     * Note: The precedingVersionUid parameter technically is optional for contributions but necessary when invoking other change types than creation.
     * @param version RM object of Version type
     * @return versionedUid
     * @throws IllegalArgumentException Given {@link Version} has no or no valid precedingVersionUid
     */
    private UUID getVersionedUidFromVersion(Version version) {
        ObjectVersionId precedingVersionUid = version.getPrecedingVersionUid();
        if(precedingVersionUid == null) {
            throw new IllegalArgumentException("Input invalid. Composition can't be modified without pointer to precedingVersionUid in Version container.");
        }
        if(precedingVersionUid.toString().split("::").length != 3) {
            throw new IllegalArgumentException("Input invalid. Given precedingVersionUid is not a versionUid.");
        }

        String versionedUid = precedingVersionUid.toString().split("::")[0];
        return UUID.fromString(versionedUid);
    }

    /**
     * retrieval of IDs of all objects that are saved as part of the given contribution
     * @param contribution ID of source contribution
     * @return Map with ID of the object as key and type ("composition", "folder",...) as value
     * @throws IllegalArgumentException on error when retrieving compositions
     */
    private Map<String, String> retrieveUuidsOfContributionObjects(UUID contribution) {
        Map<String, String> objRefs = new HashMap<>();

        // query for compositions
        Map<UUID, I_CompositionAccess> compositions = I_CompositionAccess.retrieveInstancesInContributionVersion(this.getDataAccess(), contribution);
        // for each fetched composition: add it to the return map and add the composition type tag - ignoring the value (the access)
        compositions.forEach((k, v) -> objRefs.put(k.toString(), TYPE_COMPOSITION));

        // TODO query for folders

        // TODO query for .... (all kind of versioned objects that are possible as per implementation)

        return objRefs;
    }

    /**
     * retrieval and building of AuditDetails object attached to the given contribution context
     * @param contributionId ID of contribution
     * @return {@link AuditDetails} object from contribution
     */
    private AuditDetails retrieveAuditDetails(UUID contributionId){
        UUID auditId = I_ContributionAccess.retrieveInstance(this.getDataAccess(), contributionId).getHasAuditDetails();

        I_AuditDetailsAccess auditDetailsAccess = new AuditDetailsAccess(this.getDataAccess()).retrieveInstance(this.getDataAccess(), auditId);

        String systemId = auditDetailsAccess.getSystemId().toString();
        PartyProxy committer = I_PartyIdentifiedAccess.retrievePartyIdentified(this.getDataAccess(), auditDetailsAccess.getCommitter());
        DvDateTime timeCommitted = new DvDateTime(LocalDateTime.ofInstant(auditDetailsAccess.getTimeCommitted().toInstant(), ZoneId.of(auditDetailsAccess.getTimeCommittedTzId())));
        int changeTypeCode = I_ConceptAccess.ContributionChangeType.valueOf(auditDetailsAccess.getChangeType().getLiteral().toUpperCase()).getCode();
        // FIXME: what's the terminology ID of the official change type terminology?
        DvCodedText changeType = new DvCodedText(auditDetailsAccess.getChangeType().getLiteral(), new CodePhrase(new TerminologyId("audit change type"), String.valueOf(changeTypeCode)));
        DvText description = new DvText(auditDetailsAccess.getDescription());

        return new AuditDetails(systemId, committer, timeCommitted, changeType, description);
    }
}
