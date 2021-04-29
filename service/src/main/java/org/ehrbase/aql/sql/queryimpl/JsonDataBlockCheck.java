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

import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class JsonDataBlockCheck {

    //CCH 191018 EHR-163 matches trailing '/value'
    // '/name,0' is to matches path relative to the name array

    public static final String MATCH_NODE_PREDICATE = "(/(content|events|protocol|data|description|instruction|items|activities|activity|composition|entry|evaluation|observation|action)\\[([(0-9)|(A-Z)|(a-z)|\\-|_|\\.]*)\\])|" +
            "(/value|/value,defining_code|/time|/time,/value|/timing,/value|/name,0|/origin|/origin,/name,0|/origin,/value|/value,mappings)|" +
            "(/value,mappings,0)(|,purpose|,target|,purpose,defining_code|,purpose,defining_code,terminology_id|,target,terminology_id)|" +
            "(/ism_transition)(|,current_state|,transition|,careflow_step)|" +
            // common locatable attributes
            "(/uid,/value|/language|/language,terminology_id|/encoding|/encoding,terminology_id|/subject)";
    
    private final List<String> jqueryPath;

    public JsonDataBlockCheck(List<String> jqueryPath){
        this.jqueryPath = jqueryPath;
    }

    public boolean isJsonBlock(){
        if (jqueryPath.isEmpty())
            return true;
        
        if (jqueryPath.get(0).toUpperCase().contains("FEEDER_AUDIT"))
            return checkFeederAuditPath();
        else
            return checkResultingPath();
    }

    /**
     * use this method, whenever the attribute comes from a table column
     * @return
     */
    public boolean isJsonBlockStaticAttributeForm(){
        if (jqueryPath.isEmpty())
            return true;

        String lastNode = jqueryPath.get(jqueryPath.size() - 1);
        String previousNode = null;
        if (jqueryPath.size() > 2)
            previousNode = jqueryPath.get(jqueryPath.size() - 2);

        if (jqueryPath.get(0).toUpperCase().contains("FEEDER_AUDIT"))
            return checkFeederAuditPath(lastNode, previousNode);
        else
            return checkResultingPath(lastNode);
    }

    private boolean checkFeederAuditPath(){
        String[] tokens = jqueryPath.get(0).split(",");

        String lastItem = tokens[tokens.length - 1];
        
        return checkFeederAuditPath(lastItem, null);
    }

    private boolean checkFeederAuditPath(String lastItem, String previousItem){

        if (lastItem.equalsIgnoreCase("value") && previousItem != null && !previousItem.equals("value"))
            return true;
        else
            return !(
                    lastItem.equalsIgnoreCase("system_id")||
                    lastItem.equalsIgnoreCase("version_id")||
                    //PartyRef
                    lastItem.equalsIgnoreCase("namespace")||
                    //dvIdentifier
                    lastItem.equalsIgnoreCase("issuer")||
                    lastItem.equalsIgnoreCase("assigner")||
                    lastItem.equalsIgnoreCase("id")||
                    lastItem.equalsIgnoreCase("type"));

    }

    private boolean checkResultingPath(){
        int index = jqueryPath.size() - 1;
        if (jqueryPath.get(index).equals("0"))
            index--;
        String terminalNode = jqueryPath.get(index);
        if (index > 0 && terminalNode.startsWith("'") && jqueryPath.get(index - 1).equals(QueryImplConstants.AQL_NODE_NAME_PREDICATE_MARKER))
            terminalNode = jqueryPath.get(index - 2); //skip the node predicate marker
        return checkResultingPath(terminalNode);
    }

    private boolean checkResultingPath(String terminalNode){
        return terminalNode.matches(MATCH_NODE_PREDICATE) || isStructuredItem(terminalNode);
    }


    private boolean isStructuredItem(String terminalNode){
        String lastItem = StringUtils.substringAfterLast(terminalNode, ",");

        if (!lastItem.isEmpty())
            return lastItem.matches("defining_code|mappings|language|encoding|terminology_id|lower|upper");
        return false;

    }
}
