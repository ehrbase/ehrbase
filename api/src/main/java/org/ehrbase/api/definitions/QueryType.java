/*
 * Copyright (c) 2025 vitasystems GmbH.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ehrbase.api.definitions;

import org.ehrbase.api.exception.InvalidApiParameterException;

/**
 * Enum representing the type of the stored query.
 */
public enum QueryType {

    AQL("AQL"),

    SQL("SQL");

    private final String type;

    QueryType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public static QueryType fromString(String type) {
        for (QueryType queryType : QueryType.values()) {
            if (queryType.type.equalsIgnoreCase(type)) {
                return queryType;
            }
        }
        throw new InvalidApiParameterException("Unsupported query type: " + type);
    }
}
