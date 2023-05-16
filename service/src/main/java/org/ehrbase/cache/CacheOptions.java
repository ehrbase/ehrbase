/*
 * Copyright (c) 2021 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.cache;

/**
 * @author Renaud Subiger
 * @since 1.0.0
 */
public class CacheOptions {

    public static final String INTROSPECT_CACHE = "introspectCache";

    public static final String OPERATIONAL_TEMPLATE_CACHE = "operationaltemplateCache";

    public static final String QUERY_CACHE = "queryCache";

    public static final String FIELDS_CACHE = "fieldsCache";

    public static final String MULTI_VALUE_CACHE = "multivaluedCache";
    public static final String CONCEPT_CACHE_ID = "conceptCacheId";
    public static final String CONCEPT_CACHE_CONCEPT_ID = "conceptCacheConceptId";
    public static final String CONCEPT_CACHE_DESCRIPTION = "conceptCacheDescription";
    public static final String TERRITORY_CACHE = "territoryCache";
    public static final String LANGUAGE_CACHE = "languageCache";

    public static final String USER_ID_CACHE = "userIdCache";
    public static final String SYS_TENANT = "sysTenant";

    private boolean preBuildQueries;

    private int preBuildQueriesDepth;

    private boolean preInitialize = false;

    public boolean isPreInitialize() {
        return preInitialize;
    }

    public void setPreInitialize(boolean preInitialize) {
        this.preInitialize = preInitialize;
    }

    public boolean isPreBuildQueries() {
        return preBuildQueries;
    }

    public void setPreBuildQueries(boolean preBuildQueries) {
        this.preBuildQueries = preBuildQueries;
    }

    public int getPreBuildQueriesDepth() {
        return preBuildQueriesDepth;
    }

    public void setPreBuildQueriesDepth(int preBuildQueriesDepth) {
        this.preBuildQueriesDepth = preBuildQueriesDepth;
    }
}
