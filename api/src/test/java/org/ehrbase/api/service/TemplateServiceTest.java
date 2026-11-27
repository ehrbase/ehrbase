/*
 * Copyright (c) 2026 vitasystems GmbH.
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
package org.ehrbase.api.service;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import org.apache.xmlbeans.XmlException;
import org.ehrbase.openehr.sdk.test_data.operationaltemplate.OperationalTemplateTestData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;

class TemplateServiceTest {

    @Test
    void buildOperationalTemplateFromStringWithByteOrderMark() throws IOException, XmlException {
        // this OPT file starts with a UTF-8 byte-order mark, which decodes to U+FEFF
        String content;
        try (InputStream in = OperationalTemplateTestData.MULTI_OCCURRENCE.getStream()) {
            content = new String(in.readAllBytes(), UTF_8);
        }
        assertThat(content).startsWith("\uFEFF");

        OPERATIONALTEMPLATE template = TemplateService.buildOperationalTemplate(content);

        assertThat(template.getTemplateId().getValue()).isEqualTo("ehrbase_multi_occurrence.de.v1");
    }

    @ParameterizedTest
    @EnumSource(OperationalTemplateTestData.class)
    void buildOperationalTemplateFromString(OperationalTemplateTestData testData) throws IOException, XmlException {
        String content;
        try (InputStream in = testData.getStream()) {
            content = new String(in.readAllBytes(), UTF_8);
        }

        OPERATIONALTEMPLATE template = TemplateService.buildOperationalTemplate(content);

        assertThat(template.getTemplateId().getValue()).isEqualTo(testData.getTemplateId());
    }
}
