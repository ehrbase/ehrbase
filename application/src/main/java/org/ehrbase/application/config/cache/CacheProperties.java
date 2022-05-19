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
package org.ehrbase.application.config.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link ConfigurationProperties} for EHRbase cache configuration.
 *
 * @author Renaud Subiger
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "cache")
public class CacheProperties {

    /**
     * Whether to initialize the caches during application startup.
     */
    private boolean initOnStartup = true;

    /**
     * Whether to pre-build queries when a new template is added.
     */
    private boolean preBuildQueries = true;

    /**
     * The default node depth for pre-built queries.
     */
    private Integer preBuildQueriesDepth = 4;

    public boolean isInitOnStartup() {
        return initOnStartup;
    }

    public void setInitOnStartup(boolean initOnStartup) {
        this.initOnStartup = initOnStartup;
    }

    public boolean isPreBuildQueries() {
        return preBuildQueries;
    }

    public void setPreBuildQueries(boolean preBuildQueries) {
        this.preBuildQueries = preBuildQueries;
    }

    public Integer getPreBuildQueriesDepth() {
        return preBuildQueriesDepth;
    }

    public void setPreBuildQueriesDepth(Integer preBuildQueriesDepth) {
        this.preBuildQueriesDepth = preBuildQueriesDepth;
    }
}
