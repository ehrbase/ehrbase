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

import com.google.common.util.concurrent.MoreExecutors;
import org.apache.xmlbeans.XmlException;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.exception.StateConflictException;
import org.ehrbase.aql.containment.JsonPathQueryResult;
import org.ehrbase.aql.containment.TemplateIdAqlTuple;
import org.ehrbase.aql.containment.TemplateIdQueryTuple;
import org.ehrbase.aql.sql.queryImpl.ItemInfo;
import org.ehrbase.configuration.CacheConfiguration;
import org.ehrbase.ehr.knowledge.I_KnowledgeCache;
import org.ehrbase.ehr.knowledge.TemplateMetaData;
import org.ehrbase.webtemplate.NodeId;
import org.ehrbase.webtemplate.OPTParser;
import org.ehrbase.webtemplate.WebTemplate;
import org.ehrbase.webtemplate.WebTemplateNode;
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

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.cache.Cache;
import javax.cache.CacheManager;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.ehrbase.configuration.CacheConfiguration.FIELDS_CACHE;
import static org.ehrbase.configuration.CacheConfiguration.MULTI_VALUE_CACHE;
import static org.ehrbase.configuration.CacheConfiguration.OPERATIONAL_TEMPLATE_CACHE;
import static org.ehrbase.configuration.CacheConfiguration.QUERY_CACHE;

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
public class KnowledgeCacheService implements I_KnowledgeCache, IntrospectService {


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
        allTemplateId = listAllOperationalTemplates().stream().map(TemplateMetaData::getOperationaltemplate).map(OPERATIONALTEMPLATE::getTemplateId).map(OBJECTID::getValue).collect(Collectors.toSet());
        listAllOperationalTemplates().stream().map(TemplateMetaData::getOperationaltemplate).forEach(this::putIntoCache);


        if (cacheConfiguration.isPreBuildQueries()) {
            ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
            ExecutorService executorService =
                    MoreExecutors.getExitingExecutorService(executor,
                            100, TimeUnit.MILLISECONDS);

            executor.submit(() ->
                    allTemplateId.forEach(this::precalculateQuerys));
        }

    }

    @PreDestroy
    public void closeCache() {
        cacheManager.close();
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
        }

        templateStorage.storeTemplate(template);


        invalidateCache(template);

        putIntoCache(template);
        if (cacheConfiguration.isPreBuildQueries()) {
            ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
            ExecutorService executorService =
                    MoreExecutors.getExitingExecutorService(executor,
                            100, TimeUnit.MILLISECONDS);

            executor.submit(() -> precalculateQuerys(templateId));
        }

        //retrieve the template Id for this new entry
        return template.getTemplateId().getValue();
    }

    private void putIntoCache(OPERATIONALTEMPLATE template) {
        String templateId = template.getTemplateId().getValue();
        atOptCache.put(templateId, template);
        idxCacheUuidToTemplateId.put(UUID.fromString(template.getUid().getValue()), templateId);
        idxCacheTemplateIdToUuid.put(templateId, UUID.fromString(template.getUid().getValue()));
        allTemplateId.add(templateId);

        getQueryOptMetaData(templateId);
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
        webTemplateCache.remove(UUID.fromString(template.getUid().getValue()));
        Set<TemplateIdQueryTuple> collect = StreamSupport.stream(jsonPathQueryResultCache.spliterator(), true)
                .map(Cache.Entry::getKey)

                .filter(k -> k.getTemplateId().equals(template.getTemplateId().getValue()))
                .collect(Collectors.toSet());
        jsonPathQueryResultCache.removeAll(collect);
    }


    @Override
    public List<TemplateMetaData> listAllOperationalTemplates() {
        return templateStorage.listAllOperationalTemplates();
    }


    @Override
    public Optional<OPERATIONALTEMPLATE> retrieveOperationalTemplate(String key) {
        log.debug("retrieveOperationalTemplate({})", key);
        OPERATIONALTEMPLATE template = atOptCache.get(key);
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
            retval = webTemplateCache.get(uuid);
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
            throw new InternalServerException(e.getMessage(), e);
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

        JsonPathQueryResult jsonPathQueryResult = jsonPathQueryResultCache.get(key);
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
                //FIXME see https://github.com/ehrbase/project_management/issues/377
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
        ItemInfo itemInfo = fieldCache.get(key);
        if (itemInfo == null) {

            WebTemplate webTemplate = getQueryOptMetaData(templateId);
            String type;
            Optional<WebTemplateNode> node = webTemplate.findByAqlPath(aql);
            if (node.isEmpty()) {
                type = null;
            } else if (node.get().getRmType().equals("ELEMENT")) {
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
                category = webTemplate.findByAqlPath(aql.replace("/value", "")).filter(n -> n.getRmType().equals("ELEMENT")).map(n -> "ELEMENT").orElse("DATA_STRUCTURE");
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
        List<String> list = multivaluedCache.get(templateId);
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
