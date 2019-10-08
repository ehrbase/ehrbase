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

import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.configuration.CacheConfiguration;
import org.ehrbase.ehr.knowledge.I_KnowledgeCache;
import org.ehrbase.ehr.knowledge.KnowledgeType;
import org.ehrbase.ehr.knowledge.TemplateMetaData;
import org.ehrbase.opt.OptVisitor;
import org.ehrbase.opt.query.I_QueryOptMetaData;
import org.ehrbase.opt.query.MapJson;
import org.ehrbase.opt.query.QueryOptMetaData;
import openEHR.v1.template.TEMPLATE;
import openEHR.v1.template.TemplateDocument;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.xmlbeans.XmlException;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;
import org.openehr.schemas.v1.TEMPLATEID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

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
public class KnowledgeCacheService implements I_KnowledgeCache, IntrospectService {

    private static final String KNOWLEDGE_FORCECACHE = "knowledge.forcecache";
    private static final String KNOWLEDGE_CACHELOCATABLE = "knowledge.cachelocatable";
    private static final String KNOWLEDGE_PATH_ARCHETYPE = "knowledge.path.archetype";
    private static final String KNOWLEDGE_PATH_TEMPLATE = "knowledge.path.template";
    private static final String KNOWLEDGE_PATH_OPT = "knowledge.path.opt";
    private static final String KNOWLEDGE_PATH_BACKUP = "knowledge.path.backup";

    private final Logger log = LoggerFactory.getLogger(this.getClass());


    private final TemplateFileStorageService templateFileStorageService;

    //Cache
    private Cache<String, TEMPLATE> atTemplatesCache;
    private Cache<String, OPERATIONALTEMPLATE> atOptCache;
    private final Cache<UUID, I_QueryOptMetaData> queryOptMetaDataCache;

    //index
    //template index with UUID (not used so far...)
    private Map<UUID, String> idxCache = new ConcurrentHashMap<>();

    //processing error (for JMX)
    private Map<String, String> errorMap = new ConcurrentHashMap<>();

    private final CacheManager cacheManager;

    @Autowired
    public KnowledgeCacheService(TemplateFileStorageService templateFileStorageService, CacheManager cacheManager) {
        this.templateFileStorageService = templateFileStorageService;
        this.cacheManager = cacheManager;

        atTemplatesCache = cacheManager.getCache(TEMPLATE_CACHE, String.class, TEMPLATE.class);
        atOptCache = cacheManager.getCache(OPERATIONAL_TEMPLATE_CACHE, String.class, OPERATIONALTEMPLATE.class);
        queryOptMetaDataCache = cacheManager.getCache(CacheConfiguration.INTROSPECT_CACHE, UUID.class, I_QueryOptMetaData.class);
    }

    @PreDestroy
    public void closeCache() {
        cacheManager.close();
    }

    /**
     * initialize with properties passed from a ServiceInfo context
     * the following are valid service properties:
     * <ul>
     * <li>knowledge.path.archetype</li>
     * <li>knowledge.path.template</li>
     * <li>knowledge.path.opt</li>
     * </ul>
     *
     * @param props the service properties
     * @deprecated User {@link #KnowledgeCacheService(TemplateFileStorageService, CacheManager)}
     */
    @Deprecated
    public KnowledgeCacheService(Properties props) {
        templateFileStorageService = new TemplateFileStorageService();
        setVariablesFromServiceProperties(props);
        templateFileStorageService.init();
        cacheManager = Caching.getCachingProvider().getCacheManager();

        String templateCacheName = RandomStringUtils.random(10);
        String opCacheName = RandomStringUtils.random(10);
        String metaCacheName = RandomStringUtils.random(10);

        buildCache(templateCacheName, String.class, TEMPLATE.class, cacheManager);
        buildCache(opCacheName, String.class, OPERATIONALTEMPLATE.class, cacheManager);
        buildCache(metaCacheName, UUID.class, I_QueryOptMetaData.class, cacheManager);

        atTemplatesCache = cacheManager.getCache(templateCacheName, String.class, TEMPLATE.class);
        atOptCache = cacheManager.getCache(opCacheName, String.class, OPERATIONALTEMPLATE.class);
        queryOptMetaDataCache = cacheManager.getCache(metaCacheName, UUID.class, I_QueryOptMetaData.class);

    }

    /**
     * grab variables from service properties
     *
     * @param props
     * @return
     */
    private boolean setVariablesFromServiceProperties(Properties props) {

        for (Entry<Object, Object> entry : props.entrySet()) {
            if (entry != null && ((String) entry.getKey()).compareTo(KNOWLEDGE_FORCECACHE) == 0) {

            } else if (entry != null && ((String) entry.getKey()).compareTo(KNOWLEDGE_CACHELOCATABLE) == 0) {


            } else if (entry != null && ((String) entry.getKey()).compareTo(KNOWLEDGE_PATH_ARCHETYPE) == 0) {

                try {
                    templateFileStorageService.setArchetypePath((String) entry.getValue());
                    log.debug("mapping archetype path:" + templateFileStorageService.getArchetypePath());
                    templateFileStorageService.addKnowledgeSourcePath(templateFileStorageService.getArchetypePath(), KnowledgeType.ARCHETYPE);
                } catch (Exception e) {
                    log.error("Could not map archetype path:" + entry.getValue());
                    throw new IllegalArgumentException("Invalid archetype path:" + entry.getValue());
                }
            } else if (entry != null && ((String) entry.getKey()).compareTo(KNOWLEDGE_PATH_TEMPLATE) == 0) {
                try {
                    templateFileStorageService.setTemplatePath((String) entry.getValue());
                    log.debug("mapping template path:" + templateFileStorageService.getTemplatePath());
                    templateFileStorageService.addKnowledgeSourcePath(templateFileStorageService.getTemplatePath(), KnowledgeType.TEMPLATE);
                } catch (Exception e) {
                    log.error("Could not map template path:" + entry.getValue());
                    throw new IllegalArgumentException("Invalid template path:" + entry.getValue());
                }
            } else if (entry != null && ((String) entry.getKey()).compareTo(KNOWLEDGE_PATH_OPT) == 0) {
                try {
                    templateFileStorageService.setOptPath((String) (entry.getValue()));
                    log.debug("mapping operational template path:" + templateFileStorageService.getOptPath());
                    templateFileStorageService.addKnowledgeSourcePath(templateFileStorageService.getOptPath(), KnowledgeType.OPT);
                } catch (Exception e) {
                    log.error("Could not map OPT path:" + entry.getValue());
                    throw new IllegalArgumentException("Invalid OPT path:" + entry.getValue());
                }
            } else if (entry != null && ((String) entry.getKey()).compareTo(KNOWLEDGE_PATH_BACKUP) == 0) {
                try {
                    templateFileStorageService.setBackupPath((String) (entry.getValue()));
                    log.debug("Backup path:" + templateFileStorageService.getBackupPath());
                } catch (Exception e) {
                    log.error("Could not map backup path:" + entry.getValue());
                    throw new IllegalArgumentException("Invalid backup path:" + entry.getValue());
                }
            }
        }
        return true;
    }


    @Override
    public String addOperationalTemplate(byte[] content) {

        InputStream inputStream = new ByteArrayInputStream(content);

        org.openehr.schemas.v1.TemplateDocument document = null;
        try {
            document = org.openehr.schemas.v1.TemplateDocument.Factory.parse(inputStream);
        } catch (XmlException | IOException e) {
            throw new InvalidApiParameterException(e.getMessage());
        }
        OPERATIONALTEMPLATE template = document.getTemplate();

        if (template == null) {
            throw new InvalidApiParameterException("Could not parse input template");
        }

        //get the filename from the template template Id
        Optional<TEMPLATEID> filenameOptional = Optional.ofNullable(template.getTemplateId());
        String filename = filenameOptional.orElseThrow(() -> new InvalidApiParameterException("Invalid template input content")).getValue();

        // pre-check: if already existing throw proper exception
        // TODO: disabled due to conflict with integration test implementation. activating will break many other tests.
        /*if (retrieveOperationalTemplate(filename).isPresent()) {
            throw new StateConflictException("Operational template with this template ID already exists");
        }*/

        try {
            templateFileStorageService.saveTemplateFile(filename, content);
        } catch (IOException e) {
            throw new InternalServerException(e.getMessage(), e);
        }

        invalidateCache(template);

        atOptCache.put(filename, template);
        idxCache.put(UUID.fromString(template.getUid().getValue()), filename);

        //retrieve the template Id for this new entry
        return template.getTemplateId().getValue();
    }

    // invalidates some derived caches like the queryOptMetaDataCache which depend on the template
    private void invalidateCache(OPERATIONALTEMPLATE template) {
        String templateId = template.getTemplateId().getValue();
        //invalidate the cache for this template
        queryOptMetaDataCache.remove(UUID.fromString(template.getUid().getValue()));
    }


    @Override
    public List<TemplateMetaData> listAllOperationalTemplates() {
        ZoneId zoneId = ZoneId.systemDefault();
        List<TemplateMetaData> templateMetaDataList = new ArrayList<>();
        for (String filename : templateFileStorageService.getAllOperationalTemplates()) {

            TemplateMetaData template = new TemplateMetaData();
            OPERATIONALTEMPLATE operationaltemplate = atOptCache.get(filename);

            if (operationaltemplate == null) {   // null if not in cache already, which triggers the following retrieval and putting into cache
                operationaltemplate = getOperationaltemplateFromFileStorage(filename);
            }

            if (operationaltemplate == null) {   // null if the file couldn't be fetched from cache or read from file storage

                template.addError("Reported error for file:" + filename + ", error:" + errorMap.get(filename));

            } else {
                template.setOperationaltemplate(operationaltemplate);
                if (operationaltemplate.getTemplateId() == null) {
                    template.addError("Could not get template id for template in file:" + filename);
                }
            }

            Path path = Paths.get(templateFileStorageService.getOptPath() + "/" + filename + ".opt");
            template.setPath(path);
            try {
                BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
                ZonedDateTime creationTime = ZonedDateTime.parse(attributes.creationTime().toString()).withZoneSameInstant(zoneId);
                template.setCreatedOn(creationTime);

                ZonedDateTime lastAccessTime = ZonedDateTime.parse(attributes.lastAccessTime().toString()).withZoneSameInstant(zoneId);
                template.setLastAccessTime(lastAccessTime);

                ZonedDateTime lastModifiedTime = ZonedDateTime.parse(attributes.lastModifiedTime().toString()).withZoneSameInstant(zoneId);
                template.setLastModifiedTime(lastModifiedTime);

            } catch (Exception e) {
                template.addError("disconnected file? tried:" + templateFileStorageService.getOptPath() + "/" + filename + ".opt");
            }
            templateMetaDataList.add(template);
        }
        return templateMetaDataList;
    }

    @Override
    public Map<String, File> retrieveFileMap(Pattern includes, Pattern excludes) {

        return templateFileStorageService.retrieveFileMap(includes, excludes);
    }


    @Override
    public TEMPLATE retrieveOpenehrTemplate(String key) {
        log.debug("retrieveOpenehrTemplate(" + key + ")");
        TEMPLATE template = atTemplatesCache.get(key);
        if (template == null) {
            InputStream in = null;
            try {
                in = templateFileStorageService.getStream(key, KnowledgeType.TEMPLATE);

                TemplateDocument tdoc = TemplateDocument.Factory.parse(in);
                template = tdoc.getTemplate();

                atTemplatesCache.put(key, template);
                idxCache.put(UUID.fromString(template.getId()), key);

            } catch (Exception e) {
                errorMap.put(key, e.getMessage());
                log.error("Could not parse template:" + key + " error:" + e);
//                throw new ServiceManagerException(global, SysErrorCode.INTERNAL_ILLEGALARGUMENT, "Could not parse template:"+key+" error:"+e);
            } finally {
                if (in != null)
                    try {
                        in.close();
                    } catch (IOException e) {
                        log.error(e.getMessage(), e);
                    }
            }
        }

        return template;
    }

    @Override
    public Optional<OPERATIONALTEMPLATE> retrieveOperationalTemplate(String key) {
        log.debug("retrieveOperationalTemplate(" + key + ")");
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

    public String findTemplateIdByUuid(UUID uuid) {
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
    public TEMPLATE retrieveTemplate(UUID uuid) {
        String key = findTemplateIdByUuid(uuid);

        return retrieveOpenehrTemplate(key);
    }




    @Override
    public String settings() {
        StringBuffer sb = new StringBuffer();
        sb.append("Force Cache              :" + false);
        sb.append("\nArchetype Path           :" + templateFileStorageService.getArchetypePath());
        sb.append("\nTemplate Path            :" + templateFileStorageService.getTemplatePath());
        sb.append("\nBackup Path              :" + (StringUtils.isNotBlank(templateFileStorageService.getBackupPath()) ? "*no template backup will be done*" : templateFileStorageService.getBackupPath()));
        sb.append("\nOperational Template Path:" + templateFileStorageService.getOptPath());
        sb.append("\n");
        return sb.toString();
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
        OPERATIONALTEMPLATE operationaltemplate = null;
        try (InputStream in = templateFileStorageService.getStream(filename, KnowledgeType.OPT)) { // manual reading of OPT file and following parsing into object
            org.openehr.schemas.v1.TemplateDocument document = org.openehr.schemas.v1.TemplateDocument.Factory.parse(in);
            operationaltemplate = document.getTemplate();
            //use the template id instead of the file name as key
            atOptCache.put(filename, operationaltemplate);      // manual putting into cache (actual opt cache and then id cache)
            idxCache.put(UUID.fromString(operationaltemplate.getUid().getValue()), filename);
        } catch (Exception e) {
            errorMap.put(filename, e.getMessage());
            log.error("Could not parse operational template:" + filename + " error:" + e);
//                throw new ServiceManagerException(global, SysErrorCode.INTERNAL_ILLEGALARGUMENT, "Could not parse operational template:"+key+" error:"+e);
        }
        return operationaltemplate;
    }

    @Override
    public I_KnowledgeCache getKnowledge() {
        return this;
    }
}
