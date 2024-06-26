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
import static org.ehrbase.repository.AbstractVersionedObjectRepository.extractUid;
import static org.ehrbase.repository.AbstractVersionedObjectRepository.extractVersion;

import com.nedap.archie.rm.changecontrol.OriginalVersion;
import com.nedap.archie.rm.composition.Composition;
import com.nedap.archie.rm.ehr.VersionedComposition;
import com.nedap.archie.rm.generic.Attestation;
import com.nedap.archie.rm.generic.AuditDetails;
import com.nedap.archie.rm.generic.RevisionHistory;
import com.nedap.archie.rm.generic.RevisionHistoryItem;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import com.nedap.archie.rm.support.identification.UIDBasedId;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import org.ehrbase.api.exception.BadGatewayException;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.exception.PreconditionFailedException;
import org.ehrbase.api.exception.UnexpectedSwitchCaseException;
import org.ehrbase.api.exception.UnprocessableEntityException;
import org.ehrbase.api.exception.ValidationException;
import org.ehrbase.api.service.CompositionService;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.api.service.SystemService;
import org.ehrbase.api.service.ValidationService;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.CompositionDto;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.CompositionFormat;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.StructuredString;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.StructuredStringFormat;
import org.ehrbase.openehr.sdk.serialisation.flatencoding.FlatFormat;
import org.ehrbase.openehr.sdk.serialisation.flatencoding.FlatJasonProvider;
import org.ehrbase.openehr.sdk.serialisation.jsonencoding.CanonicalJson;
import org.ehrbase.openehr.sdk.serialisation.xmlencoding.CanonicalXML;
import org.ehrbase.openehr.sdk.webtemplate.model.WebTemplate;
import org.ehrbase.openehr.sdk.webtemplate.templateprovider.TemplateProvider;
import org.ehrbase.repository.CompositionRepository;
import org.ehrbase.util.UuidGenerator;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * {@link CompositionService} implementation.
 */
@Service
public class CompositionServiceImp implements CompositionService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final ValidationService validationService;
    private final KnowledgeCacheServiceImp knowledgeCacheService;
    private final EhrService ehrService;

    private final CompositionRepository compositionRepository;

    private final SystemService systemService;

    public CompositionServiceImp(
            KnowledgeCacheServiceImp knowledgeCacheService,
            ValidationService validationService,
            EhrService ehrService,
            SystemService systemService,
            CompositionRepository compositionRepository) {

        this.validationService = validationService;
        this.ehrService = ehrService;
        this.knowledgeCacheService = knowledgeCacheService;
        this.compositionRepository = compositionRepository;
        this.systemService = systemService;
    }

    @Override
    public Optional<UUID> create(UUID ehrId, Composition objData, UUID contribution, UUID audit) {
        UUID compositionId = createInternal(ehrId, objData, contribution, audit);
        return Optional.of(compositionId);
    }

    @Override
    public Optional<UUID> create(UUID ehrId, Composition objData) {
        UUID compositionId = createInternal(ehrId, objData, null, null);
        return Optional.of(compositionId);
    }

    /**
     * Creation of a new composition. With optional custom contribution, or one will be created.
     *
     * @param ehrId          ID of EHR
     * @param composition    RMObject instance of the given Composition to be created
     * @param contributionId NULL if is not needed, or ID of given custom contribution
     * @param audit
     * @return ID of created composition
     * @throws InternalServerException when creation failed
     */
    private UUID createInternal(UUID ehrId, Composition composition, UUID contributionId, UUID audit) {

        // pre-step: check for existing and modifiable ehr
        ehrService.checkEhrExistsAndIsModifiable(ehrId);

        // pre-step: validate
        try {
            validationService.check(composition);

        } catch (UnprocessableEntityException | ValidationException | BadGatewayException e) {
            throw e; // forward exception
        } catch (org.ehrbase.openehr.sdk.validation.ValidationException e) {
            throw new UnprocessableEntityException(e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new ValidationException(e);
        } catch (Exception e) {
            throw new InternalServerException(e);
        }

        final ObjectVersionId objectVersionId = checkOrConstructObjectVersionId(composition.getUid());

        composition.setUid(objectVersionId);
        // actual creation
        final UUID compositionId = extractUid(objectVersionId);

        compositionRepository.commit(ehrId, composition, contributionId, audit);

        logger.debug("Composition created: id={}", compositionId);

        return compositionId;
    }

    private ObjectVersionId checkOrConstructObjectVersionId(@Nullable UIDBasedId uid) {
        if (uid == null) {
            return buildObjectVersionId(UuidGenerator.randomUUID(), 1, systemService);

        } else if (uid instanceof ObjectVersionId objectVersionId) {

            if (!"1".equals(objectVersionId.getVersionTreeId().getValue())) {
                throw new PreconditionFailedException(
                        "Provided Id %s has a invalid Version. Expect Version 1".formatted(uid));
            }

            if (!Objects.equals(
                    systemService.getSystemId(),
                    objectVersionId.getCreatingSystemId().getValue())) {
                throw new PreconditionFailedException("Mismatch of creating_system_id: %s !=: %s"
                        .formatted(objectVersionId.getCreatingSystemId().getValue(), systemService.getSystemId()));
            }

            if (compositionRepository.exists(
                    UUID.fromString(objectVersionId.getObjectId().getValue()))) {
                throw new PreconditionFailedException("Provided Id %s already exists".formatted(uid));
            }
            return (ObjectVersionId) uid;
        } else {
            throw new PreconditionFailedException("Provided Id %s is not a ObjectVersionId".formatted(uid));
        }
    }

    @Override
    public Optional<UUID> update(
            UUID ehrId, ObjectVersionId targetObjId, Composition objData, UUID contribution, UUID audit) {

        var compoId = internalUpdate(ehrId, targetObjId, objData, contribution, audit);
        return Optional.of(compoId);
    }

    @Override
    public Optional<UUID> update(UUID ehrId, ObjectVersionId targetObjId, Composition objData) {
        var compoId = internalUpdate(ehrId, targetObjId, objData, null, null);
        return Optional.of(compoId);
    }

    /**
     * Update of an existing composition. With optional custom contribution, or existing one will be
     * updated.
     *
     * @param compositionId  ID of existing composition
     * @param composition    RMObject instance of the given Composition which represents the new version
     * @param contributionId NULL if new one should be created; or ID of given custom contribution
     * @param audit
     * @return UUID pointing to updated composition
     */
    private UUID internalUpdate(
            UUID ehrId, ObjectVersionId compositionId, Composition composition, UUID contributionId, UUID audit) {

        // pre-step: check for existing and modifiable ehr
        ehrService.checkEhrExistsAndIsModifiable(ehrId);

        // pre-step: validate
        try {
            validationService.check(composition);

        } catch (org.ehrbase.openehr.sdk.validation.ValidationException e) {
            throw new UnprocessableEntityException(e.getMessage());
        } catch (UnprocessableEntityException | ValidationException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw new ValidationException(e);
        } catch (Exception e) {
            throw new InternalServerException(e);
        }

        UUID compId = UUID.fromString(compositionId.getObjectId().getValue());
        int version = Integer.parseInt(compositionId.getVersionTreeId().getValue());

        String existingTemplateId = compositionRepository
                .findTemplateId(compId)
                .orElseThrow(() -> new ObjectNotFoundException(
                        "composition", "No COMPOSITION with given id: %s".formatted(compId)));

        String inputTemplateId =
                composition.getArchetypeDetails().getTemplateId().getValue();
        if (!existingTemplateId.equals(inputTemplateId)) {
            // check if base template ID doesn't match  (template ID schema: "$NAME.$LANG.v$VER")
            if (!existingTemplateId.split("\\.")[0].equals(inputTemplateId.split("\\.")[0])) {
                throw new InvalidApiParameterException("Can't update composition to have different template.");
            }
            // if base matches, check if given template ID is just a new version of the correct template
            int existingTemplateIdVersion = Integer.parseInt(existingTemplateId.split("\\.v")[1]);
            int inputTemplateIdVersion =
                    Integer.parseInt(inputTemplateId.substring(inputTemplateId.lastIndexOf("\\.v") + 1));
            if (inputTemplateIdVersion < existingTemplateIdVersion) {
                throw new InvalidApiParameterException("Can't update composition with wrong template version bump.");
            }
        }

        composition.setUid(buildObjectVersionId(compId, version + 1, systemService));

        compositionRepository.update(ehrId, composition, contributionId, audit);

        return compId;
    }

    @Override
    public void delete(UUID ehrId, ObjectVersionId targetObjId, UUID contribution, UUID audit) {
        internalDelete(ehrId, targetObjId, contribution, audit);
    }

    @Override
    public void delete(UUID ehrId, ObjectVersionId targetObjId) {
        internalDelete(ehrId, targetObjId, null, null);
    }

    /**
     * Deletion of an existing composition. With optional custom contribution, or existing one will be
     * updated.
     *
     * @param compositionId  ID of existing composition
     * @param contributionId NULL if is not needed, or ID of given custom contribution
     * @param audit
     */
    private void internalDelete(UUID ehrId, ObjectVersionId compositionId, UUID contributionId, UUID audit) {

        // pre-step: check if ehr exists and is modifiable
        ehrService.checkEhrExistsAndIsModifiable(ehrId);

        compositionRepository.delete(
                ehrId,
                UUID.fromString(compositionId.getObjectId().getValue()),
                extractVersion(compositionId),
                contributionId,
                audit);
    }

    @Override
    public Optional<Composition> retrieve(UUID ehrId, UUID compositionId, Integer version)
            throws InternalServerException {

        Optional<Composition> result;

        if (version == null) {
            result = compositionRepository.findHead(ehrId, compositionId);
        } else {
            result = compositionRepository.findByVersion(ehrId, compositionId, version);
        }

        if (result.isEmpty()) {
            // check that the ehr exists and throw error if not
            ehrService.checkEhrExists(ehrId);
        }

        return result;
    }

    @Override
    public Optional<UUID> getEhrIdForComposition(UUID compositionId) {
        return compositionRepository.findEHRforComposition(compositionId);
    }

    /**
     * Public serializer entry point which will be called with composition dto fetched from database
     * and the desired target serialized string format. Will parse the composition dto into target
     * format either with a custom lambda expression for desired target format
     *
     * @param composition Composition dto from database
     * @param format      Target format
     * @return Structured string with string of data and content format
     */
    @Override
    public StructuredString serialize(CompositionDto composition, CompositionFormat format) {
        final StructuredString compositionString;
        switch (format) {
            case XML:
                compositionString = new StructuredString(
                        new CanonicalXML().marshal(composition.getComposition(), false), StructuredStringFormat.XML);
                break;
            case JSON:
                compositionString = new StructuredString(
                        new CanonicalJson().marshal(composition.getComposition()), StructuredStringFormat.JSON);
                break;
            case FLAT:
                compositionString = new StructuredString(
                        new FlatJasonProvider(createTemplateProvider())
                                .buildFlatJson(FlatFormat.SIM_SDT, composition.getTemplateId())
                                .marshal(composition.getComposition()),
                        StructuredStringFormat.JSON);
                break;
            case STRUCTURED:
                compositionString = new StructuredString(
                        new FlatJasonProvider(createTemplateProvider())
                                .buildFlatJson(FlatFormat.STRUCTURED, composition.getTemplateId())
                                .marshal(composition.getComposition()),
                        StructuredStringFormat.JSON);
                break;
            default:
                throw new UnexpectedSwitchCaseException(format);
        }
        return compositionString;
    }

    public Composition buildComposition(String content, CompositionFormat format, String templateId) {
        final Composition composition;
        switch (format) {
            case XML:
                composition = new CanonicalXML().unmarshal(content, Composition.class);
                break;
            case JSON:
                composition = new CanonicalJson().unmarshal(content, Composition.class);
                break;
            case FLAT:
                composition = new FlatJasonProvider(createTemplateProvider())
                        .buildFlatJson(FlatFormat.SIM_SDT, templateId)
                        .unmarshal(content);
                break;
            case STRUCTURED:
                composition = new FlatJasonProvider(createTemplateProvider())
                        .buildFlatJson(FlatFormat.STRUCTURED, templateId)
                        .unmarshal(content);
                break;
            default:
                throw new UnexpectedSwitchCaseException(format);
        }
        return composition;
    }

    //    private

    private TemplateProvider createTemplateProvider() {
        return new TemplateProvider() {
            @Override
            public Optional<OPERATIONALTEMPLATE> find(String s) {
                return knowledgeCacheService.retrieveOperationalTemplate(s);
            }

            @Override
            public Optional<WebTemplate> buildIntrospect(String templateId) {
                if (templateId == null) {
                    return Optional.empty();
                }
                return Optional.ofNullable(knowledgeCacheService.getQueryOptMetaData(templateId));
            }
        };
    }

    @Override
    public int getLastVersionNumber(UUID compositionId) {

        Optional<Integer> versionNumber = compositionRepository.getLatestVersionNumber(compositionId);
        return versionNumber.orElseThrow(() -> new ObjectNotFoundException(
                "composition", "No COMPOSITION with given id: %s".formatted(compositionId)));
    }

    @Override
    public int getVersionByTimestamp(UUID compositionId, OffsetDateTime timestamp) {

        Optional<Integer> versionByTime = compositionRepository.findVersionByTime(compositionId, timestamp);
        return versionByTime.orElseThrow(() -> new ObjectNotFoundException(
                "composition", "No COMPOSITION with given id: %s".formatted(compositionId)));
    }

    @Override
    public String retrieveTemplateId(UUID compositionId) {

        return compositionRepository.findTemplateId(compositionId).orElseThrow();
    }

    @Override
    public boolean exists(UUID versionedObjectId) {
        return compositionRepository.exists(versionedObjectId);
    }

    @Override
    public boolean isDeleted(UUID ehrId, UUID versionedObjectId, Integer version) {

        if (version == null) {
            Optional<Integer> versionNumber = compositionRepository.getLatestVersionNumber(versionedObjectId);
            if (versionNumber.isEmpty()) {
                return false;
            }
            version = versionNumber.get();
        }

        return compositionRepository.isDeleted(ehrId, versionedObjectId, version);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public void adminDelete(UUID compositionId) {

        compositionRepository.adminDelete(compositionId);
    }

    @Override
    public VersionedComposition getVersionedComposition(UUID ehrId, UUID composition) {

        ehrService.checkEhrExists(ehrId);

        Optional<VersionedComposition> versionedComposition =
                compositionRepository.getVersionedComposition(ehrId, composition);

        if (versionedComposition.isEmpty()) {
            throw new ObjectNotFoundException(
                    "versioned_composition", "No VERSIONED_COMPOSITION with given id: " + composition);
        }
        return versionedComposition.get();
    }

    @Override
    public RevisionHistory getRevisionHistoryOfVersionedComposition(UUID ehrUid, UUID composition) {
        // get number of versions
        int versions = getLastVersionNumber(composition);
        // fetch each version and add to revision history
        RevisionHistory revisionHistory = new RevisionHistory();
        for (int i = 1; i <= versions; i++) {
            Optional<OriginalVersion<Composition>> compoVersion = getOriginalVersionComposition(ehrUid, composition, i);
            compoVersion.ifPresent(compositionOriginalVersion ->
                    revisionHistory.addItem(revisionHistoryItemFromComposition(compositionOriginalVersion)));
        }

        if (revisionHistory.getItems().isEmpty()) {
            throw new InternalServerException("Problem creating RevisionHistory"); // never should be empty; not valid
        }
        return revisionHistory;
    }

    private RevisionHistoryItem revisionHistoryItemFromComposition(OriginalVersion<Composition> composition) {

        ObjectVersionId objectVersionId = composition.getUid();

        // Note: is List but only has more than one item when there are contributions regarding this
        // object of change type attestation
        List<AuditDetails> auditDetailsList = new ArrayList<>();
        // retrieving the audits
        auditDetailsList.add(composition.getCommitAudit());

        // add retrieval of attestations, if there are any
        if (composition.getAttestations() != null) {
            for (Attestation a : composition.getAttestations()) {
                AuditDetails newAudit = new AuditDetails(
                        a.getSystemId(), a.getCommitter(), a.getTimeCommitted(), a.getChangeType(), a.getDescription());
                auditDetailsList.add(newAudit);
            }
        }

        return new RevisionHistoryItem(objectVersionId, auditDetailsList);
    }

    @Override
    public Optional<OriginalVersion<Composition>> getOriginalVersionComposition(
            UUID ehrUid, UUID versionedObjectUid, int version) {

        return compositionRepository.getOriginalVersionComposition(ehrUid, versionedObjectUid, version);
    }
}
