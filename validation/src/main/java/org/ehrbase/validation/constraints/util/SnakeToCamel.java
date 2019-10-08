/*
 * Copyright (c) 2019 Vitasystems GmbH and Hannover Medical School.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ehrbase.validation.constraints.util;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Handle snake case to camel case conversion
 * Used to deal with attribute such as 'archetype_node_id' and convert to 'archetypeNodeId'
 */
public class SnakeToCamel {

    private String string;

    public SnakeToCamel(String string) {
        this.string = string;
    }

    /**
     * Convert to camel case
     * @return converted string
     */
    public String convert(){
        return Arrays.stream(string.split("\\_"))
                .map(String::toLowerCase)
                .map(s -> s.substring(0, 1).toUpperCase() + s.substring(1))
                .collect(Collectors.joining());
    }


}
