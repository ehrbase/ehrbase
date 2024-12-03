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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.tuple.Pair;
import org.ehrbase.api.exception.UnprocessableEntityException;
import org.ehrbase.api.knowledge.TemplateMetaData;
import org.ehrbase.repository.CompositionRepository;
import org.ehrbase.repository.TemplateStoreRepository;
import org.ehrbase.util.TemplateUtils;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TemplateDBStorageService implements TemplateStorage {

    private static final String PROP_ALLOW_TEMPLATE_OVERWRITE = "ehrbase.template.allow-overwrite";

    private static final Logger log = LoggerFactory.getLogger(TemplateDBStorageService.class);

    private final CompositionRepository compositionRepository;

    private final TemplateStoreRepository templateStoreRepository;

    private final boolean allowTemplateOverwrite;

    public TemplateDBStorageService(
            @Lazy CompositionRepository compositionRepository,
            TemplateStoreRepository templateStoreRepository,
            @Value("${" + PROP_ALLOW_TEMPLATE_OVERWRITE + ":false}") boolean allowTemplateOverwrite) {

        this.compositionRepository = compositionRepository;
        this.templateStoreRepository = templateStoreRepository;
        this.allowTemplateOverwrite = allowTemplateOverwrite;

        if (allowTemplateOverwrite) {
            log.warn(
                    "Template overwriting is enabled, this is not recommended for production use and can lead to unexpected behavior, consider disabling {}",
                    PROP_ALLOW_TEMPLATE_OVERWRITE);
        }
    }

    @Override
    public boolean allowTemplateOverwrite() {
        return allowTemplateOverwrite;
    }

    @Override
    public List<TemplateMetaData> listAllOperationalTemplates() {
        return templateStoreRepository.findAll();
    }

    @Override
    public Map<UUID, String> findAllTemplateIds() {
        return templateStoreRepository.findAllTemplateIds();
    }

    @Override
    public TemplateMetaData storeTemplate(OPERATIONALTEMPLATE template) {
        String templateId = TemplateUtils.getTemplateId(template);
        if (findUuidByTemplateId(templateId).isEmpty()) {
            return templateStoreRepository.store(template);
        } else {
            boolean templateUsed = compositionRepository.isTemplateUsed(templateId);
            if (templateUsed) {
                if (allowTemplateOverwrite) {
                    log.warn(
                            "Updating template {} that is in use by at least one composition because {} is enabled",
                            templateId,
                            PROP_ALLOW_TEMPLATE_OVERWRITE);
                } else {
                    // There are compositions using this template -> Return list of uuids
                    throw new UnprocessableEntityException(
                            "Cannot update template %s since it is used by at least one composition"
                                    .formatted(templateId));
                }
            }
            return templateStoreRepository.update(template);
        }
    }

    @Override
    public Optional<TemplateMetaData> readTemplate(String templateId) {
        return templateStoreRepository.findByTemplateId(templateId);
    }

    private void checkUsages() {
        List<String> usedTemplateIds = templateStoreRepository.getTemplateUsages();
        if (!usedTemplateIds.isEmpty()) {
            boolean single = usedTemplateIds.size() == 1;
            throw new UnprocessableEntityException("Cannot delete %s %s since %s used by at least one composition"
                    .formatted(
                            single ? "template" : "templates",
                            String.join(", ", usedTemplateIds),
                            single ? "it is" : "they are"));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteTemplate(String templateId) {

        if (compositionRepository.isTemplateUsed(templateId)) {
            // There are compositions using this template -> Return list of uuids
            throw new UnprocessableEntityException(
                    "Cannot delete template %s since it is used by at least one composition".formatted(templateId));
        }
        templateStoreRepository.delete(templateId);
    }

    @Override
    public List<Pair<UUID, String>> deleteAllTemplates() {
        checkUsages();

        return templateStoreRepository.findAll().stream()
                .map(t -> {
                    String templateId = TemplateUtils.getTemplateId(t.getOperationaltemplate());
                    templateStoreRepository.delete(templateId);
                    return Pair.of(t.getInternalId(), templateId);
                })
                .toList();
    }

    @Override
    public Optional<String> findTemplateIdByUuid(UUID uuid) {
        return templateStoreRepository.findTemplateIdByUuid(uuid);
    }

    @Override
    public Optional<UUID> findUuidByTemplateId(String templateId) {
        return templateStoreRepository.findUuidByTemplateId(templateId);
    }
}
