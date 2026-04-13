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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.xmlbeans.XmlException;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.exception.StateConflictException;
import org.ehrbase.api.exception.UnprocessableEntityException;
import org.ehrbase.api.knowledge.TemplateCacheService;
import org.ehrbase.api.knowledge.TemplateMetaData;
import org.ehrbase.api.service.TemplateService;
import org.ehrbase.cache.CacheProvider;
import org.ehrbase.openehr.sdk.webtemplate.model.WebTemplate;
import org.ehrbase.openehr.sdk.webtemplate.parser.OPTParser;
import org.ehrbase.repository.TemplateStoreRepository;
import org.ehrbase.util.TemplateUtils;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lookup and caching for Web and Operational Templates.<br>
 * A list of existing templates is cached separately from the templates that are being used.<br>
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
// There are few instances where a transaction is needed.
// Also, there are few instances where a transaction is not available.
public class DefaultTemplateCacheService implements TemplateCacheService {
    /*
     * CDR-2305 template cache landscape
     * - templateId -> uuid [TEMPLATE_ID_UUID_CACHE, sync TEMPLATE_UUID_ID_CACHE]
     * - uuid -> templateId [sync TEMPLATE_ID_UUID_CACHE, TEMPLATE_UUID_ID_CACHE]
     * - templateId -> WebTemplate [TEMPLATE_CACHE, sync TEMPLATE_ID_UUID_CACHE, sync TEMPLATE_UUID_ID_CACHE]
     * - templateId -> OPT [TEMPLATE_OPT_CACHE]
     * - () -> template details [no dedicated cache?? sync TEMPLATE_ID_UUID_CACHE?, sync TEMPLATE_UUID_ID_CACHE?]
     * - startup: fill WebTemplate cache [TEMPLATE_CACHE, sync TEMPLATE_ID_UUID_CACHE, sync TEMPLATE_UUID_ID_CACHE]
     * - startup: fill id maps? [TEMPLATE_ID_UUID_CACHE, TEMPLATE_UUID_ID_CACHE]
     */

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final TemplateStoreRepository templateStoreRepository;

    private final CacheHelper cacheHelper;

    @Value("${ehrbase.cache.template-init-on-startup:false}")
    private boolean initTemplateCache;

    public DefaultTemplateCacheService(TemplateStoreRepository templateStoreRepository, CacheProvider cacheProvider) {
        this.templateStoreRepository = templateStoreRepository;
        this.cacheHelper = new CacheHelper(cacheProvider);
    }

    @PostConstruct
    public void init() {
        if (initTemplateCache) {
            // TODO CDR-2305 customizable preload strategy; fill templateId/uuid caches

            // TODO CDR-2305 initTemplateCache lists templateIds
            String[] templateIds = templateStoreRepository.findAllTemplates().stream()
                    .map(TemplateService.TemplateDetails::templateId)
                    .toArray(String[]::new);
            List<TemplateMetaData> templateMetaData = templateStoreRepository.findByTemplateIds(templateIds);
            log.info("Preparing WebTemplate cache for {} templates", templateMetaData.size());
            templateMetaData.forEach(this::addWebTemplateToCache);
        }
    }

    @Override
    @Transactional
    public String addOperationalTemplate(
            TemplateMetaData templateData, boolean allowTemplateOverwrite, boolean allowUsedTemplateOverwrite) {

        // pre-check: if already existing throw proper exception
        String templateId = templateData.meta().templateId();
        Optional<UUID> existingTid = findUuidByTemplateId(templateId);

        TemplateMetaData templateMetaData;
        boolean mustUpdate = existingTid.isPresent();
        if (mustUpdate) {
            if (!allowTemplateOverwrite) {
                throw new StateConflictException(
                        "Operational template with this template ID already exists: " + templateId);
            }
            if (templateStoreRepository.isTemplateUsed(existingTid.get())) {
                if (allowUsedTemplateOverwrite) {
                    log.warn(
                            "Updating template {} that is in use by at least one composition because {} is enabled",
                            templateData.meta().templateId(),
                            TemplateService.PROP_ALLOW_TEMPLATE_OVERWRITE);
                } else {
                    throw new UnprocessableEntityException(
                            "Cannot update template %s since it is used by at least one composition"
                                    .formatted(templateId));
                }
            }
            templateMetaData = templateStoreRepository.update(templateData);
            cacheHelper.invalidateCaches(templateId, templateMetaData.meta().id());
        } else {
            templateMetaData = templateStoreRepository.update(templateData);
        }

        log.debug("Updating WebTemplate cache for template: {}", templateId);
        addWebTemplateToCache(templateMetaData);

        return templateId;
    }

    private void addWebTemplateToCache(TemplateMetaData template) {
        OPERATIONALTEMPLATE operationaltemplate;
        try {
            operationaltemplate = TemplateService.buildOperationalTemplate(template.operationalTemplate());
        } catch (XmlException e) {
            throw new InternalServerException(e.getMessage(), e);
        }
        String templateId = TemplateUtils.getTemplateId(operationaltemplate);
        WebTemplate tpl = buildWebTemplate(operationaltemplate);

        cacheHelper.addToCache(
                template.meta().id(), templateId, tpl, template.meta().creationTime());
    }

    @Override
    public List<TemplateService.TemplateDetails> findAllTemplates() {
        // TODO CDR-2305 cache? For AQL absolutely; for REST unclear
        return templateStoreRepository.findAllTemplates();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void deleteOperationalTemplate(String templateId) {
        UUID templateUuid = findUuidByTemplateId(templateId)
                .orElseThrow(() -> new ObjectNotFoundException(
                        "ADMIN TEMPLATE", String.format("Operational template with id %s not found.", templateId)));
        templateStoreRepository.deleteTemplate(templateUuid);
        cacheHelper.invalidateCaches(templateId, templateUuid);
    }

    @Override
    @Transactional
    public int deleteAllOperationalTemplates() {
        int deleted = templateStoreRepository.deleteAllTemplates();
        cacheHelper.clearCaches();
        return deleted;
    }

    @Override
    public Optional<String> findTemplateIdByUuid(UUID uuid) {
        return Optional.ofNullable(cacheHelper.findTemplateIdByUuid(
                uuid, u -> templateStoreRepository.findTemplateIdByUuid(u).orElseThrow()));
    }

    @Override
    public Optional<UUID> findUuidByTemplateId(String templateId) {
        return Optional.ofNullable(cacheHelper.findUuidByTemplateId(
                templateId,
                tid -> templateStoreRepository.findUuidByTemplateId(tid).orElseThrow()));
    }

    public String retrieveOperationalTemplate(String templateId) {
        log.trace("retrieveOperationalTemplate({})", templateId);
        try {
            return cacheHelper.retrieveOperationalTemplate(
                    templateId,
                    tid -> templateStoreRepository.findByTemplateIds(tid).stream()
                            .findFirst()
                            .map(TemplateMetaData::operationalTemplate)
                            .orElseThrow(() -> new ObjectNotFoundException(
                                    "template", "Template with the specified id does not exist")));
        } catch (Cache.ValueRetrievalException ex) {
            // unwrap exception
            throw (RuntimeException) ex.getCause();
        }
    }

    /**
     *
     * @param templateId
     * @return
     * @throws ObjectNotFoundException if the template is missing
     * @throws InternalServerException if the OPT cannot be parsed
     */
    public WebTemplate getInternalTemplate(String templateId) {
        try {
            return cacheHelper
                    .getInternalTemplate(templateId, tid -> {
                        log.info("Updating WebTemplate cache for template: {}", tid);
                        return templateStoreRepository.findByTemplateIds(tid).stream()
                                .findFirst()
                                .map(d -> {
                                    OPERATIONALTEMPLATE operationaltemplate;
                                    try {
                                        operationaltemplate =
                                                TemplateService.buildOperationalTemplate(d.operationalTemplate());
                                    } catch (XmlException e) {
                                        throw new InternalServerException(
                                                "Cannot process template: " + e.getMessage(), e);
                                    }
                                    return Pair.of(
                                            buildWebTemplate(operationaltemplate),
                                            d.meta().creationTime());
                                })
                                .orElseThrow(() -> new ObjectNotFoundException(
                                        "template", "Template with the specified id does not exist"));
                    })
                    .getLeft();
        } catch (Cache.ValueRetrievalException ex) {
            // unwrap exception
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

    static final class CacheHelper {
        private final CacheProvider cacheProvider;

        public CacheHelper(CacheProvider cacheProvider) {
            this.cacheProvider = cacheProvider;
        }

        public void addToCache(UUID internalId, String templateId, WebTemplate tpl, OffsetDateTime createdOn) {
            CacheProvider.TEMPLATE_CACHE.put(cacheProvider, templateId, Pair.of(tpl, createdOn));
            CacheProvider.TEMPLATE_UUID_ID_CACHE.put(cacheProvider, internalId, templateId);
            CacheProvider.TEMPLATE_ID_UUID_CACHE.put(cacheProvider, templateId, internalId);
        }

        public void invalidateCaches(String templateId, UUID internalId) {
            CacheProvider.TEMPLATE_CACHE.evict(cacheProvider, templateId);
            CacheProvider.TEMPLATE_OPT_CACHE.evict(cacheProvider, templateId);
            CacheProvider.TEMPLATE_ID_UUID_CACHE.evict(cacheProvider, templateId);
            CacheProvider.TEMPLATE_UUID_ID_CACHE.evict(cacheProvider, internalId);
        }

        public void clearCaches() {
            Stream.of(
                            CacheProvider.TEMPLATE_CACHE,
                            CacheProvider.TEMPLATE_OPT_CACHE,
                            CacheProvider.TEMPLATE_ID_UUID_CACHE,
                            CacheProvider.TEMPLATE_UUID_ID_CACHE)
                    .forEach(c -> c.clear(cacheProvider));
        }

        public String retrieveOperationalTemplate(String templateId, Function<String, String> loader) {
            return find(CacheProvider.TEMPLATE_OPT_CACHE, templateId, loader);
        }

        private static <T> T handleCacheMismatch(Cache.ValueRetrievalException ex) {
            Throwable cause = ex.getCause();
            return switch (cause) {
                // No template with that UUID exists
                case NoSuchElementException _ -> null;
                case RuntimeException re -> throw re;
                default -> throw ex;
            };
        }

        public String findTemplateIdByUuid(UUID uuid, Function<UUID, String> loader) {
            try {
                return find(CacheProvider.TEMPLATE_UUID_ID_CACHE, uuid, u -> {
                    String tid = loader.apply(u);
                    // reverse cache
                    CacheProvider.TEMPLATE_ID_UUID_CACHE.put(cacheProvider, tid, u);
                    return tid;
                });
            } catch (Cache.ValueRetrievalException ex) {
                return handleCacheMismatch(ex);
            }
        }

        public UUID findUuidByTemplateId(String templateId, Function<String, UUID> loader) {
            try {
                return find(CacheProvider.TEMPLATE_ID_UUID_CACHE, templateId, tid -> {
                    UUID u = loader.apply(tid);
                    // reverse cache
                    CacheProvider.TEMPLATE_UUID_ID_CACHE.put(cacheProvider, u, tid);
                    return u;
                });
            } catch (Cache.ValueRetrievalException ex) {
                return handleCacheMismatch(ex);
            }
        }

        public Pair<WebTemplate, OffsetDateTime> getInternalTemplate(
                String templateId, Function<String, Pair<WebTemplate, OffsetDateTime>> loader) {
            return find(CacheProvider.TEMPLATE_CACHE, templateId, loader);
        }

        private <K, V> V find(CacheProvider.EhrBaseCache<K, V> cache, K key, Function<K, V> loader) {
            return cache.get(cacheProvider, key, () -> loader.apply(key));
        }
    }
}
