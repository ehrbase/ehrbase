/*
 * Copyright (c) 2022 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.plugin.dto;

import java.util.Map;
import java.util.Objects;
import org.springframework.lang.Nullable;

/**
 * Wrapper for an Aql String <code>query</code> with optional <code>parameters</code>
 *
 * @author Stefan Spiska
 */
public class QueryWithParameters {

    private final String query;
    private final Map<String, Object> parameters;

    public QueryWithParameters(String query, @Nullable Map<String, Object> parameters) {
        this.query = query;
        this.parameters = parameters;
    }

    public String getQuery() {
        return query;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        QueryWithParameters that = (QueryWithParameters) o;
        return Objects.equals(query, that.query) && Objects.equals(parameters, that.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(query, parameters);
    }

    @Override
    public String toString() {
        return "QueryWithParameters{" + "query='" + query + '\'' + ", parameters=" + parameters + '}';
    }
}
