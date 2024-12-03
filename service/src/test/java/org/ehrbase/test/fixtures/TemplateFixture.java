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
import java.time.OffsetDateTime;
import java.util.UUID;
import org.apache.xmlbeans.XmlException;
import org.ehrbase.api.knowledge.TemplateMetaData;
import org.ehrbase.openehr.sdk.test_data.operationaltemplate.OperationalTemplateTestData;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;
import org.openehr.schemas.v1.TemplateDocument;

public class TemplateFixture {

    public record TestTemplate(String templateId, OPERATIONALTEMPLATE operationaltemplate, TemplateMetaData metaData) {}

    public static TestTemplate fixtureTemplate(OperationalTemplateTestData operationalTemplateTestData) {
        return fixtureTemplate(operationalTemplateTestData, UUID.fromString("b65165e8-b4a6-4c23-84a0-ec58cfb481c1"));
    }

    public static TestTemplate fixtureTemplate(
            OperationalTemplateTestData operationalTemplateTestData, UUID internalUUID) {
        try (var in = operationalTemplateTestData.getStream()) {
            OPERATIONALTEMPLATE template = TemplateDocument.Factory.parse(in).getTemplate();

            TemplateMetaData metaData = new TemplateMetaData();
            metaData.setOperationalTemplate(template);
            metaData.setInternalId(internalUUID);
            metaData.setCreatedOn(OffsetDateTime.parse("2020-10-10T12:00:00Z"));

            return new TestTemplate(operationalTemplateTestData.getTemplateId(), template, metaData);

        } catch (XmlException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
