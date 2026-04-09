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
import java.text.MessageFormat;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.xml.namespace.QName;
import org.apache.commons.lang3.StringUtils;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.ehrbase.api.definitions.OperationalTemplateFormat;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.exception.NotAcceptableException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.knowledge.TemplateCacheService;
import org.ehrbase.api.knowledge.TemplateMetaData;
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
import org.ehrbase.openehr.sdk.webtemplate.parser.OPTParser;
import org.ehrbase.openehr.sdk.webtemplate.webtemplateskeletonbuilder.WebTemplateSkeletonBuilder;
import org.ehrbase.util.TemplateUtils;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;
import org.openehr.schemas.v1.RESOURCEDESCRIPTION;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TemplateServiceImp implements TemplateService {

    private static final Logger log = LoggerFactory.getLogger(TemplateServiceImp.class);

    private final boolean allowTemplateOverwrite;

    private final TemplateCacheService templateCacheService;

    public TemplateServiceImp(
            TemplateCacheService templateCacheService,
            @Value("${" + PROP_ALLOW_TEMPLATE_OVERWRITE + ":false}") boolean allowTemplateOverwrite) {
        this.templateCacheService = Objects.requireNonNull(templateCacheService);

        this.allowTemplateOverwrite = allowTemplateOverwrite;
        if (allowTemplateOverwrite) {
            log.warn(
                    "Template overwriting is enabled, this is not recommended for production use and can lead to unexpected behavior, consider disabling {}",
                    PROP_ALLOW_TEMPLATE_OVERWRITE);
        }
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

    private static TemplateMetaData getTemplateFields(OPERATIONALTEMPLATE template) {

        validateTemplate(template);
        String templateId = TemplateUtils.getTemplateId(template);

        XmlOptions opts = new XmlOptions();
        opts.setSaveSyntheticDocumentElement(
                new QName("http://schemas.openehr.org/v1", "template")); // XXX CDR-2305 v2???
        template.xmlText(opts);

        String concept = template.getConcept();
        String archetypeId = template.getDefinition().getArchetypeId().getValue();
        return new TemplateMetaData(
                template.xmlText(opts), new TemplateDetails(null, templateId, null, concept, archetypeId));
    }

    /**
     * Validates that the given template is valid and supported by EHRbase.
     *
     * @param template the template to validate
     */
    private static void validateTemplate(OPERATIONALTEMPLATE template) {
        if (template == null) {
            throw new InvalidApiParameterException("Could not parse input template");
        }

        if (StringUtils.isEmpty(template.getConcept())) {
            throw new IllegalArgumentException("Supplied template has nil or empty concept");
        }
        XmlObject language = template.getLanguage();
        if (language == null || language.isNil()) {
            throw new IllegalArgumentException("Supplied template has nil or empty language");
        }

        XmlObject definition = template.getDefinition();
        if (definition == null || definition.isNil()) {
            throw new IllegalArgumentException("Supplied template has nil or empty definition");
        }

        RESOURCEDESCRIPTION description = template.getDescription();
        if (description == null || !description.validate()) {
            throw new IllegalArgumentException("Supplied template has nil or empty description");
        }

        var webTemplate = new OPTParser(template).parse();
        if (!TemplateUtils.isSupported(webTemplate)) {
            throw new IllegalArgumentException(MessageFormat.format(
                    "The supplied template is not supported (unsupported types: {0})",
                    String.join(",", TemplateUtils.UNSUPPORTED_RM_TYPES)));
        }
    }

    @Override
    public String create(OPERATIONALTEMPLATE template) {
        TemplateMetaData templateMeta = getTemplateFields(template);
        return templateCacheService.addOperationalTemplate(templateMeta, allowTemplateOverwrite);
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
    public String adminUpdateTemplate(OPERATIONALTEMPLATE template) {
        TemplateMetaData templateMeta = getTemplateFields(template);
        String templateId = templateMeta.meta().templateId();
        templateCacheService
                .findUuidByTemplateId(templateId)
                .orElseThrow(() -> new ObjectNotFoundException(
                        "ADMIN TEMPLATE UPDATE", String.format("Template with id %s does not exist", templateId)));

        // Replace content
        return templateCacheService.addOperationalTemplate(templateMeta, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int adminDeleteAllTemplates() {
        return this.templateCacheService.deleteAllOperationalTemplates();
    }
}
