/*
 * Copyright (c) 2019-2022 vitasystems GmbH and Hannover Medical School.
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

import static org.ehrbase.jooq.pg.Tables.COMPOSITION;
import static org.ehrbase.jooq.pg.Tables.COMPOSITION_HISTORY;

import com.nedap.archie.rm.changecontrol.OriginalVersion;
import com.nedap.archie.rm.composition.Composition;
import com.nedap.archie.rm.datatypes.CodePhrase;
import com.nedap.archie.rm.datavalues.DvCodedText;
import com.nedap.archie.rm.datavalues.quantity.datetime.DvDateTime;
import com.nedap.archie.rm.ehr.VersionedComposition;
import com.nedap.archie.rm.generic.Attestation;
import com.nedap.archie.rm.generic.AuditDetails;
import com.nedap.archie.rm.generic.RevisionHistory;
import com.nedap.archie.rm.generic.RevisionHistoryItem;
import com.nedap.archie.rm.support.identification.HierObjectId;
import com.nedap.archie.rm.support.identification.ObjectRef;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
import org.ehrbase.api.service.EhrService;
import org.ehrbase.api.service.TenantService;
import org.ehrbase.api.service.ValidationService;
import org.ehrbase.dao.access.interfaces.I_AttestationAccess;
import org.ehrbase.dao.access.interfaces.I_CompositionAccess;
import org.ehrbase.dao.access.interfaces.I_ConceptAccess.ContributionChangeType;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.dao.access.interfaces.I_EntryAccess;
import org.ehrbase.dao.access.jooq.AttestationAccess;
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
import org.jooq.DSLContext;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link CompositionService} implementation.
 *
 * @author Jake Smolka
 * @author Luis Marco-Ruiz
 * @author Stefan Spiska
 * @since 1.0.0
 */
@Service
@Transactional
public class CompositionServiceImp extends BaseServiceImp implements CompositionService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final ValidationService validationService;
    private final KnowledgeCacheService knowledgeCacheService;
    private final EhrService ehrService;
    private final TenantService tenantService;

    public CompositionServiceImp(
            KnowledgeCacheService knowledgeCacheService,
            ValidationService validationService,
            EhrService ehrService,
            DSLContext context,
            ServerConfig serverConfig,
            TenantService tenantService) {

        super(knowledgeCacheService, context, serverConfig);
        this.validationService = validationService;
        this.ehrService = ehrService;
        this.knowledgeCacheService = knowledgeCacheService;
        this.tenantService = tenantService;
    }

    @Override
    public Optional<UUID> create(UUID ehrId, Composition objData, UUID systemId, UUID committerId, String description) {

        UUID compositionId = createInternal(ehrId, objData, systemId, committerId, description, null, null);
        return Optional.of(compositionId);
    }

    @Override
    public Optional<UUID> create(UUID ehrId, Composition objData, UUID contribution, UUID audit) {
        UUID compositionId = createInternal(ehrId, objData, null, null, null, contribution, audit);
        return Optional.of(compositionId);
    }

    @Override
    public Optional<UUID> create(UUID ehrId, Composition objData) {
        return create(ehrId, objData, getSystemUuid(), getCurrentUserId(), null);
    }

    /**
     * Creation of a new composition. With optional custom contribution, or one will be created.
     *
     * @param ehrId          ID of EHR
     * @param composition    RMObject instance of the given Composition to be created
     * @param systemId       Audit system; or NULL if contribution is given
     * @param committerId    Audit committer; or NULL if contribution is given
     * @param description    (Optional) Audit description; or NULL if contribution is given
     * @param contributionId NULL if is not needed, or ID of given custom contribution
     * @param audit
     * @return ID of created composition
     * @throws InternalServerException when creation failed
     */
    private UUID createInternal(
            UUID ehrId,
            Composition composition,
            UUID systemId,
            UUID committerId,
            String description,
            UUID contributionId,
            UUID audit) {

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

        if (composition.getUid() instanceof ObjectVersionId objectVersionId) {

            if (!"1".equals(objectVersionId.getVersionTreeId().getValue())) {
                throw new PreconditionFailedException(
                        "Provided Id %s has a invalid Version. Expect Version 1".formatted(composition.getUid()));
            }

            if (!Objects.equals(
                    getDataAccess().getServerConfig().getNodename(),
                    objectVersionId.getCreatingSystemId().getValue())) {
                throw new PreconditionFailedException("Mismatch of creating_system_id: %s !=: %s"
                        .formatted(
                                objectVersionId.getCreatingSystemId().getValue(),
                                getDataAccess().getServerConfig().getNodename()));
            }

            I_DomainAccess domainAccess = getDataAccess();
            UUID versionedObjectId =
                    UUID.fromString(objectVersionId.getObjectId().getValue());

            if (domainAccess.getContext().fetchExists(COMPOSITION, COMPOSITION.ID.eq(versionedObjectId))
                    || domainAccess
                            .getContext()
                            .fetchExists(COMPOSITION_HISTORY, COMPOSITION_HISTORY.ID.eq(versionedObjectId))) {

                throw new PreconditionFailedException("Provided Id %s already exists".formatted(composition.getUid()));
            }

        } else if (composition.getUid() != null) {
            throw new PreconditionFailedException(
                    "Provided Id %s is not a ObjectVersionId".formatted(composition.getUid()));
        }

        // actual creation
        final UUID compositionId;
        Short sysTenant = tenantService.getCurrentSysTenant();
        try {
            var compositionAccess = I_CompositionAccess.getNewInstance(getDataAccess(), composition, ehrId, sysTenant);
            var entryAccess = I_EntryAccess.getNewInstance(
                    getDataAccess(),
                    Objects.requireNonNull(composition.getArchetypeDetails().getTemplateId())
                            .getValue(),
                    0,
                    compositionAccess.getId(),
                    composition,
                    sysTenant);
            compositionAccess.setContent(entryAccess);
            if (contributionId != null) { // in case of custom contribution, set it and invoke commit that allows custom
                // contributions
                compositionAccess.setContributionId(contributionId);
                compositionId = compositionAccess.commit(LocalDateTime.now(), contributionId, audit);
            } else { // else, invoke commit that ad hoc creates a new contribution for the composition
                if (committerId == null || systemId == null) { // mandatory fields
                    throw new InternalServerException(
                            "Error on internal contribution handling for composition creation.");
                }
                compositionId = compositionAccess.commit(LocalDateTime.now(), committerId, systemId, description);
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new InternalServerException(e);
        }

        logger.debug("Composition created: id={}", compositionId);

        return compositionId;
    }

    @Override
    public Optional<UUID> update(
            UUID ehrId,
            ObjectVersionId targetObjId,
            Composition objData,
            UUID systemId,
            UUID committerId,
            String description) {

        var compoId = internalUpdate(
                ehrId,
                UUID.fromString(targetObjId.getObjectId().getValue()),
                objData,
                systemId,
                committerId,
                description,
                null,
                null);

        return Optional.of(compoId);
    }

    @Override
    public Optional<UUID> update(
            UUID ehrId, ObjectVersionId targetObjId, Composition objData, UUID contribution, UUID audit) {

        var compoId = internalUpdate(
                ehrId,
                UUID.fromString(targetObjId.getObjectId().getValue()),
                objData,
                null,
                null,
                null,
                contribution,
                audit);
        return Optional.of(compoId);
    }

    @Override
    public Optional<UUID> update(UUID ehrId, ObjectVersionId targetObjId, Composition objData) {
        return update(ehrId, targetObjId, objData, getSystemUuid(), getCurrentUserId(), null);
    }

    /**
     * Update of an existing composition. With optional custom contribution, or existing one will be
     * updated.
     *
     * @param compositionId  ID of existing composition
     * @param composition    RMObject instance of the given Composition which represents the new version
     * @param systemId       Audit system; or NULL if contribution is given
     * @param committerId    Audit committer; or NULL if contribution is given
     * @param description    (Optional) Audit description; or NULL if contribution is given
     * @param contributionId NULL if new one should be created; or ID of given custom contribution
     * @param audit
     * @return UUID pointing to updated composition
     */
    private UUID internalUpdate(
            UUID ehrId,
            UUID compositionId,
            Composition composition,
            UUID systemId,
            UUID committerId,
            String description,
            UUID contributionId,
            UUID audit) {

        // pre-step: check ehr exists and is modifiable
        ehrService.checkEhrExistsAndIsModifiable(ehrId);

        boolean result;
        try {
            var compositionAccess = I_CompositionAccess.retrieveInstance(getDataAccess(), compositionId);
            if (compositionAccess == null) {
                throw new ObjectNotFoundException(
                        I_CompositionAccess.class.getName(), "Could not find composition: " + compositionId);
            }
            // check that the  composition is actually in the ehr
            checkCompositionIsInEhr(ehrId, compositionAccess);

            // validate RM composition
            validationService.check(composition);

            // Check if template ID is not the same in existing and given data -> error
            String existingTemplateId = compositionAccess.getContent().getTemplateId();
            String inputTemplateId =
                    composition.getArchetypeDetails().getTemplateId().getValue();
            if (!existingTemplateId.equals(inputTemplateId)) {
                // check if base template ID doesn't match  (template ID schema: "$NAME.$LANG.v$VER")
                if (!existingTemplateId.split("\\.")[0].equals(inputTemplateId.split("\\.")[0])) {
                    throw new InvalidApiParameterException("Can't update composition to have different template.");
                }
                // if base matches, check if given template ID is just a new version of the correct template
                int existingTemplateIdVersion =
                        Integer.parseInt(existingTemplateId.split("\\.v")[1]);
                int inputTemplateIdVersion =
                        Integer.parseInt(inputTemplateId.substring(inputTemplateId.lastIndexOf("\\.v") + 1));
                if (inputTemplateIdVersion < existingTemplateIdVersion) {
                    throw new InvalidApiParameterException(
                            "Can't update composition with wrong template version bump.");
                }
            }

            // to keep reference to entry to update: pull entry out of composition access and replace
            // composition content with input, then write back to the original access
            I_EntryAccess content = compositionAccess.getContent();
            content.setCompositionData(composition);
            compositionAccess.setContent(content);
            compositionAccess.setComposition(composition);
            if (contributionId != null) { // if custom contribution should be set
                compositionAccess.setContributionId(contributionId);
                result = compositionAccess.update(LocalDateTime.now(), contributionId, audit);
            } else { // else existing one will be updated
                if (committerId == null || systemId == null) {
                    throw new InternalServerException(
                            "Failed to update composition, missing mandatory audit meta data.");
                }
                result = compositionAccess.update(
                        LocalDateTime.now(), committerId, systemId, description, ContributionChangeType.MODIFICATION);
            }

        } catch (ObjectNotFoundException
                | InvalidApiParameterException
                        e) { // otherwise exceptions would always get sucked up by the catch below
            throw e;
        } catch (Exception e) {
            throw new InternalServerException(e);
        }

        if (!result) {
            throw new InternalServerException("Update failed on composition:" + compositionId);
        }
        return compositionId;
    }

    private void checkCompositionIsInEhr(UUID ehrId, I_CompositionAccess compositionAccess) {
        if (!ehrId.equals(compositionAccess.getEhrid())) {
            throw new ObjectNotFoundException(
                    "COMPOSITION",
                    String.format(
                            "EHR with id %s does not contain composition with id %s",
                            ehrId, compositionAccess.getEhrid()));
        }
    }

    @Override
    public void delete(UUID ehrId, ObjectVersionId targetObjId, UUID systemId, UUID committerId, String description) {
        internalDelete(
                ehrId,
                UUID.fromString(targetObjId.getObjectId().getValue()),
                systemId,
                committerId,
                description,
                null,
                null);
    }

    @Override
    public void delete(UUID ehrId, ObjectVersionId targetObjId, UUID contribution, UUID audit) {
        internalDelete(
                ehrId, UUID.fromString(targetObjId.getObjectId().getValue()), null, null, null, contribution, audit);
    }

    @Override
    public void delete(UUID ehrId, ObjectVersionId targetObjId) {
        delete(ehrId, targetObjId, getSystemUuid(), getCurrentUserId(), null);
    }

    /**
     * Deletion of an existing composition. With optional custom contribution, or existing one will be
     * updated.
     *
     * @param compositionId  ID of existing composition
     * @param systemId       Audit system; or NULL if contribution is given
     * @param committerId    Audit committer; or NULL if contribution is given
     * @param description    (Optional) Audit description; or NULL if contribution is given
     * @param contributionId NULL if is not needed, or ID of given custom contribution
     * @param audit
     */
    private void internalDelete(
            UUID ehrId,
            UUID compositionId,
            UUID systemId,
            UUID committerId,
            String description,
            UUID contributionId,
            UUID audit) {

        // pre-step: check if ehr exists and is modifiable
        ehrService.checkEhrExistsAndIsModifiable(ehrId);

        I_CompositionAccess compositionAccess;
        try {
            compositionAccess = I_CompositionAccess.retrieveInstance(getDataAccess(), compositionId);
        } catch (Exception e) {
            throw new ObjectNotFoundException(
                    I_CompositionAccess.class.getName(), "Error while retrieving composition", e);
        }
        if (compositionAccess == null) {
            throw new ObjectNotFoundException(
                    I_CompositionAccess.class.getName(), "Could not find composition:" + compositionId);
        }
        // check that the  composition is actually in the ehr
        checkCompositionIsInEhr(ehrId, compositionAccess);

        int result;
        if (contributionId != null) { // if custom contribution should be set
            compositionAccess.setContributionId(contributionId);
            try {
                result = compositionAccess.delete(LocalDateTime.now(), contributionId, audit);
            } catch (Exception e) {
                throw new InternalServerException(e);
            }
        } else { // if not continue with standard delete
            try {
                if (committerId == null || systemId == null) {
                    throw new InternalServerException(
                            "Failed to update composition, missing mandatory audit meta data.");
                }
                result = compositionAccess.delete(LocalDateTime.now(), committerId, systemId, description);
            } catch (Exception e) {
                throw new InternalServerException(e);
            }
        }
        if (result <= 0) {
            throw new InternalServerException("Delete failed on composition:" + compositionAccess.getId());
        }
    }

    @Override
    public Optional<Composition> retrieve(UUID ehrId, UUID compositionId, Integer version)
            throws InternalServerException {

        // check that the ehr exists
        ehrService.checkEhrExists(ehrId);

        final I_CompositionAccess compositionAccess;
        if (version != null) {
            compositionAccess = I_CompositionAccess.retrieveCompositionVersion(getDataAccess(), compositionId, version);
        } else { // default to latest version
            compositionAccess = I_CompositionAccess.retrieveCompositionVersion(
                    getDataAccess(), compositionId, getLastVersionNumber(compositionId));
        }

        // check that the composition is actually in the ehr
        if (compositionAccess != null) {
            checkCompositionIsInEhr(ehrId, compositionAccess);
        }
        return getComposition(compositionAccess);
    }

    @Override
    public UUID getEhrId(UUID compositionId) {
        return I_CompositionAccess.getEhrId(getDataAccess(), compositionId);
    }

    private Optional<Composition> getComposition(I_CompositionAccess compositionAccess) {
        return Optional.ofNullable(compositionAccess)
                .map(I_CompositionAccess::getContent)
                .map(I_EntryAccess::getComposition);
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
                        new FlatJasonProvider(new TemplateProvider() {
                                    @Override
                                    public Optional<OPERATIONALTEMPLATE> find(String s) {
                                        return knowledgeCacheService.retrieveOperationalTemplate(s);
                                    }

                                    @Override
                                    public Optional<WebTemplate> buildIntrospect(String templateId) {
                                        return Optional.ofNullable(
                                                knowledgeCacheService.getQueryOptMetaData(templateId));
                                    }
                                })
                                .buildFlatJson(FlatFormat.SIM_SDT, composition.getTemplateId())
                                .marshal(composition.getComposition()),
                        StructuredStringFormat.JSON);
                break;
            case STRUCTURED:
                compositionString = new StructuredString(
                        new FlatJasonProvider(new TemplateProvider() {
                                    @Override
                                    public Optional<OPERATIONALTEMPLATE> find(String s) {
                                        return knowledgeCacheService.retrieveOperationalTemplate(s);
                                    }

                                    @Override
                                    public Optional<WebTemplate> buildIntrospect(String templateId) {
                                        return Optional.ofNullable(
                                                knowledgeCacheService.getQueryOptMetaData(templateId));
                                    }
                                })
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
                composition = new FlatJasonProvider(new TemplateProvider() {
                            @Override
                            public Optional<OPERATIONALTEMPLATE> find(String s) {
                                return knowledgeCacheService.retrieveOperationalTemplate(s);
                            }

                            @Override
                            public Optional<WebTemplate> buildIntrospect(String templateId) {
                                return Optional.ofNullable(knowledgeCacheService.getQueryOptMetaData(templateId));
                            }
                        })
                        .buildFlatJson(FlatFormat.SIM_SDT, templateId)
                        .unmarshal(content);
                break;
            case STRUCTURED:
                composition = new FlatJasonProvider(new TemplateProvider() {
                            @Override
                            public Optional<OPERATIONALTEMPLATE> find(String s) {
                                return knowledgeCacheService.retrieveOperationalTemplate(s);
                            }

                            @Override
                            public Optional<WebTemplate> buildIntrospect(String templateId) {
                                return Optional.ofNullable(knowledgeCacheService.getQueryOptMetaData(templateId));
                            }
                        })
                        .buildFlatJson(FlatFormat.STRUCTURED, templateId)
                        .unmarshal(content);
                break;
            default:
                throw new UnexpectedSwitchCaseException(format);
        }
        return composition;
    }

    @Override
    public Integer getLastVersionNumber(UUID compositionId) throws InternalServerException {
        try {
            return I_CompositionAccess.getLastVersionNumber(getDataAccess(), compositionId);
        } catch (Exception e) {
            throw new InternalServerException(e);
        }
    }

    @Override
    public Integer getVersionByTimestamp(UUID compositionId, LocalDateTime timestamp) {
        int version;
        try {
            version = I_CompositionAccess.getVersionFromTimeStamp(
                    getDataAccess(), compositionId, Timestamp.valueOf(timestamp));
        } catch (ObjectNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new InternalServerException(e);
        }
        if (version <= 0) {
            throw new InternalServerException("Invalid version number calculated.");
        } else {
            return version;
        }
    }

    @Override
    public String getTemplateIdFromInputComposition(String content, CompositionFormat format) {
        Composition composition = buildComposition(content, format, null);
        if (composition.getArchetypeDetails() == null
                || composition.getArchetypeDetails().getTemplateId() == null) {
            return null;
        } else {
            return composition.getArchetypeDetails().getTemplateId().getValue();
        }
    }

    @Override
    public String retrieveTemplateId(UUID compositionId) {
        return I_EntryAccess.getTemplateIdFromEntry(getDataAccess(), compositionId);
    }

    @Override
    public boolean exists(UUID versionedObjectId) {
        return I_CompositionAccess.exists(this.getDataAccess(), versionedObjectId);
    }

    @Override
    public boolean isDeleted(UUID versionedObjectId) {
        return I_CompositionAccess.isDeleted(this.getDataAccess(), versionedObjectId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public void adminDelete(UUID compositionId) {
        I_CompositionAccess compositionAccess = I_CompositionAccess.retrieveInstance(getDataAccess(), compositionId);
        if (compositionAccess != null) {
            compositionAccess.adminDelete();
        }
    }

    @Override
    public VersionedComposition getVersionedComposition(UUID ehrId, UUID composition) {
        Optional<CompositionDto> dto = retrieve(ehrId, composition, 1).map(c -> CompositionService.from(ehrId, c));

        VersionedComposition compo = new VersionedComposition();
        if (dto.isPresent()) {
            compo.setUid(new HierObjectId(dto.get().getUuid().toString()));
            compo.setOwnerId(
                    new ObjectRef<>(new HierObjectId(dto.get().getEhrId().toString()), "local", "ehr"));

            Map<Integer, I_CompositionAccess> compos =
                    I_CompositionAccess.getVersionMapOfComposition(getDataAccess(), composition);
            if (compos.containsKey(1)) {
                compo.setTimeCreated(new DvDateTime(OffsetDateTime.of(
                        compos.get(1).getSysTransaction().toLocalDateTime(),
                        OffsetDateTime.now().getOffset())));
            } else {
                throw new InternalServerException("Inconsistent composition data, no version 1 available");
            }
        }

        return compo;
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
        // check for valid version parameter
        if ((version == 0) || I_CompositionAccess.getLastVersionNumber(getDataAccess(), versionedObjectUid) < version) {
            throw new ObjectNotFoundException(
                    "versioned_composition", "No VERSIONED_COMPOSITION with given version: " + version);
        }

        // retrieve requested object
        I_CompositionAccess compositionAccess =
                I_CompositionAccess.retrieveCompositionVersion(getDataAccess(), versionedObjectUid, version);
        if (compositionAccess == null) {
            return Optional.empty();
        }

        // create data for output, i.e. fields of the OriginalVersion<Composition>
        ObjectVersionId versionId = new ObjectVersionId(
                versionedObjectUid + "::" + getServerConfig().getNodename() + "::" + version);
        DvCodedText lifecycleState = new DvCodedText(
                "complete", new CodePhrase("532")); // TODO: once lifecycle state is supported, get it here dynamically
        AuditDetails commitAudit = compositionAccess.getAuditDetailsAccess().getAsAuditDetails();
        ObjectRef<HierObjectId> contribution = new ObjectRef<>(
                new HierObjectId(compositionAccess.getContributionId().toString()), "openehr", "contribution");
        List<UUID> attestationIdList = I_AttestationAccess.retrieveListOfAttestationsByRef(
                getDataAccess(), compositionAccess.getAttestationRef());
        List<Attestation> attestations = null; // as default, gets content if available in the following lines
        if (!attestationIdList.isEmpty()) {
            attestations = new ArrayList<>();
            for (UUID id : attestationIdList) {
                I_AttestationAccess a = new AttestationAccess(getDataAccess()).retrieveInstance(id);
                attestations.add(a.getAsAttestation());
            }
        }

        ObjectVersionId precedingVersionId = null;
        // check if there is a preceding version and set it, if available
        if (version > 1) {
            // in the current scope version is an int and therefore: preceding = current - 1
            precedingVersionId = new ObjectVersionId(
                    versionedObjectUid + "::" + getServerConfig().getNodename() + "::" + (version - 1));
        }

        Optional<Composition> compositionDto = retrieve(ehrUid, versionedObjectUid, version);
        Composition composition = null;
        if (compositionDto.isPresent()) {
            composition = compositionDto.get();
        }

        OriginalVersion<Composition> versionComposition = new OriginalVersion<>(
                versionId,
                precedingVersionId,
                composition,
                lifecycleState,
                commitAudit,
                contribution,
                null,
                null,
                attestations);

        return Optional.of(versionComposition);
    }
}
