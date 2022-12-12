/*
 * Copyright (c) 2020 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.service;

import java.lang.management.ManagementFactory;
import org.ehrbase.api.definitions.ServerConfig;
import org.ehrbase.api.service.StatusService;
import org.ehrbase.dao.access.interfaces.I_DatabaseStatusAccess;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class StatusServiceImp extends BaseServiceImp implements StatusService {

    @Autowired(required = false)
    private BuildProperties buildProperties;

    @Autowired
    public StatusServiceImp(
            KnowledgeCacheService knowledgeCacheService, DSLContext dslContext, ServerConfig serverConfig) {
        super(knowledgeCacheService, dslContext, serverConfig);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getOperatingSystemInformation() {
        return String.format(
                "%s %s %s",
                ManagementFactory.getOperatingSystemMXBean().getName(),
                ManagementFactory.getOperatingSystemMXBean().getArch(),
                ManagementFactory.getOperatingSystemMXBean().getVersion());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getJavaVMInformation() {
        return String.format(
                "%s %s",
                ManagementFactory.getRuntimeMXBean().getVmVendor(),
                ManagementFactory.getRuntimeMXBean().getSystemProperties().get("java.runtime.version"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDatabaseInformation() {
        return I_DatabaseStatusAccess.retrieveDatabaseVersion(getDataAccess());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getEhrbaseVersion() {
        return this.buildProperties.getVersion();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getArchieVersion() {
        return this.buildProperties.get("archie.version");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getOpenEHR_SDK_Version() {
        return this.buildProperties.get("openEHR_SDK.version");
    }
}
