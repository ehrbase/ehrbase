/*
 * Copyright (c) 2019 Vitasystems GmbH and Jake Smolka (Hannover Medical School).
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

package org.ehrbase.rest.openehr.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.ehrbase.api.dto.QueryResultDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.*;

@JacksonXmlRootElement
public class QueryResponseData {

    //the initial query without substitution (!)
    @JsonProperty(value = "q")
    private String query;

    @JsonProperty(value = "name")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String name;

    //the list of columns as defined in the SELECT clause (with a path...)
    @JsonProperty(value = "columns")
    private List<Map<String, String>> columns;
    //the actual resultset
    @JsonProperty(value = "rows")
    private List<List<Object>> rows;

    public QueryResponseData(QueryResultDto queryResultDto) {
        this.query = queryResultDto.getExecutedAQL();
        this.name = null;

        this.columns = new ArrayList<>();
        this.rows = new ArrayList<>();

        //set the columns definitions
        if (!queryResultDto.variablesIsEmpty()) {
            if (queryResultDto.getResultSet().size() > 0) {
                //the order of the column definitions is set by the resultSet ordering
                Map<String, Object> record = queryResultDto.getResultSet().get(0);
                int count = 0;

                for (String columnId : record.keySet()) {
                    Map<String, String> fieldMap = new HashMap<>();

                    if (queryResultDto.variablesContainsColumnId(columnId)) {
                        fieldMap.put("name", columnId);
                        fieldMap.put("path", queryResultDto.variablesPath(columnId));
                    } else {
                        fieldMap.put("name", "#" + count);
                        fieldMap.put("path", columnId);
                    }
                    count++;
                    columns.add(fieldMap);
                }
            }
            else {
                //use the variable definition instead
                int count = 0;
                Iterator<Map.Entry<String, String>> variablesIterator = queryResultDto.variablesIterator();
                while (variablesIterator.hasNext()){
                    Map<String, String> fieldMap = new HashMap<>();
                    Map.Entry<String, String> variableEntry = variablesIterator.next();
                    if (variableEntry.getKey() != null) {
                        fieldMap.put("name", variableEntry.getKey());
                        fieldMap.put("path", variableEntry.getValue());
                    } else {
                        fieldMap.put("name", "#" + count);
                        fieldMap.put("path", variableEntry.getValue());
                    }
                    count++;
                    columns.add(fieldMap);
                }
            }

            //set the row results
            for (Map valueSet : queryResultDto.getResultSet()){
                List values = new ArrayList(valueSet.values());
                rows.add(values);
            }
        }
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public List<Map<String, String>> getColumns() {
        return columns;
    }

    public void setColumns(List<Map<String, String>> columns) {
        this.columns = columns;
    }

    public List<List<Object>> getRows() {
        return rows;
    }

    public void setRows(List<List<Object>> rows) {
        this.rows = rows;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
