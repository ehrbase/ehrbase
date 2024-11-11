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

import java.util.function.Function;

public class NullSafeFuncWrapper<I, O> {

    public static <I0, O0> NullSafeFuncWrapper<I0, O0> of(Function<I0, O0> func) {
        return new NullSafeFuncWrapper<>(func);
    }

    private final Function<I, O> access;

    private NullSafeFuncWrapper(Function<I, O> access) {
        this.access = access;
    }

    public <OO> NullSafeFuncWrapper<I, OO> after(Function<O, OO> func) {
        Function<I, OO> acc = in -> {
            if (in == null) return null;

            O out = NullSafeFuncWrapper.this.access.apply(in);

            if (out == null) return null;

            return func.apply(out);
        };

        return new NullSafeFuncWrapper<>(acc);
    }

    public O apply(I in) {
        return access.apply(in);
    }
}
