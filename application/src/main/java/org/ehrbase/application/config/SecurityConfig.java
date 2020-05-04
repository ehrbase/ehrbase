/*
 * Copyright (c) 2020 Vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.application.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Formatter;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled=true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private SecurityYAMLConfig securityYAMLConfig;

    private Logger logger = LoggerFactory.getLogger(getClass());
    private Formatter formatter = new Formatter();

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {

        auth.inMemoryAuthentication()
                .withUser(securityYAMLConfig.getAuthUser())
                .password(passwordEncoder().encode(securityYAMLConfig.getAuthPassword()))
                .authorities("ROLE_USER");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        switch (securityYAMLConfig.getAuthType()) {
            case BASIC:
                logger.info("Using basic authentication.");
                logger.info(formatter.format(
                        "Username: %s Password: %s", securityYAMLConfig.getAuthUser(), securityYAMLConfig.getAuthPassword()
                ).toString());
                http
                        .csrf().disable()
                        .authorizeRequests().anyRequest().authenticated()
                        .and()
                        .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                        .and()
                        .httpBasic();
                break;
            case OAUTH:
                logger.info("Using OAuth2 authentication.");
                http
                        .authorizeRequests()
                        .anyRequest().authenticated()
                        .and()
                        .oauth2ResourceServer()
                        .jwt()
                        .jwtAuthenticationConverter(new JwtGrantedAuthoritiesConverter());
                break;
            case NONE:
            default:
                logger.warn("Authentication disabled!");
                logger.warn("To enable security set security.authType to BASIC or OAUTH in yaml properties file.");
                http
                        .csrf().disable()
                        .authorizeRequests().anyRequest().permitAll();
                break;
        }
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
