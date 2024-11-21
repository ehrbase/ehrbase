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
package org.ehrbase.openehr.aqlengine.testdata;

import com.nedap.archie.rm.composition.Composition;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Supplier;
import org.apache.xmlbeans.XmlException;
import org.ehrbase.openehr.sdk.client.openehrclient.OpenEhrClientConfig;
import org.ehrbase.openehr.sdk.client.openehrclient.defaultrestclient.DefaultRestClient;
import org.ehrbase.openehr.sdk.client.openehrclient.defaultrestclient.DefaultRestTemplateEndpoint;
import org.ehrbase.openehr.sdk.test_data.operationaltemplate.OperationalTemplateTestData;
import org.ehrbase.openehr.sdk.webtemplate.model.WebTemplate;
import org.ehrbase.openehr.sdk.webtemplate.templateprovider.TemplateProvider;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;
import org.openehr.schemas.v1.TemplateDocument;

public class TemplateSupport {

    private final Supplier<DefaultRestTemplateEndpoint> endpoint;

    public TemplateSupport(OpenEhrClientConfig cfg) {
        endpoint = () -> new DefaultRestTemplateEndpoint(new DefaultRestClient(cfg, new TemplateProvider() {
            @Override
            public Optional<OPERATIONALTEMPLATE> find(String templateId) {
                try {
                    OPERATIONALTEMPLATE template = TemplateDocument.Factory.parse(
                                    OperationalTemplateTestData.findByTemplateId(templateId)
                                            .getStream())
                            .getTemplate();
                    return Optional.of(template);
                } catch (XmlException | IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public Optional<WebTemplate> buildIntrospect(String templateId) {
                return TemplateProvider.super.buildIntrospect(templateId);
            }
        }));
    }

    public void ensureTemplateExistence(Composition composition) {
        String templateId = composition.getArchetypeDetails().getTemplateId().getValue();
        endpoint.get().ensureExistence(templateId);
    }
}
