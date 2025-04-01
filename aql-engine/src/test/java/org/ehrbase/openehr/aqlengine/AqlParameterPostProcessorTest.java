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
package org.ehrbase.openehr.aqlengine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.assertj.core.api.AbstractThrowableAssert;
import org.ehrbase.openehr.sdk.aql.dto.AqlQuery;
import org.ehrbase.openehr.sdk.aql.dto.operand.StringPrimitive;
import org.ehrbase.openehr.sdk.aql.parser.AqlParseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class AqlParameterPostProcessorTest {

    @ParameterizedTest
    @ValueSource(
            strings = {
                "2020-12-31",
                "20201231",
                "23:59:59",
                "235959",
                "23:59:59.9",
                "23:59:59.98",
                "23:59:59.987",
                "23:59:59.9876",
                "23:59:59.98765",
                "23:59:59.987654",
                "23:59:59.9876543",
                "23:59:59.98765432",
                "23:59:59.987654321",
                "235959.987",
                "23:59:59Z",
                "235959Z",
                "23:59:59.987Z",
                "235959.987Z",
                "23:59:59+12",
                "235959-12:59",
                "23:59:59.987+12",
                "235959.987-12:59",
                "235959.987+1259",
                "235959.987-1259",
                "2020-12-31T23:59:59",
                "2020-12-31T23:59:59.9",
                "2020-12-31T23:59:59.98",
                "2020-12-31T23:59:59.987",
                "2020-12-31T23:59:59.9876",
                "2020-12-31T23:59:59.98765",
                "2020-12-31T23:59:59.987654",
                "2020-12-31T23:59:59.9876543",
                "2020-12-31T23:59:59.98765432",
                "2020-12-31T23:59:59.987654321",
                "2020-12-31T23:59:59Z",
                "2020-12-31T23:59:59-0200",
                "2020-12-31T23:59:59.013-0200",
                // leap year
                "20200229",
                "2020-02-29",
                "20200229T235959",
                "2020-02-29T23:59:59-0200",
                // syntactically correct, but non-existing
                "20200431",
                "20200230",
                "20210229",
                "2020-04-31",
                "2020-02-30",
                "2021-02-29",
                "20210229T235959",
                "2020-04-31T23:59:59",
                "2020-02-30T23:59:59",
                "2021-02-29T23:59:59",
            })
    void confirmTemporalPattern(String example) {
        assertThat(AqlParameterPostProcessor.TemporalPrimitivePattern.matches(example))
                .isTrue();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                // syntactically correct, but non-existing
                "20200431",
                "20200230",
                "20210229",
                "2020-04-31",
                "2020-02-30",
                "2021-02-29",
                "20210229T235959",
                "2020-04-31T23:59:59",
                "2020-02-30T23:59:59",
                "2021-02-29T23:59:59",
            })
    void falsePositivePatterns(String example) {
        assertThat(AqlParameterPostProcessor.Utils.stringToPrimitive(example))
                .isExactlyInstanceOf(StringPrimitive.class);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "",
                "T",
                "2020-1231",
                "2020",
                "2020:12:31",
                "23-59-59",
                "23-59",
                "236060",
                "23:60:59.987",
                "23:59:59.",
                "23:59:59.1234567890",
                "23:59:59.987z",
                "23:59:59+120",
                "23:59:59.987+2",
                "23:59:59.987+123",
                "23:59:59.987+12345",
                "23:59:59.987Z+1234",
                "2020-12-31T23:59:59.",
                "2020-12-31T23:59:59.9876543210",
                "2020-12-31t23:59:59.013-0200",
                "2020-12-31T235959",
                "20201231T23:59:59",
                "23:59:59T2020-12-31"
            })
    void rejectTemporalPattern(String example) {
        assertThat(AqlParameterPostProcessor.TemporalPrimitivePattern.matches(example))
                .isFalse();
    }

    @ParameterizedTest
    @MethodSource("replaceWhereParametersSrc")
    void replaceWhereParameters(ReplacementTestParam check) {
        check.doAssert();
    }

    static Stream<ReplacementTestParam> replaceWhereParametersSrc() {
        return Stream.of(
                // Simple string replacement
                ReplacementTestParam.success(
                        "SELECT d FROM DUMMY d WHERE d/foo = $bar",
                        Map.of("bar", "baz"),
                        "SELECT d FROM DUMMY d WHERE d/foo = 'baz'"),

                // Data types
                ReplacementTestParam.success(
                        "SELECT d FROM DUMMY d WHERE (d/int = $int AND d/bool = $bool AND d/double = $double AND d/str = $str AND d/date = $date)",
                        Map.of("int", 42, "bool", true, "double", 1., "str", "foo", "date", "2012-12-31"),
                        "SELECT d FROM DUMMY d WHERE (d/int = 42 AND d/bool = true AND d/double = 1.0 AND d/str = 'foo' AND d/date = '2012-12-31')"),

                // IdentifiedPath: archetype_node_id
                ReplacementTestParam.success(
                        "SELECT d FROM DUMMY d WHERE d[$ani]/foo[$ani2] = 42",
                        Map.of("ani", "at0001", "ani2", "at0002"),
                        "SELECT d FROM DUMMY d WHERE d[at0001]/foo[at0002] = 42"),

                // IdentifiedPath: nodeConstraint + name
                ReplacementTestParam.success(
                        "SELECT d FROM DUMMY d WHERE d[at0001,$nameConstraint]/foo[at0002,$nameConstraint2] = 42",
                        Map.of("nameConstraint", "Results", "nameConstraint2", "Results2"),
                        "SELECT d FROM DUMMY d WHERE d[at0001, 'Results']/foo[at0002, 'Results2'] = 42"),

                // IdentifiedPath: nodeConstraint + local terminology => interpreted as String
                ReplacementTestParam.success(
                        "SELECT d FROM DUMMY d WHERE d[at0001,$nameConstraint]/foo[at0002,$nameConstraint2] = 42",
                        Map.of("nameConstraint", "at0002", "nameConstraint2", "at0003"),
                        "SELECT d FROM DUMMY d WHERE d[at0001, 'at0002']/foo[at0002, 'at0003'] = 42"),

                // IdentifiedPath: nodeConstraint + TERM_CODE => interpreted as String
                ReplacementTestParam.success(
                        "SELECT d FROM DUMMY d WHERE d[at0001,$nameConstraint]/foo[at0002,$nameConstraint2] = 42",
                        Map.of("nameConstraint", "ISO_639-1::en", "nameConstraint2", "ISO_639-1::de"),
                        "SELECT d FROM DUMMY d WHERE d[at0001, 'ISO_639-1::en']/foo[at0002, 'ISO_639-1::de'] = 42"),

                // IdentifiedPath: standard predicates
                ReplacementTestParam.success(
                        "SELECT d FROM DUMMY d WHERE d[foo=$foo AND bar=$bar]/foo[foo=$foo2 AND bar=$bar2] = 42",
                        Map.of("foo", "FOO", "bar", 13, "foo2", "FOO2", "bar2", 31),
                        "SELECT d FROM DUMMY d WHERE d[foo='FOO' AND bar=13]/foo[foo='FOO2' AND bar=31] = 42"),

                // ignored + duplicate usage
                ReplacementTestParam.success(
                        "SELECT d FROM DUMMY d WHERE (d/f1 = $bar AND d/f2 = $bar AND d/f3 = $baz)",
                        Map.of("foo", "bob", "bar", "alice", "baz", "charly"),
                        "SELECT d FROM DUMMY d WHERE (d/f1 = 'alice' AND d/f2 = 'alice' AND d/f3 = 'charly')"),

                // missing
                ReplacementTestParam.rejected(
                        "SELECT d FROM DUMMY d WHERE (d/f1 = $bar AND d/f2 = $bar AND d/f3 = $baz)",
                        Map.of("foo", "bob", "bar", "alice"),
                        "Missing parameter"));
    }

    @ParameterizedTest
    @MethodSource("replaceFromParametersSrc")
    void replaceFromParameters(ReplacementTestParam check) {
        check.doAssert();
    }

    static Stream<ReplacementTestParam> replaceFromParametersSrc() {
        return Stream.of(
                // archetype_node_id
                ReplacementTestParam.success(
                        "SELECT d FROM DUMMY d[$ani]", Map.of("ani", "at0001"), "SELECT d FROM DUMMY d[at0001]"),
                ReplacementTestParam.rejected("SELECT d FROM DUMMY d[$ani]", Map.of("ani", "invalid-id"), null),

                // nodeConstraint + name
                ReplacementTestParam.success(
                        "SELECT d FROM DUMMY d[at0001,$nameConstraint]",
                        Map.of("nameConstraint", "Results"),
                        "SELECT d FROM DUMMY d[at0001, 'Results']"),

                // nodeConstraint + local terminology => interpreted as String
                ReplacementTestParam.success(
                        "SELECT d FROM DUMMY d[at0001,$nameConstraint]",
                        Map.of("nameConstraint", "at0002"),
                        "SELECT d FROM DUMMY d[at0001, 'at0002']"),

                // nodeConstraint + TERM_CODE => interpreted as String
                ReplacementTestParam.success(
                        "SELECT d FROM DUMMY d[at0001,$nameConstraint]",
                        Map.of("nameConstraint", "ISO_639-1::en"),
                        "SELECT d FROM DUMMY d[at0001, 'ISO_639-1::en']"),

                // standard predicates
                ReplacementTestParam.success(
                        "SELECT d FROM DUMMY d[foo=$foo AND bar=$bar]",
                        Map.of("foo", "FOO", "bar", 42),
                        "SELECT d FROM DUMMY d[foo='FOO' AND bar=42]"),

                // VERSION
                ReplacementTestParam.success(
                        "SELECT v FROM VERSION v[commit_audit/time_committed>$time_committed]",
                        Map.of("time_committed", "2021-12-03T16:05:19.514097+01:00"),
                        "SELECT v FROM VERSION v[commit_audit/time_committed>'2021-12-03T16:05:19.514097+01:00']"));
    }

    @ParameterizedTest
    @MethodSource("replaceSelectParametersSrc")
    void replaceSelectParameters(ReplacementTestParam check) {
        check.doAssert();
    }

    static Stream<ReplacementTestParam> replaceSelectParametersSrc() {
        return Stream.of(
                ReplacementTestParam.success(
                        "SELECT d[$foo]/e[bar=$foo AND ba/z=$baz] FROM DUMMY d",
                        Map.of("foo", "at0001", "baz", 42),
                        "SELECT d[at0001]/e[bar='at0001' AND ba/z=42] FROM DUMMY d"),
                ReplacementTestParam.rejected(
                        "SELECT d[$foo]/e[bar=$foo AND ba/z=$baz] FROM DUMMY d",
                        Map.of("foo", List.of("at0001"), "baz", List.of(42, 24)),
                        "One of the parameters does not support multiple values"),
                ReplacementTestParam.success(
                        "SELECT SUM(d[$foo]/e[bar=$foo AND ba/z=$baz]), LENGTH(d[$foo]/e[bar=$foo AND ba/z=$baz]) FROM DUMMY d",
                        Map.of("foo", "at0001", "baz", 42),
                        "SELECT SUM(d[at0001]/e[bar='at0001' AND ba/z=42]), LENGTH(d[at0001]/e[bar='at0001' AND ba/z=42]) FROM DUMMY d"),
                ReplacementTestParam.rejected(
                        "SELECT d[$foo]/e[bar=$foo AND ba/z=$baz] FROM DUMMY d",
                        Map.of("foo", "invalid-id", "baz", 42),
                        null),
                ReplacementTestParam.rejected("SELECT d/e[$foo] FROM DUMMY d", Map.of("foo", 42), null));
    }

    @Test
    void replaceOrderByParameters() {
        assertReplaceParameters(
                "SELECT d[$foo]/e[bar=$foo AND ba/z=$baz] FROM DUMMY d ORDER BY d[$foo]/e[bar=$foo AND ba/z=$baz] DESC",
                Map.of("foo", "at0001", "baz", 42),
                "SELECT d[at0001]/e[bar='at0001' AND ba/z=42] FROM DUMMY d ORDER BY d[at0001]/e[bar='at0001' AND ba/z=42] DESC");
    }

    @ParameterizedTest
    @MethodSource("replaceMatchesParametersSrc")
    void replaceMatchesParameters(ReplacementTestParam check) {
        check.doAssert();
    }

    static Stream<ReplacementTestParam> replaceMatchesParametersSrc() {
        return Stream.of(
                ReplacementTestParam.success(
                        "SELECT d FROM DUMMY d WHERE d/name/value MATCHES {$m}",
                        Map.of("m", "v1"),
                        "SELECT d FROM DUMMY d WHERE d/name/value MATCHES {'v1'}"),
                ReplacementTestParam.success(
                        "SELECT d FROM DUMMY d WHERE d/name/value MATCHES {$m1, $m2}",
                        Map.of("m1", "v1", "m2", "v2"),
                        "SELECT d FROM DUMMY d WHERE d/name/value MATCHES {'v1', 'v2'}"),
                ReplacementTestParam.success(
                        "SELECT d FROM DUMMY d WHERE d/name/value MATCHES {$ma}",
                        Map.of("ma", List.of("v1", "v2")),
                        "SELECT d FROM DUMMY d WHERE d/name/value MATCHES {'v1', 'v2'}"),
                ReplacementTestParam.success(
                        "SELECT d FROM DUMMY d WHERE d/name/value MATCHES {$a, $b, $c}",
                        Map.of("a", List.of("v1", "v2"), "b", List.of(), "c", "v3"),
                        "SELECT d FROM DUMMY d WHERE d/name/value MATCHES {'v1', 'v2', 'v3'}"),
                ReplacementTestParam.rejected(
                        "SELECT d FROM DUMMY d WHERE d/name/value MATCHES {$a}",
                        Map.of("a", List.of()),
                        "Parameter replacement resulted in empty operand list"));
    }

    private static void assertReplaceParameters(String srcAql, Map<String, Object> parameterMap, String expected) {
        AqlQuery query = AqlQuery.parse(srcAql);
        AqlParameterPostProcessor.replaceParameters(query, parameterMap);
        String rendered = query.render();
        try {
            AqlQuery.parse(rendered);
        } catch (AqlParseException e) {
            fail("Produced invalid query %s : \n  %s", rendered, e.getMessage());
        }
        assertThat(rendered).isEqualTo(expected);
    }

    private static AbstractThrowableAssert<?, ? extends Throwable> assertReplaceParametersRejected(
            String srcAql, Map<String, Object> parameterMap) {
        AqlQuery query = AqlQuery.parse(srcAql);
        return assertThatThrownBy(() -> AqlParameterPostProcessor.replaceParameters(query, parameterMap));
    }

    record ReplacementTestParam(
            String srcAql,
            Map<String, Object> parameterMap,
            Class<? extends RuntimeException> expectedException,
            String expected) {

        static ReplacementTestParam success(String srcAql, Map<String, Object> parameterMap, String expectedAql) {
            return new ReplacementTestParam(srcAql, parameterMap, null, expectedAql);
        }

        static ReplacementTestParam rejected(String srcAql, Map<String, Object> parameterMap, String expectedMessage) {
            return new ReplacementTestParam(srcAql, parameterMap, AqlParseException.class, expectedMessage);
        }

        void doAssert() {
            if (expectedException == null) {
                assertReplaceParameters(srcAql, parameterMap, expected);
            } else {
                var ta = assertReplaceParametersRejected(srcAql, parameterMap).isInstanceOf(expectedException);
                if (expected != null) {
                    ta.hasMessageContaining(expected);
                }
            }
        }
    }
}
