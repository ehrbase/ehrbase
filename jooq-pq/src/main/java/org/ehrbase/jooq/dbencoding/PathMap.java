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
package org.ehrbase.jooq.dbencoding;

import java.util.Map;
import java.util.TreeMap;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.PredicateUtils;

public class PathMap {

    public PathMap() {
        throw new IllegalStateException("Use only static method in this class");
    }
    /**
     * to remain consistent regarding datastructure, we use a map which prevents duplicated keys... and throw
     * an exception if one is detected...
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> getInstance() {
        return MapUtils.predicatedMap(new TreeMap<>(), PredicateUtils.uniquePredicate(), null);
    }
}
