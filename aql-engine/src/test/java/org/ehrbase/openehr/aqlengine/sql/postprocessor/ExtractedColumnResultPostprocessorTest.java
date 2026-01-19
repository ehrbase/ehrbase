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
package org.ehrbase.openehr.aqlengine.sql.postprocessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.nedap.archie.rm.datatypes.CodePhrase;
import com.nedap.archie.rm.datavalues.DvCodedText;
import com.nedap.archie.rm.datavalues.DvText;
import com.nedap.archie.rm.datavalues.quantity.datetime.DvDateTime;
import com.nedap.archie.rm.support.identification.HierObjectId;
import com.nedap.archie.rm.support.identification.TerminologyId;
import java.time.OffsetDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.Optional;
import java.util.UUID;
import org.ehrbase.api.knowledge.KnowledgeCacheService;
import org.ehrbase.jooq.pg.enums.ContributionChangeType;
import org.ehrbase.openehr.aqlengine.ChangeTypeUtils;
import org.ehrbase.openehr.aqlengine.asl.model.AslExtractedColumn;
import org.ehrbase.openehr.sdk.util.OpenEHRDateTimeSerializationUtils;
import org.jooq.Record;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;

class ExtractedColumnResultPostprocessorTest {

    private final KnowledgeCacheService knowledgeCacheService = mock(KnowledgeCacheService.class);
    private final Record dbRecord = mock(Record.class);

    private ExtractedColumnResultPostprocessor processor(AslExtractedColumn extractedColumn) {
        return ExtractedColumnResultPostprocessor.get(extractedColumn, knowledgeCacheService, "test-node");
    }

    @BeforeEach
    void setUp() {

        Mockito.reset(knowledgeCacheService, dbRecord);
    }

    @Test
    void nullSafe() {

        assertThat(processor(AslExtractedColumn.VO_ID).postProcessColumn(null)).isNull();
    }

    @Test
    void templateId() {

        var uuid = UUID.fromString("93e01a9a-041e-4bf6-89c2-e63f8a74a4d5");
        doReturn(Optional.of("test-template")).when(knowledgeCacheService).findTemplateIdByUuid(uuid);
        assertThat(processor(AslExtractedColumn.TEMPLATE_ID).postProcessColumn(uuid))
                .isEqualTo("test-template");
    }

    @Test
    void ehrSystemId() {
        assertThat(processor(AslExtractedColumn.EHR_SYSTEM_ID_DV)
                        .postProcessColumn("e290acd1-0fa4-4eb0-97c6-e884e6ea74f3"))
                .isEqualTo(new HierObjectId("e290acd1-0fa4-4eb0-97c6-e884e6ea74f3"));
    }

    @Test
    void rootConcept() {
        assertThat(processor(AslExtractedColumn.ROOT_CONCEPT).postProcessColumn("root_concept"))
                .isEqualTo("openEHR-EHR-COMPOSITIONroot_concept");
    }

    @Test
    void archetypeNodeId() {

        doReturn(".entityConcept").when(dbRecord).get(0);
        doReturn("HX").when(dbRecord).get(1);

        assertThat(processor(AslExtractedColumn.ARCHETYPE_NODE_ID).postProcessColumn(dbRecord))
                .isEqualTo("openEHR-EHR-HIER_OBJECT_ID.entityConcept");
    }

    @Test
    void vo_id() {

        doReturn("c0817101-94fd-48e5-b4f9-cb8f0556923a").when(dbRecord).get(0);
        doReturn("42").when(dbRecord).get(1);

        assertThat(processor(AslExtractedColumn.VO_ID).postProcessColumn(dbRecord))
                .isEqualTo("c0817101-94fd-48e5-b4f9-cb8f0556923a::test-node::42");
    }

    @Test
    void auditDetailsDescription() {

        assertThat(processor(AslExtractedColumn.AD_DESCRIPTION_DV).postProcessColumn("lorem ipsum"))
                .isEqualTo(new DvText("lorem ipsum"));
    }

    @Test
    void auditDetailsChangeType() {

        ContributionChangeType changeType = ContributionChangeType.creation;
        assertThat(processor(AslExtractedColumn.AD_CHANGE_TYPE_DV).postProcessColumn(changeType))
                .isEqualTo(new DvCodedText(
                        changeType.getLiteral().toLowerCase(),
                        new CodePhrase(
                                new TerminologyId("openehr"),
                                ChangeTypeUtils.getCodeByJooqChangeType(changeType),
                                changeType.getLiteral().toLowerCase())));
    }

    @Test
    void auditDetailsChangeTypeCode() {

        assertThat(processor(AslExtractedColumn.AD_CHANGE_TYPE_CODE_STRING)
                        .postProcessColumn(ContributionChangeType.deleted))
                .isEqualTo("523");
    }

    @ParameterizedTest
    @EnumSource(
            value = AslExtractedColumn.class,
            mode = EnumSource.Mode.INCLUDE,
            names = {"AD_CHANGE_TYPE_VALUE", "AD_CHANGE_TYPE_PREFERRED_TERM"})
    void auditDetailsChangeTypeValue(AslExtractedColumn aslExtractedColumn) {

        assertThat(processor(aslExtractedColumn).postProcessColumn(ContributionChangeType.amendment))
                .isEqualTo("amendment");
    }

    @ParameterizedTest
    @EnumSource(
            value = AslExtractedColumn.class,
            mode = EnumSource.Mode.INCLUDE,
            names = {"OV_TIME_COMMITTED_DV", "EHR_TIME_CREATED_DV"})
    void dateTime(AslExtractedColumn aslExtractedColumn) {

        TemporalAccessor now = OffsetDateTime.now();
        assertThat(processor(aslExtractedColumn).postProcessColumn(now)).isEqualTo(new DvDateTime(now));
    }

    @ParameterizedTest
    @EnumSource(
            value = AslExtractedColumn.class,
            mode = EnumSource.Mode.INCLUDE,
            names = {"OV_TIME_COMMITTED", "EHR_TIME_CREATED"})
    void time(AslExtractedColumn aslExtractedColumn) {

        TemporalAccessor now = OffsetDateTime.now();
        assertThat(processor(aslExtractedColumn).postProcessColumn(now))
                .isEqualTo(OpenEHRDateTimeSerializationUtils.formatDateTime(now));
    }

    @ParameterizedTest
    @EnumSource(
            value = AslExtractedColumn.class,
            mode = EnumSource.Mode.EXCLUDE,
            names = {
                "TEMPLATE_ID",
                "EHR_SYSTEM_ID_DV",
                "ROOT_CONCEPT",
                "ARCHETYPE_NODE_ID",
                "VO_ID",
                "AD_DESCRIPTION_DV",
                "AD_CHANGE_TYPE_DV",
                "AD_CHANGE_TYPE_CODE_STRING",
                "AD_CHANGE_TYPE_VALUE",
                "AD_CHANGE_TYPE_PREFERRED_TERM",
                "OV_TIME_COMMITTED_DV",
                "EHR_TIME_CREATED_DV",
                "OV_TIME_COMMITTED",
                "EHR_TIME_CREATED"
            })
    void string(AslExtractedColumn aslExtractedColumn) {

        var testValue = "test_value";
        assertThat(processor(aslExtractedColumn).postProcessColumn(testValue)).isSameAs(testValue);
    }
}
