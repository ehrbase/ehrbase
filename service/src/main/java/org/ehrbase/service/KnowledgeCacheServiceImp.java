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

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.PostConstruct;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.xmlbeans.XmlException;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.exception.StateConflictException;
import org.ehrbase.api.knowledge.KnowledgeCacheService;
import org.ehrbase.api.knowledge.TemplateMetaData;
import org.ehrbase.cache.CacheProvider;
import org.ehrbase.openehr.sdk.webtemplate.model.WebTemplate;
import org.ehrbase.openehr.sdk.webtemplate.parser.OPTParser;
import org.ehrbase.util.TemplateUtils;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;
import org.openehr.schemas.v1.TemplateDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.stereotype.Service;

/**
 * Lookup and caching for Web and Operational Templates
 */
@Service
// This service is not @Transactional since we only want to get DB connections when we really need to and an already
// running transaction is propagated anyway
public class KnowledgeCacheServiceImp implements KnowledgeCacheService, IntrospectService {

    public static final String ELEMENT = "ELEMENT";
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final TemplateStorage templateStorage;
    private final CacheProvider cacheProvider;

    @Value("${system.allow-template-overwrite:false}")
    private boolean allowTemplateOverwrite;

    @Value("${ehrbase.cache.template-init-on-startup:false}")
    private boolean initTemplateCache;

    public KnowledgeCacheServiceImp(TemplateStorage templateStorage, CacheProvider cacheProvider) {
        this.templateStorage = templateStorage;
        this.cacheProvider = cacheProvider;
    }

    @PostConstruct
    public void init() {
        if (initTemplateCache) {
            listAllOperationalTemplates();
        }
    }

    private static OPERATIONALTEMPLATE buildOperationalTemplate(InputStream content) {
        try {
            TemplateDocument document = TemplateDocument.Factory.parse(content);
            return document.getTemplate();
        } catch (XmlException | IOException e) {
            throw new InvalidApiParameterException(e.getMessage());
        }
    }

    @Override
    public String addOperationalTemplate(OPERATIONALTEMPLATE template) {
        return addOperationalTemplateIntern(template, false);
    }

    private String addOperationalTemplateIntern(OPERATIONALTEMPLATE template, boolean overwrite) {
        validateTemplate(template);

        String templateId;
        try {
            templateId = TemplateUtils.getTemplateId(template);
        } catch (IllegalArgumentException a) {
            throw new InvalidApiParameterException("Invalid template input content");
        }

        // pre-check: if already existing throw proper exception
        if (!allowTemplateOverwrite
                && !overwrite
                && retrieveOperationalTemplate(templateId).isPresent()) {
            throw new StateConflictException(
                    "Operational template with this template ID already exists: " + templateId);
        }

        TemplateMetaData templateMetaData = templateStorage.storeTemplate(template);

        if (allowTemplateOverwrite || overwrite) {
            // Caches might be containing wrong data
            invalidateCaches(templateId, templateMetaData.getInternalId());
        }
        log.info("Updating WebTemplate cache for template: {}", templateId);
        ensureCached(templateMetaData);

        return templateId;
    }

    @Override
    public String adminUpdateOperationalTemplate(InputStream content) {
        OPERATIONALTEMPLATE template = buildOperationalTemplate(content);
        return addOperationalTemplateIntern(template, true);
    }

    private void ensureCached(TemplateMetaData template) {
        cacheProvider.get(CacheProvider.TEMPLATE_UUID_ID_CACHE, template.getInternalId(), () -> {
            String templateId = TemplateUtils.getTemplateId(template.getOperationaltemplate());
            cacheProvider.get(CacheProvider.TEMPLATE_ID_UUID_CACHE, templateId, template::getInternalId);
            cacheProvider.get(
                    CacheProvider.INTROSPECT_CACHE,
                    templateId,
                    () -> buildQueryOptMetaData(template.getOperationaltemplate()));
            return templateId;
        });
    }

    private void invalidateCaches(String templateId, UUID internalId) {
        cacheProvider.evict(CacheProvider.INTROSPECT_CACHE, templateId);
        cacheProvider.evict(CacheProvider.TEMPLATE_ID_UUID_CACHE, templateId);
        cacheProvider.evict(CacheProvider.TEMPLATE_UUID_ID_CACHE, internalId);
    }

    @Override
    public List<TemplateMetaData> listAllOperationalTemplates() {
        List<TemplateMetaData> templateMetaData = templateStorage.listAllOperationalTemplates();
        log.info("Updating WebTemplate cache for all {} templates", templateMetaData.size());
        templateMetaData.forEach(this::ensureCached);
        return templateMetaData;
    }

    @Override
    public Map<UUID, String> findAllTemplateIds() {
        return templateStorage.findAllTemplateIds();
    }

    @Override
    public Optional<OPERATIONALTEMPLATE> retrieveOperationalTemplate(String key) {
        log.debug("retrieveOperationalTemplate({})", key);
        return templateStorage.readTemplate(key).map(TemplateMetaData::getOperationaltemplate);
    }

    @Override
    public Optional<OPERATIONALTEMPLATE> retrieveOperationalTemplate(UUID uuid) {
        return findTemplateIdByUuid(uuid).flatMap(this::retrieveOperationalTemplate);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteOperationalTemplate(OPERATIONALTEMPLATE template) {
        // Remove template from storage
        String templateId = TemplateUtils.getTemplateId(template);
        Optional<UUID> internalId = findUuidByTemplateId(templateId);
        templateStorage.deleteTemplate(templateId);

        cacheProvider.evict(CacheProvider.INTROSPECT_CACHE, templateId);
        cacheProvider.evict(CacheProvider.TEMPLATE_ID_UUID_CACHE, templateId);

        internalId.ifPresent(iid -> cacheProvider.evict(CacheProvider.TEMPLATE_UUID_ID_CACHE, iid));
    }

    @Override
    public int deleteAllOperationalTemplates() {
        List<Pair<UUID, String>> deletedTemplates = templateStorage.deleteAllTemplates();

        deletedTemplates.forEach(t -> {
            cacheProvider.evict(CacheProvider.TEMPLATE_UUID_ID_CACHE, t.getKey());
            cacheProvider.evict(CacheProvider.INTROSPECT_CACHE, t.getValue());
            cacheProvider.evict(CacheProvider.TEMPLATE_ID_UUID_CACHE, t.getValue());
        });
        return deletedTemplates.size();
    }

    @Override
    public Optional<String> findTemplateIdByUuid(UUID uuid) {
        try {
            return Optional.of(cacheProvider.get(CacheProvider.TEMPLATE_UUID_ID_CACHE, uuid, () -> {
                String templateId = templateStorage.findTemplateIdByUuid(uuid).orElseThrow();
                // reverse cache
                cacheProvider.get(CacheProvider.TEMPLATE_ID_UUID_CACHE, templateId, () -> uuid);
                return templateId;
            }));
        } catch (Cache.ValueRetrievalException ex) {
            if (ex.getCause() instanceof NoSuchElementException) {
                // No template with that UUID exist
                return Optional.empty();
            } else {
                throw ex;
            }
        }
    }

    @Override
    public Optional<UUID> findUuidByTemplateId(String templateId) {
        try {
            return Optional.of(cacheProvider.get(CacheProvider.TEMPLATE_ID_UUID_CACHE, templateId, () -> {
                UUID internalId =
                        templateStorage.findUuidByTemplateId(templateId).orElseThrow();
                // reverse cache
                cacheProvider.get(CacheProvider.TEMPLATE_UUID_ID_CACHE, internalId, () -> templateId);
                return internalId;
            }));
        } catch (Cache.ValueRetrievalException ex) {
            // No template with that templateId exist
            return Optional.empty();
        }
    }

    public WebTemplate getWebTemplate(String templateId) {
        try {
            return cacheProvider.get(CacheProvider.INTROSPECT_CACHE, templateId, () -> {
                log.info("Updating WebTemplate cache for template: {}", templateId);
                return retrieveWebTemplate(templateId);
            });
        } catch (Cache.ValueRetrievalException ex) {
            throw (RuntimeException) ex.getCause();
        }
    }

    private WebTemplate retrieveWebTemplate(String templateId) {

        return retrieveOperationalTemplate(templateId)
                .map(this::buildQueryOptMetaData)
                .orElseThrow(() ->
                        new IllegalArgumentException("Could not retrieve template for template Id: " + templateId));
    }

    private WebTemplate buildQueryOptMetaData(OPERATIONALTEMPLATE operationaltemplate) {
        try {
            return new OPTParser(operationaltemplate).parse();
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(String.format("Invalid template: %s", e.getMessage()));
        }
    }

    /**
     * Validates that the given template is valid and supported by EHRbase.
     *
     * @param template the template to validate
     */
    private void validateTemplate(OPERATIONALTEMPLATE template) {
        if (template == null) {
            throw new InvalidApiParameterException("Could not parse input template");
        }

        if (template.getConcept() == null || template.getConcept().isEmpty()) {
            throw new IllegalArgumentException("Supplied template has nil or empty concept");
        }

        if (template.getLanguage() == null || template.getLanguage().isNil()) {
            throw new IllegalArgumentException("Supplied template has nil or empty language");
        }

        if (template.getDefinition() == null || template.getDefinition().isNil()) {
            throw new IllegalArgumentException("Supplied template has nil or empty definition");
        }

        if (template.getDescription() == null || !template.getDescription().validate()) {
            throw new IllegalArgumentException("Supplied template has nil or empty description");
        }

        if (!TemplateUtils.isSupported(template)) {
            throw new IllegalArgumentException(MessageFormat.format(
                    "The supplied template is not supported (unsupported types: {0})",
                    String.join(",", TemplateUtils.UNSUPPORTED_RM_TYPES)));
        }
    }
}
