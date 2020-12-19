/*
* Modifications copyright (C) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School

* This file is part of Project EHRbase

* Copyright (c) 2015 Christian Chevalley
* This file is part of Project Ethercis
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
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.exception.DataAccessException;

/** Attempt to use JAVA 8 CompletableFuture Created by christian on 10/13/2016. */
public class AsyncAqlQuery implements Supplier<Map<String, Object>> {

  I_DomainAccess domainAccess;
  String queryString;
  ExecutorService executorService;
  ;

  public AsyncAqlQuery(I_DomainAccess domainAccess, String queryString) {
    this.domainAccess = domainAccess;
    this.queryString = queryString;
  }

  public Result<Record> fetchQueryResults() throws Exception {
    try {
      Result<Record> records = domainAccess.getContext().fetch(queryString);
      return records;
    } catch (DataAccessException e) {
      String message = e.getCause().getMessage();
      throw new IllegalArgumentException("AQL exception:" + message.replaceAll("\n", ","));
    }
  }

  public Map<String, Object> toJson(Result<Record> records) {
    Map<String, Object> resultMap = new HashMap<>();

    resultMap.put("executedAQL", queryString);

    List<Map> resultList = new ArrayList<>();

    for (Record record : records) {
      Map<String, Object> fieldMap = new HashMap<>();
      for (Field field : records.fields()) {
        fieldMap.put(field.getName(), record.getValue(field));
      }

      resultList.add(fieldMap);
    }

    resultMap.put("resultSet", resultList);
    //        try {
    //            Thread.sleep(10000);
    //        } catch (InterruptedException e) {
    //            e.printStackTrace();
    //        }
    return resultMap;
  }

  @Override
  public Map<String, Object> get() {
    Result<Record> records = null;
    try {
      records = fetchQueryResults();
    } catch (Exception e) {
      e.printStackTrace();
    }

    if (records != null && !records.isEmpty()) {
      Map<String, Object> resultMap = toJson(records);

      return resultMap;
    } else return new HashMap<>();
  }

  public Map<String, Object> fetch() throws ExecutionException, InterruptedException {
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    CompletableFuture<Map<String, Object>> fetch =
        CompletableFuture.supplyAsync(this, executorService);
    return fetch.get();
  }
}
