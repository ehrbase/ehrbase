/*
 *  Copyright (c) 2020 Vitasystems GmbH and Christian Chevalley (Hannover Medical School).
 *
 *  This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and  limitations under the License.
 *
 */

package org.ehrbase.aql.compiler;

import org.ehrbase.aql.definition.FunctionDefinition;
import org.ehrbase.aql.definition.I_VariableDefinition;
import org.ehrbase.aql.definition.VariableDefinition;

import java.util.List;

public class AuditVariables {

    public static final String AUDIT_VARIABLE_PREFIX = "$__AUDIT_";
    //a list of audit variables needed if not present
    //defines as: path, alias, symbol
    public static final String[][] requiredAuditVariables = {
            {"ehr_id/value", AUDIT_VARIABLE_PREFIX+"EHR_ID", "e"}
    };

    private final List<I_VariableDefinition> variables;

    public AuditVariables(List<I_VariableDefinition> variables) {
        this.variables = variables;
    }

    /**
     * complete the query by adding missing required audit variable. Added variables are tagged hidden
     * and therefore will not appear in the query result but will be held in the audit result map
     * @return
     */
    public List<I_VariableDefinition> complete(){

        //check if we deal with (aggregate) functions... if so, we cannot just insert the audit data select
        for (I_VariableDefinition variableDefinition: variables){
            if (variableDefinition instanceof FunctionDefinition)
                return variables;
        }

        for (String[] auditVarDef: requiredAuditVariables){
            //loop in variables to check for existence
            boolean found = false;

            for (I_VariableDefinition variableDefinition: variables) {
                if (variableDefinition.getPath() != null && variableDefinition.getPath().equals(auditVarDef[0])) {
                    found = true;
                    break;
                }
            }
            if (!found){
                //add a hidden audit variable
                I_VariableDefinition variableDefinition = new VariableDefinition(auditVarDef[0],  auditVarDef[1], auditVarDef[2], false, true);
                variables.add(variableDefinition);
            }
        }

        return variables;
    }

    /**
     * return true if the variable is also an audit variable used for logging (f.e. ehr_id/value)
     * @param variable
     * @return
     */
    public static boolean isAuditData(I_VariableDefinition variable){

        boolean retval = false;

        for (String[] auditVarDef: requiredAuditVariables) {
            if (variable.getPath() != null && variable.getPath().equals(auditVarDef[0])) {
                retval = true;
                break;
            }
        }

        return retval;
    }
}
