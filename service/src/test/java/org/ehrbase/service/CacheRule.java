/*
 * Copyright (c) 2019 Stefan Spiska (Vitasystems GmbH) and Hannover Medical School.
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

package org.ehrbase.service;

import org.ehrbase.aql.containment.JsonPathQueryResult;
import org.ehrbase.aql.containment.TemplateIdAqlTuple;
import org.ehrbase.aql.containment.TemplateIdQueryTuple;
import org.ehrbase.aql.sql.queryimpl.ItemInfo;
import org.ehrbase.validation.Validator;
import org.ehrbase.webtemplate.model.WebTemplate;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;

import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.spi.CachingProvider;
import java.util.List;
import java.util.UUID;

import static org.ehrbase.configuration.CacheConfiguration.FIELDS_CACHE;
import static org.ehrbase.configuration.CacheConfiguration.INTROSPECT_CACHE;
import static org.ehrbase.configuration.CacheConfiguration.MULTI_VALUE_CACHE;
import static org.ehrbase.configuration.CacheConfiguration.OPERATIONAL_TEMPLATE_CACHE;
import static org.ehrbase.configuration.CacheConfiguration.QUERY_CACHE;
import static org.ehrbase.configuration.CacheConfiguration.VALIDATOR_CACHE;
import static org.ehrbase.configuration.CacheConfiguration.buildCache;

public class CacheRule extends TestWatcher {
    public CacheManager cacheManager;

    @Override
    protected void starting(Description description) {
        CachingProvider cachingProvider = Caching.getCachingProvider();
        cacheManager = cachingProvider.getCacheManager();
        buildCache(INTROSPECT_CACHE, UUID.class, WebTemplate.class, cacheManager, true);
        buildCache(OPERATIONAL_TEMPLATE_CACHE, String.class, OPERATIONALTEMPLATE.class, cacheManager, true);
        buildCache(VALIDATOR_CACHE, UUID.class, Validator.class, cacheManager, true);
        buildCache(QUERY_CACHE, TemplateIdQueryTuple.class, JsonPathQueryResult.class, cacheManager, true);
        buildCache(FIELDS_CACHE, TemplateIdAqlTuple.class, ItemInfo.class, cacheManager, true);
        buildCache(MULTI_VALUE_CACHE, String.class, List.class, cacheManager, false);
    }

    @Override
    protected void finished(Description description) {
        cacheManager.close();
    }
}

