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

import org.ehrbase.opt.query.I_QueryOptMetaData;
import org.ehrbase.validation.Validator;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;

import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.spi.CachingProvider;
import java.util.UUID;

import static org.ehrbase.configuration.CacheConfiguration.*;

public class CacheRule extends TestWatcher {
    public CacheManager cacheManager;

    @Override
    protected void starting(Description description) {
        CachingProvider cachingProvider = Caching.getCachingProvider();
        cacheManager = cachingProvider.getCacheManager();
        buildCache(INTROSPECT_CACHE, UUID.class, I_QueryOptMetaData.class, cacheManager, true);
        buildCache(OPERATIONAL_TEMPLATE_CACHE, String.class, OPERATIONALTEMPLATE.class, cacheManager, true);
        buildCache(VALIDATOR_CACHE, UUID.class, Validator.class, cacheManager, true);
    }

    @Override
    protected void finished(Description description) {
        cacheManager.close();
    }
}

