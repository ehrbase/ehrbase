/*
 * Copyright (c) 2020 vitasystems GmbH and Hannover Medical School.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.jooq.dbencoding.templateprovider;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.xmlbeans.XmlException;
import org.ehrbase.openehr.sdk.test_data.operationaltemplate.OperationalTemplateTestData;
import org.ehrbase.openehr.sdk.webtemplate.model.WebTemplate;
import org.ehrbase.openehr.sdk.webtemplate.templateprovider.TemplateProvider;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;
import org.openehr.schemas.v1.TemplateDocument;

public class TestDataTemplateProvider implements TemplateProvider {

    private final Map<String, Optional<WebTemplate>> webTemplateMap = new HashMap<>();

    @Override
    public Optional<OPERATIONALTEMPLATE> find(String templateId) {
        return Optional.ofNullable(OperationalTemplateTestData.findByTemplateId(templateId))
                .map(OperationalTemplateTestData::getStream)
                .map(s -> {
                    try {
                        return TemplateDocument.Factory.parse(s);
                    } catch (XmlException | IOException e) {
                        throw new RuntimeException(e.getMessage(), e);
                    }
                })
                .map(TemplateDocument::getTemplate);
    }

    @Override
    public Optional<WebTemplate> buildIntrospect(String templateId) {

        return webTemplateMap.computeIfAbsent(templateId, TemplateProvider.super::buildIntrospect);
    }

    public List<String> listTemplateIds() {
        return Arrays.stream(OperationalTemplateTestData.values())
                .map(OperationalTemplateTestData::getTemplateId)
                .collect(Collectors.toList());
    }
}
