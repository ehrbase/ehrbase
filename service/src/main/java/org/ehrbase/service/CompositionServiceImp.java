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

import com.nedap.archie.rm.changecontrol.OriginalVersion;
import com.nedap.archie.rm.composition.Composition;
import com.nedap.archie.rm.ehr.VersionedComposition;
import com.nedap.archie.rm.generic.RevisionHistory;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import com.nedap.archie.rm.support.identification.UIDBasedId;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;
import org.ehrbase.api.dto.experimental.ItemTagDto.ItemTagRMType;
import org.ehrbase.api.exception.BadGatewayException;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.exception.PreconditionFailedException;
import org.ehrbase.api.exception.UnexpectedSwitchCaseException;
import org.ehrbase.api.exception.UnprocessableEntityException;
import org.ehrbase.api.exception.ValidationException;
import org.ehrbase.api.knowledge.TemplateCacheService;
import org.ehrbase.api.service.CompositionService;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.api.service.SystemService;
import org.ehrbase.api.service.ValidationService;
import org.ehrbase.api.util.LocatableUtils;
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
import org.ehrbase.repository.experimental.ItemTagRepository;
import org.ehrbase.util.SemVer;
import org.ehrbase.util.UuidGenerator;
import org.jspecify.annotations.NonNull;
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

    private static final Pattern TEMPLATE_VERSION_PATTERN;

    static {
        UnaryOperator<String> capturing = content -> "(" + content + ")";

        UnaryOperator<String> nonCapturing = content -> "(?:" + content + ")";

        UnaryOperator<String> optionalNonCapturing = content -> nonCapturing.apply(content) + '?';

        String dot = "\\.";

        // lazy capturing so lang and version can be matched
        String namePart = capturing.apply(".+?");

        String langPart = "[a-zA-Z]{2}";
        // language is ignored for comparisons
        String languagePart = optionalNonCapturing.apply(dot + langPart + optionalNonCapturing.apply("-" + langPart));

        String versionPart = "0|[1-9]\\d*";
        String versionGroup = capturing.apply(versionPart);

        String fullVersionPart =
                // major
                versionGroup
                        + optionalNonCapturing.apply(dot
                                // minor
                                + versionGroup
                                + optionalNonCapturing.apply(
                                        // patch
                                        dot + versionGroup));

        // several separators are recognized
        String versionSeparator = "[._ ][vV]";

        TEMPLATE_VERSION_PATTERN = Pattern.compile(
                namePart + languagePart + optionalNonCapturing.apply(versionSeparator + fullVersionPart));
    }

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final ValidationService validationService;
    private final TemplateCacheService templateCacheService;
    private final EhrService ehrService;

    private final CompositionRepository compositionRepository;
    private final ItemTagRepository itemTagRepository;

    private final SystemService systemService;

    public CompositionServiceImp(
            TemplateCacheService templateCacheService,
            ValidationService validationService,
            EhrService ehrService,
            SystemService systemService,
            CompositionRepository compositionRepository,
            ItemTagRepository itemTagRepository) {

        this.validationService = validationService;
        this.ehrService = ehrService;
        this.templateCacheService = templateCacheService;
        this.compositionRepository = compositionRepository;
        this.itemTagRepository = itemTagRepository;
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

        UUID templateId = getTemplateUuid(composition);

        compositionRepository.commit(ehrId, composition, contributionId, audit, templateId);

        logger.debug("Composition created: id={}", compositionId);

        return compositionId;
    }

    private ObjectVersionId checkOrConstructObjectVersionId(UIDBasedId uid) {
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

        UUID compId = LocatableUtils.getUuid(compositionId);
        int version = LocatableUtils.getUidVersion(compositionId);

        String existingTemplateId = compositionRepository
                .findTemplateId(compId)
                .flatMap(templateCacheService::findTemplateIdByUuid)
                .orElseThrow(() -> new ObjectNotFoundException(
                        "composition", "No COMPOSITION with given id: %s".formatted(compId)));

        ensureTemplateCompatible(LocatableUtils.getTemplateId(composition), existingTemplateId);

        composition.setUid(buildObjectVersionId(compId, version + 1, systemService));

        UUID templateId = getTemplateUuid(composition);

        compositionRepository.update(ehrId, composition, contributionId, audit, templateId);

        return compId;
    }

    private @NonNull UUID getTemplateUuid(Composition composition) {
        return Optional.of(composition)
                .map(LocatableUtils::getTemplateId)
                .flatMap(templateCacheService::findUuidByTemplateId)
                .orElseThrow(
                        () -> new IllegalArgumentException("Unknown or missing template in composition to be stored"));
    }

    /**
     * check if base template ID doesn't match  (template ID schema: "$NAME.$LANG.v$VER")
     * @param inputTemplateId
     * @param existingTemplateId
     */
    static void ensureTemplateCompatible(String inputTemplateId, String existingTemplateId) {
        if (existingTemplateId.equals(inputTemplateId)) {
            return;
        }

        Pair<String, SemVer> oldVersion = getTemplateSemVer(existingTemplateId);
        Pair<String, SemVer> newVersion = getTemplateSemVer(inputTemplateId);

        // check if base template ID doesn't match  (template ID schema: "$NAME.$LANG.v$VER")
        if (!oldVersion.getKey().equals(newVersion.getKey())) {
            throw new InvalidApiParameterException("Can't update composition to have different template.");
        }
        // if base matches, check if given template ID is just a new version of the correct template
        if (isOlder(newVersion.getRight(), oldVersion.getRight())) {
            throw new InvalidApiParameterException("Can't update composition with wrong template version bump.");
        }
    }

    static boolean isOlder(SemVer version, SemVer baseVersion) {
        return Stream.<Function<SemVer, Integer>>of(SemVer::major, SemVer::minor, SemVer::patch)
                .map(f -> {
                    Integer v = f.apply(version);
                    Integer b = f.apply(baseVersion);

                    if (b == null) {
                        return false;
                    }
                    if (v == null) {
                        return true;
                    }
                    return Objects.equals(v, b) ? null : v < b;
                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(false);
    }

    private static Optional<String> group(Matcher matcher, int groupNr) {
        return Optional.of(matcher).filter(m -> m.groupCount() >= groupNr).map(m -> m.group(groupNr));
    }

    private static Integer integerFromGroup(Matcher matcher, int groupNr) {
        return group(matcher, groupNr).map(Integer::parseInt).orElse(null);
    }

    private static Pair<String, SemVer> getTemplateSemVer(String templateId) {
        Matcher matcher = TEMPLATE_VERSION_PATTERN.matcher(templateId);
        if (matcher.matches() && matcher.groupCount() > 1) {
            return Pair.of(
                    matcher.group(1),
                    new SemVer(
                            integerFromGroup(matcher, 2),
                            integerFromGroup(matcher, 3),
                            integerFromGroup(matcher, 4),
                            null));
        } else {
            return Pair.of(matcher.matches() ? matcher.group(1) : templateId, SemVer.NO_VERSION);
        }
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
                LocatableUtils.getUuid(compositionId),
                LocatableUtils.getUidVersion(compositionId),
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
     * @param composition Composition from database
     * @param format      Target format
     * @return Structured string with string of data and content format
     */
    @Override
    public StructuredString serialize(Composition composition, CompositionFormat format) {

        final String marshalled;
        final StructuredStringFormat stringFormat;
        switch (format) {
            case XML:
                marshalled = CanonicalXML.DEFAULT_INSTANCE.marshal(composition, false);
                stringFormat = StructuredStringFormat.XML;
                break;
            case JSON:
                marshalled = CanonicalJson.DEFAULT_INSTANCE.marshal(composition);
                stringFormat = StructuredStringFormat.JSON;
                break;
            case FLAT:
                marshalled = new FlatJasonProvider(createTemplateProvider())
                        .buildFlatJson(FlatFormat.SIM_SDT, LocatableUtils.getTemplateId(composition))
                        .marshal(composition);
                stringFormat = StructuredStringFormat.JSON;
                break;
            case STRUCTURED:
                marshalled = new FlatJasonProvider(createTemplateProvider())
                        .buildFlatJson(FlatFormat.STRUCTURED, LocatableUtils.getTemplateId(composition))
                        .marshal(composition);
                stringFormat = StructuredStringFormat.JSON;
                break;
            default:
                throw new UnexpectedSwitchCaseException(format);
        }
        return new StructuredString(marshalled, stringFormat);
    }

    public Composition buildComposition(String content, CompositionFormat format, String templateId) {
        final Composition composition;
        switch (format) {
            case XML:
                composition = CanonicalXML.DEFAULT_INSTANCE.unmarshal(content, Composition.class);
                break;
            case JSON:
                composition = CanonicalJson.DEFAULT_INSTANCE.unmarshal(content, Composition.class);
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
                throw new UnsupportedOperationException();
            }

            @Override
            public Optional<WebTemplate> buildIntrospect(String templateId) {
                if (templateId == null) {
                    return Optional.empty();
                }
                return Optional.ofNullable(templateCacheService.getInternalTemplate(templateId));
            }
        };
    }

    @Override
    public int getLastVersionNumber(UUID ehrId, UUID compositionId) {
        Optional<Integer> versionNumber;
        if (ehrId == null) {
            versionNumber = compositionRepository.getLatestVersionNumber(compositionId);
        } else {
            versionNumber = compositionRepository.getLatestVersionNumber(ehrId, compositionId);
        }
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
        return compositionRepository
                .findTemplateId(compositionId)
                .flatMap(templateCacheService::findTemplateIdByUuid)
                .orElseThrow();
    }

    @Override
    public boolean exists(UUID versionedObjectId) {
        return compositionRepository.exists(versionedObjectId);
    }

    @Override
    public boolean isDeleted(UUID ehrId, UUID versionedObjectId, Integer version) {
        if (version == null) {
            Optional<Integer> versionNumber = compositionRepository.getLatestVersionNumber(ehrId, versionedObjectId);
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

        itemTagRepository.adminDelete(compositionId, ItemTagRMType.COMPOSITION);
        compositionRepository.adminDelete(compositionId);
    }

    @Override
    public VersionedComposition getVersionedComposition(UUID ehrId, UUID composition) {
        return compositionRepository.getVersionedComposition(ehrId, composition).orElseGet(() -> {
            ehrService.checkEhrExists(ehrId);
            throw new ObjectNotFoundException(
                    "versioned_composition", "No VERSIONED_COMPOSITION with given id: %s".formatted(composition));
        });
    }

    @Override
    public RevisionHistory getRevisionHistoryOfVersionedComposition(UUID ehrUid, UUID composition) {

        RevisionHistory revisionHistory = compositionRepository.getRevisionHistory(ehrUid, composition);
        if (revisionHistory.getItems().isEmpty()) {
            throw new ObjectNotFoundException(
                    "VERSIONED_COMPOSITION", "No VERSIONED_COMPOSITION with given id: %s".formatted(composition));
        }
        return revisionHistory;
    }

    @Override
    public Optional<OriginalVersion<Composition>> getOriginalVersionComposition(
            UUID ehrUid, UUID versionedObjectUid, int version) {

        return compositionRepository.getOriginalVersionComposition(ehrUid, versionedObjectUid, version);
    }
}
