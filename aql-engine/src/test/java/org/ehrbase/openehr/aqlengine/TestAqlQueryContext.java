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
package org.ehrbase.openehr.aqlengine;

import org.ehrbase.api.dto.AbstractAqlQueryContext;
import org.ehrbase.api.service.StatusService;

public class TestAqlQueryContext extends AbstractAqlQueryContext {

    private static final StatusService STATUS_SERVICE = new StatusService() {

        @Override
        public String getOperatingSystemInformation() {
            return "";
        }

        @Override
        public String getJavaVMInformation() {
            return "";
        }

        @Override
        public String getDatabaseInformation() {
            return "";
        }

        @Override
        public String getEhrbaseVersion() {
            return "";
        }

        @Override
        public String getArchieVersion() {
            return "";
        }

        @Override
        public String getOpenEHR_SDK_Version() {
            return "";
        }
    };

    public TestAqlQueryContext() {
        super(STATUS_SERVICE, true, true);
    }

    @Override
    protected boolean isGeneratorDetailsEnabled() {
        return false;
    }

    @Override
    public boolean showExecutedAql() {
        return false;
    }

    @Override
    public boolean isDryRun() {
        return false;
    }

    @Override
    public boolean showExecutedSql() {
        return false;
    }

    @Override
    public boolean showQueryPlan() {
        return false;
    }
}
