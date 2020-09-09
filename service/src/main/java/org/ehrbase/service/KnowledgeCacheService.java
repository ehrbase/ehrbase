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

import static org.ehrbase.configuration.CacheConfiguration.OPERATIONAL_TEMPLATE_CACHE;
import static org.ehrbase.configuration.CacheConfiguration.QUERY_CACHE;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.cache.Cache;
import javax.cache.CacheManager;

import org.apache.commons.collections4.MapUtils;
import org.apache.xmlbeans.XmlException;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.exception.StateConflictException;
import org.ehrbase.aql.containment.JsonPathQueryResult;
import org.ehrbase.aql.containment.OptJsonPath;
import org.ehrbase.aql.containment.TemplateIdQueryTuple;
import org.ehrbase.configuration.CacheConfiguration;
import org.ehrbase.dao.access.interfaces.I_CompositionAccess;
import org.ehrbase.dao.access.interfaces.I_EntryAccess;
import org.ehrbase.ehr.knowledge.I_KnowledgeCache;
import org.ehrbase.ehr.knowledge.TemplateMetaData;
import org.ehrbase.opt.OptVisitor;
import org.ehrbase.opt.query.I_QueryOptMetaData;
import org.ehrbase.opt.query.MapJson;
import org.ehrbase.opt.query.QueryOptMetaData;
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

    private Cache<String, OPERATIONALTEMPLATE> atOptCache;
    private final Cache<UUID, I_QueryOptMetaData> queryOptMetaDataCache;

    //index
    //template index with UUID (not used so far...)
    private Map<UUID, String> idxCache = new ConcurrentHashMap<>();

    private Set<String> allTemplateId = new HashSet<>();

    private Map<String, Set<String>> nodeIdsByTemplateIdMap = new HashMap<>();


    private final CacheManager cacheManager;

    @Value("${system.allow-template-overwrite:false}")
    private boolean allowTemplateOverwrite;

    @Autowired
    public KnowledgeCacheService(
            @Qualifier("templateDBStorageService") TemplateStorage templateStorage,
            CacheManager cacheManager
            ) {
        this.templateStorage = templateStorage;
        this.cacheManager = cacheManager;

        atOptCache = cacheManager.getCache(OPERATIONAL_TEMPLATE_CACHE, String.class, OPERATIONALTEMPLATE.class);
        queryOptMetaDataCache = cacheManager.getCache(CacheConfiguration.INTROSPECT_CACHE, UUID.class, I_QueryOptMetaData.class);
        jsonPathQueryResultCache = cacheManager.getCache(QUERY_CACHE, TemplateIdQueryTuple.class, JsonPathQueryResult.class);
    }

    @PostConstruct
    public void init() {
        allTemplateId = listAllOperationalTemplates().stream().map(TemplateMetaData::getOperationaltemplate).map(OPERATIONALTEMPLATE::getTemplateId).map(OBJECTID::getValue).collect(Collectors.toSet());
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
        return addOperationalTemplate(content, false);
    }

    /**
     * Creates a new or replaces an existing operational template. If the template does not exist it will be created.
     * If there is already a template with the given id inside the content and either the configuration setting
     * system.allow-template-overwrite or param overwrite is set to true the template will be replaced with the new
     * content. Of none of these flags is set a conflict exception will be thrown.
     *
     * @param content - New template content to write / set
     * @param overwrite - Allow overwrite of existing templates
     * @return - New created template id
     */
    private String addOperationalTemplate(byte[] content, boolean overwrite) {

        OPERATIONALTEMPLATE template = parseTemplate(content);

        //get the filename from the template template Id
        Optional<TEMPLATEID> filenameOptional = Optional.ofNullable(template.getTemplateId());
        String templateId = filenameOptional.orElseThrow(() -> new InvalidApiParameterException("Invalid template input content")).getValue();

        // pre-check: if already existing and overwrite is forbidden throw proper exception
        if (!allowTemplateOverwrite && !overwrite && retrieveOperationalTemplate(templateId).isPresent()) {
            throw new StateConflictException("Operational template with this template ID already exists");
        }

        templateStorage.storeTemplate(template);

        invalidateCache(template);

        atOptCache.put(templateId, template);
        idxCache.put(UUID.fromString(template.getUid().getValue()), templateId);
        allTemplateId.add(templateId);

        //retrieve the template Id for this new entry
        return template.getTemplateId().getValue();
    }

    public String adminUpdateOperationalTemplate(byte[] content) {

        OPERATIONALTEMPLATE template = parseTemplate(content);

        String templateId = Optional.ofNullable(
                template.getTemplateId())
                .orElseThrow(() -> new InvalidApiParameterException("Invalid template input content"))
                .getValue();

        // Replace template
        templateStorage.adminUpdateTemplate(template);

        // Refresh template caches
        invalidateCache(template);
        atOptCache.replace(templateId, template);
        idxCache.replace(UUID.fromString(template.getUid().getValue()), templateId);

        return template.xmlText();
    }


    // invalidates some derived caches like the queryOptMetaDataCache which depend on the template
    private void invalidateCache(OPERATIONALTEMPLATE template) {

        //invalidate the cache for this template
        queryOptMetaDataCache.remove(UUID.fromString(template.getUid().getValue()));
        atOptCache.remove(template.getUid().getValue());
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

    private String findTemplateIdByUuid(UUID uuid) {
        String key = idxCache.get(uuid);

        if (key == null) {
            key = listAllOperationalTemplates()
                    .stream()
                    .filter(t -> t.getErrorList().isEmpty())
                    .filter(t -> t.getOperationaltemplate().getUid().getValue().equals(uuid.toString()))
                    .map(t -> t.getOperationaltemplate().getTemplateId().getValue())
                    .findFirst()
                    .orElse(null);
        }
        return key;
    }


    @Override
    public I_QueryOptMetaData getQueryOptMetaData(UUID uuid) {

        final I_QueryOptMetaData retval;

        if (queryOptMetaDataCache.containsKey(uuid))
            retval = queryOptMetaDataCache.get(uuid);
        else {
            retval = buildAndCacheQueryOptMetaData(uuid);
        }
        return retval;
    }

    @Override
    public I_QueryOptMetaData getQueryOptMetaData(String templateId) {

        //get the matching template if any


        Optional<OPERATIONALTEMPLATE> operationaltemplate = retrieveOperationalTemplate(templateId);

        if (operationaltemplate.isPresent())
            return getQueryOptMetaData(UUID.fromString(operationaltemplate.get().getUid().getValue()));
        else {

            Optional<OPERATIONALTEMPLATE> cachedOpt = Optional.empty();
            try {
                cachedOpt = retrieveOperationalTemplate(templateId);
            } catch (Exception e) {
                log.warn(e.getMessage(), e);
            }
            if (cachedOpt.isPresent()) {
                UUID uuid = UUID.fromString(cachedOpt.get().getUid().getValue());
                return getQueryOptMetaData(uuid);
            } else
                throw new IllegalArgumentException("Could not retrieve  knowledgeCacheService.getKnowledgeCache() cache for template id:" + templateId);
        }
    }

    private I_QueryOptMetaData buildAndCacheQueryOptMetaData(UUID uuid) {
        I_QueryOptMetaData retval;
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

    private I_QueryOptMetaData buildAndCacheQueryOptMetaData(OPERATIONALTEMPLATE operationaltemplate) {
        log.info("Updating getQueryOptMetaData cache for template: {}", operationaltemplate.getTemplateId().getValue());
        final I_QueryOptMetaData visitor;
        try {
            Map map = new OptVisitor().traverse(operationaltemplate);
            visitor = QueryOptMetaData.getInstance(new MapJson(map).toJson());
        } catch (Exception e) {
            throw new InternalServerException(e.getMessage(), e);
        }

        queryOptMetaDataCache.put(UUID.fromString(operationaltemplate.getUid().getValue()), visitor);
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
            idxCache.put(UUID.fromString(operationaltemplate.getUid().getValue()), filename);
        }
        return operationaltemplate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String updateOperationalTemplate(byte[] content) {
        return this.addOperationalTemplate(content, true);
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
            this.atOptCache.remove(template.getTemplateId().getValue());
            this.idxCache.remove(UUID.fromString(template.getUid().getValue()));
        }

        return deleted;
    }

    public int deleteAllOperationalTemplates() {
        // Get all operational templates
        List<TemplateMetaData> templateList = this.templateStorage.listAllOperationalTemplates();
        // If list is empty no deletion required
        if (templateList.isEmpty()) {
            return 0;
        }
        int deleted = this.templateStorage.adminDeleteAllTemplates(templateList);

        // Clear cache
        this.atOptCache.clear();
        this.idxCache.clear();

        return deleted;
    }
    
    @Override
    public boolean containsNodeIds(String templateId, Collection<String> nodeIds) {
        Set<String> templateNodeIds = nodeIdsByTemplateIdMap.computeIfAbsent(templateId, t -> getQueryOptMetaData(t).getAllNodeIds());
        return templateNodeIds.containsAll(nodeIds);
    }

    @Override
    public JsonPathQueryResult resolveForTemplate(String templateId, String jsonQueryExpression) {
        TemplateIdQueryTuple key = new TemplateIdQueryTuple(templateId, jsonQueryExpression);

        JsonPathQueryResult jsonPathQueryResult = jsonPathQueryResultCache.get(key);

        if (jsonPathQueryResult == null) {
            Map<String, Object> evaluate = new OptJsonPath(this).evaluate(templateId, jsonQueryExpression);
            if (!MapUtils.isEmpty(evaluate)) {
                jsonPathQueryResult = new JsonPathQueryResult(templateId, evaluate);
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
    public I_KnowledgeCache getKnowledge() {
        return this;
    }

    /**
     * Check an input byte array for a template if it is a valid template and generate a new template instance for it.
     *
     * @param templateContent - Byte array with template content
     * @return - New instance of an OPT
     */
    private OPERATIONALTEMPLATE parseTemplate(byte[] templateContent) {

        InputStream inputStream = new ByteArrayInputStream(templateContent);

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

        return template;
    }
}
