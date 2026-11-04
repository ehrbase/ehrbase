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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.ehrbase.api.exception.IllegalAqlException;
import org.ehrbase.jooq.pg.tables.CompData;
import org.ehrbase.jooq.pg.tables.CompVersion;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class ConditionUtilsTest {

    @Test
    void testComplexExtractedColumnConditionVoIdIn() {
        AslQuery q = new AslStructureQuery(
                "asq",
                AslStructureQuery.AslSourceRelation.COMPOSITION,
                List.of(
                        AslStructureColumn.VO_ID.field(),
                        AslStructureColumn.RM_ENTITY.field(),
                        AslStructureColumn.SYS_VERSION.field()),
                List.of("CO"),
                List.of(),
                null,
                true,
                false,
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

    // Text blocks still process Java escapes (\\ is one backslash); ` quotes cells needing leading/trailing spaces
    @ParameterizedTest
    @CsvSource(delimiter = '|', quoteCharacter = '`', nullValues = "NULL", textBlock = """
            NULL            | NULL
            ` Test `        | `" Test "`
            ``              | ""
            "Test"          | "\\"Test\\""
            C:\\temp\\      | "C:\\\\temp\\\\"
            Cluck Ol' Hen   | "Cluck Ol' Hen"
            """)
    void escapeAsJsonString(String input, String expected) {
        assertThat(ConditionUtils.escapeAsJsonString(input)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            a                   | a
            a*                  | a%
            a?                  | a_
            a\\*                | a*
            a\\?                | a?
            a\\\\               | a\\\\
            abc                 | abc
            *                   | %
            ?                   | _
            \\\\                | \\\\
            X\\\\\\?\\*%_*?X    | X\\\\?*\\%\\_%_X
            \\\\\\?\\*%_*?X     | \\\\?*\\%\\_%_X
            X%_*?X\\\\\\?\\*    | X\\%\\_%_X\\\\?*
            20%                 | 20\\%
            20_                 | 20\\_
            * 20*               | % 20%
            * 20%*              | % 20\\%%
            *20\\\\%*           | %20\\\\\\%%
            *20\\\\\\\\%*       | %20\\\\\\\\\\%%
            """)
    void translateAqlLikePattern(String aqlPattern, String expected) {
        assertEquals(expected, ConditionUtils.translateAqlLikePatternToSql(aqlPattern));
    }

    @ParameterizedTest
    @ValueSource(strings = {"\\", "\\%", "\\_", "\\n"})
    void translateAqlLikePatternInvalid(String aqlPattern) {
        assertThrows(IllegalAqlException.class, () -> ConditionUtils.translateAqlLikePatternToSql(aqlPattern));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            a                   | "a_
            a*                  | "a%_
            a?                  | "a__
            a\\*                | "a*_
            a\\?                | "a?_
            a\\\\               | "a\\\\\\\\_
            abc                 | "abc_
            *                   | "%_
            ?                   | "__
            \\\\                | "\\\\\\\\_
            20%                 | "20\\%_
            20_                 | "20\\__
            * 20*               | "% 20%_
            * 20%*              | "% 20\\%%_
            *20\\\\%*           | "%20\\\\\\\\\\%%_
            *20\\\\\\\\%*       | "%20\\\\\\\\\\\\\\\\\\%%_
            """)
    void translateAqlJsonLikePattern(String aqlPattern, String expected) {
        assertEquals(expected, ConditionUtils.translateAqlLikePatternToJsonSql(aqlPattern));
    }

    @ParameterizedTest
    @ValueSource(strings = {"\\", "\\%", "\\_", "\\n"})
    void translateAqlJsonLikePatternInvalid(String aqlPattern) {
        assertThrows(IllegalAqlException.class, () -> ConditionUtils.translateAqlLikePatternToJsonSql(aqlPattern));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', nullValues = "NULL", textBlock = """
            # version part is stripped
            EQ   | a6ddec4c-a68a-49ef-963e-3e0bc1970a28                         | a6ddec4c-a68a-49ef-963e-3e0bc1970a28
            EQ   | a6ddec4c-a68a-49ef-963e-3e0bc1970a28::local.ehrbase.org::1   | a6ddec4c-a68a-49ef-963e-3e0bc1970a28
            EQ   | abc::::2                                                     | abc
            # wildcards are not interpreted for non-LIKE operators
            EQ   | ab*c                                                         | ab*c
            EQ   | *ab::x::1                                                    | *ab
            EQ   | ab?c                                                         | ab?c
            EQ   | ?ab::x::1                                                    | ?ab
            # LIKE: wildcards in the root part truncate it to a prefix pattern
            LIKE | ab*                                                          | ab*
            LIKE | ab?                                                          | ab*
            LIKE | ab?cd                                                        | ab*
            LIKE | ab*cd::x::1                                                  | ab*
            LIKE | ab?cd::x::1                                                  | ab*
            # LIKE: leading wildcard -> no uid_root condition possible
            LIKE | *                                                            | NULL
            LIKE | ?                                                            | NULL
            LIKE | *ab                                                          | NULL
            LIKE | ?ab::x::1                                                    | NULL
            # LIKE: wildcards only in the version part do not affect the root
            LIKE | abc::*                                                       | abc
            LIKE | abc::local.ehrbase.org::?                                    | abc
            # LIKE: escaped wildcards are kept as literals
            LIKE | a\\*b                                                        | a\\*b
            LIKE | a\\?b::x::1                                                  | a\\?b
            LIKE | \\*ab                                                        | \\*ab
            LIKE | \\?ab                                                        | \\?ab
            LIKE | a\\*b*c                                                      | a\\*b*
            LIKE | a\\?b?c                                                      | a\\?b*
            LIKE | a\\\\*                                                       | a\\\\*
            """)
    void extractUidRootConditionValue(AslQueryCondition.AslConditionOperator op, String value, String expected) {
        assertThat(ConditionUtils.extractUidRootConditionValue(op, value)).isEqualTo(expected);
    }

    @ParameterizedTest
    @EnumSource(
            value = AslQueryCondition.AslConditionOperator.class,
            names = {"IS_NULL", "IS_NOT_NULL"},
            mode = EnumSource.Mode.EXCLUDE)
    void extractUidRootConditionValueReturnsSameInstanceWithoutVersion(AslQueryCondition.AslConditionOperator op) {
        // callers rely on identity to detect that no full uid condition is needed
        String plainUid = "a6ddec4c-a68a-49ef-963e-3e0bc1970a28";
        assertThat(ConditionUtils.extractUidRootConditionValue(op, plainUid)).isSameAs(plainUid);
    }
}
