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

    public abstract static class CtxAttr<T> {
        private final String name = getClass().getSimpleName();

        public int hashCode() {
            return name.hashCode();
        }

        public boolean equals(Object obj) {
            return name.equals(obj);
        }
    }

    private static class Query extends CtxAttr<String> {}

    private static class QueryId extends CtxAttr<String> {}

    private static class Location extends CtxAttr<String> {}

    private static class Version extends CtxAttr<Integer> {}

    private static class EhrId extends CtxAttr<UUID> {}

    private static class TemplateId extends CtxAttr<String> {}

    private static class CompositionId extends CtxAttr<UUID> {}

    private static class DirectoryId extends CtxAttr<String> {}

    private static class ContributionId extends CtxAttr<UUID> {}

    private static class RemovedPatients extends CtxAttr<Set<String>> {}

    private static class QueryExecuteEndpoint extends CtxAttr<Boolean> {}

    public static CtxAttr<String> QUERY = new Query();
    public static CtxAttr<String> QUERY_ID = new QueryId();
    public static CtxAttr<String> LOCATION = new Location();
    public static CtxAttr<Integer> VERSION = new Version();
    public static CtxAttr<UUID> EHR_ID = new EhrId();
    public static CtxAttr<String> TEMPLATE_ID = new TemplateId();
    public static CtxAttr<UUID> COMPOSITION_ID = new CompositionId();
    public static CtxAttr<String> DIRECTORY_ID = new DirectoryId();
    public static CtxAttr<UUID> CONTRIBUTION_ID = new ContributionId();
    public static CtxAttr<Set<String>> REMOVED_PATIENTS = new RemovedPatients();
    public static CtxAttr<Boolean> QUERY_EXECUTE_ENDPOINT = new QueryExecuteEndpoint();

    public static final class HttpCtxMap extends HashMap<CtxAttr<?>, Object> {
        private static final long serialVersionUID = -910609086038543024L;

        private HttpCtxMap() {}
    }

    private static ThreadLocal<HttpCtxMap> httpContext = ThreadLocal.withInitial(() -> new HttpCtxMap());

    public static void clear() {
        httpContext.remove();
    }

    @SuppressWarnings("unchecked")
    public <T> T getValueBy(CtxAttr<T> key) {
        Map<CtxAttr<?>, Object> ctxMap = httpContext.get();
        return (T) ctxMap.get(key);
    }

    public static <V> void register(CtxAttr<V> key, V value) {
        httpContext.get().put(key, value);
    }

    public static <V0, V1> void register(CtxAttr<V0> key0, V0 value0, CtxAttr<V1> key1, V1 value1) {
        register(key0, value0);
        register(key1, value1);
    }

    public static <V0, V1, V2> void register(
            CtxAttr<V0> key0, V0 value0, CtxAttr<V1> key1, V1 value1, CtxAttr<V2> key2, V2 value2) {
        register(key0, value0);
        register(key1, value1);
        register(key2, value2);
    }

    public static <V0, V1, V2, V3> void register(
            CtxAttr<V0> key0,
            V0 value0,
            CtxAttr<V1> key1,
            V1 value1,
            CtxAttr<V2> key2,
            V2 value2,
            CtxAttr<V3> key3,
            V3 value3) {
        register(key0, value0);
        register(key1, value1);
        register(key2, value2);
        register(key3, value3);
    }

    public static <V0, V1, V2, V3, V4> void register(
            CtxAttr<V0> key0,
            V0 value0,
            CtxAttr<V1> key1,
            V1 value1,
            CtxAttr<V2> key2,
            V2 value2,
            CtxAttr<V3> key3,
            V3 value3,
            CtxAttr<V4> key4,
            V4 value4) {
        register(key0, value0);
        register(key1, value1);
        register(key2, value2);
        register(key3, value3);
        register(key4, value4);
    }

    public static void handle(HttpRestContextHandler handler) {
        handler.handle(httpContext.get());
    }
}
