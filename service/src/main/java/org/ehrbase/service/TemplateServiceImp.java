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
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;
import org.apache.commons.lang3.StringUtils;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.exception.StateConflictException;
import org.ehrbase.api.exception.UnprocessableEntityException;
import org.ehrbase.api.service.TemplateService;
import org.ehrbase.cache.CacheProperties;
import org.ehrbase.cache.CacheProvider;
import org.ehrbase.openehr.sdk.examplegenerator.ExampleGeneratorConfig;
import org.ehrbase.openehr.sdk.examplegenerator.ExampleGeneratorToCompositionWalker;
import org.ehrbase.openehr.sdk.generator.commons.shareddefinition.Language;
import org.ehrbase.openehr.sdk.generator.commons.shareddefinition.Setting;
import org.ehrbase.openehr.sdk.generator.commons.shareddefinition.Territory;
import org.ehrbase.openehr.sdk.serialisation.walker.FlatHelper;
import org.ehrbase.openehr.sdk.serialisation.walker.defaultvalues.DefaultValuePath;
import org.ehrbase.openehr.sdk.serialisation.walker.defaultvalues.DefaultValues;
import org.ehrbase.openehr.sdk.util.exception.SdkException;
import org.ehrbase.openehr.sdk.webtemplate.filter.Filter;
import org.ehrbase.openehr.sdk.webtemplate.model.WebTemplate;
import org.ehrbase.openehr.sdk.webtemplate.parser.OPTParser;
import org.ehrbase.openehr.sdk.webtemplate.webtemplateskeletonbuilder.WebTemplateSkeletonBuilder;
import org.ehrbase.repository.TemplateStoreRepository;
import org.ehrbase.util.TemplateUtils;
import org.jspecify.annotations.NonNull;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;
import org.openehr.schemas.v1.RESOURCEDESCRIPTION;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lookup and caching for Web and Operational Templates.<br>
 * A list of existing templates is cached separately from the templates that are being used.<br>
 *
 * Szenarios:
 * <ol>
 *    <li>WebTemplate needed for writing/validating COMPOSITIONs</li>
 *    <li>OPT handling for external retrieval</li>
 *    <li>Listing available templates: external requests + aql to sql mapping</li>
 *    <li>Translation between template_id and internal id: CRUD</li>
 * </ol>
 */
@Service
// This service is not @Transactional since we only want to get DB connections when we really need to and an already
// running transaction is propagated anyway
public class TemplateServiceImp implements TemplateService {

    public record TemplateWithDetails(String operationalTemplate, TemplateDetails meta) {}

    public static final String PROP_ALLOW_TEMPLATE_OVERWRITE = "ehrbase.template.allow-overwrite";

    private static final Logger log = LoggerFactory.getLogger(TemplateServiceImp.class);

    private final TemplateStoreRepository templateStoreRepository;

    private final TemplateCacheHelper cacheHelper;

    private final String[] initTemplateCache;

    private final boolean allowTemplateOverwrite;

    public TemplateServiceImp(
            TemplateStoreRepository templateStoreRepository,
            CacheProvider cacheProvider,
            CacheProperties cacheProperties,
            @Value("${" + PROP_ALLOW_TEMPLATE_OVERWRITE + ":false}") boolean allowTemplateOverwrite) {
        this.templateStoreRepository = templateStoreRepository;
        this.cacheHelper = new TemplateCacheHelper(cacheProvider);

        String templateInitOnStartup = cacheProperties.getTemplateInitOnStartup();
        this.initTemplateCache = switch (templateInitOnStartup) {
            case "false", "" -> null;
            case null -> null;
            case "true", "ALL" -> new String[0];
            default -> templateInitOnStartup.split("\\s*,\\s*");
        };

        this.allowTemplateOverwrite = allowTemplateOverwrite;
        if (allowTemplateOverwrite) {
            log.warn(
                    "Template overwriting is enabled, this is not recommended for production use and can lead to unexpected behavior, consider disabling {}",
                    PROP_ALLOW_TEMPLATE_OVERWRITE);
        }
    }

    @PostConstruct
    void init() {
        if (initTemplateCache != null) {
            // side-effect: caches all details
            Collection<TemplateDetails> allTemplates = findAllTemplates();

            String[] templateIds = initTemplateCache.length > 0
                    ? initTemplateCache
                    : allTemplates.stream().map(TemplateDetails::templateId).toArray(String[]::new);
            List<TemplateWithDetails> templateMetaData = templateStoreRepository.findByTemplateIds(templateIds);
            if (log.isInfoEnabled()) {
                String templateIdsStr = templateMetaData.stream()
                        .map(d -> d.meta().templateId())
                        .sorted()
                        .collect(Collectors.joining(", "));
                log.info("Preparing WebTemplate cache for templates: {}", templateIdsStr);
            }
            templateMetaData.forEach(template -> addWebTemplateToCache(template, false));
        }
    }

    private void addWebTemplateToCache(TemplateWithDetails template, boolean inbound) {
        OPERATIONALTEMPLATE operationaltemplate;
        try {
            operationaltemplate = TemplateService.buildOperationalTemplate(template.operationalTemplate());
        } catch (XmlException e) {
            String message = e.getMessage();
            if (inbound) {
                throw new IllegalArgumentException(message, e);
            } else {
                throw new InternalServerException(message, e);
            }
        }
        String templateId = TemplateUtils.getTemplateId(operationaltemplate);
        WebTemplate tpl = buildWebTemplate(operationaltemplate, inbound);

        cacheHelper.addToCache(template.meta().id(), templateId, tpl, inbound);
    }

    String storeOperationalTemplate(
            OPERATIONALTEMPLATE template,
            boolean allowTemplateOverwrite,
            boolean allowUsedTemplateOverwrite,
            boolean updateOnly) {

        validateTemplate(template);
        TemplateWithDetails templateData = getTemplateFields(template);

        return storeOperationalTemplate(templateData, allowTemplateOverwrite, allowUsedTemplateOverwrite, updateOnly);
    }

    String storeOperationalTemplate(
            TemplateWithDetails templateData,
            boolean allowTemplateOverwrite,
            boolean allowUsedTemplateOverwrite,
            boolean updateOnly) {

        String templateId = templateData.meta().templateId();

        UUID existingTid = findUuidByTemplateId(templateId);

        if (updateOnly && existingTid == null) {
            throw templateNotFound(templateId);
        }

        TemplateWithDetails templateMetaData;
        boolean performUpdate = existingTid != null;
        if (performUpdate) {
            if (!allowTemplateOverwrite) {
                throw new StateConflictException(
                        "Operational template with this template ID already exists: " + templateId);
            }
            if (templateStoreRepository.isTemplateUsed(existingTid)) {
                if (allowUsedTemplateOverwrite) {
                    log.warn(
                            "Updating template {} that is in use by at least one composition because {} is enabled",
                            templateData.meta().templateId(),
                            PROP_ALLOW_TEMPLATE_OVERWRITE);
                } else {
                    throw new UnprocessableEntityException(
                            "Cannot update template %s since it is used by at least one composition"
                                    .formatted(templateId));
                }
            }
            templateMetaData = templateStoreRepository.update(templateData);
            cacheHelper.invalidateCaches(templateId, templateMetaData.meta().id());
        } else {
            templateMetaData = templateStoreRepository.store(templateData);
        }

        log.debug("Updating WebTemplate cache for template: {}", templateId);
        addWebTemplateToCache(templateMetaData, true);

        return templateId;
    }

    /**
     *
     * @param operationaltemplate
     * @param inbound  for exception handling when from DB: illegal state, when from api: illegal argument
     * @return
     */
    private static WebTemplate buildWebTemplate(OPERATIONALTEMPLATE operationaltemplate, boolean inbound) {
        try {
            return new OPTParser(operationaltemplate).parse();
        } catch (SdkException | NoSuchElementException | IllegalArgumentException | IllegalStateException e) {
            String message = "Invalid template: %s".formatted(e.getMessage());
            if (inbound) {
                throw new IllegalArgumentException(message, e);
            } else {
                throw new InternalServerException(message, e);
            }
        }
    }

    @Override
    public Collection<TemplateDetails> findAllTemplates() {
        return cacheHelper.findAllTemplates(templateStoreRepository::findAllTemplates);
    }

    /**
     *
     * @param templateId
     * @return
     * @throws ObjectNotFoundException if the template is missing
     * @throws InternalServerException if the OPT cannot be parsed
     */
    @Override
    public WebTemplate getInternalTemplate(String templateId) {
        try {
            return cacheHelper.getInternalTemplate(templateId, tid -> {
                log.info("Updating WebTemplate cache for template: {}", tid);
                return templateStoreRepository.findByTemplateIds(tid).stream()
                        .findFirst()
                        .map(TemplateWithDetails::operationalTemplate)
                        .map(t -> {
                            OPERATIONALTEMPLATE operationaltemplate;
                            try {
                                operationaltemplate = TemplateService.buildOperationalTemplate(t);
                            } catch (XmlException e) {
                                throw new InternalServerException("Cannot process template: " + e.getMessage(), e);
                            }
                            return buildWebTemplate(operationaltemplate, false);
                        })
                        .orElseThrow(() -> templateNotFound(templateId));
            });
        } catch (Cache.ValueRetrievalException ex) {
            // unwrap exception
            throw (RuntimeException) ex.getCause();
        }
    }

    private static @NonNull ObjectNotFoundException templateNotFound(String templateId) {
        return new ObjectNotFoundException(
                "TEMPLATE", "Template with template_id '%s' does not exist".formatted(templateId));
    }

    @Override
    public WebTemplate findWebTemplate(String templateId) {
        return new Filter().filter(this.getInternalTemplate(templateId));
    }
    /**
     * retrieve an operational template document
     *
     * @param templateId the template_id of the operational template
     * @return String representation of an OPERATIONALTEMPLATE
     * @throws ObjectNotFoundException if the template is missing
     */
    @Override
    public String findOperationalTemplate(String templateId) throws ObjectNotFoundException {
        log.trace("retrieveOperationalTemplate({})", templateId);
        try {
            return cacheHelper.retrieveOperationalTemplate(
                    templateId,
                    tid -> templateStoreRepository.findByTemplateIds(tid).stream()
                            .findFirst()
                            .map(TemplateWithDetails::operationalTemplate)
                            .orElseThrow(() -> templateNotFound(templateId)));
        } catch (Cache.ValueRetrievalException ex) {
            // unwrap exception
            throw (RuntimeException) ex.getCause();
        }
    }

    private static TemplateWithDetails getTemplateFields(OPERATIONALTEMPLATE template) {
        String templateId = TemplateUtils.getTemplateId(template);

        XmlOptions opts = new XmlOptions();
        opts.setSaveSyntheticDocumentElement(new QName("http://schemas.openehr.org/v1", "template"));
        template.xmlText(opts);

        String concept = template.getConcept();
        String archetypeId = template.getDefinition().getArchetypeId().getValue();
        return new TemplateWithDetails(
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

        var webTemplate = buildWebTemplate(template, true);
        if (!TemplateUtils.isSupported(webTemplate)) {
            throw new IllegalArgumentException(MessageFormat.format(
                    "The supplied template is not supported (unsupported types: {0})",
                    String.join(",", TemplateUtils.UNSUPPORTED_RM_TYPES)));
        }
    }

    @Override
    @Transactional
    public String create(OPERATIONALTEMPLATE template) {
        return storeOperationalTemplate(template, allowTemplateOverwrite, allowTemplateOverwrite, false);
    }

    @Override
    public String findTemplateIdByUuid(UUID uuid) {
        return cacheHelper.findTemplateIdByUuid(uuid, templateStoreRepository::findTemplateIdByUuid);
    }

    @Override
    public UUID findUuidByTemplateId(String templateId) {
        return cacheHelper.findUuidByTemplateId(templateId, templateStoreRepository::findUuidByTemplateId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public String adminUpdateTemplate(OPERATIONALTEMPLATE template) {
        return storeOperationalTemplate(template, true, allowTemplateOverwrite, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void adminDeleteTemplate(String templateId) {
        UUID templateUuid =
                Optional.of(templateId).map(this::findUuidByTemplateId).orElseThrow(() -> templateNotFound(templateId));
        templateStoreRepository.deleteTemplate(templateUuid);
        cacheHelper.invalidateCaches(templateId, templateUuid);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public int adminDeleteAllTemplates() {
        int deleted = templateStoreRepository.deleteAllTemplates();
        cacheHelper.clearCaches();
        return deleted;
    }

    @Override
    public Composition buildExample(String templateId) {
        WebTemplate webTemplate = getInternalTemplate(templateId);
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
}
