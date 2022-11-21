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
package org.ehrbase.aql.sql;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jooq.Record;
import org.jooq.Result;

/**
 * Wrapper calls for a query result
 */
public class AqlResult {
    private final Result<Record> records;
    private final List<List<String>> explain;
    private Map<String, String> aqlColumns;
    private Map<String, Set<Object>> auditResultMap = new HashMap<>();

    AqlResult(Result<Record> records, List<List<String>> explain) {
        this.records = records;
        this.explain = explain;
    }

    public Result<Record> getRecords() {
        return records;
    }

    public List<List<String>> getExplain() {
        return explain;
    }

    public Map<String, String> getVariables() {
        return aqlColumns;
    }

    public void setVariables(Map<String, String> variables) {
        aqlColumns = variables;
    }

    public void setAuditResultMap(Map<String, Set<Object>> auditResultMap) {
        this.auditResultMap = auditResultMap;
    }

    public Map<String, Set<Object>> getAuditResultMap() {
        return auditResultMap;
    }

    public boolean variablesContains(String fieldName) {
        if (aqlColumns.containsKey(fieldName)) return true;

        // else iterate on values
        for (String value : aqlColumns.values()) {
            if (value.equals(fieldName)) return true;
        }

        return false;
    }
}
