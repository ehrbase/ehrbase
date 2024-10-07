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
import java.util.Optional;
import java.util.UUID;
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

    public KnowledgeCacheServiceImp(TemplateStorage templateStorage, CacheProvider cacheProvider) {

        this.templateStorage = templateStorage;
        this.cacheProvider = cacheProvider;
    }

    @Override
    public String addOperationalTemplate(InputStream inputStream) {
        OPERATIONALTEMPLATE template = buildOperationalTemplate(inputStream);
        return addOperationalTemplateIntern(template, false);
    }

    private OPERATIONALTEMPLATE buildOperationalTemplate(InputStream content) {
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

        templateStorage.storeTemplate(template);
        if (allowTemplateOverwrite && !overwrite) {
            // Caches might not be containing wrong data
            invalidateCache(template);
        }

        return templateId;
    }

    public String adminUpdateOperationalTemplate(InputStream content) {
        OPERATIONALTEMPLATE template = buildOperationalTemplate(content);
        return addOperationalTemplateIntern(template, true);
    }

    // invalidates some derived caches like the queryOptMetaDataCache which depend on the template
    private void invalidateCache(OPERATIONALTEMPLATE template) {

        String templateId = template.getTemplateId().getValue();
        CacheProvider.INTROSPECT_CACHE.evict(cacheProvider, templateId);
        Optional.of(templateId)
                .map(t -> CacheProvider.TEMPLATE_ID_UUID_CACHE.get(cacheProvider, t, () -> null))
                .ifPresent(uuid -> CacheProvider.TEMPLATE_UUID_ID_CACHE.evict(cacheProvider, uuid));
        CacheProvider.TEMPLATE_ID_UUID_CACHE.evict(cacheProvider, templateId);
    }

    @Override
    public List<TemplateMetaData> listAllOperationalTemplates() {
        return templateStorage.listAllOperationalTemplates();
    }

    @Override
    public Optional<OPERATIONALTEMPLATE> retrieveOperationalTemplate(String key) {
        log.debug("retrieveOperationalTemplate({})", key);
        return templateStorage.readOperationaltemplate(key);
    }

    @Override
    public Optional<OPERATIONALTEMPLATE> retrieveOperationalTemplate(UUID uuid) {
        return findTemplateIdByUuid(uuid).flatMap(this::retrieveOperationalTemplate);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean deleteOperationalTemplate(OPERATIONALTEMPLATE template) {
        // Remove template from storage
        boolean deleted =
                this.templateStorage.deleteTemplate(template.getTemplateId().getValue());

        if (deleted) {
            // Remove template from caches
            invalidateCache(template);
        }

        return deleted;
    }

    @Override
    public Optional<String> findTemplateIdByUuid(UUID uuid) {
        try {

            return Optional.of(CacheProvider.TEMPLATE_UUID_ID_CACHE.get(cacheProvider, uuid, () -> templateStorage
                    .findTemplateIdByUuid(uuid)
                    .orElseThrow()));
        } catch (Cache.ValueRetrievalException ex) {
            // No template with that UUID exist
            return Optional.empty();
        }
    }

    @Override
    public Optional<UUID> findUuidByTemplateId(String templateId) {
        try {

            return Optional.of(CacheProvider.TEMPLATE_ID_UUID_CACHE.get(cacheProvider, templateId, () -> templateStorage
                    .findUuidByTemplateId(templateId)
                    .orElseThrow()));
        } catch (Cache.ValueRetrievalException ex) {
            // No template with that templateId exist
            return Optional.empty();
        }
    }

    @Override
    public WebTemplate getQueryOptMetaData(String templateId) {

        try {

            return CacheProvider.INTROSPECT_CACHE.get(
                    cacheProvider, templateId, () -> buildQueryOptMetaData(templateId));
        } catch (Cache.ValueRetrievalException ex) {
            throw (RuntimeException) ex.getCause();
        }
    }

    private WebTemplate buildQueryOptMetaData(String templateId) {

        return retrieveOperationalTemplate(templateId)
                .map(this::buildQueryOptMetaData)
                .orElseThrow(() ->
                        new IllegalArgumentException("Could not retrieve  template for  template Id:" + templateId));
    }

    private WebTemplate buildQueryOptMetaData(OPERATIONALTEMPLATE operationaltemplate) {
        log.info("Updating WebTemplate cache for template: {}", TemplateUtils.getTemplateId(operationaltemplate));
        try {
            return new OPTParser(operationaltemplate).parse();
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("Invalid template: %s", e.getMessage()));
        }
    }

    public int deleteAllOperationalTemplates() {
        // Get all operational templates
        List<TemplateMetaData> templateList = this.templateStorage.listAllOperationalTemplates();
        // If list is empty no deletion required
        if (templateList.isEmpty()) {
            return 0;
        }
        int deleted = 0;
        for (TemplateMetaData metaData : templateList) {
            if (deleteOperationalTemplate(metaData.getOperationaltemplate())) {
                deleted++;
            }
        }

        return deleted;
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
