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
package org.ehrbase.util;

import java.util.function.Supplier;

/**
 * Lazy property value that can be used to cache values with "expensive" calculations during first time of access.
 * </br>
 * Usage:
 * <pre>
 * {@code
 *     import static org.ehrbase.util.Lazy.lazy;
 *
 *     class MyClass {
 *
 *         private final Supplier<String> lazyValue = lazy(() -> "some expensive calculated value");
 *
 *         public String getValue() {
 *             return lazyValue.get();
 *         }
 *     }
 * }
 * </pre>
 * @param <V>
 */
public final class Lazy<V> implements Supplier<V> {

    private V value; // = null
    private final Supplier<V> supplier;

    public Lazy(Supplier<V> supplier) {
        this.supplier = supplier;
    }

    @Override
    public V get() {
        if (value == null) {
            value = supplier.get();
        }
        return value;
    }

    public static <V> Supplier<V> lazy(Supplier<V> supplier) {
        return new Lazy<V>(supplier);
    }
}
