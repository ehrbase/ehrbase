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
import java.util.Optional;
import java.util.UUID;
import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.exception.StateConflictException;
import org.ehrbase.api.exception.UnprocessableEntityException;
import org.ehrbase.api.service.TemplateService;
import org.ehrbase.cache.CacheProvider;
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
import org.ehrbase.repository.TemplateStoreRepository;
import org.ehrbase.util.TemplateUtils;
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
// There are few instances where a transaction is needed.
// Also, there are few instances where a transaction is not available.
public class TemplateServiceImp implements TemplateService {

    /*
     * CDR-2305 template cache landscape
     * - templateId -> uuid [TEMPLATE_ID_UUID_CACHE, sync TEMPLATE_UUID_ID_CACHE]
     * - uuid -> templateId [sync TEMPLATE_ID_UUID_CACHE, TEMPLATE_UUID_ID_CACHE]
     * - templateId -> WebTemplate [TEMPLATE_CACHE, sync TEMPLATE_ID_UUID_CACHE, sync TEMPLATE_UUID_ID_CACHE]
     * - templateId -> OPT [TEMPLATE_OPT_CACHE]
     * - () -> template details [no dedicated cache?? sync TEMPLATE_ID_UUID_CACHE?, sync TEMPLATE_UUID_ID_CACHE?]
     * - startup: fill WebTemplate cache [TEMPLATE_CACHE, sync TEMPLATE_ID_UUID_CACHE, sync TEMPLATE_UUID_ID_CACHE]
     * - startup: fill id maps? [TEMPLATE_ID_UUID_CACHE, TEMPLATE_UUID_ID_CACHE]
     */

    public record TemplateMetaData(String operationalTemplate, TemplateService.TemplateDetails meta) {}

    public static final String PROP_INIT_TEMPLATE_CACHE = "ehrbase.cache.template-init-on-startup";

    public static final String PROP_ALLOW_TEMPLATE_OVERWRITE = "ehrbase.template.allow-overwrite";

    private static final Logger log = LoggerFactory.getLogger(TemplateServiceImp.class);

    private final TemplateStoreRepository templateStoreRepository;

    private final TemplateCacheHelper cacheHelper;

    private final boolean initTemplateCache;

    private final boolean allowTemplateOverwrite;

    public TemplateServiceImp(
            TemplateStoreRepository templateStoreRepository,
            CacheProvider cacheProvider,
            @Value("${" + PROP_INIT_TEMPLATE_CACHE + ":false}") boolean initTemplateCache,
            @Value("${" + PROP_ALLOW_TEMPLATE_OVERWRITE + ":false}") boolean allowTemplateOverwrite) {
        this.templateStoreRepository = templateStoreRepository;
        this.cacheHelper = new TemplateCacheHelper(cacheProvider);

        this.initTemplateCache = initTemplateCache;

        this.allowTemplateOverwrite = allowTemplateOverwrite;
        if (allowTemplateOverwrite) {
            log.warn(
                    "Template overwriting is enabled, this is not recommended for production use and can lead to unexpected behavior, consider disabling {}",
                    PROP_ALLOW_TEMPLATE_OVERWRITE);
        }
    }

    @PostConstruct
    void init() {
        if (initTemplateCache) {
            // TODO CDR-2305 customizable preload strategy; fill templateId/uuid caches

            // TODO CDR-2305 initTemplateCache lists templateIds
            String[] templateIds = templateStoreRepository.findAllTemplates().stream()
                    .map(TemplateService.TemplateDetails::templateId)
                    .toArray(String[]::new);
            List<TemplateMetaData> templateMetaData = templateStoreRepository.findByTemplateIds(templateIds);
            log.info("Preparing WebTemplate cache for {} templates", templateMetaData.size());
            templateMetaData.forEach(this::addWebTemplateToCache);
        }
    }

    private void addWebTemplateToCache(TemplateMetaData template) {
        OPERATIONALTEMPLATE operationaltemplate;
        try {
            operationaltemplate = TemplateService.buildOperationalTemplate(template.operationalTemplate());
        } catch (XmlException e) {
            throw new InternalServerException(e.getMessage(), e);
        }
        String templateId = TemplateUtils.getTemplateId(operationaltemplate);
        WebTemplate tpl = buildWebTemplate(operationaltemplate);

        cacheHelper.addToCache(
                template.meta().id(), templateId, tpl, template.meta().creationTime());
    }

    String addOperationalTemplate(
            TemplateMetaData templateData, boolean allowTemplateOverwrite, boolean allowUsedTemplateOverwrite) {

        // pre-check: if already existing throw proper exception
        String templateId = templateData.meta().templateId();
        Optional<UUID> existingTid = findUuidByTemplateId(templateId);

        TemplateMetaData templateMetaData;
        boolean mustUpdate = existingTid.isPresent();
        if (mustUpdate) {
            if (!allowTemplateOverwrite) {
                throw new StateConflictException(
                        "Operational template with this template ID already exists: " + templateId);
            }
            if (templateStoreRepository.isTemplateUsed(existingTid.get())) {
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
        addWebTemplateToCache(templateMetaData);

        return templateId;
    }

    private static WebTemplate buildWebTemplate(OPERATIONALTEMPLATE operationaltemplate) {
        try {
            return new OPTParser(operationaltemplate).parse();
            // TODO CDR-2305 when from DB: illegal state, when from api: illegal argument?
            // TODO CDR-2305 consolidate with ::validateTemplate
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(String.format("Invalid template: %s", e.getMessage()));
        }
    }

    @Override
    public Collection<TemplateDetails> findAllTemplates() {
        // TODO CDR-2305 cache? For AQL absolutely; for REST unclear
        return templateStoreRepository.findAllTemplates();
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
            return cacheHelper
                    .getInternalTemplate(templateId, tid -> {
                        log.info("Updating WebTemplate cache for template: {}", tid);
                        return templateStoreRepository.findByTemplateIds(tid).stream()
                                .findFirst()
                                .map(d -> {
                                    OPERATIONALTEMPLATE operationaltemplate;
                                    try {
                                        operationaltemplate =
                                                TemplateService.buildOperationalTemplate(d.operationalTemplate());
                                    } catch (XmlException e) {
                                        throw new InternalServerException(
                                                "Cannot process template: " + e.getMessage(), e);
                                    }
                                    return Pair.of(
                                            buildWebTemplate(operationaltemplate),
                                            d.meta().creationTime());
                                })
                                .orElseThrow(() -> new ObjectNotFoundException(
                                        "template", "Template with the specified id does not exist"));
                    })
                    .getLeft();
        } catch (Cache.ValueRetrievalException ex) {
            // unwrap exception
            throw (RuntimeException) ex.getCause();
        }
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
                            .map(TemplateMetaData::operationalTemplate)
                            .orElseThrow(() -> new ObjectNotFoundException(
                                    "template", "Template with the specified id does not exist")));
        } catch (Cache.ValueRetrievalException ex) {
            // unwrap exception
            throw (RuntimeException) ex.getCause();
        }
    }

    private static TemplateMetaData getTemplateFields(OPERATIONALTEMPLATE template) {
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
    @Transactional
    public String create(OPERATIONALTEMPLATE template) {
        validateTemplate(template);
        TemplateMetaData templateMeta = getTemplateFields(template);
        // TODO CDR-2305 clarify PROP_ALLOW_TEMPLATE_OVERWRITE
        return addOperationalTemplate(templateMeta, allowTemplateOverwrite, allowTemplateOverwrite);
    }

    @Override
    public Optional<String> findTemplateIdByUuid(UUID uuid) {
        return Optional.ofNullable(cacheHelper.findTemplateIdByUuid(
                uuid, u -> templateStoreRepository.findTemplateIdByUuid(u).orElseThrow()));
    }

    @Override
    public Optional<UUID> findUuidByTemplateId(String templateId) {
        return Optional.ofNullable(cacheHelper.findUuidByTemplateId(
                templateId,
                tid -> templateStoreRepository.findUuidByTemplateId(tid).orElseThrow()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void adminDeleteTemplate(String templateId) {
        UUID templateUuid = findUuidByTemplateId(templateId)
                .orElseThrow(() -> new ObjectNotFoundException(
                        "ADMIN TEMPLATE", String.format("Operational template with id %s not found.", templateId)));
        templateStoreRepository.deleteTemplate(templateUuid);
        cacheHelper.invalidateCaches(templateId, templateUuid);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public String adminUpdateTemplate(OPERATIONALTEMPLATE template) {
        validateTemplate(template);
        TemplateMetaData templateMeta = getTemplateFields(template);
        String templateId = templateMeta.meta().templateId();
        findUuidByTemplateId(templateId)
                .orElseThrow(() -> new ObjectNotFoundException(
                        "ADMIN TEMPLATE UPDATE", String.format("Template with id %s does not exist", templateId)));

        return addOperationalTemplate(templateMeta, true, allowTemplateOverwrite);
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
