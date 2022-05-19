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
package org.ehrbase.aql.compiler;

import java.util.*;
import org.ehrbase.aql.definition.FunctionDefinition;
import org.ehrbase.aql.definition.I_VariableDefinition;

public class AuditVariables {

    public static final String AUDIT_VARIABLE_PREFIX = "$__AUDIT_";
    public static final String EHR_PATH = "ehr_id/value";
    public static final String TEMPLATE_PATH = "archetype_details/template_id/value";
    // a list of audit variables needed if not present
    // defined as: path, alias, usual symbol
    // NB. only the path is relevant for identification
    public static final String[][] requiredAuditVariables = {
        {EHR_PATH, AUDIT_VARIABLE_PREFIX + "EHR_ID", "e"},
        {TEMPLATE_PATH, AUDIT_VARIABLE_PREFIX + "TEMPLATE_ID", "c"}
    };

    // set of path to skip whenever used in aggregate function
    private Set<String> skipList;

    public AuditVariables() {
        this.skipList = new HashSet<>();
    }

    /**
     * return true if the variable is also an audit variable used for logging (f.e. ehr_id/value)
     * @param variable
     * @return
     */
    public boolean isAuditVariable(I_VariableDefinition variable) {

        boolean retval = false;

        for (String[] auditVarDef : requiredAuditVariables) {
            if (variable instanceof FunctionDefinition) {
                // check if this function uses an audit variable
                if (variable.getPath().contains(auditVarDef[0])) {
                    skipList.add(auditVarDef[0]);
                }
            } else if (variable.getPath() != null && variable.getPath().equals(auditVarDef[0])) {
                retval = true;
                break;
            }
        }

        return retval;
    }

    public void addResults(Map<String, Set<Object>> auditResultMap, String path, Set<Object> resultSetForVariable) {
        // check if this path is not to be skipped
        if (!skipList.contains(path)) {
            auditResultMap.put(path, resultSetForVariable);
        }
    }
}
