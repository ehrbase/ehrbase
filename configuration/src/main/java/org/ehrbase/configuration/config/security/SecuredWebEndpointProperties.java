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
package org.ehrbase.configuration.config.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param access extended property on spring actuator config that defines who can access the management endpoint.
 * @param csrfValidationEnabled enables or disabled CSRF validation on management endpoint, default is enabled.
 */
@ConfigurationProperties(prefix = "management.endpoints.web")
public record SecuredWebEndpointProperties(AccessType access, boolean csrfValidationEnabled) {

    /**
     * Supported values for the <code>management.endpoints.web.access</code> property value.
     */
    public enum AccessType {
        ADMIN_ONLY,
        PRIVATE,
        PUBLIC
    }
}
