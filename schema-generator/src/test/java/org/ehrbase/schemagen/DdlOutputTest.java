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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.ehrbase.openehr.sdk.webtemplate.model.WebTemplate;
import org.ehrbase.schemagen.enums.TemplateFormat;
import org.ehrbase.schemagen.model.GeneratedSchema;
import org.ehrbase.schemagen.model.TableDescriptor;
import org.junit.jupiter.api.Test;

/**
 * Prints generated DDL for manual inspection.
 */
class DdlOutputTest {

    @Test
    void printBloodPressureDdl() throws IOException {
        String xml = loadTemplate("templates/ehrbase_blood_pressure_simple.de.v0.opt");
        WebTemplate wt = new TemplateParserService().parse(xml, TemplateFormat.XML);
        TableDescriptor table = new TemplateAnalyzer().analyze(wt);
        GeneratedSchema schema = new SchemaGenerator().generate(table);

        System.out.println("=== Generated DDL for Blood Pressure ===");
        System.out.println("Table: " + schema.tableName());
        System.out.println("Columns: " + table.getColumns().size());
        System.out.println();
        System.out.println(schema.ddl());
    }

    @Test
    void printMinimalObservationDdl() throws IOException {
        String xml = loadTemplate("templates/minimal_observation.opt");
        WebTemplate wt = new TemplateParserService().parse(xml, TemplateFormat.XML);
        TableDescriptor table = new TemplateAnalyzer().analyze(wt);
        GeneratedSchema schema = new SchemaGenerator().generate(table);

        System.out.println("=== Generated DDL for Minimal Observation ===");
        System.out.println("Table: " + schema.tableName());
        System.out.println("Columns: " + table.getColumns().size());
        System.out.println();
        System.out.println(schema.ddl());
    }

    private String loadTemplate(String path) throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            if (is == null) throw new IOException("Template not found: " + path);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
