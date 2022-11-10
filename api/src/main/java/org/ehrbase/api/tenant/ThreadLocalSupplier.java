/*
 * Copyright (c) 2022 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.api.tenant;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class ThreadLocalSupplier<V> implements Supplier<V>, Consumer<V> {
    private static Map<Class<?>, ThreadLocalSupplier<?>> supplier = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public static <V0> ThreadLocalSupplier<V0> supplyFor(Class<V0> clazz) {
        return (ThreadLocalSupplier<V0>) supplier.computeIfAbsent(clazz, c -> new ThreadLocalSupplier<V0>());
    }

    private final ThreadLocal<V> tl = new ThreadLocal<>();

    private ThreadLocalSupplier() {}

    public V get() {
        return tl.get();
    }

    public void accept(V val) {
        tl.set(val);
    }

    public void reset() {
        tl.remove();
    }
}
