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
package org.ehrbase.jooq.pg.util;

import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.Stream;
import org.jooq.AggregateFunction;
import org.jooq.Field;
import org.jooq.JSONB;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

public final class AdditionalSQLFunctions {
    private AdditionalSQLFunctions() {
        // NOOP
    }

    public static Field<JSONB> jsonb_array_elements(Field<JSONB> jsonbArray) {
        return DSL.function("jsonb_array_elements", JSONB.class, jsonbArray);
    }

    public static <T> Field<T> jsonb_extract_path_text(Class<T> aClass, Field<JSONB> jsonb, String... path) {
        Field<?>[] arguments = Stream.concat(
                        Stream.of(jsonb), Arrays.stream(path).map(DSL::inline))
                .toArray(Field<?>[]::new);
        return DSL.function("jsonb_extract_path_text", aClass, arguments);
    }

    public static Field<JSONB> jsonb_dv_ordered_magnitude(Field<JSONB> dvOrderedField) {
        return DSL.function("jsonb_dv_ordered_magnitude", JSONB.class, dvOrderedField);
    }

    /**
     * @see #jsonbAttributePathText
     */
    public static Field<String> jsonbAttributePathText(Field<JSONB> jsonb, String... path) {
        return jsonbAttributePathText(jsonb, Arrays.stream(path));
    }

    /**
     * Extract the text value from the given <code>JSONB</code> at the requested <code>path</code>
     * </p>
     * <code>data -> path[0] -> … -> path[n] ->> 0</code>
     *
     * @param jsonb to extract text as path from
     * @param path json path of the text
     * @return textValue as field
     */
    public static Field<String> jsonbAttributePathText(Field<JSONB> jsonb, Stream<String> path) {
        Field<JSONB> jsonbField = jsonb;
        Iterator<String> it = path.iterator();
        while (it.hasNext()) {
            jsonbField = DSL.jsonbGetAttribute(jsonbField, DSL.inline(it.next()));
        }
        return DSL.jsonbGetElementAsText(jsonbField, DSL.inline(0));
    }

    public static Field<JSONB> to_jsonb(Object value) {
        Field<?> inline = DSL.inline(value);
        if (value instanceof String) {
            inline = inline.cast(String.class);
        }
        return to_jsonb(inline);
    }

    public static Field<JSONB> to_jsonb(Field target) {
        return DSL.function("to_jsonb", JSONB.class, target);
    }

    public static AggregateFunction<JSONB> max_dv_ordered(Field<?> f) {
        return DSL.aggregate("max_dv_ordered", SQLDataType.JSONB, f);
    }

    public static AggregateFunction<JSONB> min_dv_ordered(Field<?> f) {
        return DSL.aggregate("min_dv_ordered", SQLDataType.JSONB, f);
    }

    /**
     * DSL::count returns an integer.
     * Since row counts may require a long we have to use this workaround.
     *
     * @param distinct
     * @param f
     * @return
     */
    public static AggregateFunction<Long> count(boolean distinct, Field<?> f) {
        return distinct
                ? DSL.aggregateDistinct("count", SQLDataType.BIGINT, f)
                : DSL.aggregate("count", SQLDataType.BIGINT, f == null ? DSL.field(DSL.raw("*")) : f);
    }
}
