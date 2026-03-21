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
package org.ehrbase.schemagen;

import org.apache.xmlbeans.XmlException;
import org.ehrbase.openehr.sdk.webtemplate.model.WebTemplate;
import org.ehrbase.openehr.sdk.webtemplate.parser.OPTParser;
import org.ehrbase.schemagen.enums.TemplateFormat;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;
import org.openehr.schemas.v1.TemplateDocument;

/**
 * Parses openEHR Operational Templates (OPT) into WebTemplate format.
 * WebTemplate is the universal intermediate format used by the schema generator.
 */
public class TemplateParserService {

    /**
     * Parses template content into a WebTemplate.
     *
     * @param content the template content (XML, ADL text, etc.)
     * @param format  the format of the content
     * @return parsed WebTemplate
     */
    public WebTemplate parse(String content, TemplateFormat format) {
        return switch (format) {
            case XML, OPT -> parseOpt14Xml(content);
            case ADL -> parseAdl14Text(content);
            case JSON ->
                throw new UnsupportedOperationException(
                        "ADL 2.4 JSON format not yet supported. See Phase 11 (ADL 2.4 Future).");
        };
    }

    private WebTemplate parseOpt14Xml(String xmlContent) {
        try {
            TemplateDocument document = TemplateDocument.Factory.parse(xmlContent);
            OPERATIONALTEMPLATE operationalTemplate = document.getTemplate();

            if (operationalTemplate == null) {
                throw new IllegalArgumentException("OPT document does not contain a valid template");
            }

            return new OPTParser(operationalTemplate).parse();
        } catch (XmlException e) {
            throw new IllegalArgumentException("Invalid OPT XML: " + e.getMessage(), e);
        }
    }

    private WebTemplate parseAdl14Text(String adlContent) {
        // ADL 1.4 text format parsing via Archie
        // Archie 3.17+ provides ADLParser for ADL 1.4 text
        throw new UnsupportedOperationException(
                "ADL 1.4 text format parsing not yet implemented. Use XML (OPT) format.");
    }
}
