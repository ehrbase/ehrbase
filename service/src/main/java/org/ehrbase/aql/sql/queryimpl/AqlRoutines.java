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

import java.util.Arrays;
import org.apache.commons.lang3.ArrayUtils;
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

        return DSL.function(AQL_NODE_ITERATIVE_FUNCTION, JSONB.class, jsonbVal);
    }

    public static Field<JSONB> jsonArraySplitElements(Configuration configuration, Field<JSONB> jsonbVal) {
        isSupported(configuration);

        return jsonArraySplitElements(jsonbVal);
    }

    public static Field<JSONB> jsonpathItem(Field<JSONB> jsonbVal, String[] elements) {

        return DSL.function("jsonb_extract_path", JSONB.class, ArrayUtils.addFirst(buildParameter(elements), jsonbVal));
    }

    public static Field<JSONB> jsonpathItem(Configuration configuration, Field<JSONB> jsonbVal, String[] elements) {
        isSupported(configuration);
        return jsonpathItem(jsonbVal, elements);
    }

    public static Field<String> jsonpathItemAsText(Field<JSONB> jsonbVal, String[] elements) {

        return DSL.function(
                "jsonb_extract_path_text", String.class, ArrayUtils.addFirst(buildParameter(elements), jsonbVal));
    }

    public static Field<String> jsonpathItemAsText(
            Configuration configuration, Field<JSONB> jsonbVal, String[] elements) {
        isSupported(configuration);
        return jsonpathItemAsText(jsonbVal, elements);
    }

    private static Field[] buildParameter(String[] elements) {
        return Arrays.stream(elements).map(DSL::val).toArray(Field[]::new);
    }

    public static String[] jsonpathParameters(String rawParameters) {
        String parametersFormatted = StringUtils.remove(StringUtils.remove(rawParameters, "'{"), "}'");
        return Arrays.stream(parametersFormatted.split(","))
                .map(s -> (s.startsWith("'") ? s.replace("'", "") : s))
                //      .map(s -> (!s.equals(AQL_NODE_NAME_PREDICATE_MARKER) ? "'" + s + "'" : s))
                .toArray(String[]::new);
    }
}
