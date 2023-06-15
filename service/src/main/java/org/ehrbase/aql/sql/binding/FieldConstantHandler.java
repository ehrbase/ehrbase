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
package org.ehrbase.aql.sql.binding;

import static org.ehrbase.aql.sql.queryimpl.attribute.GenericJsonPath.OTHER_CONTEXT;

import java.util.List;
import org.ehrbase.aql.definition.I_VariableDefinition;
import org.ehrbase.aql.sql.queryimpl.DefaultColumnId;
import org.ehrbase.ehr.util.LocatableHelper;
import org.ehrbase.jooq.dbencoding.wrappers.json.I_DvTypeAdapter;
import org.jooq.Field;
import org.jooq.impl.DSL;

public class FieldConstantHandler {
    private final I_VariableDefinition variableDefinition;

    public FieldConstantHandler(I_VariableDefinition variableDefinition) {
        this.variableDefinition = variableDefinition;
    }

    public boolean isConstant() {
        // split path in segments
        if (variableDefinition.getPath() == null
                || variableDefinition.getPath().toUpperCase().startsWith("EHR_STATUS")) return false;

        List<String> segments = LocatableHelper.dividePathIntoSegments(variableDefinition.getPath());

        if (segments.size() >= 2 && segments.get(segments.size() - 1).equals(I_DvTypeAdapter.ARCHETYPE_NODE_ID))
            return true;

        return false;
    }

    public Field<?> field() {
        List<String> segments = LocatableHelper.dividePathIntoSegments(variableDefinition.getPath());

        if (segments.get(segments.size() - 1).equals(I_DvTypeAdapter.ARCHETYPE_NODE_ID))
            return DSL.field(DSL.val(implicitArchetypeNodeId(segments.get(segments.size() - 2))))
                    .as(alias());
        return null;
    }

    private String implicitArchetypeNodeId(String nodeId) {
        if (nodeId.equals(OTHER_CONTEXT)) return "at0001";
        return nodeId.substring(nodeId.indexOf("[") + 1, nodeId.lastIndexOf("]"));
    }

    String alias() {
        String alias = variableDefinition.getAlias();
        if (alias == null) alias = DefaultColumnId.value(variableDefinition);
        return alias;
    }
}
