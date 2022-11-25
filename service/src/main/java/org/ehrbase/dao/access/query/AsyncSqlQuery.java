/*
 * Copyright (c) 2016-2022 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.dao.access.query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.exception.DataAccessException;

/**
 * Attempt to use JAVA 8 CompletableFuture.
 *
 * @author Christian Chevalley
 * @author Jake Smolka
 * @since 1.0
 */
public class AsyncSqlQuery implements Supplier<Map<String, Object>> {

    private final I_DomainAccess domainAccess;

    private final String queryString;

    public AsyncSqlQuery(I_DomainAccess domainAccess, String queryString) {
        this.domainAccess = domainAccess;
        this.queryString = queryString;
    }

    /**
     * Fetches query result from database.
     *
     * @return results fetched from the database
     * @throws IllegalArgumentException on SQL exception
     */
    public Result<Record> fetchQueryResults() {
        try {
            return domainAccess.getContext().fetch(queryString);
        } catch (DataAccessException e) {
            throw new IllegalArgumentException("Error occurred while executing the SQL statement", e);
        }
    }

    public Map<String, Object> toJson(Result<Record> records) {
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("executedSQL", queryString);

        List<Map<String, Object>> resultList = new ArrayList<>();
        for (Record current : records) {
            Map<String, Object> fieldMap = new HashMap<>();
            for (Field<?> field : records.fields()) {
                fieldMap.put(field.getName(), current.getValue(field));
            }
            resultList.add(fieldMap);
        }

        resultMap.put("resultSet", resultList);
        return resultMap;
    }

    @Override
    public Map<String, Object> get() {
        Result<Record> records = fetchQueryResults();
        if (records != null && !records.isEmpty()) {
            return toJson(records);
        } else return new HashMap<>();
    }

    /**
     * Executes the fetch asynchronously.
     *
     * @return the query result
     * @throws InternalServerException if fetching failed
     */
    public Map<String, Object> fetch() throws InternalServerException {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        CompletableFuture<Map<String, Object>> fetch = CompletableFuture.supplyAsync(this, executorService);
        try {
            return fetch.get();
        } catch (ExecutionException e) {
            throw new InternalServerException("fetching of async SQL query failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore interrupted state, according to
            // https://rules.sonarsource.com/java/RSPEC-2142
            throw new InternalServerException("ThreadDeath", e);
        }
    }
}
