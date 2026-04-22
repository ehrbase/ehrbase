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
    private String templateInitOnStartup = Boolean.TRUE.toString();

    private boolean storedQueryInitOnStartup = true;
    private CacheConfig internalTemplateCacheConfig = new CacheConfig();
    private CacheConfig operationalTemplateCacheConfig = new CacheConfig();
    private CacheConfig templateIdUuidCacheConfig = new CacheConfig();
    private CacheConfig templateUuidIdCacheConfig = new CacheConfig();
    private CacheConfig templateListCacheConfig = new CacheConfig();
    private CacheConfig externalFhirTerminologyCacheConfig = new CacheConfig();
    private CacheConfig userIdCacheConfig = new CacheConfig();
    private CacheConfig storedQueryCacheConfig = new CacheConfig();

    public String getTemplateInitOnStartup() {
        return templateInitOnStartup;
    }

    public void setTemplateInitOnStartup(String templateInitOnStartup) {
        this.templateInitOnStartup = templateInitOnStartup;
    }

    public boolean isStoredQueryInitOnStartup() {
        return storedQueryInitOnStartup;
    }

    public void setStoredQueryInitOnStartup(boolean storedQueryInitOnStartup) {
        this.storedQueryInitOnStartup = storedQueryInitOnStartup;
    }

    public CacheConfig getInternalTemplateCacheConfig() {
        return internalTemplateCacheConfig;
    }

    public void setInternalTemplateCacheConfig(CacheConfig internalTemplateCacheConfig) {
        this.internalTemplateCacheConfig = internalTemplateCacheConfig;
    }

    public CacheConfig getOperationalTemplateCacheConfig() {
        return operationalTemplateCacheConfig;
    }

    public void setOperationalTemplateCacheConfig(CacheConfig operationalTemplateCacheConfig) {
        this.operationalTemplateCacheConfig = operationalTemplateCacheConfig;
    }

    public CacheConfig getTemplateUuidIdCacheConfig() {
        return templateUuidIdCacheConfig;
    }

    public void setTemplateUuidIdCacheConfig(CacheConfig templateUuidIdCacheConfig) {
        this.templateUuidIdCacheConfig = templateUuidIdCacheConfig;
    }

    public CacheConfig getTemplateIdUuidCacheConfig() {
        return templateIdUuidCacheConfig;
    }

    public void setTemplateIdUuidCacheConfig(CacheConfig templateIdUuidCacheConfig) {
        this.templateIdUuidCacheConfig = templateIdUuidCacheConfig;
    }

    public CacheConfig getTemplateListCacheConfig() {
        return templateListCacheConfig;
    }

    public void setTemplateListCacheConfig(CacheConfig templateListCacheConfig) {
        this.templateListCacheConfig = templateListCacheConfig;
    }

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

    public CacheConfig getStoredQueryCacheConfig() {
        return storedQueryCacheConfig;
    }

    public void setStoredQueryCacheConfig(CacheConfig storedQueryCacheConfig) {
        this.storedQueryCacheConfig = storedQueryCacheConfig;
    }

    public static class CacheConfig {

        private Integer initialCapacity;
        private Integer maximumSize;

        private ExpireTime expireAfterAccess;
        private ExpireTime expireAfterWrite;

        public Integer getInitialCapacity() {
            return initialCapacity;
        }

        public void setInitialCapacity(Integer initialCapacity) {
            this.initialCapacity = initialCapacity;
        }

        public Integer getMaximumSize() {
            return maximumSize;
        }

        public void setMaximumSize(Integer maximumSize) {
            this.maximumSize = maximumSize;
        }

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
