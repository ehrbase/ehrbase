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
import static org.ehrbase.application.config.security.SecurityProperties.USER;

import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;

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
public class BasicAuthSecurityConfiguration extends WebSecurityConfigurerAdapter {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final SecurityProperties properties;

    public BasicAuthSecurityConfiguration(SecurityProperties securityProperties) {
        this.properties = securityProperties;
    }

    @PostConstruct
    public void initialize() {
        logger.info("Using basic authentication");
    }

    @Override
    public void configure(AuthenticationManagerBuilder auth) throws Exception {
        // @formatter:off
        auth.inMemoryAuthentication()
                .withUser(properties.getAuthUser())
                .password("{noop}" + properties.getAuthPassword())
                .roles(USER)
                .and()
                .withUser(properties.getAuthAdminUser())
                .password("{noop}" + properties.getAuthAdminPassword())
                .roles(ADMIN);
        // @formatter:on
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // @formatter:off
        http.cors()
                .and()
                .csrf()
                .ignoringAntMatchers("/rest/**")
                .and()
                .authorizeRequests()
                .antMatchers("/rest/admin/**", "/management/**")
                .hasRole(ADMIN)
                .anyRequest()
                .hasAnyRole(ADMIN, USER)
                .and()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .httpBasic();
        // @formatter:on
    }
}
