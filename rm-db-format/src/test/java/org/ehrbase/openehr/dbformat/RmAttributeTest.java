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

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class RmAttributeTest {

    @Disabled
    @Test
    void createSqlAliasingFunction() {
        String func =
                """
        CREATE OR REPLACE FUNCTION rm_attribute_alias(a text)
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

        Map<Field<Object>, Field<Object>> attributeToAliasMap = RmAttribute.VALUES.stream()
                .sorted(Comparator.comparing(RmAttribute::attribute))
                .collect(Collectors.toMap(
                        r -> DSL.field(DSL.sql("'" + r.attribute() + "'")),
                        r -> DSL.field(DSL.sql("RETURN '" + r.alias() + "';")),
                        (a, b) -> null,
                        LinkedHashMap::new));
        String a = DSL.case_(DSL.field(DSL.sql("a")))
                        .mapFields(attributeToAliasMap)
                        .else_(DSL.field(DSL.sql("RAISE EXCEPTION 'Missing attribute alias for %', a;")))
                + " case;";
        System.out.printf(
                func,
                a.indent(4)
                        .replace("when ", "WHEN ")
                        .replace("then ", "THEN ")
                        .replace("case ", "CASE ")
                        .replace("else ", "ELSE ")
                        .replace("end case;", "END CASE;"));
    }

    @Test
    void checkAliases() {
        Set<String> attributes =
                RmAttribute.VALUES.stream().map(RmAttribute::attribute).collect(Collectors.toSet());

        attributes.forEach(a -> assertThatThrownBy(() -> RmAttribute.getAttribute(a))
                .withFailMessage(() -> "Alias name clashes with an existing attribute " + a)
                .isInstanceOf(IllegalArgumentException.class));
    }

    @Test
    void rmToJsonPathParts() {
        assertThat(RmAttribute.rmToJsonPathParts("archetype_details/template_id/value"))
                .isEqualTo(new String[] {"ad", "tm", "V"});
        assertThat(RmAttribute.rmToJsonPathParts("subject/external_ref/id/value"))
                .isEqualTo(new String[] {"su", "er", "X", "V"});
    }
}
