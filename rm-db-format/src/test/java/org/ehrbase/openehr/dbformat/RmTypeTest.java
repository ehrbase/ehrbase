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
package org.ehrbase.openehr.dbformat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class RmTypeTest {

    @Disabled
    @Test
    void createSqlAliasingFunction() {
        String func =
                """
        CREATE OR REPLACE FUNCTION rm_type_alias(t text)
        RETURNS text AS $$
        BEGIN
        %s
        END;
        $$
            LANGUAGE plpgsql
            IMMUTABLE
            STRICT
            PARALLEL SAFE;
        """;

        String statement = RmType.values.stream()
                .sorted(Comparator.comparing(RmType::type))
                .map(att -> "WHEN '%s' THEN RETURN '%s';"
                        .formatted(att.type(), att.alias())
                        .indent(4))
                .collect(Collectors.joining(
                        "", "CASE t\n", "ELSE RAISE EXCEPTION 'Missing type alias for %', t;".indent(4) + "END CASE;"));
        System.out.printf(func, statement.indent(4));
    }

    @Test
    void checkStructureAliases() {
        Arrays.stream(StructureRmType.values())
                .forEach(v -> assertThat(RmType.getAlias(v.name())).isEqualTo(v.getAlias()));

        Set<String> typesWithAliases = RmType.values.stream().map(RmType::type).collect(Collectors.toSet());

        typesWithAliases.forEach(t -> assertThatThrownBy(() -> RmType.getRmType(t))
                .withFailMessage(() -> "Alias name clashes with an existing type: " + t)
                .isInstanceOf(IllegalArgumentException.class));
    }
}
