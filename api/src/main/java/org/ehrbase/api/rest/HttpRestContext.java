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
package org.ehrbase.api.rest;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class HttpRestContext {

    public interface HttpCtx {
        <T> T get(CtxAttr<T> attr);
    }

    public static final class CtxAttr<T> { }

    public static CtxAttr<String> QUERY = new CtxAttr<>();
    public static CtxAttr<String> QUERY_ID = new CtxAttr<>();
    public static CtxAttr<String> LOCATION = new CtxAttr<>();
    public static CtxAttr<Integer> VERSION = new CtxAttr<>();
    public static CtxAttr<UUID> EHR_ID = new CtxAttr<>();
    public static CtxAttr<String> TEMPLATE_ID = new CtxAttr<>();
    public static CtxAttr<UUID> COMPOSITION_ID = new CtxAttr<>();
    public static CtxAttr<String> DIRECTORY_ID = new CtxAttr<>();
    public static CtxAttr<UUID> CONTRIBUTION_ID = new CtxAttr<>();
    public static CtxAttr<Set<String>> REMOVED_PATIENTS = new CtxAttr<>();
    public static CtxAttr<Boolean> QUERY_EXECUTE_ENDPOINT = new CtxAttr<>();

    private static final ThreadLocal<Map<CtxAttr<?>, Object>> httpContext = ThreadLocal.withInitial(HashMap::new);

    public static void clear() {
        httpContext.remove();
    }

    public static <V> void register(CtxAttr<V> key, V value) {
        httpContext.get().put(key, value);
    }

    public static <V0, V1> void register(CtxAttr<V0> key0, V0 value0, CtxAttr<V1> key1, V1 value1) {
        Map<CtxAttr<?>, Object> map = httpContext.get();
        map.put(key0, value0);
        map.put(key1, value1);
    }

    public static <V0, V1, V2> void register(
            CtxAttr<V0> key0, V0 value0, CtxAttr<V1> key1, V1 value1, CtxAttr<V2> key2, V2 value2) {
        Map<CtxAttr<?>, Object> map = httpContext.get();
        map.put(key0, value0);
        map.put(key1, value1);
        map.put(key2, value2);
    }

    public static <V0, V1, V2, V3> void register(
            CtxAttr<V0> key0, V0 value0, CtxAttr<V1> key1, V1 value1, CtxAttr<V2> key2, V2 value2, CtxAttr<V3> key3, V3 value3) {
        Map<CtxAttr<?>, Object> map = httpContext.get();
        map.put(key0, value0);
        map.put(key1, value1);
        map.put(key2, value2);
        map.put(key3, value3);
    }

    public static void handle(HttpRestContextHandler handler) {
        Map<CtxAttr<?>, Object> ctxAttrObjectMap = httpContext.get();
        handler.handle(new HttpCtx() {
            @Override
            public <T> T get(CtxAttr<T> attr) {
                return (T) ctxAttrObjectMap.get(attr);
            }
        });
    }
}
