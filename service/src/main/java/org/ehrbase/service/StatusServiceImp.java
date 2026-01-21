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
import java.util.Optional;
import java.util.function.Supplier;
import org.ehrbase.api.service.StatusService;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StatusServiceImp implements StatusService {

    public static final String UNKNOWN = "<unknown>";
    private final Supplier<String> operatingSystemInformation;
    private final Supplier<String> javaVMInformation;
    private final Supplier<String> ehrbaseVersion;
    private final Supplier<String> databaseInformation;
    private final Supplier<String> archieVersion;
    private final Supplier<String> ehrbaseSdkVersion;

    public StatusServiceImp(DSLContext dslContext, Optional<BuildProperties> buildProperties) {

        this.operatingSystemInformation = lazy(() -> String.format(
                "%s %s %s",
                ManagementFactory.getOperatingSystemMXBean().getName(),
                ManagementFactory.getOperatingSystemMXBean().getArch(),
                ManagementFactory.getOperatingSystemMXBean().getVersion()));

        this.javaVMInformation = lazy(() -> String.format(
                "%s %s",
                ManagementFactory.getRuntimeMXBean().getVmVendor(),
                ManagementFactory.getRuntimeMXBean().getSystemProperties().get("java.runtime.version")));

        this.ehrbaseVersion =
                lazy(() -> buildProperties.map(BuildProperties::getVersion).orElse(UNKNOWN));

        this.databaseInformation = lazy(() -> dslContext
                .select(DSL.function("VERSION", String.class))
                .fetchOne()
                .value1());

        this.archieVersion =
                lazy(() -> buildProperties.map(p -> p.get("archie.version")).orElse(UNKNOWN));

        this.ehrbaseSdkVersion = lazy(
                () -> buildProperties.map(p -> p.get("openEHR_SDK.version")).orElse(UNKNOWN));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getOperatingSystemInformation() {
        return operatingSystemInformation.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getJavaVMInformation() {
        return javaVMInformation.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public String getDatabaseInformation() {
        return databaseInformation.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getEhrbaseVersion() {
        return ehrbaseVersion.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getArchieVersion() {
        return archieVersion.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getOpenEHR_SDK_Version() {
        return ehrbaseSdkVersion.get();
    }
}
