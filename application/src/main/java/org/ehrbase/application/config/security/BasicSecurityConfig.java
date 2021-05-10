/*
 * Copyright (c) 2021 Vitasystems GmbH and Jake Smolka (Hannover Medical School).
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.application.config.security;

import static org.ehrbase.application.config.security.SecurityYAMLConfig.ADMIN;
import static org.ehrbase.application.config.security.SecurityYAMLConfig.USER;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ConditionalOnProperty(name = "security.authType", havingValue = "BASIC")
@Configuration
@EnableWebSecurity
public class BasicSecurityConfig extends WebSecurityConfigurerAdapter {

  private final SecurityYAMLConfig securityYAMLConfig;

  private final Logger logger = LoggerFactory.getLogger(getClass());

  public BasicSecurityConfig(SecurityYAMLConfig securityYAMLConfig) {
    this.securityYAMLConfig = securityYAMLConfig;
  }

  @Autowired
  public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {

    // For Basic Auth: assigns specific roles to specific users. Enables conditional handling in
    // configure()
    auth.inMemoryAuthentication()
        .withUser(securityYAMLConfig.getAuthUser())
        .password(passwordEncoder().encode(securityYAMLConfig.getAuthPassword()))
        .roles(USER)
        .and()
        .withUser(securityYAMLConfig.getAuthAdminUser())
        .password(passwordEncoder().encode(securityYAMLConfig.getAuthAdminPassword()))
        .roles(ADMIN);
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {

    logger.info("Using basic authentication.");

    http.cors()
        .and()
        .csrf()
        .disable()
        .authorizeRequests()
        // Specific routes with ../admin/.. and actuator /status/.. endpoints require admin role
        .antMatchers("/rest/openehr/v1/admin/**", "/status/**")
        .hasRole(ADMIN)
        // Everything else is open to all users of role admin and user
        .antMatchers("/**")
        .hasAnyRole(ADMIN, USER)
        .and()
        .sessionManagement()
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        .and()
        .httpBasic();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
