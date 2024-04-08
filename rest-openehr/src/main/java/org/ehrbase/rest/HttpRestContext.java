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
package org.ehrbase.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class HttpRestContext {

    public interface RestAttr {
        public String name();

        public Class<?> getAttributeType();
    }

    public enum StdRestAttr implements RestAttr {
        QUERY(String.class),
        QUERY_ID(String.class),
        LOCATION(String.class),
        VERSION(Integer.class),
        EHR_ID(UUID.class),
        TEMPLATE_ID(String.class),
        COMPOSITION_ID(String.class),
        DIRECTORY_ID(String.class),
        CONTRIBUTION_ID(String.class),
        REMOVED_PATIENTS(Set.class),
        QUERY_EXECUTE_ENDPOINT(Boolean.class);

        private final Class<?> attributeType;

        private StdRestAttr(Class<?> type) {
            this.attributeType = type;
        }

        public Class<?> getAttributeType() {
            return attributeType;
        }
    };

    private static final String ERR_BAD_ARG = "Value should be of type[%s]";

    private static ThreadLocal<Map<RestAttr, List<Object>>> httpContext =
            ThreadLocal.withInitial(() -> new HashMap<>());

    public static void remove() {
        httpContext.remove();
    }

    public static void register(RestAttr key, Object value) {
        if (!key.getAttributeType().isAssignableFrom(value.getClass()))
            throw new IllegalArgumentException(ERR_BAD_ARG.formatted(key.getAttributeType()));

        httpContext.get().computeIfAbsent(key, s -> new ArrayList<>());
        httpContext.get().get(key).add(value);
    }
    ;

    public static void register(RestAttr key0, Object value0, RestAttr key1, Object value1) {
        register(key0, value0);
        register(key1, value1);
    }
    ;

    public static void register(
            RestAttr key0, Object value0, RestAttr key1, Object value1, RestAttr key2, Object value2) {
        register(key0, value0);
        register(key1, value1);
        register(key2, value2);
    }
    ;

    public static void register(
            RestAttr key0,
            Object value0,
            RestAttr key1,
            Object value1,
            RestAttr key2,
            Object value2,
            RestAttr key3,
            Object value3) {
        register(key0, value0);
        register(key1, value1);
        register(key2, value2);
        register(key3, value3);
    }

    public static void register(
            RestAttr key0,
            Object value0,
            RestAttr key1,
            Object value1,
            RestAttr key2,
            Object value2,
            RestAttr key3,
            Object value3,
            RestAttr key4,
            Object value4) {
        register(key0, value0);
        register(key1, value1);
        register(key2, value2);
        register(key3, value3);
        register(key4, value4);
    }
}
