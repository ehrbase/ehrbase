/*
 * Copyright (c) 2019 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.jooq.dbencoding.wrappers.json.writer.translator_db2raw;

public class ArchieCompositionProlog {
    String compositionRoot;
    String compositionName;

    public ArchieCompositionProlog(String compositionRoot, String compositionName) {
        this.compositionRoot = compositionRoot;
        this.compositionName = compositionName;
    }

    public String toString() {
        String predicate = new NodeId(compositionRoot).predicate();

        if (predicate.isEmpty()) return "";

        String archetypeNodeId = predicate;

        StringBuffer prolog = new StringBuffer();
        prolog.append("{");
        prolog.append("\"archetype_node_id\":")
                .append("\"")
                .append(archetypeNodeId)
                .append("\"")
                .append(",");
        prolog.append("\"_type\":").append("\"COMPOSITION\"").append(",");
        prolog.append("\"name\" : {\n" + "    \"_type\" : \"DV_TEXT\",\n" + "    \"value\" : ")
                .append("\"")
                .append(compositionName)
                .append("\"")
                .append("}")
                .append(",");

        return prolog.toString();
    }
}
