/*
 * Copyright 2019-2022 vitasystems GmbH and Hannover Medical School.
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

import org.apache.xmlbeans.XmlOptions;
import org.ehrbase.api.definitions.OperationalTemplateFormat;
import org.ehrbase.api.definitions.ServerConfig;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.service.TemplateService;
import org.ehrbase.ehr.knowledge.TemplateMetaData;
import org.ehrbase.response.ehrscape.CompositionFormat;
import org.ehrbase.response.ehrscape.StructuredString;
import org.ehrbase.response.ehrscape.StructuredStringFormat;
import org.ehrbase.response.ehrscape.TemplateMetaDataDto;
import org.ehrbase.webtemplate.model.WebTemplate;
import org.jooq.DSLContext;
import org.openehr.schemas.v1.CARCHETYPEROOT;
import org.openehr.schemas.v1.OBJECTID;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.namespace.QName;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Stefan Spiska
 * @author Jake Smolka
 * @since 1.0
 */
@Service
@Transactional
public class TemplateServiceImp extends BaseServiceImp implements TemplateService {

    private final KnowledgeCacheService knowledgeCacheService;

    public TemplateServiceImp(KnowledgeCacheService knowledgeCacheService, DSLContext context, ServerConfig serverConfig) {
        super(knowledgeCacheService, context, serverConfig);
        this.knowledgeCacheService = Objects.requireNonNull(knowledgeCacheService);
    }

    @Override
    public List<TemplateMetaDataDto> getAllTemplates() {
        return knowledgeCacheService.listAllOperationalTemplates().stream().map(this::mapToDto).collect(Collectors.toList());
    }

    private TemplateMetaDataDto mapToDto(TemplateMetaData data) {
        TemplateMetaDataDto dto = new TemplateMetaDataDto();
        dto.setCreatedOn(data.getCreatedOn());

        Optional<OPERATIONALTEMPLATE> operationalTemplate = Optional.ofNullable(data.getOperationaltemplate());
        dto.setTemplateId(
                operationalTemplate.map(OPERATIONALTEMPLATE::getTemplateId)
                        .map(OBJECTID::getValue)
                        .orElse(null));
        dto.setArchetypeId(
                operationalTemplate.map(OPERATIONALTEMPLATE::getDefinition)
                        .map(CARCHETYPEROOT::getArchetypeId)
                        .map(OBJECTID::getValue)
                        .orElse(null));

        dto.setConcept(operationalTemplate.map(OPERATIONALTEMPLATE::getConcept).orElse(null));
        return dto;
    }

    @Override
    public StructuredString buildExample(String templateId, CompositionFormat format) {
        return new StructuredString("", StructuredStringFormat.fromCompositionFormat(format));
    }

    @Override
    public WebTemplate findTemplate(String templateId) {
        try {
            return knowledgeCacheService.getQueryOptMetaData(templateId);
        } catch (NullPointerException e) {
            throw new ObjectNotFoundException("template", "Template with the specified id does not exist", e);
        } catch (Exception e) {
            throw new InternalServerException("Could not generate web template", e);
        }
    }

    @Override
    public String findOperationalTemplate(String templateId, OperationalTemplateFormat format) throws ObjectNotFoundException, InvalidApiParameterException, InternalServerException {
        if (format != OperationalTemplateFormat.XML) {
            throw new InvalidApiParameterException("Requested operational template type not supported");
        }

        Optional<OPERATIONALTEMPLATE> existingTemplate = this.knowledgeCacheService.retrieveOperationalTemplate(templateId);

        return existingTemplate
                .map(template -> {
                    XmlOptions opts = new XmlOptions();
                    opts.setSaveSyntheticDocumentElement(new QName("http://schemas.openehr.org/v1", "template"));
                    return template.xmlText(opts);
                })
                .orElseThrow(() -> new ObjectNotFoundException("template", "Template with the specified id does not exist"));
    }

    @Override
    public String create(String content) {
        return this.knowledgeCacheService.addOperationalTemplate(content.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean adminDeleteTemplate(String templateId) {
        Optional<OPERATIONALTEMPLATE> existingTemplate = knowledgeCacheService.retrieveOperationalTemplate(templateId);
        if (existingTemplate.isEmpty()) {
            throw new ObjectNotFoundException("ADMIN TEMPLATE", String.format(
                    "Operational template with id %s not found.", templateId
            ));
        }

        // Delete template if not used
        return knowledgeCacheService.deleteOperationalTemplate(existingTemplate.get());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String adminUpdateTemplate(String templateId, String content) {
        Optional<OPERATIONALTEMPLATE> existingTemplate = knowledgeCacheService.retrieveOperationalTemplate(templateId);

        // Check if template exists
        if (existingTemplate.isEmpty()) {
            throw new ObjectNotFoundException(
                    "ADMIN TEMPLATE UPDATE",
                    String.format("Template with id %s does not exist", templateId)
            );
        }

        // Replace content
        return knowledgeCacheService.adminUpdateOperationalTemplate(content.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int adminDeleteAllTemplates() {
        return this.knowledgeCacheService.deleteAllOperationalTemplates();
    }
}
