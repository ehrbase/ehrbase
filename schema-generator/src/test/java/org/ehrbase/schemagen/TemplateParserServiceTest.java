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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.ehrbase.openehr.sdk.webtemplate.model.WebTemplate;
import org.ehrbase.openehr.sdk.webtemplate.model.WebTemplateNode;
import org.ehrbase.schemagen.enums.TemplateFormat;
import org.junit.jupiter.api.Test;

class TemplateParserServiceTest {

    private final TemplateParserService parser = new TemplateParserService();

    @Test
    void parseMinimalObservation_returnsWebTemplate() throws IOException {
        String xml = loadTemplate("templates/minimal_observation.opt");
        WebTemplate wt = parser.parse(xml, TemplateFormat.XML);

        assertThat(wt).isNotNull();
        assertThat(wt.getTemplateId()).isNotBlank();

        WebTemplateNode root = wt.getTree();
        assertThat(root).isNotNull();
        assertThat(root.getRmType()).isEqualTo("COMPOSITION");
        assertThat(root.getChildren()).isNotEmpty();
    }

    @Test
    void parseBloodPressure_returnsWebTemplateWithObservation() throws IOException {
        String xml = loadTemplate("templates/ehrbase_blood_pressure_simple.de.v0.opt");
        WebTemplate wt = parser.parse(xml, TemplateFormat.XML);

        assertThat(wt).isNotNull();
        assertThat(wt.getTemplateId()).contains("blood_pressure");
    }

    @Test
    void parseInvalidXml_throwsIllegalArgument() {
        assertThatThrownBy(() -> parser.parse("<invalid>xml", TemplateFormat.XML))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid OPT XML");
    }

    @Test
    void parseAdlText_throwsUnsupported() {
        assertThatThrownBy(() -> parser.parse("archetype text", TemplateFormat.ADL))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void parseJson_throwsUnsupported() {
        assertThatThrownBy(() -> parser.parse("{}", TemplateFormat.JSON))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("Phase 11");
    }

    @Test
    void parseOptFormat_sameAsXml() throws IOException {
        String xml = loadTemplate("templates/minimal_observation.opt");
        WebTemplate wt = parser.parse(xml, TemplateFormat.OPT);
        assertThat(wt).isNotNull();
        assertThat(wt.getTree().getRmType()).isEqualTo("COMPOSITION");
    }

    private String loadTemplate(String path) throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            if (is == null) throw new IOException("Template not found: " + path);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
