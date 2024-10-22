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
package org.ehrbase.openehr.aqlengine.featurecheck;

import org.ehrbase.api.service.SystemService;
import org.ehrbase.openehr.aqlengine.AqlConfigurationProperties;
import org.ehrbase.openehr.sdk.aql.dto.AqlQuery;
import org.springframework.stereotype.Component;

@Component
public final class AqlQueryFeatureCheck {

    private final FeatureCheck[] featureChecks;

    public AqlQueryFeatureCheck(SystemService systemService, AqlConfigurationProperties aqlConfigurationProperties) {
        this.featureChecks = new FeatureCheck[] {
            new FromCheck(systemService, aqlConfigurationProperties),
            new SelectCheck(systemService),
            new WhereCheck(systemService),
            new OrderByCheck(systemService)
        };
    }

    public void ensureQuerySupported(AqlQuery aqlQuery) {
        for (FeatureCheck featureCheck : featureChecks) {
            featureCheck.ensureSupported(aqlQuery);
        }
    }
}
