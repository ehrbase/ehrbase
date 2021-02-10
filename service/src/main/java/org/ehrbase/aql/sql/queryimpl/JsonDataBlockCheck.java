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

package org.ehrbase.aql.sql.queryimpl;

import java.util.List;

public class JsonDataBlockCheck {

    //CCH 191018 EHR-163 matches trailing '/value'
    // '/name,0' is to matches path relative to the name array

    public static final String MATCH_NODE_PREDICATE = "(/(content|events|protocol|data|description|instruction|items|activities|activity|composition|entry|evaluation|observation|action)\\[([(0-9)|(A-Z)|(a-z)|\\-|_|\\.]*)\\])|" +
            "(/value|/value,definingCode|/time|/name,0|/origin|/origin,/name,0|/origin,/value|/value,mappings)|" +
            "(/value,mappings,0)(|,purpose|,target|,purpose,definingCode|,purpose,definingCode,terminologyId|,target,terminologyId)|" +
            // common locatable attributes
            "(/uid,/value|/language|/language,terminologyId|/encoding|/encoding,terminologyId)";
    
    private final List<String> jqueryPath;

    public JsonDataBlockCheck(List<String> jqueryPath){
        this.jqueryPath = jqueryPath;
    }

    public boolean isJsonBlock(){
        if (jqueryPath.isEmpty())
            return true;
        
        if (jqueryPath.get(0).startsWith("/feeder_audit"))
            return checkFeederAuditPath();
        else
            return jqueryPath.get(jqueryPath.size() - 1).matches(MATCH_NODE_PREDICATE);
    }

    private boolean checkFeederAuditPath(){
        String[] tokens = jqueryPath.get(0).split(",");

        String lastItem = tokens[tokens.length - 1];
        
        return !(lastItem.equalsIgnoreCase("value")||
                 lastItem.equalsIgnoreCase("system_id")||
                lastItem.equalsIgnoreCase("version_id")||
                 //PartyIdentified
                 lastItem.equalsIgnoreCase("name")||
                 //PartyRef
                lastItem.equalsIgnoreCase("namespace")||
                //dvIdentifier
                 lastItem.equalsIgnoreCase("issuer")||
                 lastItem.equalsIgnoreCase("assigner")||
                 lastItem.equalsIgnoreCase("id")||
                 lastItem.equalsIgnoreCase("type"));

    }
}
