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

import java.text.MessageFormat;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.PostConstruct;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.xmlbeans.XmlException;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.exception.StateConflictException;
import org.ehrbase.api.exception.ValidationException;
import org.ehrbase.api.knowledge.TemplateCacheService;
import org.ehrbase.api.knowledge.TemplateMetaData;
import org.ehrbase.api.service.TemplateService;
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
 * Lookup and caching for Web and Operational Templates.<br>
 * A list of existing templates is cached separately from the templates that are being used.<br>
 * TODO CDR-2305 During startup both can be initialized? Or better only WebTemplates?
 *
 * Szenarios:
 * <ol>
 *    <li>WebTemplate needed for writing/validating COMPOSITIONs</li>
 *    <li>OPT handling for external retrieval</li>
 *    <li>Listing available templates: external requests + aql to sql mapping</li>
 *    <li>Translation between template_id and internal id: CRUD</li>
 * </ol>
 */
@Service
// This service is not @Transactional since we only want to get DB connections when we really need to and an already
// running transaction is propagated anyway
public class DefaultTemplateCacheService implements TemplateCacheService {
    /*
     * CDR-2305 template cache landscape
     * - templateId -> uuid [TEMPLATE_ID_UUID_CACHE, sync TEMPLATE_UUID_ID_CACHE]
     * - uuid -> templateId [sync TEMPLATE_ID_UUID_CACHE, TEMPLATE_UUID_ID_CACHE]
     * - templateId -> WebTemplate [TEMPLATE_CACHE, sync TEMPLATE_ID_UUID_CACHE, sync TEMPLATE_UUID_ID_CACHE]
     * - templateId -> OPT [TEMPLATE_OPT_CACHE]
     * - () -> template details [no dedicated cache? sync TEMPLATE_ID_UUID_CACHE, TEMPLATE_UUID_ID_CACHE?]
     * - startup: fill WebTemplate cache [TEMPLATE_CACHE, sync TEMPLATE_ID_UUID_CACHE, sync TEMPLATE_UUID_ID_CACHE]
     * - startup: fill id maps? [TEMPLATE_ID_UUID_CACHE, TEMPLATE_UUID_ID_CACHE]
     */
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final TemplateStorage templateStorage;
    private final CacheProvider cacheProvider;

    @Value("${ehrbase.cache.template-init-on-startup:false}")
    private boolean initTemplateCache;

    public DefaultTemplateCacheService(TemplateStorage templateStorage, CacheProvider cacheProvider) {
        this.templateStorage = templateStorage;
        this.cacheProvider = cacheProvider;
    }

    @PostConstruct
    public void init() {
        if (initTemplateCache) {
            // TODO CDR-2305 initTemplateCache lists templateIds
            String[] templateIds = templateStorage.findAllTemplates().stream()
                    .map(TemplateService.TemplateDetails::templateId)
                    .toArray(String[]::new);
            cacheTemplates(templateIds);
        }
    }

    private static OPERATIONALTEMPLATE buildOperationalTemplate(String content) {
        try {
            return TemplateDocument.Factory.parse(content).getTemplate();
        } catch (XmlException e) {
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
        } catch (IllegalArgumentException _) {
            throw new InvalidApiParameterException("Invalid template input content");
        }

        boolean canOverwrite = templateStorage.allowTemplateOverwrite() || overwrite;

        // pre-check: if already existing throw proper exception
        if (!canOverwrite && findUuidByTemplateId(templateId).isPresent()) {
            throw new StateConflictException(
                    "Operational template with this template ID already exists: " + templateId);
        }

        TemplateMetaData templateMetaData = templateStorage.storeTemplate(template);

        if (canOverwrite) {
            // Caches might be containing wrong data
            invalidateCaches(templateId, templateMetaData.getInternalId());
        }
        log.info("Updating WebTemplate cache for template: {}", templateId);
        addToCache(templateMetaData);

        return templateId;
    }
    // XXX CDR-2305 Why is this not based on OPERATIONALTEMPLATE?
    @Override
    public String adminUpdateOperationalTemplate(String templateId, String content) {
        OPERATIONALTEMPLATE template = buildOperationalTemplate(content);
        String newTemplateId = template.getTemplateId().getValue();
        if (!templateId.equals(newTemplateId)) {
            throw new ValidationException("Inconsistent template_id");
        }
        return addOperationalTemplateIntern(template, true);
    }

    private void addToCache(TemplateMetaData template) {
        OPERATIONALTEMPLATE operationaltemplate =
                TemplateService.buildOperationalTemplate(template.getOperationaltemplate());
        String templateId = TemplateUtils.getTemplateId(operationaltemplate);
        // FIXME CDR-2305 cache must be clear: web or opt
        CacheProvider.TEMPLATE_CACHE.put(
                cacheProvider, templateId, Pair.of(buildWebTemplate(operationaltemplate), template.getCreatedOn()));
        CacheProvider.TEMPLATE_UUID_ID_CACHE.put(cacheProvider, template.getInternalId(), templateId);
        CacheProvider.TEMPLATE_ID_UUID_CACHE.put(cacheProvider, templateId, template.getInternalId());
    }

    private void invalidateCaches(String templateId, UUID internalId) {
        CacheProvider.TEMPLATE_CACHE.evict(cacheProvider, templateId);
        CacheProvider.TEMPLATE_OPT_CACHE.evict(cacheProvider, templateId);
        CacheProvider.TEMPLATE_ID_UUID_CACHE.evict(cacheProvider, templateId);
        CacheProvider.TEMPLATE_UUID_ID_CACHE.evict(cacheProvider, internalId);
    }

    @Override
    public List<TemplateMetaData> cacheTemplates(String... templateIds) {
        List<TemplateMetaData> templateMetaData = templateStorage.readTemplates(templateIds);
        // CDR-2305 FIXME
        log.info("Updating WebTemplate cache for {} templates", templateMetaData.size());
        templateMetaData.forEach(this::addToCache);
        return templateMetaData;
    }

    @Override
    public List<TemplateService.TemplateDetails> findAllTemplates() {
        // TODO CDR-2305 cache
        return templateStorage.findAllTemplates();
    }

    public String retrieveOperationalTemplate(String templateId) {
        log.trace("retrieveOperationalTemplate({})", templateId);
        return CacheProvider.TEMPLATE_OPT_CACHE.get(
                cacheProvider,
                templateId,
                () -> templateStorage
                        .readTemplate(templateId)
                        .map(TemplateMetaData::getOperationaltemplate)
                        .orElse(null));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteOperationalTemplate(UUID uuid) {
        // Remove template from storage
        findTemplateIdByUuid(uuid).ifPresent(templateId -> {
            templateStorage.deleteTemplate(uuid);
            CacheProvider.TEMPLATE_UUID_ID_CACHE.evict(cacheProvider, uuid);
            CacheProvider.TEMPLATE_CACHE.evict(cacheProvider, templateId);
            CacheProvider.TEMPLATE_OPT_CACHE.evict(cacheProvider, templateId);
            CacheProvider.TEMPLATE_ID_UUID_CACHE.evict(cacheProvider, templateId);
        });
    }

    @Override
    public int deleteAllOperationalTemplates() {
        List<TemplateService.TemplateDetails> deletedTemplates = templateStorage.deleteAllTemplates();

        deletedTemplates.forEach(t -> {
            CacheProvider.TEMPLATE_UUID_ID_CACHE.evict(cacheProvider, t.id());
            CacheProvider.TEMPLATE_CACHE.evict(cacheProvider, t.templateId());
            CacheProvider.TEMPLATE_OPT_CACHE.evict(cacheProvider, t.templateId());
            CacheProvider.TEMPLATE_ID_UUID_CACHE.evict(cacheProvider, t.templateId());
        });
        return deletedTemplates.size();
    }

    @Override
    public Optional<String> findTemplateIdByUuid(UUID uuid) {
        try {
            return Optional.of(CacheProvider.TEMPLATE_UUID_ID_CACHE.get(cacheProvider, uuid, () -> {
                String templateId = templateStorage.findTemplateIdByUuid(uuid).orElseThrow();
                // reverse cache
                CacheProvider.TEMPLATE_ID_UUID_CACHE.get(cacheProvider, templateId, () -> uuid);
                return templateId;
            }));
        } catch (Cache.ValueRetrievalException ex) {
            return handleCacheMismatch(ex);
        }
    }

    @Override
    public Optional<UUID> findUuidByTemplateId(String templateId) {
        try {
            return Optional.of(CacheProvider.TEMPLATE_ID_UUID_CACHE.get(cacheProvider, templateId, () -> {
                UUID internalId =
                        templateStorage.findUuidByTemplateId(templateId).orElseThrow();
                // reverse cache
                CacheProvider.TEMPLATE_UUID_ID_CACHE.get(cacheProvider, internalId, () -> templateId);
                return internalId;
            }));
        } catch (Cache.ValueRetrievalException ex) {
            return handleCacheMismatch(ex);
        }
    }

    public WebTemplate getInternalTemplate(String templateId) {
        try {
            return CacheProvider.TEMPLATE_CACHE
                    .get(cacheProvider, templateId, () -> {
                        log.info("Updating WebTemplate cache for template: {}", templateId);
                        return templateStorage
                                .readTemplate(templateId)
                                .map(d -> Pair.of(
                                        buildWebTemplate(
                                                TemplateService.buildOperationalTemplate(d.getOperationaltemplate())),
                                        d.getCreatedOn()))
                                .orElseThrow(() -> new IllegalArgumentException(
                                        "Could not retrieve template for template Id: " + templateId));
                    })
                    .getLeft();
        } catch (Cache.ValueRetrievalException ex) {
            throw (RuntimeException) ex.getCause();
        }
    }

    private WebTemplate buildWebTemplate(OPERATIONALTEMPLATE operationaltemplate) {
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

    private static <T> Optional<T> handleCacheMismatch(Cache.ValueRetrievalException ex) {
        Throwable cause = ex.getCause();
        return switch (cause) {
            // No template with that UUID exists
            case NoSuchElementException _ -> Optional.empty();
            case RuntimeException re -> throw re;
            default -> throw ex;
        };
    }
}
