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

import org.apache.velocity.io.UnicodeInputStream;
import org.apache.xmlbeans.XmlOptions;
import org.ehrbase.ehr.knowledge.TemplateMetaData;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;


public class TemplateFileStorageService implements TemplateStorage {


    private Map<String, File> optFileMap = new ConcurrentHashMap<>();
    //processing error (for JMX)
    private Map<String, String> errorMap = new ConcurrentHashMap<>();


    private String optPath;


    @Autowired
    public TemplateFileStorageService() {
    }

    public TemplateFileStorageService(String optPath) {

        this.optPath = optPath;
    }

    @PostConstruct
    public void init() {
        addKnowledgeSourcePath(getOptPath());
    }


    public String getOptPath() {
        return optPath;
    }

    void setOptPath(String optPath) {
        this.optPath = optPath;
    }


    @Override
    public List<TemplateMetaData> listAllOperationalTemplates() {
        ZoneId zoneId = ZoneId.systemDefault();
        List<TemplateMetaData> templateMetaDataList = new ArrayList<>();
        for (String filename : optFileMap.keySet()) {

            TemplateMetaData template = new TemplateMetaData();
            OPERATIONALTEMPLATE operationaltemplate;


            operationaltemplate = readOperationaltemplate(filename).orElse(null);


            if (operationaltemplate == null) {   // null if the file couldn't be fetched from cache or read from file storage

                template.addError("Reported error for file:" + filename + ", error:" + errorMap.get(filename));

            } else {
                template.setOperationaltemplate(operationaltemplate);
                if (operationaltemplate.getTemplateId() == null) {
                    template.addError("Could not get template id for template in file:" + filename);
                }
            }

            Path path = Paths.get(getOptPath() + "/" + filename + ".opt");

            try {
                BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
                ZonedDateTime creationTime = ZonedDateTime.parse(attributes.creationTime().toString()).withZoneSameInstant(zoneId);
                template.setCreatedOn(creationTime.toOffsetDateTime());


            } catch (Exception e) {
                template.addError("disconnected file? tried:" + getOptPath() + "/" + filename + ".opt");
            }
            templateMetaDataList.add(template);
        }
        return templateMetaDataList;
    }

    @Override
    public void storeTemplate(OPERATIONALTEMPLATE template) {
        XmlOptions opts = new XmlOptions();
        opts.setSaveSyntheticDocumentElement(new QName("http://schemas.openehr.org/v1", "template"));
        saveTemplateFile(template.getTemplateId().getValue(), template.xmlText(opts).getBytes());
    }

    @Override
    public Optional<OPERATIONALTEMPLATE> readOperationaltemplate(String templateId) {
        OPERATIONALTEMPLATE operationaltemplate = null;

        File file = optFileMap.get(templateId);

        try (InputStream in = file != null ? new UnicodeInputStream(new FileInputStream(file), true) : null) { // manual reading of OPT file and following parsing into object
            org.openehr.schemas.v1.TemplateDocument document = org.openehr.schemas.v1.TemplateDocument.Factory.parse(in);
            operationaltemplate = document.getTemplate();
            //use the template id instead of the file name as key

        } catch (Exception e) {
            errorMap.put(templateId, e.getMessage());
            // log.error("Could not parse operational template:" + filename + " error:" + e);
//                throw new ServiceManagerException(global, SysErrorCode.INTERNAL_ILLEGALARGUMENT, "Could not parse operational template:"+key+" error:"+e);
        }
        return Optional.ofNullable(operationaltemplate);
    }


    boolean addKnowledgeSourcePath(String path) {

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
                    String key = f.getName().replaceAll("([^\\\\\\/]+)\\." + "opt", "$1");
                    optFileMap.put(key, f);
                } else if (f.isDirectory()) {
                    tr.add(f);
                }
            }
        }
        return true;

    }

    private synchronized void saveTemplateFile(String filename, byte[] content) {
        //copy the content to filename in OPT path
        Path path = Paths.get(getOptPath(), filename + ".opt");


        try {
            Files.write(path, content, StandardOpenOption.CREATE);
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not write file:" + filename + " in directory:" + getOptPath() + ", reason:" + e);
        }

        //load it in the cache
        optFileMap.put(filename, path.toFile());
    }


}