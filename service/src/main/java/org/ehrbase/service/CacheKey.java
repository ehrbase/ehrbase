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
package org.ehrbase.service;

import java.io.Serializable;
import java.util.Objects;

public class CacheKey<T extends Serializable> implements Serializable {
    private static final long serialVersionUID = -5926035933645900703L;

    static <T0 extends Serializable> CacheKey<T0> of(T0 val, Short tenantId) {
        return new CacheKey<>(val, tenantId);
    }

    private final T val;
    private final Short sysTenant;

    public T getVal() {
        return val;
    }

    public Short getSysTenant() {
        return sysTenant;
    }

    private CacheKey(T val, Short sysTenant) {
        this.val = val;
        this.sysTenant = sysTenant;
    }

    public int hashCode() {
        return Objects.hash(val, sysTenant);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof CacheKey) || ((CacheKey) obj).val.getClass() != val.getClass())
            return false;
        CacheKey<T> ck = (CacheKey<T>) obj;
        return val.equals(ck.val) && sysTenant.equals(ck.sysTenant);
    }
}
