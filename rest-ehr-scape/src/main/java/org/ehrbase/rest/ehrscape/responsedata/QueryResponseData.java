/*
 * Copyright (c) 2019 Stefan Spiska (Vitasystems GmbH)  and Hannover Medical School.
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

package org.ehrbase.rest.ehrscape.responsedata;

import org.ehrbase.response.ehrscape.QueryResultDto;
import org.ehrbase.response.ehrscape.query.ResultHolder;

import java.util.List;
import java.util.Map;

public class QueryResponseData extends ActionRestResponseData {
    private String executedAQL;
    private List<List<String>> explain;
    private List<ResultHolder> resultSet;

    public QueryResponseData(QueryResultDto dto) {
        executedAQL = dto.getExecutedAQL();
        explain = dto.getExplain();
        resultSet = dto.getResultSet();
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

    public List<ResultHolder> getResultSet() {
        return resultSet;
    }

    public void setResultSet(List<ResultHolder> resultSet) {
        this.resultSet = resultSet;
    }
}
