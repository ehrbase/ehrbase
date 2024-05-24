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
package org.ehrbase.service;

import static org.ehrbase.util.Lazy.lazy;

import java.lang.management.ManagementFactory;
import java.util.function.Supplier;
import org.ehrbase.api.service.StatusService;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StatusServiceImp implements StatusService {

    @SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection")
    @Autowired(required = false)
    private BuildProperties buildProperties;

    private final DSLContext dslContext;

    public StatusServiceImp(DSLContext dslContext) {

        this.dslContext = dslContext;
    }

    private final Supplier<String> operatingSystemInformation = lazy(() -> String.format(
            "%s %s %s",
            ManagementFactory.getOperatingSystemMXBean().getName(),
            ManagementFactory.getOperatingSystemMXBean().getArch(),
            ManagementFactory.getOperatingSystemMXBean().getVersion()));

    /**
     * {@inheritDoc}
     */
    @Override
    public String getOperatingSystemInformation() {
        return operatingSystemInformation.get();
    }

    private final Supplier<String> javaVMInformation = lazy(() -> String.format(
            "%s %s",
            ManagementFactory.getRuntimeMXBean().getVmVendor(),
            ManagementFactory.getRuntimeMXBean().getSystemProperties().get("java.runtime.version")));

    /**
     * {@inheritDoc}
     */
    @Override
    public String getJavaVMInformation() {
        return javaVMInformation.get();
    }

    private final Supplier<String> databaseInformation = lazy(() -> getDSLContext()
            .select(DSL.function("VERSION", String.class))
            .fetchOne()
            .value1());

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public String getDatabaseInformation() {
        return databaseInformation.get();
    }

    private final Supplier<String> ehrbaseVersion =
            lazy(() -> getBuildProperties().getVersion());

    /**
     * {@inheritDoc}
     */
    @Override
    public String getEhrbaseVersion() {
        return ehrbaseVersion.get();
    }

    private final Supplier<String> archieVersion =
            lazy(() -> getBuildProperties().get("archie.version"));

    /**
     * {@inheritDoc}
     */
    @Override
    public String getArchieVersion() {
        return archieVersion.get();
    }

    private final Supplier<String> ehrbaseSdkVersion =
            lazy(() -> getBuildProperties().get("openEHR_SDK.version"));

    /**
     * {@inheritDoc}
     */
    @Override
    public String getOpenEHR_SDK_Version() {
        return ehrbaseSdkVersion.get();
    }

    private BuildProperties getBuildProperties() {
        return buildProperties;
    }

    private DSLContext getDSLContext() {
        return dslContext;
    }
}
