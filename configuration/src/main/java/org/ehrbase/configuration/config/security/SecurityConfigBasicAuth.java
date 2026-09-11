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

import jakarta.annotation.PostConstruct;
import java.util.List;
import org.ehrbase.configuration.config.security.SecurityProperties.AuthTypes;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.stereotype.Component;

/**
 * {@link Component} for Basic authentication.
 */
@Component
@ConditionalOnProperty(prefix = "security", name = "authType", havingValue = "basic")
public final class SecurityConfigBasicAuth extends SecurityConfig {

    // Roles, when not using OAuth2
    public static final String ADMIN = "ADMIN";

    public static final String USER = "USER";

    private final SecurityProperties securityProperties;

    public SecurityConfigBasicAuth(WebEndpointProperties webEndpointProperties, SecurityProperties securityProperties) {
        super(webEndpointProperties);
        this.securityProperties = securityProperties;
    }

    @PostConstruct
    public void initialize() {
        logger.info("Using basic authentication");
    }

    @Override
    protected SecurityConfigParams securityConfigParams() {
        return new SecurityConfigParams(
                BasicAuthenticationFilter.class,
                AuthTypes.BASIC,
                ADMIN,
                USER,
                List.of(ADMIN, USER),
                List.of(ADMIN, USER),
                securityProperties.getAdditionalAuthorizations());
    }

    @Override
    public HttpSecurity configureHttpSecurity(HttpSecurity http) throws Exception {
        return super.configureHttpSecurity(http).httpBasic(Customizer.withDefaults());
    }

    @SuppressWarnings("deprecation")
    @Bean
    public PasswordEncoder passwordEncoder() {
        // We use a nop encoder because BCrypt slows down request by 10x on some systems
        return NoOpPasswordEncoder.getInstance();
    }

    @Bean
    public UserDetailsManager userDetailsManager(SecurityProperties properties, PasswordEncoder passwordEncoder) {

        return new InMemoryUserDetailsManager(
                User.withUsername(properties.getAuthUser())
                        .password(properties.getAuthPassword())
                        .roles(USER)
                        .passwordEncoder(passwordEncoder::encode)
                        .build(),
                User.withUsername(properties.getAuthAdminUser())
                        .password(properties.getAuthAdminPassword())
                        .roles(ADMIN)
                        .passwordEncoder(passwordEncoder::encode)
                        .build());
    }
}
