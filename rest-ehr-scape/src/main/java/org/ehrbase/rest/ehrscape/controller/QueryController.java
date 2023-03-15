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
package org.ehrbase.rest.ehrscape.controller;

import java.util.HashMap;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.ehrbase.api.annotations.TenantAware;
import org.ehrbase.api.authorization.EhrbaseAuthorization;
import org.ehrbase.api.authorization.EhrbasePermission;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.service.QueryService;
import org.ehrbase.rest.ehrscape.responsedata.Action;
import org.ehrbase.rest.ehrscape.responsedata.QueryResponseData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@TenantAware
@RestController
@RequestMapping(
        path = "/rest/ecis/v1/query",
        produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
public class QueryController extends BaseController {

    private final QueryService queryService;

    @Autowired
    public QueryController(QueryService queryService) {
        this.queryService = Objects.requireNonNull(queryService);
    }

    @EhrbaseAuthorization(permission = EhrbasePermission.EHRBASE_QUERY_SEARCH_AD_HOC)
    @PostMapping
    public ResponseEntity<QueryResponseData> query(
            @RequestParam(value = "explain", defaultValue = "false") Boolean explain, @RequestBody String content) {

        String aqlString = extractQuery(content);

        QueryResponseData responseData =
                new QueryResponseData(queryService.query(aqlString, null, explain, new HashMap<>()));
        responseData.setAction(Action.EXECUTE);
        return ResponseEntity.ok(responseData);
    }

    private static String extractQuery(String content) {
        Pattern patternKey = Pattern.compile("(?<=\\\")(.*?)(?=\")");
        Matcher matcherKey = patternKey.matcher(content);

        if (!matcherKey.find()) {
            throw new IllegalArgumentException("Could not identified query type in content: " + content);
        }

        String type = matcherKey.group(1);
        if ("aql".equalsIgnoreCase(type)) {
            String query = content.substring(content.indexOf(':') + 1, content.lastIndexOf('\"'));
            return query.substring(query.indexOf('\"') + 1);

        } else if ("sql".equalsIgnoreCase(type)) {
            throw new InvalidApiParameterException("SQL queries are no longer supported");
        } else {
            throw new InvalidApiParameterException("No query parameter supplied");
        }
    }
}
