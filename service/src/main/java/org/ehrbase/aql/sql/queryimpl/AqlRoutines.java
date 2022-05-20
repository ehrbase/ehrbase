/*
 * Copyright (c) 2020 vitasystems GmbH and Hannover Medical School.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.aql.sql.queryimpl;

import static org.ehrbase.aql.sql.queryimpl.QueryImplConstants.AQL_NODE_ITERATIVE_FUNCTION;
import static org.ehrbase.aql.sql.queryimpl.QueryImplConstants.AQL_NODE_NAME_PREDICATE_MARKER;

import java.util.Arrays;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Configuration;
import org.jooq.Field;
import org.jooq.JSONB;
import org.jooq.impl.DSL;

public class AqlRoutines extends AqlDialects {

    private AqlRoutines() {
        super();
    }

    public static Field<JSONB> jsonArraySplitElements(Field<JSONB> jsonbVal) {
        return DSL.field(AQL_NODE_ITERATIVE_FUNCTION + "(" + jsonbVal + ")").cast(JSONB.class);
    }

    public static Field<JSONB> jsonArraySplitElements(Configuration configuration, Field<JSONB> jsonbVal) {
        isSupported(configuration);
        return DSL.field(AQL_NODE_ITERATIVE_FUNCTION + "(" + jsonbVal + ")").cast(JSONB.class);
    }

    public static Field<JSONB> jsonpathItem(Field<JSONB> jsonbVal, String[] elements) {
        return DSL.field("jsonb_extract_path(" + jsonbVal + "," + String.join(",", elements) + ")")
                .cast(JSONB.class);
    }

    public static Field<JSONB> jsonpathItem(Configuration configuration, Field<JSONB> jsonbVal, String[] elements) {
        isSupported(configuration);
        return DSL.field("jsonb_extract_path(" + jsonbVal + "," + String.join(",", elements) + ")")
                .cast(JSONB.class);
    }

    public static Field<JSONB> toJson(Configuration configuration, String expression) {
        isSupported(configuration);
        return DSL.field("to_json(" + expression + ")").cast(JSONB.class);
    }

    public static String jsonpathItemAsText(Field<JSONB> jsonbVal, String[] elements) {
        return "jsonb_extract_path_text(" + jsonbVal + "," + String.join(",", elements) + ")";
    }

    public static String jsonpathItemAsText(Configuration configuration, Field<JSONB> jsonbVal, String[] elements) {
        isSupported(configuration);
        return "jsonb_extract_path_text(" + jsonbVal + "," + String.join(",", elements) + ")";
    }

    public static String[] jsonpathParameters(String rawParameters) {
        String parametersFormatted = StringUtils.remove(StringUtils.remove(rawParameters, "'{"), "}'");
        return Arrays.stream(parametersFormatted.split(","))
                .map(s -> (s.startsWith("'") ? s.replace("'", "") : s))
                .map(s -> (!s.equals(AQL_NODE_NAME_PREDICATE_MARKER) ? "'" + s + "'" : s))
                .collect(Collectors.toList())
                .toArray(new String[] {});
    }
}
