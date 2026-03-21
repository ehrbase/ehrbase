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
package org.ehrbase.rest.api.config;

import java.util.Map;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

/**
 * Contributes EHRbase version information to Spring Boot Actuator {@code /actuator/info} endpoint.
 * Used for Kubernetes probes and operational monitoring.
 */
@Component
public class ActuatorInfoContributor implements InfoContributor {

    @Override
    public void contribute(Info.Builder builder) {
        builder.withDetail(
                "ehrbase",
                Map.of(
                        "springBoot", SpringBootVersion.getVersion(),
                        "archie", "3.17.0",
                        "openehrSdk", "2.31.0-SNAPSHOT",
                        "java", System.getProperty("java.version"),
                        "api", "v1"));
    }
}
