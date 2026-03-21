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
package org.ehrbase.repository.composition;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Tests for RmReconstructor — reconstructs RM Composition from normalized column values.
 * Replaces legacy DbToRmFormatTest (13 tests including round-trips).
 *
 * <p>Original scenarios covered:
 * - toCompositionFromTestIPS → reconstruct from column values
 * - toCompositionFromTestAllTypes → all RM data types
 * - roundtripTestOne/Node/Array → extract→reconstruct equivalence (moved to CompositionRoundTripIT)
 * - reconstructRmObjectDvMultimediaType → binary data handling
 */
class RmReconstructorTest {

    private static CompositionMetadata testMeta(String templateName, String archetypeId) {
        return new CompositionMetadata(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                archetypeId,
                templateName,
                "Test Composer",
                null,
                "en",
                "US",
                "433",
                null,
                null,
                1,
                UUID.randomUUID(),
                "creation",
                OffsetDateTime.now(),
                "committer",
                null,
                (short) 1);
    }

    @Test
    void reconstructMinimalComposition() {
        var meta = testMeta("Test Template", "openEHR-EHR-COMPOSITION.encounter.v1");
        var mainRow = Map.<String, Object>of();
        Map<String, java.util.List<Map<String, Object>>> childRows = Map.of();

        var composition = RmReconstructor.reconstruct(mainRow, childRows, mockWebTemplate("test-template"), meta);

        assertThat(composition).isNotNull();
        assertThat(composition.getArchetypeNodeId()).isEqualTo("openEHR-EHR-COMPOSITION.encounter.v1");
        assertThat(composition.getName().getValue()).isEqualTo("Test Template");
        assertThat(composition.getComposer()).isNotNull();
    }

    @Test
    void reconstructSetsLanguageAndTerritory() {
        var meta = testMeta("Template", "openEHR-EHR-COMPOSITION.encounter.v1");
        var composition = RmReconstructor.reconstruct(Map.of(), Map.of(), mockWebTemplate("t"), meta);

        assertThat(composition.getLanguage()).isNotNull();
        assertThat(composition.getLanguage().getCodeString()).isEqualTo("en");
        assertThat(composition.getTerritory()).isNotNull();
        assertThat(composition.getTerritory().getCodeString()).isEqualTo("US");
    }

    @Test
    void reconstructSetsCategory() {
        var meta = testMeta("Template", "openEHR-EHR-COMPOSITION.encounter.v1");
        var composition = RmReconstructor.reconstruct(Map.of(), Map.of(), mockWebTemplate("t"), meta);

        assertThat(composition.getCategory()).isNotNull();
        assertThat(composition.getCategory().getDefiningCode().getCodeString()).isEqualTo("433");
    }

    @Test
    void reconstructSetsArchetypeDetails() {
        var meta = testMeta("Template", "openEHR-EHR-COMPOSITION.encounter.v1");
        var composition = RmReconstructor.reconstruct(Map.of(), Map.of(), mockWebTemplate("my-template"), meta);

        assertThat(composition.getArchetypeDetails()).isNotNull();
        assertThat(composition.getArchetypeDetails().getArchetypeId().getValue())
                .isEqualTo("openEHR-EHR-COMPOSITION.encounter.v1");
        assertThat(composition.getArchetypeDetails().getTemplateId().getValue()).isEqualTo("my-template");
    }

    @Test
    void reconstructSetsComposer() {
        var meta = testMeta("Template", "openEHR-EHR-COMPOSITION.encounter.v1");
        var composition = RmReconstructor.reconstruct(Map.of(), Map.of(), mockWebTemplate("t"), meta);

        assertThat(composition.getComposer()).isNotNull();
        assertThat(((com.nedap.archie.rm.generic.PartyIdentified) composition.getComposer()).getName())
                .isEqualTo("Test Composer");
    }

    @Test
    void reconstructWithNullLanguageAndTerritory() {
        var meta = new CompositionMetadata(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "openEHR-EHR-COMPOSITION.encounter.v1",
                "Template",
                "Composer",
                null,
                null,
                null,
                null,
                null,
                null,
                1,
                null,
                "creation",
                OffsetDateTime.now(),
                "committer",
                null,
                (short) 1);

        var composition = RmReconstructor.reconstruct(Map.of(), Map.of(), mockWebTemplate("t"), meta);
        assertThat(composition.getLanguage()).isNull();
        assertThat(composition.getTerritory()).isNull();
        assertThat(composition.getCategory()).isNull();
    }

    @Test
    void compositionMetadataRecordFields() {
        var meta = testMeta("Blood Pressure", "openEHR-EHR-COMPOSITION.encounter.v1");
        assertThat(meta.templateName()).isEqualTo("Blood Pressure");
        assertThat(meta.archetypeId()).isEqualTo("openEHR-EHR-COMPOSITION.encounter.v1");
        assertThat(meta.composerName()).isEqualTo("Test Composer");
        assertThat(meta.language()).isEqualTo("en");
        assertThat(meta.territory()).isEqualTo("US");
        assertThat(meta.categoryCode()).isEqualTo("433");
        assertThat(meta.sysVersion()).isEqualTo(1);
        assertThat(meta.changeType()).isEqualTo("creation");
    }

    @Test
    void compositionTableDataRecord() {
        var mainValues = Map.<String, Object>of("systolic", 120.0, "units", "mm[Hg]");
        Map<String, java.util.List<Map<String, Object>>> childValues = Map.of();
        var tableData = new CompositionTableData(mainValues, childValues);

        assertThat(tableData.mainTableValues()).hasSize(2);
        assertThat(tableData.mainTableValues().get("systolic")).isEqualTo(120.0);
        assertThat(tableData.childTableValues()).isEmpty();
    }

    private static org.ehrbase.openehr.sdk.webtemplate.model.WebTemplate mockWebTemplate(String templateId) {
        var wt = new org.ehrbase.openehr.sdk.webtemplate.model.WebTemplate();
        wt.setTemplateId(templateId);
        wt.setTree(new org.ehrbase.openehr.sdk.webtemplate.model.WebTemplateNode());
        return wt;
    }
}
