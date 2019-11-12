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
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.exception.StateConflictException;
import org.ehrbase.configuration.CacheConfiguration;
import org.ehrbase.ehr.knowledge.I_KnowledgeCache;
import org.ehrbase.ehr.knowledge.TemplateMetaData;
import org.ehrbase.opt.OptVisitor;
import org.ehrbase.opt.query.I_QueryOptMetaData;
import org.ehrbase.opt.query.MapJson;
import org.ehrbase.opt.query.QueryOptMetaData;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;
import org.openehr.schemas.v1.TEMPLATEID;
import org.openehr.schemas.v1.TemplateDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import javax.cache.Cache;
import javax.cache.CacheManager;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.ehrbase.configuration.CacheConfiguration.OPERATIONAL_TEMPLATE_CACHE;

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

    private Cache<String, OPERATIONALTEMPLATE> atOptCache;
    private final Cache<UUID, I_QueryOptMetaData> queryOptMetaDataCache;

    //index
    //template index with UUID (not used so far...)
    private Map<UUID, String> idxCache = new ConcurrentHashMap<>();


    private final CacheManager cacheManager;

    @Autowired
    public KnowledgeCacheService(@Qualifier("templateDBStorageService") TemplateStorage templateStorage, CacheManager cacheManager) {
        this.templateStorage = templateStorage;
        this.cacheManager = cacheManager;

        atOptCache = cacheManager.getCache(OPERATIONAL_TEMPLATE_CACHE, String.class, OPERATIONALTEMPLATE.class);
        queryOptMetaDataCache = cacheManager.getCache(CacheConfiguration.INTROSPECT_CACHE, UUID.class, I_QueryOptMetaData.class);
    }

    @PreDestroy
    public void closeCache() {
        cacheManager.close();
    }


    @Override
    public String addOperationalTemplate(byte[] content) {

        InputStream inputStream = new ByteArrayInputStream(content);

        TemplateDocument document = null;
        try {
            document = TemplateDocument.Factory.parse(inputStream);
        } catch (XmlException | IOException e) {
            throw new InvalidApiParameterException(e.getMessage());
        }
        OPERATIONALTEMPLATE template = document.getTemplate();

        if (template == null) {
            throw new InvalidApiParameterException("Could not parse input template");
        }

        //get the filename from the template template Id
        Optional<TEMPLATEID> filenameOptional = Optional.ofNullable(template.getTemplateId());
        String templateId = filenameOptional.orElseThrow(() -> new InvalidApiParameterException("Invalid template input content")).getValue();


        // pre-check: if already existing throw proper exception
        if (retrieveOperationalTemplate(templateId).isPresent()) {
            throw new StateConflictException("Operational template with this template ID already exists");
        }

        templateStorage.storeTemplate(template);


        invalidateCache(template);

        atOptCache.put(templateId, template);
        idxCache.put(UUID.fromString(template.getUid().getValue()), templateId);

        //retrieve the template Id for this new entry
        return template.getTemplateId().getValue();
    }


    // invalidates some derived caches like the queryOptMetaDataCache which depend on the template
    private void invalidateCache(OPERATIONALTEMPLATE template) {

        //invalidate the cache for this template
        queryOptMetaDataCache.remove(UUID.fromString(template.getUid().getValue()));
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
            I_QueryOptMetaData visitor = buildAndCacheQueryOptMetaData(operationaltemplate.get());
            retval = visitor;
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


    @Override
    public I_KnowledgeCache getKnowledge() {
        return this;
    }
}
