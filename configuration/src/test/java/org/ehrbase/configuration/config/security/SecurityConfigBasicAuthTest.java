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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;

class SecurityConfigBasicAuthTest {

    private SecuredWebEndpointProperties securedWebEndpointProperties() {
        return new SecuredWebEndpointProperties(SecuredWebEndpointProperties.AccessType.ADMIN_ONLY, false);
    }

    @SuppressWarnings("deprecation")
    @Test
    void ensureNopPasswordEncoderIsUsed() throws NoSuchMethodException {

        SecurityConfigBasicAuth config =
                new SecurityConfigBasicAuth(new WebEndpointProperties(), securedWebEndpointProperties());

        Bean bean = config.getClass().getMethod("passwordEncoder").getAnnotation(Bean.class);
        assertNotNull(bean, "Expected PasswordEncoder bean to be defined");

        assertSame(
                NoOpPasswordEncoder.getInstance(),
                config.passwordEncoder(),
                "Expected NoOpPasswordEncoder oassword encoder to be used.");
    }
}
