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
package org.ehrbase.aql.sql.queryimpl;

import java.util.StringTokenizer;

public class NodeIds { // from Locatable

    private NodeIds() {}

    public static String toCamelCase(String underscoreSeparated) {
        if (!underscoreSeparated.contains("_")) {
            return underscoreSeparated;
        }
        StringTokenizer tokens = new StringTokenizer(underscoreSeparated, "_");
        StringBuilder buf = new StringBuilder();
        while (tokens.hasMoreTokens()) {
            String word = tokens.nextToken();
            if (buf.length() == 0) {
                buf.append(word);
            } else {
                buf.append(word.substring(0, 1).toUpperCase());
                buf.append(word.substring(1));
            }
        }
        return buf.toString();
    }
}
