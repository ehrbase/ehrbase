/*
 * Copyright (c) 2024 vitasystems GmbH.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.service;

import com.nedap.archie.rm.composition.Composition;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.ehrbase.api.definitions.OperationalTemplateFormat;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.exception.NotAcceptableException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.knowledge.TemplateCacheService;
import org.ehrbase.api.service.TemplateService;
import org.ehrbase.openehr.sdk.examplegenerator.ExampleGeneratorConfig;
import org.ehrbase.openehr.sdk.examplegenerator.ExampleGeneratorToCompositionWalker;
import org.ehrbase.openehr.sdk.generator.commons.shareddefinition.Language;
import org.ehrbase.openehr.sdk.generator.commons.shareddefinition.Setting;
import org.ehrbase.openehr.sdk.generator.commons.shareddefinition.Territory;
import org.ehrbase.openehr.sdk.serialisation.walker.FlatHelper;
import org.ehrbase.openehr.sdk.serialisation.walker.defaultvalues.DefaultValuePath;
import org.ehrbase.openehr.sdk.serialisation.walker.defaultvalues.DefaultValues;
import org.ehrbase.openehr.sdk.webtemplate.filter.Filter;
import org.ehrbase.openehr.sdk.webtemplate.model.WebTemplate;
import org.ehrbase.openehr.sdk.webtemplate.webtemplateskeletonbuilder.WebTemplateSkeletonBuilder;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TemplateServiceImp implements TemplateService {

    private final TemplateCacheService templateCacheService;

    public TemplateServiceImp(TemplateCacheService templateCacheService) {
        this.templateCacheService = Objects.requireNonNull(templateCacheService);
    }

    @Override
    public Collection<TemplateDetails> findAllTemplates() {
        return templateCacheService.findAllTemplates();
    }

    @Override
    public Composition buildExample(String templateId) {
        WebTemplate webTemplate = findInternalTemplate(templateId);
        Composition composition = WebTemplateSkeletonBuilder.build(webTemplate, false);

        ExampleGeneratorConfig object = new ExampleGeneratorConfig();

        DefaultValues defaultValues = new DefaultValues();
        defaultValues.addDefaultValue(DefaultValuePath.TIME, OffsetDateTime.now());
        defaultValues.addDefaultValue(
                DefaultValuePath.LANGUAGE,
                FlatHelper.findEnumValueOrThrow(webTemplate.getDefaultLanguage(), Language.class));
        defaultValues.addDefaultValue(DefaultValuePath.TERRITORY, Territory.DE);
        defaultValues.addDefaultValue(DefaultValuePath.SETTING, Setting.OTHER_CARE);
        defaultValues.addDefaultValue(DefaultValuePath.COMPOSER_NAME, "Max Mustermann");

        ExampleGeneratorToCompositionWalker walker = new ExampleGeneratorToCompositionWalker();
        walker.walk(composition, object, webTemplate, defaultValues, templateId);

        composition.setTerritory(Territory.DE.toCodePhrase());
        return composition;
    }

    public WebTemplate findInternalTemplate(String templateId) {
        try {
            return templateCacheService.getInternalTemplate(templateId);
        } catch (NullPointerException | IllegalArgumentException e) {
            throw new ObjectNotFoundException("template", "Template with the specified id does not exist", e);
        } catch (Exception e) {
            throw new InternalServerException("Could not generate web template", e);
        }
    }

    @Override
    public WebTemplate findWebTemplate(String templateId) {
        return new Filter().filter(this.findInternalTemplate(templateId));
    }

    @Override
    public String findOperationalTemplate(String templateId, OperationalTemplateFormat format)
            throws ObjectNotFoundException, InvalidApiParameterException, InternalServerException {
        if (format != OperationalTemplateFormat.XML) {
            throw new NotAcceptableException("Requested operational template type not supported");
        }

        String existingTemplate = this.templateCacheService.retrieveOperationalTemplate(templateId);

        return Optional.ofNullable(existingTemplate)
                // XXX CDR-2305 should this be cached???
                .orElseThrow(
                        () -> new ObjectNotFoundException("template", "Template with the specified id does not exist"));
    }

    @Override
    public String create(OPERATIONALTEMPLATE content) {
        return this.templateCacheService.addOperationalTemplate(content);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void adminDeleteTemplate(String templateId) {
        UUID templateUuid = templateCacheService
                .findUuidByTemplateId(templateId)
                .orElseThrow(() -> new ObjectNotFoundException(
                        "ADMIN TEMPLATE", String.format("Operational template with id %s not found.", templateId)));

        // Delete template if not used
        templateCacheService.deleteOperationalTemplate(templateUuid);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String adminUpdateTemplate(String templateId, String content) {
        templateCacheService
                .findUuidByTemplateId(templateId)
                .orElseThrow(() -> new ObjectNotFoundException(
                        "ADMIN TEMPLATE UPDATE", String.format("Template with id %s does not exist", templateId)));
        // Replace content
        return templateCacheService.adminUpdateOperationalTemplate(templateId, content);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int adminDeleteAllTemplates() {
        return this.templateCacheService.deleteAllOperationalTemplates();
    }
}
