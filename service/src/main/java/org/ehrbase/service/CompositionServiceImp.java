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
import com.nedap.archie.rm.composition.Composition;
import com.nedap.archie.rm.ehr.VersionedComposition;
import com.nedap.archie.rm.generic.RevisionHistory;
import com.nedap.archie.rm.support.identification.HierObjectId;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.knowledge.KnowledgeCacheService;
import org.ehrbase.api.service.CompositionService;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.api.service.SystemService;
import org.ehrbase.api.service.ValidationService;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.CompositionFormat;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.StructuredString;
import org.ehrbase.openehr.sdk.serialisation.jsonencoding.CanonicalJson;
import org.ehrbase.openehr.sdk.serialisation.xmlencoding.CanonicalXML;
import org.ehrbase.openehr.sdk.webtemplate.model.WebTemplate;
import org.ehrbase.repository.CompositionRepository;
import org.ehrbase.repository.ContributionRepository;
import org.ehrbase.repository.composition.CompositionMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for composition CRUD operations.
 * Rewritten for normalized template tables — no DbToRmFormat or AQL dependencies.
 */
@Service
@Transactional
public class CompositionServiceImp implements CompositionService {

    private static final Logger log = LoggerFactory.getLogger(CompositionServiceImp.class);

    private final CompositionRepository compositionRepository;
    private final ContributionRepository contributionRepository;
    private final EhrService ehrService;
    private final ValidationService validationService;
    private final KnowledgeCacheService knowledgeCache;
    private final SystemService systemService;

    public CompositionServiceImp(
            CompositionRepository compositionRepository,
            ContributionRepository contributionRepository,
            EhrService ehrService,
            ValidationService validationService,
            KnowledgeCacheService knowledgeCache,
            SystemService systemService) {
        this.compositionRepository = compositionRepository;
        this.contributionRepository = contributionRepository;
        this.ehrService = ehrService;
        this.validationService = validationService;
        this.knowledgeCache = knowledgeCache;
        this.systemService = systemService;
    }

    @Override
    public Optional<UUID> create(UUID ehrId, Composition composition) {
        return create(ehrId, composition, null, null);
    }

    @Override
    public Optional<UUID> create(UUID ehrId, Composition composition, UUID contributionId, UUID audit) {
        Objects.requireNonNull(ehrId, "ehrId must not be null");
        Objects.requireNonNull(composition, "composition must not be null");

        ehrService.checkEhrExistsAndIsModifiable(ehrId);
        validationService.check(composition);

        String templateId = resolveTemplateId(composition);
        UUID templateUuid = knowledgeCache
                .findUuidByTemplateId(templateId)
                .orElseThrow(() -> new ObjectNotFoundException("template", templateId));
        WebTemplate webTemplate = knowledgeCache
                .getQueryOptMetaData(templateId)
                .orElseThrow(() -> new ObjectNotFoundException("web_template", templateId));

        if (contributionId == null) {
            contributionId = contributionRepository.createContribution(ehrId, "composition", "creation");
        }

        CompositionMetadata meta =
                compositionRepository.create(ehrId, composition, templateUuid, templateId, contributionId, webTemplate);

        return Optional.of(meta.id());
    }

    @Override
    public Optional<UUID> update(UUID ehrId, ObjectVersionId targetObjId, Composition composition) {
        return update(ehrId, targetObjId, composition, null, null);
    }

    @Override
    public Optional<UUID> update(
            UUID ehrId, ObjectVersionId targetObjId, Composition composition, UUID contributionId, UUID audit) {
        Objects.requireNonNull(ehrId, "ehrId must not be null");
        Objects.requireNonNull(targetObjId, "targetObjId must not be null");
        Objects.requireNonNull(composition, "composition must not be null");

        ehrService.checkEhrExistsAndIsModifiable(ehrId);
        validationService.check(composition);

        UUID compositionId = extractUuid(targetObjId);
        int expectedVersion = extractVersion(targetObjId);

        String templateId = resolveTemplateId(composition);
        UUID templateUuid = knowledgeCache
                .findUuidByTemplateId(templateId)
                .orElseThrow(() -> new ObjectNotFoundException("template", templateId));
        WebTemplate webTemplate = knowledgeCache
                .getQueryOptMetaData(templateId)
                .orElseThrow(() -> new ObjectNotFoundException("web_template", templateId));

        if (contributionId == null) {
            contributionId = contributionRepository.createContribution(ehrId, "composition", "modification");
        }

        CompositionMetadata meta = compositionRepository.update(
                ehrId,
                compositionId,
                composition,
                templateUuid,
                templateId,
                contributionId,
                expectedVersion,
                webTemplate);

        return Optional.of(meta.id());
    }

    @Override
    public void delete(UUID ehrId, ObjectVersionId targetObjId) {
        delete(ehrId, targetObjId, null, null);
    }

    @Override
    public void delete(UUID ehrId, ObjectVersionId targetObjId, UUID contributionId, UUID audit) {
        Objects.requireNonNull(ehrId, "ehrId must not be null");
        Objects.requireNonNull(targetObjId, "targetObjId must not be null");

        ehrService.checkEhrExistsAndIsModifiable(ehrId);

        UUID compositionId = extractUuid(targetObjId);
        int expectedVersion = extractVersion(targetObjId);

        if (contributionId == null) {
            contributionId = contributionRepository.createContribution(ehrId, "composition", "deleted");
        }

        compositionRepository.delete(ehrId, compositionId, expectedVersion, contributionId);
    }

    @Override
    public Optional<Composition> retrieve(UUID ehrId, UUID compositionId, Integer version) {
        if (version != null) {
            return compositionRepository.findByVersion(ehrId, compositionId, version);
        }
        return compositionRepository.findHead(ehrId, compositionId);
    }

    @Override
    public StructuredString serialize(Composition composition, CompositionFormat format) {
        return switch (format) {
            case JSON -> new StructuredString(new CanonicalJson().marshal(composition), CompositionFormat.JSON);
            case XML -> new StructuredString(new CanonicalXML().marshal(composition), CompositionFormat.XML);
            default -> throw new IllegalArgumentException("Unsupported format: " + format);
        };
    }

    @Override
    public Composition buildComposition(String content, CompositionFormat format, String templateId) {
        return switch (format) {
            case JSON -> new CanonicalJson().unmarshal(content, Composition.class);
            case XML -> new CanonicalXML().unmarshal(content, Composition.class);
            default -> throw new IllegalArgumentException("Unsupported format: " + format);
        };
    }

    @Override
    public int getLastVersionNumber(UUID ehrId, UUID compositionId) {
        return compositionRepository
                .getLatestVersionNumber(compositionId)
                .orElseThrow(() -> new ObjectNotFoundException("composition", compositionId.toString()));
    }

    @Override
    public String retrieveTemplateId(UUID compositionId) {
        return compositionRepository.retrieveTemplateIdForComposition(compositionId);
    }

    @Override
    public int getVersionByTimestamp(UUID compositionId, OffsetDateTime timestamp) {
        return compositionRepository.getVersionByTimestamp(compositionId, timestamp);
    }

    @Override
    public boolean exists(UUID versionedObjectId) {
        return compositionRepository.exists(versionedObjectId);
    }

    @Override
    public boolean isDeleted(UUID ehrId, UUID versionedObjectId, Integer version) {
        return compositionRepository.isDeleted(ehrId, versionedObjectId, version);
    }

    @Override
    public void adminDelete(UUID compositionId) {
        throw new UnsupportedOperationException("Admin delete deferred to Phase 9 (Legacy Code Removal)");
    }

    @Override
    public VersionedComposition getVersionedComposition(UUID ehrUid, UUID compositionId) {
        ehrService.checkEhrExists(ehrUid);
        if (!compositionRepository.exists(compositionId)
                && !compositionRepository.isDeleted(ehrUid, compositionId, null)) {
            throw new ObjectNotFoundException("composition", compositionId.toString());
        }
        VersionedComposition vc = new VersionedComposition();
        vc.setUid(new HierObjectId(compositionId.toString()));
        vc.setOwnerId(new com.nedap.archie.rm.support.identification.ObjectRef<>(
                new HierObjectId(ehrUid.toString()), "local", "EHR"));
        return vc;
    }

    @Override
    public RevisionHistory getRevisionHistoryOfVersionedComposition(UUID ehrUid, UUID compositionId) {
        ehrService.checkEhrExists(ehrUid);
        return new RevisionHistory();
    }

    @Override
    public Optional<OriginalVersion<Composition>> getOriginalVersionComposition(
            UUID ehrUid, UUID versionedObjectUid, int version) {
        Optional<Composition> composition = compositionRepository.findByVersion(ehrUid, versionedObjectUid, version);
        if (composition.isEmpty()) {
            return Optional.empty();
        }
        OriginalVersion<Composition> originalVersion = new OriginalVersion<>();
        originalVersion.setUid(buildObjectVersionId(versionedObjectUid, version, systemService.getSystemId()));
        originalVersion.setData(composition.get());
        return Optional.of(originalVersion);
    }

    @Override
    public Optional<UUID> getEhrIdForComposition(UUID compositionId) {
        return compositionRepository.getEhrIdForComposition(compositionId);
    }

    private String resolveTemplateId(Composition composition) {
        if (composition.getArchetypeDetails() != null
                && composition.getArchetypeDetails().getTemplateId() != null) {
            return composition.getArchetypeDetails().getTemplateId().getValue();
        }
        throw new IllegalArgumentException("Composition must have a template_id in archetype_details");
    }

    static ObjectVersionId buildObjectVersionId(UUID objectId, int version, String systemId) {
        return new ObjectVersionId(objectId.toString() + "::" + systemId + "::" + version);
    }

    private static UUID extractUuid(ObjectVersionId versionId) {
        String id = versionId.getValue();
        int firstSep = id.indexOf("::");
        return UUID.fromString(firstSep > 0 ? id.substring(0, firstSep) : id);
    }

    private static int extractVersion(ObjectVersionId versionId) {
        String id = versionId.getValue();
        int lastSep = id.lastIndexOf("::");
        return lastSep > 0 ? Integer.parseInt(id.substring(lastSep + 2)) : 1;
    }
}
