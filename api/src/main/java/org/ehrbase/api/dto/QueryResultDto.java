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

package org.ehrbase.api.dto;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class QueryResultDto {
    private String executedAQL;
    private List<List<String>> explain;
    private Map<String, String> variables;
    private List<Map<String, Object>> resultSet;

    public List<Map<String, Object>> getResultSet() {
        return resultSet;
    }

    public void setResultSet(List<Map<String, Object>> resultSet) {
        this.resultSet = resultSet;
    }

    public String getExecutedAQL() {
        return executedAQL;
    }

    public void setExecutedAQL(String executedAQL) {
        this.executedAQL = executedAQL;
    }

    public List<List<String>> getExplain() {
        return explain;
    }

    public void setExplain(List<List<String>> explain) {
        this.explain = explain;
    }

    public Map<String, String> getVariables() {
        return variables;
    }

    public void setVariables(Map<String, String> variables) {
        this.variables = variables;
    }

    public boolean variablesIsEmpty() {
        return variables.size() == 0;
    }

    public boolean variablesContainsColumnId(String columnId){
        return getVariables().containsKey(columnId);
    }

    public String variablesPath(String columnId){
        return getVariables().get(columnId);
    }

    public Iterator<Map.Entry<String, String>> variablesIterator(){
        return variables.entrySet().iterator();
    }
}
