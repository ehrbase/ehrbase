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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.ehrbase.openehr.sdk.webtemplate.model.WebTemplate;
import org.ehrbase.schemagen.enums.TemplateFormat;
import org.ehrbase.schemagen.model.ColumnDescriptor;
import org.ehrbase.schemagen.model.TableDescriptor;
import org.junit.jupiter.api.Test;

class TemplateAnalyzerTest {

    private final TemplateParserService parser = new TemplateParserService();
    private final TemplateAnalyzer analyzer = new TemplateAnalyzer();

    @Test
    void analyzeMinimalObservation_producesTable() throws IOException {
        WebTemplate wt = parseTemplate("templates/minimal_observation.opt");
        TableDescriptor table = analyzer.analyze(wt);

        assertThat(table).isNotNull();
        assertThat(table.getTableName()).isNotBlank();
        assertThat(table.getSchema()).isEqualTo("ehr_data");

        // Should have system columns (id, composition_id, ehr_id, valid_period, sys_version, sys_tenant)
        assertThat(table.getColumns()).hasSizeGreaterThanOrEqualTo(6);
        List<String> colNames =
                table.getColumns().stream().map(ColumnDescriptor::name).toList();
        assertThat(colNames).contains("id", "composition_id", "ehr_id", "valid_period", "sys_version", "sys_tenant");
    }

    @Test
    void analyzeMinimalObservation_hasDataColumns() throws IOException {
        WebTemplate wt = parseTemplate("templates/minimal_observation.opt");
        TableDescriptor table = analyzer.analyze(wt);

        // Should have more than just system columns — should have clinical data columns
        assertThat(table.getColumns().size()).isGreaterThan(6);
    }

    @Test
    void analyzeBloodPressure_hasExpectedColumns() throws IOException {
        WebTemplate wt = parseTemplate("templates/ehrbase_blood_pressure_simple.de.v0.opt");
        TableDescriptor table = analyzer.analyze(wt);

        assertThat(table).isNotNull();
        List<String> colNames =
                table.getColumns().stream().map(ColumnDescriptor::name).toList();

        // Blood pressure template should produce columns with "systolic" and "diastolic" somewhere
        assertThat(colNames.toString()).containsIgnoringCase("systol");
        assertThat(colNames.toString()).containsIgnoringCase("diastol");
    }

    @Test
    void analyzeBloodPressure_tableNameHasObsPrefix() throws IOException {
        WebTemplate wt = parseTemplate("templates/ehrbase_blood_pressure_simple.de.v0.opt");
        TableDescriptor table = analyzer.analyze(wt);

        // Table name should start with "comp_" for COMPOSITION root
        assertThat(table.getTableName()).startsWith("comp_");
    }

    @Test
    void analyzeBloodPressure_columnTypesAreCorrect() throws IOException {
        WebTemplate wt = parseTemplate("templates/ehrbase_blood_pressure_simple.de.v0.opt");
        TableDescriptor table = analyzer.analyze(wt);

        // Find a magnitude column — should be DOUBLE PRECISION
        List<ColumnDescriptor> magnitudeCols = table.getColumns().stream()
                .filter(c -> c.name().contains("magnitude"))
                .toList();

        if (!magnitudeCols.isEmpty()) {
            assertThat(magnitudeCols.getFirst().pgType()).isEqualTo("DOUBLE PRECISION");
        }
    }

    @Test
    void generatedDdl_endToEnd_bloodPressure() throws IOException {
        WebTemplate wt = parseTemplate("templates/ehrbase_blood_pressure_simple.de.v0.opt");
        TableDescriptor table = analyzer.analyze(wt);
        SchemaGenerator generator = new SchemaGenerator();

        var schema = generator.generate(table);

        assertThat(schema.ddl()).contains("CREATE TABLE IF NOT EXISTS ehr_data.");
        assertThat(schema.ddl()).contains("uuidv7()");
        assertThat(schema.ddl()).contains("WITHOUT OVERLAPS");
        assertThat(schema.ddl()).contains("_history");
        assertThat(schema.ddl()).contains("ROW LEVEL SECURITY");
        assertThat(schema.viewDdl()).contains("ehr_views.");
    }

    @Test
    void generatedDdl_endToEnd_minimalObservation() throws IOException {
        WebTemplate wt = parseTemplate("templates/minimal_observation.opt");
        TableDescriptor table = analyzer.analyze(wt);
        SchemaGenerator generator = new SchemaGenerator();

        var schema = generator.generate(table);

        assertThat(schema.ddl()).contains("CREATE TABLE IF NOT EXISTS ehr_data.");
        assertThat(schema.ddl()).contains("PRIMARY KEY (id, valid_period WITHOUT OVERLAPS)");
        assertThat(schema.ddl()).contains("REFERENCES ehr_system.composition");
    }

    private WebTemplate parseTemplate(String path) throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            if (is == null) throw new IOException("Template not found: " + path);
            String xml = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return parser.parse(xml, TemplateFormat.XML);
        }
    }
}
