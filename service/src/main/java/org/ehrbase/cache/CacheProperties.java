/*
 * Copyright (c) 2024 vitasystems GmbH.
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
package org.ehrbase.cache;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link ConfigurationProperties} for EHRbase cache configuration.
 */
@ConfigurationProperties(prefix = "cache")
public class CacheProperties {

    private List<String> txProxyExcludedBeanNames = new ArrayList<>();
    /**
     * Whether to initialize the caches during application startup.
     */
    private boolean templateInitOnStartup = true;

    private boolean storedQueryInitOnStartup = true;

    public boolean isTemplateInitOnStartup() {
        return templateInitOnStartup;
    }

    public void setTemplateInitOnStartup(boolean templateInitOnStartup) {
        this.templateInitOnStartup = templateInitOnStartup;
    }

    public boolean isStoredQueryInitOnStartup() {
        return storedQueryInitOnStartup;
    }

    public void setStoredQueryInitOnStartup(boolean storedQueryInitOnStartup) {
        this.storedQueryInitOnStartup = storedQueryInitOnStartup;
    }

    private CacheConfig externalFhirTerminologyCacheConfig = new CacheConfig();
    private CacheConfig userIdCacheConfig = new CacheConfig();

    public CacheConfig getExternalFhirTerminologyCacheConfig() {
        return externalFhirTerminologyCacheConfig;
    }

    public void setExternalFhirTerminologyCacheConfig(CacheConfig externalFhirTerminologyCacheConfig) {
        this.externalFhirTerminologyCacheConfig = externalFhirTerminologyCacheConfig;
    }

    public CacheConfig getUserIdCacheConfig() {
        return userIdCacheConfig;
    }

    public void setUserIdCacheConfig(CacheConfig userIdCacheConfig) {
        this.userIdCacheConfig = userIdCacheConfig;
    }

    public List<String> getTxProxyExcludedBeanNames() {
        return txProxyExcludedBeanNames;
    }

    public void setTxProxyExcludedBeanNames(List<String> txProxyExcludedBeanNames) {
        this.txProxyExcludedBeanNames = txProxyExcludedBeanNames;
    }

    public static class CacheConfig {

        private ExpireTime expireAfterAccess;
        private ExpireTime expireAfterWrite;

        public ExpireTime getExpireAfterAccess() {
            return expireAfterAccess;
        }

        public void setExpireAfterAccess(ExpireTime expireAfterAccess) {
            this.expireAfterAccess = expireAfterAccess;
        }

        public ExpireTime getExpireAfterWrite() {
            return expireAfterWrite;
        }

        public void setExpireAfterWrite(ExpireTime expireAfterWrite) {
            this.expireAfterWrite = expireAfterWrite;
        }

        public static class ExpireTime {
            private long duration = 5;
            private TimeUnit unit = TimeUnit.MINUTES;

            public long getDuration() {
                return duration;
            }

            public void setDuration(long duration) {
                this.duration = duration;
            }

            public TimeUnit getUnit() {
                return unit;
            }

            public void setUnit(TimeUnit unit) {
                this.unit = unit;
            }
        }
    }
}
