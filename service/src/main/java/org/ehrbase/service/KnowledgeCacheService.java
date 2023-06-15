/*
 * Copyright (c) 2019 vitasystems GmbH and Hannover Medical School.
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

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.xmlbeans.XmlException;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.exception.StateConflictException;
import org.ehrbase.api.service.TenantService;
import org.ehrbase.api.tenant.Tenant;
import org.ehrbase.aql.containment.JsonPathQueryResult;
import org.ehrbase.aql.containment.TemplateIdAqlTuple;
import org.ehrbase.aql.sql.queryimpl.ItemInfo;
import org.ehrbase.cache.CacheOptions;
import org.ehrbase.ehr.knowledge.I_KnowledgeCache;
import org.ehrbase.ehr.knowledge.TemplateMetaData;
import org.ehrbase.openehr.sdk.webtemplate.model.WebTemplate;
import org.ehrbase.openehr.sdk.webtemplate.model.WebTemplateNode;
import org.ehrbase.openehr.sdk.webtemplate.parser.NodeId;
import org.ehrbase.openehr.sdk.webtemplate.parser.OPTParser;
import org.ehrbase.tenant.DefaultTenantAuthentication;
import org.ehrbase.util.TemplateUtils;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;
import org.openehr.schemas.v1.TemplateDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Look up and caching for archetypes, openEHR showTemplates and Operational Templates. Search in
 * path defined as
 *
 * <ul>
 *   <li>1. System environment ETHERCIS_ARCHETYPE_DIR, ETHERCIS_TEMPLATE_DIR, ETHERCIS_OPT_DIR
 *   <li>2. Application path %USER_HOME%/.ethercis/archetype, %USER_HOME%/.ethercis/template,
 *       %USER_HOME%/.ethercis/opt
 *   <li>3. User can also include a source directory by invoking addXYZPath method
 * </ul>
 *
 * <p>The resources extensions are defined by the following default:
 *
 * <ul>
 *   <li>ADL: archetype
 *   <li>OET: openehr template
 *   <li>OPT: operational template
 * </ul>
 *
 * @author C. Chevalley
 */
@Service
// This service is not @Transactional since we only want to get DB connections when we really need to and an already
// running transaction is propagated anyway
public class KnowledgeCacheService implements I_KnowledgeCache, IntrospectService {

    public static final String ELEMENT = "ELEMENT";
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final TemplateStorage templateStorage;
    private final CacheOptions cacheOptions;

    private final Cache jsonPathQueryResultCache;
    private final Cache webTemplateCache;
    private final Cache fieldCache;
    private final Cache multivaluedCache;
    private final TenantService tenantService;

    // index val to val
    private final Map<CacheKey<UUID>, String> idxCacheUuidToTemplateId = new ConcurrentHashMap<>();
    // index val to val
    private final Map<String, CacheKey<UUID>> idxCacheTemplateIdToUuid = new ConcurrentHashMap<>();
    private final Cache /*<UUID, ConceptValue>*/ conceptById;
    private final Cache /*<Pair<Integer, String>, ConceptValue>*/ conceptByConceptId;
    private final Cache /*<Pair<String, String>, ConceptValue>*/ conceptByDescription;
    private final Cache /*<String, TerritoryValue>*/ territoryCache;
    private final Cache /*<String, LanguageValue>*/ languageCache;

    @Value("${system.allow-template-overwrite:false}")
    private boolean allowTemplateOverwrite;

    public KnowledgeCacheService(
            TemplateStorage templateStorage,
            CacheManager cacheManager,
            CacheOptions cacheOptions,
            TenantService tenantService) {

        this.templateStorage = templateStorage;
        this.cacheOptions = cacheOptions;
        this.tenantService = tenantService;

        webTemplateCache = cacheManager.getCache(CacheOptions.INTROSPECT_CACHE);
        jsonPathQueryResultCache = cacheManager.getCache(CacheOptions.QUERY_CACHE);
        fieldCache = cacheManager.getCache(CacheOptions.FIELDS_CACHE);
        multivaluedCache = cacheManager.getCache(CacheOptions.MULTI_VALUE_CACHE);
        conceptById = cacheManager.getCache(CacheOptions.CONCEPT_CACHE_ID);
        conceptByConceptId = cacheManager.getCache(CacheOptions.CONCEPT_CACHE_CONCEPT_ID);
        conceptByDescription = cacheManager.getCache(CacheOptions.CONCEPT_CACHE_DESCRIPTION);

        territoryCache = cacheManager.getCache(CacheOptions.TERRITORY_CACHE);
        languageCache = cacheManager.getCache(CacheOptions.LANGUAGE_CACHE);
    }

    @PostConstruct
    void init() throws InterruptedException {

        initializeCaches(cacheOptions.isPreInitialize());
    }

    // fetch all tenants and initialize the caches for each tenant seperatly
    private static final int NUM_OF_PROC = Runtime.getRuntime().availableProcessors();
    private static final ExecutorService execService = Executors.newFixedThreadPool(NUM_OF_PROC);

    private abstract static class SecCtxAwareRunnable implements Runnable {
        private Authentication auth;

        private SecCtxAwareRunnable(Authentication auth) {
            this.auth = auth;
        }

        public void run() {
            SecurityContextHolder.getContext().setAuthentication(auth);
            doRun();
        }

        abstract void doRun();
    }

    private static final String ERR_CACHE_ERROR = "An error occurred while caching template: {}";
    private static final String ERR_GEN_ERROR = "An error occurred while calculating queries for template: {}";

    private Future<?> initCachePerTenant(String tenantId) {
        SecCtxAwareRunnable runMe = new SecCtxAwareRunnable(DefaultTenantAuthentication.of(tenantId)) {
            void doRun() {
                Set<String> templateIds = new HashSet<>();

                listAllOperationalTemplates().forEach(metadata -> {
                    var template = metadata.getOperationaltemplate();
                    var templateId = TemplateUtils.getTemplateId(template);
                    templateIds.add(templateId);
                    try {
                        putIntoCache(template, tenantService.getCurrentSysTenant());
                    } catch (RuntimeException e) {
                        log.error(ERR_CACHE_ERROR, templateId, e);
                    }
                });

                templateIds.forEach(templateId -> {
                    try {
                        preBuildQueries(templateId, cacheOptions.isPreBuildQueries());
                    } catch (RuntimeException e) {
                        log.error(ERR_GEN_ERROR, templateId, e);
                    }
                });
            }
        };

        return execService.submit(runMe);
    }

    private void initializeCaches(boolean init) throws InterruptedException {
        if (!init) return;

        List<Tenant> tenants = tenantService.getAll();
        List<Future<?>> collect = tenants.stream()
                .map(Tenant::getTenantId)
                .map(this::initCachePerTenant)
                .collect(Collectors.toList());

        for (int i = 0; i < 16; ) {
            boolean res = collect.stream().map(Future::isDone).reduce(true, (a, b) -> a && b);
            if (res) return;
            i = Math.max(1, 2 * i);
            Thread.sleep(i * 1000L);
        }

        collect.forEach(f -> {
            if (!f.isDone()) f.cancel(false);
        });
    }

    @Override
    public Set<String> getAllTemplateIds() {
        return templateStorage.findAllTemplateIds();
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
        } else {
            invalidateCache(template);
        }

        templateStorage.storeTemplate(template, tenantService.getCurrentSysTenant());
        putIntoCache(template, tenantService.getCurrentSysTenant());

        preBuildQueries(templateId, cacheOptions.isPreBuildQueries());

        return templateId;
    }

    private void preBuildQueries(String templateId, boolean preBuild) {
        if (!preBuild) return;

        getQueryOptMetaData(templateId).findAllContainmentCombinations().stream()
                .filter(nodeIds -> !nodeIds.isEmpty() && nodeIds.size() <= cacheOptions.getPreBuildQueriesDepth())
                .forEach(nodeIds -> execService.submit(
                        new SecCtxAwareRunnable(
                                SecurityContextHolder.getContext().getAuthentication()) {
                            void doRun() {
                                resolveForTemplate(templateId, nodeIds);
                            }
                        }));
    }

    private void putIntoCache(OPERATIONALTEMPLATE template, Short sysTenant) {
        var templateId = TemplateUtils.getTemplateId(template);
        var uid = TemplateUtils.getUid(template);

        try {
            idxCacheUuidToTemplateId.put(CacheKey.of(uid, sysTenant), templateId);
            idxCacheTemplateIdToUuid.put(templateId, CacheKey.of(uid, sysTenant));

            getQueryOptMetaData(templateId);
        } catch (RuntimeException e) {
            log.error("Invalid template {}", templateId);
            invalidateCache(template);
            throw e;
        }
    }

    public String adminUpdateOperationalTemplate(InputStream content) {
        OPERATIONALTEMPLATE template = buildOperationalTemplate(content);
        return addOperationalTemplateIntern(template, true);
    }

    // invalidates some derived caches like the queryOptMetaDataCache which depend on the template
    private void invalidateCache(OPERATIONALTEMPLATE template) {
        // invalidate the cache for this template
        webTemplateCache.evict(CacheKey.of(TemplateUtils.getUid(template), tenantService.getCurrentSysTenant()));

        jsonPathQueryResultCache.invalidate();
        fieldCache.invalidate();
        multivaluedCache.invalidate();
    }

    @Override
    public List<TemplateMetaData> listAllOperationalTemplates() {
        return templateStorage.listAllOperationalTemplates();
    }

    @Override
    public Optional<OPERATIONALTEMPLATE> retrieveOperationalTemplate(String key) {
        log.debug("retrieveOperationalTemplate({})", key);
        return Optional.ofNullable(getOperationaltemplateFromFileStorage(key));
    }

    @Override
    public Optional<OPERATIONALTEMPLATE> retrieveOperationalTemplate(UUID uuid) {
        return Optional.ofNullable(findTemplateIdByUuid(uuid)).flatMap(key -> retrieveOperationalTemplate(key));
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

    private String findTemplateIdByUuid(UUID uuid) {
        return idxCacheUuidToTemplateId.computeIfAbsent(
                CacheKey.of(uuid, tenantService.getCurrentSysTenant()), ck -> listAllOperationalTemplates().stream()
                        .filter(t -> t.getErrorList().isEmpty())
                        .filter(t -> t.getOperationaltemplate()
                                .getUid()
                                .getValue()
                                .equals(ck.getVal().toString()))
                        .map(t -> t.getOperationaltemplate().getTemplateId().getValue())
                        .findFirst()
                        .orElse(null));
    }

    private UUID findUuidByTemplateId(String templateId) {
        return idxCacheTemplateIdToUuid
                .computeIfAbsent(templateId, id -> {
                    OPERATIONALTEMPLATE templ = retrieveOperationalTemplate(id)
                            .orElseThrow(() ->
                                    new IllegalArgumentException(String.format("Unknown template %s", templateId)));
                    return CacheKey.of(UUID.fromString(templ.getUid().getValue()), tenantService.getCurrentSysTenant());
                })
                .getVal();
    }

    @Override
    public WebTemplate getQueryOptMetaData(UUID uuid) {
        CacheKey<UUID> ck = CacheKey.of(uuid, tenantService.getCurrentSysTenant());
        return webTemplateCache.get(ck, () -> buildQueryOptMetaData(uuid));
    }

    @Override
    public WebTemplate getQueryOptMetaData(String templateId) {
        return getQueryOptMetaData(findUuidByTemplateId(templateId));
    }

    private WebTemplate buildQueryOptMetaData(UUID uuid) {
        Optional<OPERATIONALTEMPLATE> operationaltemplate;

        try {
            operationaltemplate = retrieveOperationalTemplate(uuid);
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
            operationaltemplate = Optional.empty();
        }

        return operationaltemplate
                .map(this::buildQueryOptMetaData)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Could not retrieve  knowledgeCacheService.getKnowledgeCache() cache for template Uid:"
                                + uuid));
    }

    private WebTemplate buildQueryOptMetaData(OPERATIONALTEMPLATE operationaltemplate) {
        log.info("Updating WebTemplate cache for template: {}", TemplateUtils.getTemplateId(operationaltemplate));
        try {
            return new OPTParser(operationaltemplate).parse();
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("Invalid template: %s", e.getMessage()));
        }
    }

    /**
     * Helper function to retrieve the operational template from file storage and put it into the
     * cache. For instance, to handle first time access to an operational template before it was
     * written to cache already.
     *
     * @param filename of the OPT file in storage
     * @return The operational template or null.
     */
    private OPERATIONALTEMPLATE getOperationaltemplateFromFileStorage(String filename) {
        var template = templateStorage.readOperationaltemplate(filename);
        template.ifPresent(existingTemplate -> idxCacheUuidToTemplateId.put(
                CacheKey.of(TemplateUtils.getUid(existingTemplate), tenantService.getCurrentSysTenant()), filename));
        return template.orElse(null);
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

    @Override
    public JsonPathQueryResult resolveForTemplate(String templateId, Collection<NodeId> nodeIds) {
        Triple<String, Short, Collection<NodeId>> key =
                Triple.of(templateId, tenantService.getCurrentSysTenant(), nodeIds);
        JsonPathQueryResult jsonPathQueryResult =
                jsonPathQueryResultCache.get(key, () -> createJsonPathQueryResult(key));

        return jsonPathQueryResult.getTemplateId() != null ? jsonPathQueryResult : null;
    }

    private JsonPathQueryResult createJsonPathQueryResult(Triple<String, Short, Collection<NodeId>> key) {
        JsonPathQueryResult jsonPathQueryResult;
        WebTemplate webTemplate = getQueryOptMetaData(key.getLeft());
        List<WebTemplateNode> webTemplateNodeList = new ArrayList<>();
        webTemplateNodeList.add(webTemplate.getTree());

        for (NodeId nodeId : key.getRight()) {
            webTemplateNodeList = webTemplateNodeList.stream()
                    .map(n -> n.findMatching(f -> {
                        if (f.getNodeId() == null) return false;
                        // compere only classname
                        else if (nodeId.getNodeId() == null)
                            return nodeId.getClassName().equals(new NodeId(f.getNodeId()).getClassName());
                        else return nodeId.equals(new NodeId(f.getNodeId()));
                    }))
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
        }

        Set<String> uniquePaths = new TreeSet<>();
        webTemplateNodeList.stream().map(n -> n.getAqlPath(false)).forEach(uniquePaths::add);

        if (!uniquePaths.isEmpty()) {
            jsonPathQueryResult = new JsonPathQueryResult(key.getLeft(), uniquePaths);
        } else {
            // dummy result since null can not be path of a cache
            jsonPathQueryResult = new JsonPathQueryResult(null, Collections.emptyMap());
        }
        return jsonPathQueryResult;
    }

    @Override
    public ItemInfo getInfo(String templateId, String aql) {
        TemplateIdAqlTuple key = new TemplateIdAqlTuple(templateId, aql, tenantService.getCurrentSysTenant());
        return fieldCache.get(key, () -> createItemInfo(key));
    }

    private ItemInfo createItemInfo(TemplateIdAqlTuple key) {
        WebTemplate webTemplate = getQueryOptMetaData(key.getTemplateId());
        String keyAql = key.getAql();
        Optional<WebTemplateNode> node = webTemplate.findByAqlPath(keyAql);
        final String type;
        if (node.isEmpty()) {
            type = null;
        } else if (node.get().getRmType().equals(ELEMENT)) {
            // for element unwrap
            type = node.get().getChildren().get(0).getRmType();
        } else {
            type = node.get().getRmType();
        }
        String category;

        if (node.isEmpty()) {
            category = null;
        } else if (keyAql.endsWith("/value")) {
            // for element unwrap
            category = webTemplate
                    .findByAqlPath(keyAql.replace("/value", ""))
                    .filter(n -> n.getRmType().equals(ELEMENT))
                    .map(n -> ELEMENT)
                    .orElse("DATA_STRUCTURE");
        } else {
            category = "DATA_STRUCTURE";
        }

        return new ItemInfo(type, category);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> multiValued(String templateId) {
        return multivaluedCache.get(
                CacheKey.of(templateId, tenantService.getCurrentSysTenant()),
                () -> getQueryOptMetaData(templateId).multiValued().stream()
                        .map(webTemplateNode -> webTemplateNode.getAqlPath(false))
                        .collect(Collectors.toList()));
    }

    @Override
    public I_KnowledgeCache getKnowledge() {
        return this;
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

    @Override
    public ConceptValue getConceptByConceptId(
            int conceptId, String language, BiFunction<Integer, String, ConceptValue> provider) {
        ConceptValue concept = conceptByConceptId.get(Pair.of(conceptId, language), ConceptValue.class);
        if (concept == null) {
            concept = provider.apply(conceptId, language);
            addConceptToCaches(concept);
        }
        return concept;
    }

    @Override
    public ConceptValue getConceptById(UUID id, Function<UUID, ConceptValue> provider) {
        ConceptValue concept = conceptById.get(id, ConceptValue.class);
        if (concept == null) {
            concept = provider.apply(id);
            addConceptToCaches(concept);
        }
        return concept;
    }

    @Override
    public ConceptValue getConceptByDescription(
            String description, String language, BiFunction<String, String, ConceptValue> provider) {
        ConceptValue concept = conceptByDescription.get(Pair.of(description, language), ConceptValue.class);
        if (concept == null) {
            concept = provider.apply(description, language);
            addConceptToCaches(concept);
        }
        return concept;
    }

    private void addConceptToCaches(ConceptValue concept) {
        conceptById.put(concept.getId(), concept);
        conceptByConceptId.put(Pair.of(concept.getConceptId(), concept.getLanguage()), concept);
        conceptByDescription.put(Pair.of(concept.getDescription(), concept.getLanguage()), concept);
    }

    @Override
    public TerritoryValue getTerritoryCodeByTwoLetterCode(
            String territoryAsString, Function<String, TerritoryValue> provider) {
        return territoryCache.get(territoryAsString, () -> provider.apply(territoryAsString));
    }

    @Override
    public LanguageValue getLanguageByCode(String languageCode, Function<String, LanguageValue> provider) {
        return languageCache.get(languageCode, () -> provider.apply(languageCode));
    }
}
