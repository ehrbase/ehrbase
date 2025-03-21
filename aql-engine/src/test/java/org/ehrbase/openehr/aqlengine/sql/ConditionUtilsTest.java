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
package org.ehrbase.openehr.aqlengine.sql;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.ehrbase.jooq.pg.tables.CompData;
import org.ehrbase.jooq.pg.tables.CompVersion;
import org.ehrbase.openehr.aqlengine.asl.meta.AslTypeOrigin;
import org.ehrbase.openehr.aqlengine.asl.model.AslExtractedColumn;
import org.ehrbase.openehr.aqlengine.asl.model.AslStructureColumn;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslFieldValueQueryCondition;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslQueryCondition;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslComplexExtractedColumnField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslField;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslQuery;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslStructureQuery;
import org.jooq.Condition;
import org.jooq.impl.QOM;
import org.junit.jupiter.api.Test;

class ConditionUtilsTest {

    @Test
    void testComplexExtractedColumnConditionVoIdIn() {
        AslQuery q = new AslStructureQuery(
                "asq",
                AslStructureQuery.AslSourceRelation.COMPOSITION,
                new AslTypeOrigin.AslRmTypeOrigin("c", "COMPOSITION", List.of()),
                List.of(
                        AslStructureColumn.VO_ID.field(),
                        AslStructureColumn.RM_ENTITY.field(),
                        AslStructureColumn.SYS_VERSION.field()),
                List.of("CO"),
                List.of(),
                null,
                true);

        AslField.FieldSource fieldSource = new AslField.FieldSource(q, q, q);
        AslField field = new AslComplexExtractedColumnField(AslExtractedColumn.VO_ID, fieldSource);

        AslFieldValueQueryCondition<?> fvId =
                new AslFieldValueQueryCondition<>(field, AslQueryCondition.AslConditionOperator.IN, List.of("a", "c"));
        AslFieldValueQueryCondition<?> fvVersion = new AslFieldValueQueryCondition<>(
                field, AslQueryCondition.AslConditionOperator.IN, List.of("b::local.ehrbase.org::142", "d::::2"));
        AslFieldValueQueryCondition<?> fvMixed = new AslFieldValueQueryCondition<>(
                field,
                AslQueryCondition.AslConditionOperator.IN,
                List.of("a", "b::local.ehrbase.org::142", "c", "d::::2"));

        AslComplexExtractedColumnField ecf =
                new AslComplexExtractedColumnField(field.getExtractedColumn(), fieldSource);

        Condition conditionId = ConditionUtils.complexExtractedColumnCondition(
                false, fvId, ecf, CompData.COMP_DATA, CompVersion.COMP_VERSION);
        Condition conditionVersion = ConditionUtils.complexExtractedColumnCondition(
                false, fvVersion, ecf, CompData.COMP_DATA, CompVersion.COMP_VERSION);
        Condition conditionMixed = ConditionUtils.complexExtractedColumnCondition(
                false, fvMixed, ecf, CompData.COMP_DATA, CompVersion.COMP_VERSION);

        assertThat(conditionMixed).isInstanceOf(QOM.Or.class);
        assertThat(conditionId).isInstanceOf(QOM.InList.class);
        assertThat(conditionVersion).isInstanceOf(QOM.InList.class);

        assertThat(conditionId.toString()).doesNotContain("row");
        assertThat(conditionVersion.toString()).contains("row", "142");
        assertThat(conditionMixed.toString())
                .isEqualToIgnoringWhitespace("(%s or %s)".formatted(conditionId, conditionVersion));
    }

    @Test
    void escapeAsJsonString() {
        assertThat(ConditionUtils.escapeAsJsonString(null)).isNull();
        assertThat(ConditionUtils.escapeAsJsonString(" Test ")).isEqualTo("\" Test \"");
        assertThat(ConditionUtils.escapeAsJsonString("")).isEqualTo("\"\"");
        assertThat(ConditionUtils.escapeAsJsonString("\"Test\"")).isEqualTo("\"\\\"Test\\\"\"");
        assertThat(ConditionUtils.escapeAsJsonString("\"Test\"")).isEqualTo("\"\\\"Test\\\"\"");
        assertThat(ConditionUtils.escapeAsJsonString("C:\\temp\\")).isEqualTo("\"C:\\\\temp\\\\\"");
        assertThat(ConditionUtils.escapeAsJsonString("Cluck Ol' Hen")).isEqualTo("\"Cluck Ol' Hen\"");
    }
}
