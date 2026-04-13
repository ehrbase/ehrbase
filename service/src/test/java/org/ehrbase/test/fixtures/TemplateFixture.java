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
package org.ehrbase.test.fixtures;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.apache.xmlbeans.XmlException;
import org.ehrbase.api.service.TemplateService;
import org.ehrbase.openehr.sdk.test_data.operationaltemplate.OperationalTemplateTestData;
import org.ehrbase.service.TemplateServiceImp.TemplateWithDetails;
import org.ehrbase.util.TemplateUtils;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;

public class TemplateFixture {

    public record TestTemplate(
            String templateId, OPERATIONALTEMPLATE operationaltemplate, TemplateWithDetails metaData) {}

    public static TestTemplate fixtureTemplate(OperationalTemplateTestData operationalTemplateTestData) {
        return fixtureTemplate(operationalTemplateTestData, UUID.fromString("b65165e8-b4a6-4c23-84a0-ec58cfb481c1"));
    }

    public static TestTemplate fixtureTemplate(
            OperationalTemplateTestData operationalTemplateTestData, UUID internalUUID) {
        String templateDocument;
        try {
            templateDocument = IOUtils.toString(operationalTemplateTestData.getStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        OPERATIONALTEMPLATE operationaltemplate;
        try {
            operationaltemplate = TemplateService.buildOperationalTemplate(templateDocument);
        } catch (XmlException e) {
            throw new RuntimeException(e);
        }

        TemplateWithDetails metaData = new TemplateWithDetails(
                templateDocument,
                new TemplateService.TemplateDetails(
                        internalUUID,
                        TemplateUtils.getTemplateId(operationaltemplate),
                        OffsetDateTime.parse("2020-10-10T12:00:00Z"),
                        operationaltemplate.getConcept(),
                        operationaltemplate.getDefinition().getArchetypeId().getValue()));

        return new TestTemplate(operationalTemplateTestData.getTemplateId(), operationaltemplate, metaData);
    }
}
