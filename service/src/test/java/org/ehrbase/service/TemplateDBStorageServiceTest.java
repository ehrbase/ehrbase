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
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import org.ehrbase.api.exception.UnprocessableEntityException;
import org.ehrbase.api.knowledge.TemplateMetaData;
import org.ehrbase.openehr.sdk.test_data.operationaltemplate.OperationalTemplateTestData;
import org.ehrbase.repository.CompositionRepository;
import org.ehrbase.repository.TemplateStoreRepository;
import org.ehrbase.test.fixtures.TemplateFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;

class TemplateDBStorageServiceTest {

    private final CompositionRepository mockCompositionRepository = mock();
    private final TemplateStoreRepository mockTemplateStoreRepository = mock();

    @BeforeEach
    void setUp() {
        Mockito.reset(mockCompositionRepository, mockTemplateStoreRepository);
    }

    private TemplateDBStorageService service(boolean allowTemplateOverwrite) {
        return new TemplateDBStorageService(
                mockCompositionRepository, mockTemplateStoreRepository, allowTemplateOverwrite);
    }

    private TemplateFixture.TestTemplate testTemplate() {
        return TemplateFixture.fixtureTemplate(OperationalTemplateTestData.MINIMAL_INSTRUCTION);
    }

    @Test
    void storeTemplateNoExist() {

        TemplateFixture.TestTemplate testTemplate = testTemplate();
        TemplateMetaData expectedTemplateMetaData = new TemplateMetaData();

        // mock not exist
        doReturn(Optional.empty()).when(mockTemplateStoreRepository).findUuidByTemplateId(testTemplate.templateId());
        doReturn(expectedTemplateMetaData).when(mockTemplateStoreRepository).store(testTemplate.operationaltemplate());

        TemplateMetaData resultMetaData = service(false).storeTemplate(testTemplate.operationaltemplate());

        verify(mockTemplateStoreRepository, times(1)).store(testTemplate.operationaltemplate());
        assertThat(expectedTemplateMetaData).isSameAs(resultMetaData);
    }

    @Test
    void storeTemplateExistReplaceNotUsed() {

        TemplateFixture.TestTemplate testTemplate = testTemplate();
        TemplateMetaData expectedTemplateMetaData = new TemplateMetaData();

        // mock template exist
        doReturn(Optional.of(testTemplate.metaData().getInternalId()))
                .when(mockTemplateStoreRepository)
                .findUuidByTemplateId(testTemplate.templateId());
        doReturn(expectedTemplateMetaData).when(mockTemplateStoreRepository).update(testTemplate.operationaltemplate());

        TemplateMetaData resultMetaData = service(false).storeTemplate(testTemplate.operationaltemplate());

        verify(mockTemplateStoreRepository, times(1)).update(same(testTemplate.operationaltemplate()));
        assertThat(expectedTemplateMetaData).isSameAs(resultMetaData);
    }

    @Test
    void storeTemplateExistCouldReplaceWhenInUse() {

        TemplateFixture.TestTemplate testTemplate = testTemplate();

        // mock template exist and in use
        doReturn(Optional.of(testTemplate.metaData().getInternalId()))
                .when(mockTemplateStoreRepository)
                .findUuidByTemplateId(testTemplate.templateId());
        doReturn(true).when(mockCompositionRepository).isTemplateUsed(testTemplate.templateId());

        TemplateDBStorageService service = service(false);
        OPERATIONALTEMPLATE operationaltemplate = testTemplate.operationaltemplate();
        assertThatThrownBy(() -> service.storeTemplate(operationaltemplate))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessage("Cannot update template %s since it is used by at least one composition"
                        .formatted(testTemplate.templateId()));
    }

    @Test
    void storeTemplateExistAllowOverwriteReplaceWhenInUse() {

        TemplateFixture.TestTemplate testTemplate = testTemplate();
        TemplateMetaData expectedTemplateMetaData = new TemplateMetaData();

        // mock template exist and in use
        doReturn(Optional.of(testTemplate.metaData().getInternalId()))
                .when(mockTemplateStoreRepository)
                .findUuidByTemplateId(testTemplate.templateId());
        doReturn(true).when(mockCompositionRepository).isTemplateUsed(testTemplate.templateId());
        // mock update result
        doReturn(expectedTemplateMetaData).when(mockTemplateStoreRepository).update(testTemplate.operationaltemplate());

        TemplateMetaData resultMetaData = service(true).storeTemplate(testTemplate.operationaltemplate());

        verify(mockTemplateStoreRepository, times(1)).update(same(testTemplate.operationaltemplate()));
        assertThat(expectedTemplateMetaData).isSameAs(resultMetaData);
    }
}
