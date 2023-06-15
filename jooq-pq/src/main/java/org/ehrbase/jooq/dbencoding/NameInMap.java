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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * encode a RM object name in an array of name values
 */
public class NameInMap {

    private Map<String, Object> map;
    private Map<String, Object> nameValues;

    public NameInMap(Map<String, Object> map, Map<String, Object> nameValues) {
        this.map = map;
        this.nameValues = nameValues;
    }

    public Map<String, Object> toMap() {
        List<Map<String, Object>> nameListMap = new ArrayList<>();
        nameListMap.add(nameValues);
        map.put(CompositionSerializer.TAG_NAME, nameListMap);
        return map;
    }
}
