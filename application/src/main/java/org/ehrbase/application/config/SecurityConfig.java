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
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.util.Formatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    // Roles, independent of auth type
    public static final String ADMIN = "ADMIN";
    public static final String USER = "USER";

    private final SecurityYAMLConfig securityYAMLConfig;

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Formatter formatter = new Formatter();

    public SecurityConfig(SecurityYAMLConfig securityYAMLConfig) {
        this.securityYAMLConfig = securityYAMLConfig;
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {

        // For Basic Auth: assigns specific roles to specific users. Enables conditional handling in configure()
        auth.inMemoryAuthentication()
                    .withUser(securityYAMLConfig.getAuthUser())
                    .password(passwordEncoder().encode(securityYAMLConfig.getAuthPassword()))
                    .roles(USER)
                //.authorities("ROLE_USER");
                .and()
                    .withUser(securityYAMLConfig.getAuthAdminUser())
                    .password(passwordEncoder().encode(securityYAMLConfig.getAuthAdminPassword()))
                    .roles(ADMIN);
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
                        .authorizeRequests()
                        // Specific routes with ../admin/.. require admin role
                        .antMatchers("/rest/openehr/v1/admin/**").hasRole(ADMIN)
                        // Everything else is open to all users of role admin and user
                        .antMatchers("/**").hasAnyRole(ADMIN, USER)
                        .and()
                        .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                        .and()
                        .httpBasic();
                break;
            case OAUTH:
                logger.info("Using OAuth2 authentication.");
                http
                        .authorizeRequests()
                        // Specific routes with ../admin/.. require admin role
                        .antMatchers("/rest/openehr/v1/admin/**").hasRole(ADMIN)
                        // Everything else is open to all users of role admin and user
                        .antMatchers("/**").hasAnyRole(ADMIN, USER)
                        .and()
                        .oauth2ResourceServer()
                        .jwt()
                        .jwtAuthenticationConverter(getJwtAuthenticationConverter());
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

    // Converter creates list of "ROLE_*" (upper case) authorities for each realm access role from JWT
    private Converter<Jwt, AbstractAuthenticationToken> getJwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            final Map<String, Object> realmAccess = (Map<String, Object>) jwt.getClaims().get("realm_access");
            return ((List<String>) realmAccess.get("roles")).stream()
                    .map(roleName -> "ROLE_" + roleName.toUpperCase())
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        });
        return converter;
    }
}
