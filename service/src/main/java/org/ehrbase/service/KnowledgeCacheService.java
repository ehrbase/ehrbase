/*
 * Modifications copyright (C) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School,
 * Jake Smolka (Hannover Medical School),
 * Stefan Spiska (Vitasystems GmbH).

 * This file is part of Project EHRbase

 * Copyright (c) 2015 Christian Chevalley
 * This file is part of Project Ethercis
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.service;

import org.apache.xmlbeans.XmlException;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.exception.StateConflictException;
import org.ehrbase.aql.containment.JsonPathQueryResult;
import org.ehrbase.aql.containment.TemplateIdAqlTuple;
import org.ehrbase.aql.containment.TemplateIdQueryTuple;
import org.ehrbase.aql.sql.queryimpl.ItemInfo;
import org.ehrbase.configuration.CacheConfiguration;
import org.ehrbase.ehr.knowledge.I_KnowledgeCache;
import org.ehrbase.ehr.knowledge.TemplateMetaData;
import org.ehrbase.webtemplate.model.WebTemplate;
import org.ehrbase.webtemplate.model.WebTemplateNode;
import org.ehrbase.webtemplate.parser.NodeId;
import org.ehrbase.webtemplate.parser.OPTParser;
import org.openehr.schemas.v1.OBJECTID;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;
import org.openehr.schemas.v1.TEMPLATEID;
import org.openehr.schemas.v1.TemplateDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.cache.Cache;
import javax.cache.CacheManager;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

import static org.ehrbase.configuration.CacheConfiguration.*;

/**
 * Look up and caching for archetypes, openEHR showTemplates and Operational Templates. Search in path defined as
 * <ul>
 * <li> 1. System environment ETHERCIS_ARCHETYPE_DIR, ETHERCIS_TEMPLATE_DIR, ETHERCIS_OPT_DIR</li>
 * <li> 2. Application path %USER_HOME%/.ethercis/archetype, %USER_HOME%/.ethercis/template, %USER_HOME%/.ethercis/opt</li>
 * <li> 3. User can also include a source directory by invoking addXYZPath method</li>
 * </ul>
 *
 * <p>
 * The resources extensions are defined by the following default:
 * <ul>
 * <li>ADL: archetype</li>
 * <li>OET: openehr template</li>
 * <li>OPT: operational template</li>
 * </ul>
 * </p>
 *
 * @author C. Chevalley
 */
@Service
@Transactional
public class KnowledgeCacheService implements I_KnowledgeCache, IntrospectService {


    public static final String ELEMENT = "ELEMENT";
    private final Logger log = LoggerFactory.getLogger(this.getClass());


    private final TemplateStorage templateStorage;
    private final Cache<TemplateIdQueryTuple, JsonPathQueryResult> jsonPathQueryResultCache;

    private final Cache<String, OPERATIONALTEMPLATE> atOptCache;
    private final Cache<UUID, WebTemplate> webTemplateCache;
    private final Cache<TemplateIdAqlTuple, ItemInfo> fieldCache;
    private final Cache<String, List> multivaluedCache;

    //index uuid to templateId
    private Map<UUID, String> idxCacheUuidToTemplateId = new ConcurrentHashMap<>();
    //index templateId to uuid
    private Map<String, UUID> idxCacheTemplateIdToUuid = new ConcurrentHashMap<>();

    private Set<String> allTemplateId = new HashSet<>();


    private final CacheManager cacheManager;
    private final CacheConfiguration cacheConfiguration;

    @Value("${system.allow-template-overwrite:false}")
    private boolean allowTemplateOverwrite;

    @Autowired
    public KnowledgeCacheService(@Qualifier("templateDBStorageService") TemplateStorage templateStorage, CacheManager cacheManager, CacheConfiguration cacheConfiguration) {
        this.templateStorage = templateStorage;
        this.cacheManager = cacheManager;

        atOptCache = cacheManager.getCache(OPERATIONAL_TEMPLATE_CACHE, String.class, OPERATIONALTEMPLATE.class);
        webTemplateCache = cacheManager.getCache(CacheConfiguration.INTROSPECT_CACHE, UUID.class, WebTemplate.class);
        jsonPathQueryResultCache = cacheManager.getCache(QUERY_CACHE, TemplateIdQueryTuple.class, JsonPathQueryResult.class);
        fieldCache = cacheManager.getCache(FIELDS_CACHE, TemplateIdAqlTuple.class, ItemInfo.class);
        multivaluedCache = cacheManager.getCache(MULTI_VALUE_CACHE, String.class, List.class);
        this.cacheConfiguration = cacheConfiguration;
    }

    @PostConstruct
    public void init() {
        allTemplateId = new HashSet<>();


        for (TemplateMetaData metaData : listAllOperationalTemplates()) {
            OPERATIONALTEMPLATE operationaltemplate = metaData.getOperationaltemplate();


            String value = "";
            try {
                value = Optional.ofNullable(operationaltemplate).map(OPERATIONALTEMPLATE::getTemplateId).map(OBJECTID::getValue).orElseThrow();
                putIntoCache(operationaltemplate);
                allTemplateId.add(value);

            } catch (RuntimeException e) {
                log.error("Invalidate template : {}", value);
            }
        }


        // If the template was manually removed from db invalidate the cache.
        Set<String> templateIdInCache = new HashSet<>();
        Iterator<Cache.Entry<UUID, WebTemplate>> iterator = webTemplateCache.iterator();


        while (iterator.hasNext()) {
            try {
                Optional.ofNullable(iterator.next()).map(Cache.Entry::getValue).map(WebTemplate::getTemplateId).ifPresent(templateIdInCache::add);
            } catch (RuntimeException e) {
                //NOP
            }

        }
        templateIdInCache.removeAll(allTemplateId);

        if (!templateIdInCache.isEmpty()) {
            webTemplateCache.clear();
            jsonPathQueryResultCache.clear();
            atOptCache.clear();
            fieldCache.clear();
            multivaluedCache.clear();
        }

        if (cacheConfiguration.isPreBuildQueries()) {
            ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);

            executor.submit(() -> {

                for (String s : allTemplateId) {
                    try {
                        precalculateQuerys(s);
                    } catch (RuntimeException e) {
                        log.error("Invalidate template : {}", s);
                    }
                }
            });
        }

    }

    @PreDestroy
    public void closeCache() {
        cacheManager.close();
    }


    private <T, S> S getFromCache(T key, Cache<T, S> cache) {
        try {
            return cache.get(key);
        } catch (RuntimeException e) {
            // Tread errors in cache as miss;
            cache.remove(key);
            log.debug(String.format("Removed invalid value from cache: %s", cache.getName()), e);
            return null;
        }
    }

    @Override
    public Set<String> getAllTemplateIds() {
        return allTemplateId;
    }

    @Override
    public String addOperationalTemplate(byte[] content) {
        return addOperationalTemplateIntern(content, false);
    }

    public String addOperationalTemplateIntern(byte[] content, boolean overwrite) {

        InputStream inputStream = new ByteArrayInputStream(content);

        TemplateDocument document;
        try {
            document = TemplateDocument.Factory.parse(inputStream);
        } catch (XmlException | IOException e) {
            throw new InvalidApiParameterException(e.getMessage());
        }
        OPERATIONALTEMPLATE template = document.getTemplate();

        if (template == null) {
            throw new InvalidApiParameterException("Could not parse input template");
        }

        if (template.getConcept() == null || template.getConcept().isEmpty())
            throw new IllegalArgumentException("Supplied template has nil or empty concept");

        if (template.getDefinition() == null || template.getDefinition().isNil())
            throw new IllegalArgumentException("Supplied template has nil or empty definition");

        if (template.getDescription() == null || !template.getDescription().validate())
            throw new IllegalArgumentException("Supplied template has nil or empty description");

        //get the filename from the template template Id
        Optional<TEMPLATEID> filenameOptional = Optional.ofNullable(template.getTemplateId());
        String templateId = filenameOptional.orElseThrow(() -> new InvalidApiParameterException("Invalid template input content")).getValue();


        // pre-check: if already existing throw proper exception
        if (!allowTemplateOverwrite && !overwrite && retrieveOperationalTemplate(templateId).isPresent()) {
            throw new StateConflictException("Operational template with this template ID already exists");
        } else {
            invalidateCache(template);
        }

        templateStorage.storeTemplate(template);


        putIntoCache(template);

        if (cacheConfiguration.isPreBuildQueries()) {
            ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
            executor.submit(() -> {
                try {
                    precalculateQuerys(templateId);
                } catch (RuntimeException e) {
                    //NOP
                }
            });
        }

        //retrieve the template Id for this new entry
        return template.getTemplateId().getValue();
    }

    private void putIntoCache(OPERATIONALTEMPLATE template) {
        try {


            String templateId = template.getTemplateId().getValue();
            atOptCache.put(templateId, template);
            idxCacheUuidToTemplateId.put(UUID.fromString(template.getUid().getValue()), templateId);
            idxCacheTemplateIdToUuid.put(templateId, UUID.fromString(template.getUid().getValue()));
            allTemplateId.add(templateId);

            getQueryOptMetaData(templateId);
        } catch (RuntimeException e) {
            invalidateCache(template);
            log.error("Invalid template {}", template.getTemplateId().getValue());
            throw e;
        }
    }

    private void precalculateQuerys(String templateId) {

        getQueryOptMetaData(templateId).findAllContainmentCombinations()
                .stream()
                .filter(s -> !s.isEmpty())
                .filter(s -> s.size() <= cacheConfiguration.getPreBuildQueriesDepth())
                .forEach(s -> resolveForTemplate(templateId, s));


    }

    public String adminUpdateOperationalTemplate(byte[] content) {

        return addOperationalTemplateIntern(content, true);
    }

    // invalidates some derived caches like the queryOptMetaDataCache which depend on the template
    private void invalidateCache(OPERATIONALTEMPLATE template) {

        //invalidate the cache for this template
        allTemplateId.remove(template.getTemplateId().getValue());
        webTemplateCache.remove(UUID.fromString(template.getUid().getValue()));
        atOptCache.remove(template.getTemplateId().getValue());

        jsonPathQueryResultCache.clear();
        fieldCache.clear();
        multivaluedCache.clear();
    }


    @Override
    public List<TemplateMetaData> listAllOperationalTemplates() {
        return templateStorage.listAllOperationalTemplates();
    }


    @Override
    public Optional<OPERATIONALTEMPLATE> retrieveOperationalTemplate(String key) {
        log.debug("retrieveOperationalTemplate({})", key);
        OPERATIONALTEMPLATE template = getFromCache(key, atOptCache);
        if (template == null) {     // null if not in cache already, which triggers the following retrieval and putting into cache
            template = getOperationaltemplateFromFileStorage(key);
        }
        return Optional.ofNullable(template);
    }

    @Override
    public Optional<OPERATIONALTEMPLATE> retrieveOperationalTemplate(UUID uuid) {
        String key = findTemplateIdByUuid(uuid);
        if (key == null) {
            return Optional.empty();
        }

        return retrieveOperationalTemplate(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean deleteOperationalTemplate(OPERATIONALTEMPLATE template) {
        // Remove template from storage
        boolean deleted = this.templateStorage.deleteTemplate(template.getTemplateId().getValue());

        if (deleted) {
            // Remove template from caches
            invalidateCache(template);
        }

        return deleted;
    }

    private String findTemplateIdByUuid(UUID uuid) {
        String templateId = idxCacheUuidToTemplateId.get(uuid);

        if (templateId == null) {
            templateId = listAllOperationalTemplates()
                    .stream()
                    .filter(t -> t.getErrorList().isEmpty())
                    .filter(t -> t.getOperationaltemplate().getUid().getValue().equals(uuid.toString()))
                    .map(t -> t.getOperationaltemplate().getTemplateId().getValue())
                    .findFirst()
                    .orElse(null);
            idxCacheUuidToTemplateId.put(uuid, templateId);
        }

        return templateId;
    }

    private UUID findUuidByTemplateId(String templateId) {
        UUID uuid = idxCacheTemplateIdToUuid.get(templateId);
        if (uuid == null) {
            uuid = UUID.fromString(retrieveOperationalTemplate(templateId)
                    .orElseThrow()
                    .getUid()
                    .getValue());
            idxCacheTemplateIdToUuid.put(templateId, uuid);
        }
        return uuid;
    }


    @Override
    public WebTemplate getQueryOptMetaData(UUID uuid) {

        final WebTemplate retval;

        if (webTemplateCache.containsKey(uuid))
            retval = getFromCache(uuid, webTemplateCache);
        else {
            retval = buildAndCacheQueryOptMetaData(uuid);
        }
        return retval;
    }

    @Override
    public WebTemplate getQueryOptMetaData(String templateId) {

        return getQueryOptMetaData(findUuidByTemplateId(templateId));
    }

    private WebTemplate buildAndCacheQueryOptMetaData(UUID uuid) {
        WebTemplate retval;
        Optional<OPERATIONALTEMPLATE> operationaltemplate = Optional.empty();
        try {
            operationaltemplate = retrieveOperationalTemplate(uuid);
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        }
        if (operationaltemplate.isPresent()) {
            retval = buildAndCacheQueryOptMetaData(operationaltemplate.get());
        } else {
            throw new IllegalArgumentException("Could not retrieve  knowledgeCacheService.getKnowledgeCache() cache for template Uid:" + uuid);
        }
        return retval;
    }

    private WebTemplate buildAndCacheQueryOptMetaData(OPERATIONALTEMPLATE operationaltemplate) {
        log.info("Updating WebTemplate cache for template: {}", operationaltemplate.getTemplateId().getValue());
        final WebTemplate visitor;
        try {
            visitor = new OPTParser(operationaltemplate).parse();
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("Invalid template: %s", e.getMessage()));
        }

        webTemplateCache.put(UUID.fromString(operationaltemplate.getUid().getValue()), visitor);
        return visitor;
    }

    /**
     * Helper function to retrieve the operational template from file storage and put it into the cache. For instance,
     * to handle first time access to an operational template before it was written to cache already.
     *
     * @param filename of the OPT file in storage
     * @return The operational template or null.
     */
    private OPERATIONALTEMPLATE getOperationaltemplateFromFileStorage(String filename) {
        OPERATIONALTEMPLATE operationaltemplate = templateStorage.readOperationaltemplate(filename).orElse(null);
        if (operationaltemplate != null) {
            atOptCache.put(filename, operationaltemplate);      // manual putting into cache (actual opt cache and then id cache)
            idxCacheUuidToTemplateId.put(UUID.fromString(operationaltemplate.getUid().getValue()), filename);
        }
        return operationaltemplate;
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
        TemplateIdQueryTuple key = new TemplateIdQueryTuple(templateId, nodeIds);

        JsonPathQueryResult jsonPathQueryResult = getFromCache(key, jsonPathQueryResultCache);
        if (jsonPathQueryResult == null) {


            WebTemplate webTemplate = getQueryOptMetaData(templateId);
            List<WebTemplateNode> webTemplateNodeList = new ArrayList<>();
            webTemplateNodeList.add(webTemplate.getTree());
            for (NodeId nodeId : nodeIds) {
                webTemplateNodeList = webTemplateNodeList.stream()
                        .map(n -> n.findMatching(f -> {
                            if (f.getNodeId() == null) {
                                return false;
                            }
                            // compere only classname
                            else if (nodeId.getNodeId() == null) {
                                return nodeId.getClassName().equals(new NodeId(f.getNodeId()).getClassName());
                            } else {
                                return nodeId.equals(new NodeId(f.getNodeId()));
                            }
                        }))
                        .flatMap(List::stream)
                        .collect(Collectors.toList());
            }

            final String aql;
            Set<String> uniquePaths = new TreeSet<>();
            webTemplateNodeList.stream().map(n -> n.getAqlPath(false)).forEach(uniquePaths::add);
            if (uniquePaths.size() == 1) {
                aql = uniquePaths.iterator().next();
            } else if (webTemplateNodeList.size() > 1) {
                aql = uniquePaths.iterator().next();
                log.warn(String.format("Aql Path not unique for template %s and path %s ", templateId, nodeIds));
            } else {
                aql = null;
            }
            if (aql != null) {
                jsonPathQueryResult = new JsonPathQueryResult(templateId, aql);
            } else {
                //dummy result since null can not be path of a cache
                jsonPathQueryResult = new JsonPathQueryResult(null, Collections.emptyMap());
            }
            jsonPathQueryResultCache.put(key, jsonPathQueryResult);
        }

        if (jsonPathQueryResult.getTemplateId() != null) {
            return jsonPathQueryResult;
        }
        // Is dummy result
        else {

            return null;
        }
    }

    @Override
    public ItemInfo getInfo(String templateId, String aql) {
        TemplateIdAqlTuple key = new TemplateIdAqlTuple(templateId, aql);
        ItemInfo itemInfo = getFromCache(key, fieldCache);
        if (itemInfo == null) {

            WebTemplate webTemplate = getQueryOptMetaData(templateId);
            String type;
            Optional<WebTemplateNode> node = webTemplate.findByAqlPath(aql);
            if (node.isEmpty()) {
                type = null;
            } else if (node.get().getRmType().equals(ELEMENT)) {
                //for element unwrap
                type = node.get().getChildren().get(0).getRmType();
            } else {
                type = node.get().getRmType();
            }
            String category;

            if (node.isEmpty()) {
                category = null;
            } else if (aql.endsWith("/value")) {
                //for element unwrap
                category = webTemplate.findByAqlPath(aql.replace("/value", "")).filter(n -> n.getRmType().equals(ELEMENT)).map(n -> ELEMENT).orElse("DATA_STRUCTURE");
            } else {
                category = "DATA_STRUCTURE";
            }

            itemInfo = new ItemInfo(type, category);
            fieldCache.put(key, itemInfo);
        }
        return itemInfo;
    }

    @Override
    public List<String> multiValued(String templateId) {
        List<String> list = getFromCache(templateId, multivaluedCache);
        if (list == null) {
            list = getQueryOptMetaData(templateId).multiValued().stream().map(webTemplateNode -> webTemplateNode.getAqlPath(false)).collect(Collectors.toList());
            multivaluedCache.put(templateId, list);
        }
        return list;
    }

    @Override
    public I_KnowledgeCache getKnowledge() {
        return this;
    }


}
