/*
 * Copyright (c) 2019 Stefan Spiska (Vitasystems GmbH) and Hannover Medical School.
 *
 * This file is part of project EHRbase
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

import org.ehrbase.ehr.knowledge.KnowledgeType;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.io.UnicodeInputStream;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Service
public class TemplateFileStorageService {


    private Map<String, File> archetypeFileMap = new ConcurrentHashMap<>();
    private Map<String, File> templatesFileMap = new ConcurrentHashMap<>();
    private Map<String, File> optFileMap = new ConcurrentHashMap<>();


    @Value("${templateFileStorageService.storage.path.archetypes}")
    private String archetypePath;
    @Value("${templateFileStorageService.storage.path.templates}")
    private String templatePath;
    @Value("${templateFileStorageService.storage.path.operationaltemplates}")
    private String optPath;


    private String backupPath = "";

    @Autowired
    public TemplateFileStorageService() {
    }

    public TemplateFileStorageService(String archetypePath, String templatePath, String optPath) {
        this.archetypePath = archetypePath;
        this.templatePath = templatePath;
        this.optPath = optPath;
    }

    @PostConstruct
    public void init() {
        addKnowledgeSourcePath(getArchetypePath(), KnowledgeType.ARCHETYPE);
        addKnowledgeSourcePath(getTemplatePath(), KnowledgeType.TEMPLATE);
        addKnowledgeSourcePath(getOptPath(), KnowledgeType.OPT);
    }


    String getArchetypePath() {
        return archetypePath;
    }

    void setArchetypePath(String archetypePath) {
        this.archetypePath = archetypePath;
    }

    String getTemplatePath() {
        return templatePath;
    }

    void setTemplatePath(String templatePath) {
        this.templatePath = templatePath;
    }

    String getOptPath() {
        return optPath;
    }

    void setOptPath(String optPath) {
        this.optPath = optPath;
    }


    public Set<String> getAllOperationalTemplates() {
        return optFileMap.keySet();
    }

    /**
     * retrieve the file for an archetype
     *
     * @param key the name of the archetype
     * @return a file handler or null
     */
    private File retrieveArchetypeFile(String key) {
        return archetypeFileMap.get(key);
    }

    /**
     * retrieve the file for a template
     *
     * @param key template name
     * @return a file handler or null
     */
    private File retrieveTemplateFile(String key) {
        return templatesFileMap.get(key);
    }

    /**
     * retrieve a file associated to an operational template
     *
     * @param key an OPT ID
     * @return a file handler or null
     */
    private File retrieveOPTFile(String key) {
        return optFileMap.get(key);
    }

    /**
     * retrieve a file associated to a knowledge type
     *
     * @param key a resource id
     * @return a file handler or null
     */
    public File retrieveFile(String key, KnowledgeType what) {

        switch (what) {
            case ARCHETYPE:
                return retrieveArchetypeFile(key);
            case TEMPLATE:
                return retrieveTemplateFile(key);
            case OPT:
                return retrieveOPTFile(key);
        }
        return null;
    }


    public Map<String, File> retrieveFileMap(Pattern includes, Pattern excludes) {
        Map<String, File> mf = new LinkedHashMap<>();
        if (includes != null) {
            for (Map.Entry<String, File> s : archetypeFileMap.entrySet()) {
                if (includes.matcher(s.getKey()).find()) {
                    mf.put(s.getKey(), s.getValue());
                }
            }
        } else {
            mf.putAll(archetypeFileMap);
        }

        if (excludes != null) {
            List<String> removeList = new ArrayList<>();
            for (String s : mf.keySet()) {
                if (excludes.matcher(s).find()) {
                    removeList.add(s);
                }
            }
            for (String s : removeList) {
                mf.remove(s);
            }
        }
        return mf;
    }


    public InputStream getStream(String key, KnowledgeType what) throws IOException {
        File file = retrieveFile(key, what);
        return file != null ? new UnicodeInputStream(new FileInputStream(file), true) : null;
    }

    boolean addKnowledgeSourcePath(String path, KnowledgeType what) {
        switch (what) {
            case ARCHETYPE:
                return addKnowledgeSourcePath(path, archetypeFileMap, what.getExtension());
            case TEMPLATE:
                return addKnowledgeSourcePath(path, templatesFileMap, what.getExtension());
            case OPT:
                return addKnowledgeSourcePath(path, optFileMap, what.getExtension());
        }

        return false;
    }

    private boolean addKnowledgeSourcePath(String path, Map<String, File> resource, String extension) {
        if (path == null) return false;

        path = path.trim();
        if (path.isEmpty())
            throw new IllegalArgumentException("Source path is empty!");

        File root = new File(path);

        root = new File(root.getAbsolutePath());

        if (!root.isDirectory())
            throw new IllegalArgumentException("Supplied source path:" + path + "(" + root.getAbsolutePath() + ") is not a directory!");

        List<File> tr = new ArrayList<>();
        tr.add(root);
        while (!tr.isEmpty()) {
            File r = tr.remove(tr.size() - 1);
            for (File f : r.listFiles()) {
                if (f.isHidden())
                    continue;

                if (f.isFile()) {
                    String key = f.getName().replaceAll("([^\\\\\\/]+)\\." + extension, "$1");
                    resource.put(key, f);
                } else if (f.isDirectory()) {
                    tr.add(f);
                }
            }
        }
        return true;
    }

    public synchronized void saveTemplateFile(String filename, byte[] content) throws IOException {
        //copy the content to filename in OPT path
        Path path = Paths.get(getOptPath(), filename + ".opt");

        //check if this template already exists
        //create a backup
        if (path.toFile().exists() && StringUtils.isNotBlank(backupPath)) {
            DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyyMMddHHmmssSSS");
            Path backupRepository = Paths.get(backupPath + "/" + filename + "_" + formatter.print(DateTime.now()) + ".opt.bak");
            Files.copy(path, backupRepository);
        }

        try {
            Files.write(path, content, StandardOpenOption.CREATE);
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not write file:" + filename + " in directory:" + getOptPath() + ", reason:" + e);
        }

        //load it in the cache
        optFileMap.put(filename, path.toFile());
    }

    public void setBackupPath(String backupPath) {
        this.backupPath = backupPath;
    }

    public String getBackupPath() {
        return backupPath;
    }
}