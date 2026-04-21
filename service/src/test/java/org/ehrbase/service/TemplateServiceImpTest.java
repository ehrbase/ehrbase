/*
 * Copyright (c) 2019-2024 vitasystems GmbH.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.exception.StateConflictException;
import org.ehrbase.api.exception.UnprocessableEntityException;
import org.ehrbase.api.service.TemplateService;
import org.ehrbase.cache.CacheProperties;
import org.ehrbase.cache.CacheProvider;
import org.ehrbase.cache.CacheProviderImp;
import org.ehrbase.openehr.sdk.test_data.operationaltemplate.OperationalTemplateTestData;
import org.ehrbase.repository.TemplateStoreRepository;
import org.ehrbase.test.fixtures.TemplateFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;

class TemplateServiceImpTest {

    private final TemplateStoreRepository mockTemplateStoreRepository = mock();
    private SimpleCacheManager cacheManager;

    @BeforeEach
    void setUp() {
        Mockito.reset(mockTemplateStoreRepository);

        cacheManager = new SimpleCacheManager();

        cacheManager.setCaches(List.of(
                new ConcurrentMapCache(CacheProvider.TEMPLATE_ID_UUID_CACHE.name()),
                new ConcurrentMapCache(CacheProvider.TEMPLATE_UUID_ID_CACHE.name()),
                new ConcurrentMapCache(CacheProvider.TEMPLATE_OPT_CACHE.name()),
                new ConcurrentMapCache(CacheProvider.TEMPLATE_CACHE.name()),
                new ConcurrentMapCache(CacheProvider.TEMPLATE_LIST_CACHE.name())));
        cacheManager.initializeCaches();
    }

    private TemplateServiceImp service() {
        return service(false);
    }

    private TemplateServiceImp service(boolean allowTemplateOverwrite) {
        CacheProperties cacheProperties = new CacheProperties();
        cacheProperties.setTemplateInitOnStartup("false");
        return new TemplateServiceImp(
                mockTemplateStoreRepository,
                new CacheProviderImp(cacheManager),
                cacheProperties,
                allowTemplateOverwrite);
    }

    private TemplateServiceImp serviceWithInitOnStartup(String initOnStartup) {
        CacheProperties cacheProperties = new CacheProperties();
        cacheProperties.setTemplateInitOnStartup(initOnStartup);
        return new TemplateServiceImp(
                mockTemplateStoreRepository, new CacheProviderImp(cacheManager), cacheProperties, false);
    }

    // ---------------------------------------------------------------------------
    // Constructor / init()
    // ---------------------------------------------------------------------------

    @ParameterizedTest
    @ValueSource(strings = {"false", ""})
    void initDoesNothingWhenInitOnStartupIsDisabled(String initOnStartup) {
        TemplateServiceImp svc = serviceWithInitOnStartup(initOnStartup);
        svc.init();
        verify(mockTemplateStoreRepository, never()).findAllTemplates();
    }

    @ParameterizedTest
    @ValueSource(strings = {"true", "ALL"})
    void initLoadsAllTemplatesWhenInitOnStartupIsEnabled(String initOnStartup) {
        TemplateFixture.TestTemplate testTemplate = parseAndMock(OperationalTemplateTestData.MINIMAL_ACTION);

        Mockito.when(mockTemplateStoreRepository.findAllTemplates())
                .thenReturn(List.of(testTemplate.metaData().meta()));
        Mockito.when(mockTemplateStoreRepository.findByTemplateIds(testTemplate.templateId()))
                .thenReturn(List.of(testTemplate.metaData()));

        serviceWithInitOnStartup(initOnStartup).init();

        verify(mockTemplateStoreRepository, times(1)).findAllTemplates();
    }

    @Test
    void initLoadsSpecificTemplatesWhenInitOnStartupIsCommaSeparatedList() {
        TemplateFixture.TestTemplate t1 = parseAndMock(OperationalTemplateTestData.MINIMAL_ACTION);
        TemplateFixture.TestTemplate t2 = parseAndMock(OperationalTemplateTestData.MINIMAL_EVALUATION);
        TemplateFixture.TestTemplate t3 = parseAndMock(OperationalTemplateTestData.MINIMAL_ACTION_2);

        Mockito.when(mockTemplateStoreRepository.findAllTemplates())
                .thenReturn(List.of(
                        t1.metaData().meta(),
                        t2.metaData().meta(),
                        t3.metaData().meta()));
        Mockito.when(mockTemplateStoreRepository.findByTemplateIds(t1.templateId(), t2.templateId()))
                .thenReturn(List.of(t1.metaData(), t2.metaData()));

        TemplateServiceImp svc = serviceWithInitOnStartup(t1.templateId() + ", " + t2.templateId());
        svc.init();

        verify(mockTemplateStoreRepository, times(1)).findAllTemplates();
        verify(mockTemplateStoreRepository, times(1)).findByTemplateIds(t1.templateId(), t2.templateId());
    }

    // ---------------------------------------------------------------------------
    // storeOperationalTemplate()
    // ---------------------------------------------------------------------------

    @Test
    void storeOperationalTemplate() {
        TemplateFixture.TestTemplate testTemplate = parseAndMock(OperationalTemplateTestData.MINIMAL_ACTION);
        String templateId = service().storeOperationalTemplate(testTemplate.metaData(), false, false, false);

        verify(mockTemplateStoreRepository, times(1)).store(testTemplate.metaData());
        assertThat(templateId).isNotNull().isEqualTo(testTemplate.templateId());
    }

    @Test
    void storeOperationalTemplateRejectOverwrite() {
        TemplateFixture.TestTemplate testTemplate = parseAndMock(OperationalTemplateTestData.MINIMAL_ACTION);

        Mockito.when(mockTemplateStoreRepository.findUuidByTemplateId(testTemplate.templateId()))
                .thenReturn(Optional.of(UUID.randomUUID()));

        TemplateServiceImp service = service();
        TemplateServiceImp.TemplateWithDetails templateData = testTemplate.metaData();
        assertThatThrownBy(() -> service.storeOperationalTemplate(templateData, false, false, false))
                .isInstanceOf(StateConflictException.class)
                .hasMessage("Operational template with this template ID already exists: %s"
                        .formatted(testTemplate.templateId()));

        verify(mockTemplateStoreRepository, times(1)).findUuidByTemplateId(anyString());
    }

    @Test
    void storeOperationalTemplateAllowOverwrite() {
        TemplateFixture.TestTemplate testTemplate = parseAndMock(OperationalTemplateTestData.MINIMAL_ACTION);

        Mockito.when(mockTemplateStoreRepository.findByTemplateIds(testTemplate.templateId()))
                .thenReturn(List.of(testTemplate.metaData()));

        TemplateServiceImp service = spy(service());
        String templateId = service.storeOperationalTemplate(testTemplate.metaData(), true, true, false);

        assertThat(templateId).isNotNull().isEqualTo(testTemplate.templateId());
    }

    @Test
    void storeOperationalTemplateUpdateThrowsWhenTemplateDoesNotExist() {
        TemplateFixture.TestTemplate testTemplate = parseAndMock(OperationalTemplateTestData.MINIMAL_ACTION);

        Mockito.when(mockTemplateStoreRepository.findUuidByTemplateId(testTemplate.templateId()))
                .thenReturn(Optional.empty());

        TemplateServiceImp service = service();
        TemplateServiceImp.TemplateWithDetails templateData = testTemplate.metaData();
        assertThatThrownBy(() -> service.storeOperationalTemplate(templateData, true, true, true))
                .isInstanceOf(ObjectNotFoundException.class)
                .hasMessageContaining(testTemplate.templateId());
    }

    @Test
    void storeOperationalTemplateThrowsOnUpdateWhenOverwriteNotAllowed() {
        TemplateFixture.TestTemplate testTemplate = parseAndMock(OperationalTemplateTestData.MINIMAL_ACTION);
        UUID existingUuid = UUID.randomUUID();

        Mockito.when(mockTemplateStoreRepository.findUuidByTemplateId(testTemplate.templateId()))
                .thenReturn(Optional.of(existingUuid));
        Mockito.when(mockTemplateStoreRepository.isTemplateUsed(existingUuid)).thenReturn(true);

        TemplateServiceImp service = service();
        TemplateServiceImp.TemplateWithDetails templateData = testTemplate.metaData();
        assertThatThrownBy(() -> service.storeOperationalTemplate(templateData, true, false, false))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessageContaining(testTemplate.templateId());
    }

    @Test
    void storeOperationalTemplateAllowsOverwriteOfUsedTemplateWhenFlagSet() {
        TemplateFixture.TestTemplate testTemplate = parseAndMock(OperationalTemplateTestData.MINIMAL_ACTION);
        UUID existingUuid = UUID.randomUUID();

        Mockito.when(mockTemplateStoreRepository.findUuidByTemplateId(testTemplate.templateId()))
                .thenReturn(Optional.of(existingUuid));
        Mockito.when(mockTemplateStoreRepository.isTemplateUsed(existingUuid)).thenReturn(true);
        Mockito.when(mockTemplateStoreRepository.findByTemplateIds(testTemplate.templateId()))
                .thenReturn(List.of(testTemplate.metaData()));

        String templateId = service().storeOperationalTemplate(testTemplate.metaData(), true, true, false);

        assertThat(templateId).isEqualTo(testTemplate.templateId());
        verify(mockTemplateStoreRepository, times(1)).update(testTemplate.metaData());
    }

    @Test
    void storeOperationalTemplateFromNullOPTThrows() {
        TemplateServiceImp service = service();
        assertThatThrownBy(() -> service.storeOperationalTemplate((OPERATIONALTEMPLATE) null, false, false, false))
                .isInstanceOf(InvalidApiParameterException.class)
                .hasMessageContaining("Could not parse input template");
    }

    // ---------------------------------------------------------------------------
    // create()
    // ---------------------------------------------------------------------------

    @Test
    void createStoresTemplate() {
        TemplateFixture.TestTemplate testTemplate = parseAndMock(OperationalTemplateTestData.MINIMAL_ACTION);

        String templateId = service().create(testTemplate.operationaltemplate());

        assertThat(templateId).isEqualTo(testTemplate.templateId());
        verify(mockTemplateStoreRepository, times(1)).store(any());
    }

    @Test
    void createWithAllowOverwriteUpdatesExistingTemplate() {
        TemplateFixture.TestTemplate testTemplate = parseAndMock(OperationalTemplateTestData.MINIMAL_ACTION);
        UUID existingUuid = UUID.randomUUID();

        Mockito.when(mockTemplateStoreRepository.findUuidByTemplateId(testTemplate.templateId()))
                .thenReturn(Optional.of(existingUuid));
        Mockito.when(mockTemplateStoreRepository.isTemplateUsed(existingUuid)).thenReturn(false);
        Mockito.when(mockTemplateStoreRepository.findByTemplateIds(testTemplate.templateId()))
                .thenReturn(List.of(testTemplate.metaData()));

        String templateId = service(true).create(testTemplate.operationaltemplate());

        assertThat(templateId).isEqualTo(testTemplate.templateId());
        verify(mockTemplateStoreRepository, times(1)).update(any());
    }

    // ---------------------------------------------------------------------------
    // findAllTemplates()
    // ---------------------------------------------------------------------------

    @Test
    void findAllTemplatesReturnsCachedList() {
        TemplateFixture.TestTemplate testTemplate = parseAndMock(OperationalTemplateTestData.MINIMAL_ACTION);
        TemplateService.TemplateDetails details = testTemplate.metaData().meta();

        Mockito.when(mockTemplateStoreRepository.findAllTemplates()).thenReturn(List.of(details));

        TemplateServiceImp service = service();
        var first = service.findAllTemplates();
        var second = service.findAllTemplates();

        assertThat(first).containsExactly(details);
        assertThat(second).containsExactly(details);
        // Repository should only be called once due to caching
        verify(mockTemplateStoreRepository, times(1)).findAllTemplates();
    }

    // ---------------------------------------------------------------------------
    // getInternalTemplate()
    // ---------------------------------------------------------------------------

    @Test
    void getInternalTemplateCachesResult() {
        TemplateFixture.TestTemplate testTemplate = parseAndMock(OperationalTemplateTestData.MINIMAL_ACTION);

        Mockito.when(mockTemplateStoreRepository.findByTemplateIds(testTemplate.templateId()))
                .thenReturn(List.of(testTemplate.metaData()));

        TemplateServiceImp service = service();
        service.getInternalTemplate(testTemplate.templateId());
        service.getInternalTemplate(testTemplate.templateId());

        // Repository should only be called once due to caching
        verify(mockTemplateStoreRepository, times(1)).findByTemplateIds(testTemplate.templateId());
    }

    // ---------------------------------------------------------------------------
    // findOperationalTemplate()
    // ---------------------------------------------------------------------------

    @Test
    void findOperationalTemplateCachesResult() {
        TemplateFixture.TestTemplate testTemplate = parseAndMock(OperationalTemplateTestData.MINIMAL_ACTION);

        Mockito.when(mockTemplateStoreRepository.findByTemplateIds(testTemplate.templateId()))
                .thenReturn(List.of(testTemplate.metaData()));

        TemplateServiceImp service = service();
        service.findOperationalTemplate(testTemplate.templateId());
        service.findOperationalTemplate(testTemplate.templateId());

        verify(mockTemplateStoreRepository, times(1)).findByTemplateIds(testTemplate.templateId());
    }

    // ---------------------------------------------------------------------------
    // adminUpdateTemplate()
    // ---------------------------------------------------------------------------

    @Test
    void adminUpdateTemplateUpdatesExistingTemplate() {
        TemplateFixture.TestTemplate testTemplate = parseAndMock(OperationalTemplateTestData.MINIMAL_ACTION);
        UUID existingUuid = testTemplate.metaData().meta().id();

        Mockito.when(mockTemplateStoreRepository.findUuidByTemplateId(testTemplate.templateId()))
                .thenReturn(Optional.of(existingUuid));
        Mockito.when(mockTemplateStoreRepository.isTemplateUsed(existingUuid)).thenReturn(false);
        Mockito.when(mockTemplateStoreRepository.findByTemplateIds(testTemplate.templateId()))
                .thenReturn(List.of(testTemplate.metaData()));

        String templateId = service().adminUpdateTemplate(testTemplate.operationaltemplate());

        assertThat(templateId).isEqualTo(testTemplate.templateId());
        verify(mockTemplateStoreRepository, times(1)).update(any());
    }

    @Test
    void adminUpdateTemplateThrowsWhenTemplateDoesNotExist() {
        TemplateFixture.TestTemplate testTemplate = parseAndMock(OperationalTemplateTestData.MINIMAL_ACTION);

        Mockito.when(mockTemplateStoreRepository.findUuidByTemplateId(testTemplate.templateId()))
                .thenReturn(Optional.empty());

        TemplateServiceImp service = service();
        assertThatThrownBy(() -> service.adminUpdateTemplate(testTemplate.operationaltemplate()))
                .isInstanceOf(ObjectNotFoundException.class)
                .hasMessageContaining(testTemplate.templateId());
    }

    // ---------------------------------------------------------------------------
    // adminDeleteTemplate()
    // ---------------------------------------------------------------------------

    @Test
    void adminDeleteTemplateDeletesExistingTemplate() {
        TemplateFixture.TestTemplate testTemplate = parseAndMock(OperationalTemplateTestData.MINIMAL_ACTION);
        UUID existingUuid = testTemplate.metaData().meta().id();

        Mockito.when(mockTemplateStoreRepository.findUuidByTemplateId(testTemplate.templateId()))
                .thenReturn(Optional.of(existingUuid), Optional.empty());

        service().adminDeleteTemplate(testTemplate.templateId());
        assertThatThrownBy(() -> service().adminDeleteTemplate(testTemplate.templateId()))
                .isInstanceOf(ObjectNotFoundException.class);

        verify(mockTemplateStoreRepository, times(1)).deleteTemplate(existingUuid);
    }

    // ---------------------------------------------------------------------------
    // buildExample()
    // ---------------------------------------------------------------------------

    @Test
    void buildExampleReturnsComposition() {
        TemplateFixture.TestTemplate testTemplate = parseAndMock(OperationalTemplateTestData.MINIMAL_ACTION);

        Mockito.when(mockTemplateStoreRepository.findByTemplateIds(testTemplate.templateId()))
                .thenReturn(List.of(testTemplate.metaData()));

        var composition = service().buildExample(testTemplate.templateId());

        assertThat(composition).isNotNull();
        assertThat(composition.getTerritory()).isNotNull();
    }

    @Test
    void buildExampleThrowsWhenTemplateNotFound() {
        Mockito.when(mockTemplateStoreRepository.findByTemplateIds(anyString())).thenReturn(List.of());

        TemplateServiceImp service = service();
        assertThatThrownBy(() -> service.buildExample("missing-template")).isInstanceOf(ObjectNotFoundException.class);
    }

    // ---------------------------------------------------------------------------
    // XmlException branches
    // ---------------------------------------------------------------------------

    @Test
    void storeOperationalTemplateThrowsIllegalArgumentWhenStoredXmlIsCorrupt() {
        TemplateFixture.TestTemplate testTemplate = parseAndMock(OperationalTemplateTestData.MINIMAL_ACTION);
        TemplateServiceImp.TemplateWithDetails corrupt = corruptTemplate(testTemplate);

        // store() returns the corrupt record (simulates DB returning bad content)
        Mockito.doReturn(corrupt)
                .when(mockTemplateStoreRepository)
                .store(argThat(t ->
                        t != null && t.meta().templateId().equals(corrupt.meta().templateId())));

        TemplateServiceImp service = service();
        TemplateServiceImp.TemplateWithDetails templateData = testTemplate.metaData();
        assertThatThrownBy(() -> service.storeOperationalTemplate(templateData, false, false, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void initThrowsInternalServerExceptionWhenCachedXmlIsCorrupt() {
        TemplateFixture.TestTemplate testTemplate = parseAndMock(OperationalTemplateTestData.MINIMAL_ACTION);
        TemplateServiceImp.TemplateWithDetails corrupt = corruptTemplate(testTemplate);

        Mockito.when(mockTemplateStoreRepository.findAllTemplates())
                .thenReturn(List.of(testTemplate.metaData().meta()));
        Mockito.when(mockTemplateStoreRepository.findByTemplateIds(testTemplate.templateId()))
                .thenReturn(List.of(corrupt));

        TemplateServiceImp svc = serviceWithInitOnStartup("true");
        assertThatThrownBy(svc::init).isInstanceOf(org.ehrbase.api.exception.InternalServerException.class);
    }

    @Test
    void getInternalTemplateThrowsInternalServerExceptionWhenXmlIsCorrupt() {
        TemplateFixture.TestTemplate testTemplate = parseAndMock(OperationalTemplateTestData.MINIMAL_ACTION);
        TemplateServiceImp.TemplateWithDetails corrupt = corruptTemplate(testTemplate);

        Mockito.when(mockTemplateStoreRepository.findByTemplateIds(testTemplate.templateId()))
                .thenReturn(List.of(corrupt));

        TemplateServiceImp service = service();
        assertThatThrownBy(() -> service.getInternalTemplate(testTemplate.templateId()))
                .isInstanceOf(org.ehrbase.api.exception.InternalServerException.class)
                .hasMessageContaining("Cannot process template:");
    }

    // ObjectNotFoundExceptions

    @ParameterizedTest
    @MethodSource("templateNotFoundCases")
    void throwsObjectNotFoundWhenTemplateDoesNotExist(Consumer<TemplateService> serviceCall) {
        Mockito.when(mockTemplateStoreRepository.findByTemplateIds(anyString())).thenReturn(List.of());
        TemplateServiceImp service = service();
        assertThatThrownBy(() -> serviceCall.accept(service)).isInstanceOf(ObjectNotFoundException.class);
    }

    static Stream<Arguments> templateNotFoundCases() {
        return Stream.of(
                Arguments.of((Consumer<TemplateService>) service -> service.getInternalTemplate("missing")),
                Arguments.of((Consumer<TemplateService>) service -> service.findWebTemplate("missing")),
                Arguments.of((Consumer<TemplateService>) service -> service.findOperationalTemplate("missing")),
                Arguments.of((Consumer<TemplateService>) service -> service.buildExample("missing")));
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private TemplateFixture.TestTemplate parseAndMock(OperationalTemplateTestData operationalTemplateTestData) {
        TemplateFixture.TestTemplate testTemplate = TemplateFixture.fixtureTemplate(operationalTemplateTestData);
        TemplateServiceImp.TemplateWithDetails data = testTemplate.metaData();
        Mockito.doReturn(data)
                .when(mockTemplateStoreRepository)
                .store(argThat(t -> t.meta().templateId().equals(data.meta().templateId())));
        Mockito.doReturn(data)
                .when(mockTemplateStoreRepository)
                .update(argThat(t -> t.meta().templateId().equals(data.meta().templateId())));
        return testTemplate;
    }

    private static TemplateServiceImp.TemplateWithDetails corruptTemplate(TemplateFixture.TestTemplate base) {
        return new TemplateServiceImp.TemplateWithDetails(
                "<<not valid xml>>", base.metaData().meta());
    }
}
