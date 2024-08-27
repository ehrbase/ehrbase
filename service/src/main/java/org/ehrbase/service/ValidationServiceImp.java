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

import com.nedap.archie.query.RMPathQuery;
import com.nedap.archie.rm.composition.Composition;
import com.nedap.archie.rm.generic.PartyProxy;
import com.nedap.archie.rm.support.identification.PartyRef;
import com.nedap.archie.rmobjectvalidator.APathQueryCache;
import com.nedap.archie.rmobjectvalidator.RMObjectValidationMessage;
import com.nedap.archie.rmobjectvalidator.RMObjectValidationMessageType;
import com.nedap.archie.rmobjectvalidator.RMObjectValidator;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.apache.commons.collections4.CollectionUtils;
import org.ehrbase.api.definitions.ServerConfig;
import org.ehrbase.api.dto.EhrStatusDto;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.UnprocessableEntityException;
import org.ehrbase.api.exception.ValidationException;
import org.ehrbase.api.service.ValidationService;
import org.ehrbase.openehr.sdk.response.dto.ContributionCreateDto;
import org.ehrbase.openehr.sdk.terminology.openehr.TerminologyService;
import org.ehrbase.openehr.sdk.validation.CompositionValidator;
import org.ehrbase.openehr.sdk.validation.ConstraintViolation;
import org.ehrbase.openehr.sdk.validation.ConstraintViolationException;
import org.ehrbase.openehr.sdk.validation.terminology.ExternalTerminologyValidation;
import org.ehrbase.openehr.sdk.validation.terminology.ItemStructureVisitor;
import org.ehrbase.openehr.sdk.webtemplate.model.WebTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * {@link ValidationService} implementation.
 */
@Service
public class ValidationServiceImp implements ValidationService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final Pattern NAMESPACE_PATTERN = Pattern.compile("[a-zA-Z][a-zA-Z0-9-_:/&+?]*");

    private final KnowledgeCacheServiceImp knowledgeCacheService;

    private final TerminologyService terminologyService;

    private final ThreadLocal<CompositionValidator> compositionValidator;

    private final Map<String, RMPathQuery> rmPathQueryCache = new ConcurrentHashMap<>();

    public ValidationServiceImp(
            KnowledgeCacheServiceImp knowledgeCacheService,
            TerminologyService terminologyService,
            ServerConfig serverConfig,
            ObjectProvider<ExternalTerminologyValidation> objectProvider,
            @Value("${cache.validation.useSharedRMPathQueryCache:true}") boolean sharedAqlQueryCache) {
        this.knowledgeCacheService = knowledgeCacheService;
        this.terminologyService = terminologyService;

        boolean disableStrictValidation = serverConfig.isDisableStrictValidation();
        if (disableStrictValidation) {
            logger.warn("Disabling strict invariant validation. Caution is advised.");
        }

        APathQueryCache delegator;
        if (sharedAqlQueryCache) {
            delegator = new APathQueryCache() {
                @Override
                public RMPathQuery getApathQuery(String query) {
                    return rmPathQueryCache.computeIfAbsent(query, RMPathQuery::new);
                }
            };
        } else {
            logger.warn("shared RMPathQueryCache is disabled");
            delegator = null;
        }
        compositionValidator = ThreadLocal.withInitial(
                () -> createCompositionValidator(objectProvider, disableStrictValidation, delegator));
    }

    private static CompositionValidator createCompositionValidator(
            ObjectProvider<ExternalTerminologyValidation> objectProvider,
            boolean disableStrictValidation,
            APathQueryCache delegator) {
        CompositionValidator validator = new CompositionValidator();
        objectProvider.ifAvailable(validator::setExternalTerminologyValidation);
        validator.setRunInvariantChecks(!disableStrictValidation);
        setSharedAPathQueryCache(validator, delegator);
        return validator;
    }

    private static void setSharedAPathQueryCache(CompositionValidator validator, APathQueryCache delegator) {
        if (delegator == null) {
            return;
        }
        try {
            // as RMObjectValidator.queryCache is hard-coded, it is replaced via reflection
            Field queryCacheField = RMObjectValidator.class.getDeclaredField("queryCache");
            queryCacheField.setAccessible(true);
            queryCacheField.set(validator.getRmObjectValidator(), delegator);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new InternalServerException("Failed to inject shared RMPathQuery cache", e);
        }
    }

    @Override
    public void check(Composition composition) {

        // check if this composition is valid for processing
        compositionMandatoryProperty(composition.getName(), "name");
        compositionMandatoryProperty(composition.getArchetypeNodeId(), "archetype_node_id");
        compositionMandatoryProperty(composition.getLanguage(), "language");
        compositionMandatoryProperty(composition.getCategory(), "category");
        compositionMandatoryProperty(composition.getComposer(), "composer");
        compositionMandatoryProperty(composition.getArchetypeDetails(), "archetype details");
        compositionMandatoryProperty(
                composition.getArchetypeDetails().getTemplateId(), "archetype details/template_id");

        String templateID = composition.getArchetypeDetails().getTemplateId().getValue();
        check(templateID, composition);

        logger.debug("Validated Composition against WebTemplate[{}]", templateID);
    }

    private void check(String templateID, Composition composition) {
        WebTemplate webTemplate;
        try {
            webTemplate = knowledgeCacheService.getQueryOptMetaData(templateID);
        } catch (IllegalArgumentException e) {
            throw new UnprocessableEntityException(e.getMessage());
        }

        // Validate the composition based on WebTemplate
        List<ConstraintViolation> violations = compositionValidator.get().validate(composition, webTemplate);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }

        // check code phrases against terminologies
        try {
            ItemStructureVisitor itemStructureVisitor = new ItemStructureVisitor(terminologyService);
            itemStructureVisitor.validate(composition);
        } catch (ReflectiveOperationException e) {
            throw new InternalServerException(e);
        }
    }

    private static void compositionMandatoryProperty(Object value, String attribute) {
        if (value == null) {
            throw new ValidationException("Composition missing mandatory attribute: %s".formatted(attribute));
        }
    }

    @Override
    public void check(@Nonnull EhrStatusDto ehrStatus) {

        // second, additional specific checks and other mandatory attributes
        RMObjectValidator rmObjectValidator = compositionValidator.get().getRmObjectValidator();
        List<RMObjectValidationMessage> validationIssues = Stream.of(
                        // RM-DTO required
                        require(ehrStatus.type(), "/subject", "subject", ehrStatus.subject()),
                        require(ehrStatus.type(), "/is_queryable", "is_queryable", ehrStatus.isQueryable()),
                        require(ehrStatus.type(), "/is_modifiable", "is_modifiable", ehrStatus.isModifiable()),
                        // rm validation
                        validate(rmObjectValidator, "/uid", ehrStatus.uid()),
                        validate(rmObjectValidator, "/name", ehrStatus.name()),
                        validate(rmObjectValidator, "/subject", ehrStatus.subject()),
                        validate(rmObjectValidator, "/archetype_details", ehrStatus.archetypeDetails()),
                        validate(rmObjectValidator, "/feeder_audit", ehrStatus.feederAudit()),
                        validate(rmObjectValidator, "/other_details", ehrStatus.otherDetails()),
                        // additional checks
                        matches(
                                ehrStatus.type(),
                                "/subject/external_ref/namespace",
                                "namespace",
                                NAMESPACE_PATTERN,
                                Optional.ofNullable(ehrStatus.subject())
                                        .map(PartyProxy::getExternalRef)
                                        .map(PartyRef::getNamespace)))
                .flatMap(Collection::stream)
                .toList();

        if (!validationIssues.isEmpty()) {
            throw new ValidationException(
                    validationIssues.stream().map(Objects::toString).collect(Collectors.joining("\n")));
        }
    }

    @Override
    public void check(ContributionCreateDto contribution) {

        // first, check the built EhrStatus using the general Archie RM-Validator
        RMObjectValidator rmObjectValidator = compositionValidator.get().getRmObjectValidator();

        // UID does not have to be validated

        List<RMObjectValidationMessage> messages = new ArrayList<>();

        // validate audit details
        Optional.ofNullable(contribution.getAudit()).ifPresent(ad -> {
            // audit/time_committed must be null
            reject(messages, "/audit/time_committed", "time_committed", ad.getTimeCommitted());
            rmObjectValidator.validate(ad).stream()
                    .filter(m -> !m.getPath().equals("/time_committed"))
                    .forEach(messages::add);
        });

        if (CollectionUtils.isEmpty(contribution.getVersions())) {
            // reject contribution without versions
            messages.add(new RMObjectValidationMessage(
                    "/versions",
                    null,
                    null,
                    null,
                    "Versions must not be empty",
                    RMObjectValidationMessageType.CARDINALITY_MISMATCH));
        } else {
            // validate versions (without data)
            contribution.getVersions().stream()
                    .map(v -> {
                        // version/contribution must be null
                        reject(messages, "/version/contribution", "contribution", v.getContribution());
                        return v;
                    })
                    .map(rmObjectValidator::validate)
                    .flatMap(List::stream)
                    .filter(m -> {
                        String path = m.getPath();
                        return !(
                        // versions/commit_audit/time_committed must be null
                        path.equals("/commit_audit/time_committed")
                                ||
                                // versions/data must not be validated here
                                // TODO performance: skip validation
                                path.startsWith("/data")
                                ||
                                // version/contribution must be null
                                path.startsWith("/contribution")
                                ||
                                // versions/uid may be null
                                (path.startsWith("/uid") && m.getType() == RMObjectValidationMessageType.REQUIRED));
                    })
                    .forEach(messages::add);
        }

        if (!messages.isEmpty()) {
            String messageStr = messages.stream().map(Object::toString).collect(Collectors.joining("\n"));
            throw new ValidationException(messageStr);
        }
    }

    private static List<RMObjectValidationMessage> validate(
            RMObjectValidator rmObjectValidator, String path, Object value) {
        return Optional.ofNullable(value).map(rmObjectValidator::validate).stream()
                .flatMap(Collection::stream)
                .map(msg -> new RMObjectValidationMessage(
                        "%s%s".formatted(path, msg.getPath()),
                        msg.getArchetypeId(),
                        msg.getArchetypePath(),
                        msg.getHumanReadableArchetypePath(),
                        msg.getMessage(),
                        msg.getType()))
                .toList();
    }

    private static List<RMObjectValidationMessage> require(String type, String path, String attr, Object value) {
        if (value == null) {
            return List.of(new RMObjectValidationMessage(
                    path,
                    null,
                    null,
                    path,
                    "Attribute %s of class %s does not match existence 1..1".formatted(attr, type),
                    RMObjectValidationMessageType.REQUIRED));
        }
        return List.of();
    }

    private static List<RMObjectValidationMessage> matches(
            String type, String path, String attr, Pattern pattern, Optional<String> value) {
        boolean matches = value.map(v -> pattern.matcher(v).matches()).orElse(true);
        if (!matches) {
            return List.of(new RMObjectValidationMessage(
                    path,
                    null,
                    null,
                    path,
                    "Invariant %s of class %s does not match pattern [%s]".formatted(attr, type, pattern.pattern()),
                    RMObjectValidationMessageType.INVARIANT_ERROR));
        }
        return List.of();
    }

    private static void reject(List<RMObjectValidationMessage> messages, String path, String attr, Object mustBeNull) {
        if (mustBeNull != null) {
            messages.add(new RMObjectValidationMessage(
                    path,
                    null,
                    null,
                    path,
                    "Attribute %s must not be set".formatted(attr),
                    RMObjectValidationMessageType.CARDINALITY_MISMATCH));
        }
    }
}
