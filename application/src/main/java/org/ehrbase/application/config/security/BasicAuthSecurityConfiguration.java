/*
 * Copyright (c) 2021-2022 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.application.config.security;

import static org.ehrbase.application.config.security.SecurityProperties.ADMIN;
import static org.springframework.security.config.Customizer.withDefaults;

import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

/**
 * {@link Configuration} for Basic authentication.
 *
 * @author Jake Smolka
 * @author Renaud Subiger
 * @since 1.0.0
 */
@Configuration
@ConditionalOnProperty(prefix = "security", name = "authType", havingValue = "basic")
@EnableWebSecurity
public class BasicAuthSecurityConfiguration {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @PostConstruct
    public void initialize() {
        logger.info("Using basic authentication");
    }

    @Bean
    public InMemoryUserDetailsManager inMemoryUserDetailsManager(
            SecurityProperties properties, ObjectProvider<PasswordEncoder> passwordEncoder) {

        return new InMemoryUserDetailsManager(
                User.withUsername(properties.getAuthUser())
                        .password("{noop}" + properties.getAuthPassword())
                        .roles(SecurityProperties.USER)
                        .build(),
                User.withUsername(properties.getAuthAdminUser())
                        .password("{noop}" + properties.getAuthAdminPassword())
                        .roles(SecurityProperties.ADMIN)
                        .build());
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.addFilterBefore(new SecurityFilter(), BasicAuthenticationFilter.class);

        http.cors(withDefaults())
                .csrf(c -> c.ignoringRequestMatchers("/rest/**"))
                .authorizeHttpRequests(auth -> auth.requestMatchers("/rest/admin/**", "/management/**")
                        .hasRole(ADMIN)
                        .anyRequest()
                        .hasAnyRole(ADMIN, SecurityProperties.USER))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(withDefaults());

        return http.build();
    }
}
