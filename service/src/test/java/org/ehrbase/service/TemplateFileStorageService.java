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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.xmlbeans.XmlOptions;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.tenant.TenantAuthentication;
import org.ehrbase.ehr.knowledge.TemplateMetaData;
import org.ehrbase.util.TemplateUtils;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;

public class TemplateFileStorageService implements TemplateStorage {
    private final Supplier<Short> systemTenant = () -> TenantAuthentication.DEFAULT_SYS_TENANT;
    private Map<CacheKey<String>, File> optFileMap = new ConcurrentHashMap<>();
    private Map<String, String> errorMap = new ConcurrentHashMap<>();
    private String optPath;

    public TemplateFileStorageService() {}

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

    public void setOptPath(String optPath) {
        this.optPath = optPath;
    }

    @Override
    public List<TemplateMetaData> listAllOperationalTemplates() {
        ZoneId zoneId = ZoneId.systemDefault();

        Short currentTenantIdentifier = systemTenant.get();

        return optFileMap.keySet().stream()
                .filter(e -> e.getSysTenant().equals(currentTenantIdentifier))
                .map(e -> e.getVal())
                .map(filename -> {
                    TemplateMetaData template = new TemplateMetaData();
                    OPERATIONALTEMPLATE operationaltemplate =
                            readOperationaltemplate(filename).orElse(null);

                    // null if the file couldn't be fetched from cache or read from file storage
                    if (operationaltemplate == null) {
                        template.addError("Reported error for file:" + filename + ", error:" + errorMap.get(filename));
                    } else {
                        template.setOperationaltemplate(operationaltemplate);
                        if (operationaltemplate.getTemplateId() == null)
                            template.addError("Could not get template id for template in file:" + filename);
                    }

                    Path path = convertToTenantPath(filename, currentTenantIdentifier);

                    try {
                        BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
                        ZonedDateTime creationTime = ZonedDateTime.parse(
                                        attributes.creationTime().toString())
                                .withZoneSameInstant(zoneId);
                        template.setCreatedOn(creationTime.toOffsetDateTime());
                    } catch (Exception e) {
                        template.addError("disconnected file? tried:" + getOptPath() + "/" + filename + ".opt");
                    }

                    return template;
                })
                .collect(Collectors.toList());
    }

    @Override
    public Set<String> findAllTemplateIds() {
        return listAllOperationalTemplates().stream()
                .map(metadata -> TemplateUtils.getTemplateId(metadata.getOperationaltemplate()))
                .collect(Collectors.toSet());
    }

    @Override
    public void storeTemplate(OPERATIONALTEMPLATE template, Short sysTenant) {
        XmlOptions opts = new XmlOptions();
        opts.setSaveSyntheticDocumentElement(new QName("http://schemas.openehr.org/v1", "template"));
        saveTemplateFile(
                template.getTemplateId().getValue(),
                template.xmlText(opts).getBytes(StandardCharsets.UTF_8),
                sysTenant);
    }

    @Override
    public Optional<OPERATIONALTEMPLATE> readOperationaltemplate(String templateId) {
        OPERATIONALTEMPLATE operationaltemplate = null;

        File file = optFileMap.get(CacheKey.of(templateId, systemTenant.get()));

        try (InputStream in = (file != null ? new BOMInputStream(new FileInputStream(file), true) : null)) {
            org.openehr.schemas.v1.TemplateDocument document =
                    org.openehr.schemas.v1.TemplateDocument.Factory.parse(in);
            operationaltemplate = document.getTemplate();
        } catch (Exception e) {
            errorMap.put(templateId, e.getMessage());
        }
        return Optional.ofNullable(operationaltemplate);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String adminUpdateTemplate(OPERATIONALTEMPLATE template) {

        try {
            File file = optFileMap.get(CacheKey.of(template.getTemplateId().getValue(), systemTenant.get()));
            if (!file.exists()) {
                throw new ObjectNotFoundException(
                        "ADMIN TEMPLATE STORE FILESYSTEM",
                        String.format("File with name %s does not exist", template.getTemplateId()));
            }

            // Remove old content
            Files.delete(file.toPath());
            optFileMap.remove(CacheKey.of(template.getTemplateId().getValue(), systemTenant.get()));

            // Save new content
            XmlOptions opts = new XmlOptions();
            opts.setSaveSyntheticDocumentElement(new QName("http://schemas.openehr.org/v1", "template"));
            saveTemplateFile(
                    template.getTemplateId().getValue(),
                    template.xmlText(opts).getBytes(StandardCharsets.UTF_8),
                    systemTenant.get());

            return template.xmlText(opts);
        } catch (IOException e) {
            throw new InternalServerException(e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean deleteTemplate(String templateId) {
        boolean deleted;
        try {
            File file = optFileMap.get(CacheKey.of(templateId, systemTenant.get()));
            if (!file.exists())
                throw new ObjectNotFoundException(
                        "ADMIN TEMPLATE", String.format("File with name %s does not exist.", templateId));
            deleted = Files.deleteIfExists(file.toPath());
            if (deleted) optFileMap.remove(CacheKey.of(templateId, systemTenant.get()));
            return deleted;
        } catch (IOException e) {
            throw new InternalServerException(e.getMessage());
        }
    }

    @Override
    public int adminDeleteAllTemplates(List<TemplateMetaData> templateMetaDataList) {
        optFileMap.forEach((key, file) -> {
            try {
                Files.deleteIfExists(file.toPath());
            } catch (IOException e) {
                throw new InternalServerException(e.getMessage());
            }
        });
        optFileMap.clear();
        return templateMetaDataList.size();
    }

    boolean addKnowledgeSourcePath(String path) {
        if (path != null) path = path.trim();
        if (path.isEmpty()) throw new IllegalArgumentException("Source path is empty!");

        File root = new File(new File(path).getAbsolutePath());

        if (!root.isDirectory())
            throw new IllegalArgumentException(
                    "Supplied source path:" + path + "(" + root.getAbsolutePath() + ") is not a directory!");

        List<File> tr = new ArrayList<>();
        tr.add(root);

        while (!tr.isEmpty()) {
            File r = tr.remove(tr.size() - 1);
            for (File f : r.listFiles()) {
                if (f.isHidden()) continue;
                if (f.isFile()) {
                    String key = f.getName().replaceAll("([^\\\\\\/]+)\\." + "opt", "$1");
                    optFileMap.put(CacheKey.of(key, systemTenant.get()), f);
                } else if (f.isDirectory()) {
                    tr.add(f);
                }
            }
        }
        return true;
    }

    private static final String TEMPL_TENANT_PATH = "%s/%s";

    private Path convertToTenantPath(String fileName, Short sysTenant) {
        return Paths.get(String.format(TEMPL_TENANT_PATH, getOptPath(), sysTenant), fileName + ".opt");
    }

    private synchronized void saveTemplateFile(String filename, byte[] content, Short sysTenant) {
        Path dirPath = Paths.get(String.format(TEMPL_TENANT_PATH, getOptPath(), sysTenant));
        Path filePath = convertToTenantPath(filename, sysTenant);

        try {
            if (!Files.exists(dirPath)) Files.createDirectory(dirPath);
            Files.write(filePath, content, StandardOpenOption.CREATE);
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "Could not write file:" + filename + " in directory:" + dirPath + ", reason:" + e);
        }

        // load it in the cache
        optFileMap.put(CacheKey.of(filename, sysTenant), filePath.toFile());
    }
}
