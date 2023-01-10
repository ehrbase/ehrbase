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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

/**
 * {@link Configuration} for OAuth2 authentication.
 *
 * @author Jake Smolka
 * @since 1.0.0
 */
@Deprecated
@Configuration
@EnableWebSecurity
@ConditionalOnProperty(prefix = "security", name = "auth-type", havingValue = "oauth")
public class OAuth2SecurityConfiguration extends WebSecurityConfigurerAdapter {

    private static final String PUBLIC = "PUBLIC";
    private static final String PRIVATE = "PRIVATE";
    public static final String ADMIN_ONLY = "ADMIN_ONLY";
    public static final String PROFILE_SCOPE = "PROFILE";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${management.endpoints.web.access:ADMIN_ONLY}")
    private String managementEndpointsAccessType;

    private final SecurityProperties securityProperties;

    private final OAuth2ResourceServerProperties oAuth2Properties;

    private final WebEndpointProperties managementWebEndpointProperties;

    public OAuth2SecurityConfiguration(
            SecurityProperties securityProperties,
            OAuth2ResourceServerProperties oAuth2Properties,
            WebEndpointProperties managementWebEndpointProperties) {
        this.securityProperties = securityProperties;
        this.oAuth2Properties = oAuth2Properties;
        this.managementWebEndpointProperties = managementWebEndpointProperties;
    }

    @PostConstruct
    public void initialize() {
        logger.info("Using OAuth2 authentication");
        logger.debug("Using issuer URI: {}", oAuth2Properties.getJwt().getIssuerUri());
        logger.debug("Using user role: {}", securityProperties.getOauth2UserRole());
        logger.debug("Using admin role: {}", securityProperties.getOauth2AdminRole());
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        String userRole = securityProperties.getOauth2UserRole();
        String adminRole = securityProperties.getOauth2AdminRole();

        // @formatter:off
        var registry = http.cors()
                .and()
                .authorizeRequests()
                .antMatchers("/rest/admin/**")
                .hasRole(adminRole)
                .antMatchers("/swagger-ui/**", "/v3/api-docs/**")
                .permitAll();

        var managementAuthorizedUrl = registry.and()
                .authorizeRequests()
                .antMatchers(this.managementWebEndpointProperties.getBasePath() + "/**");

        switch (managementEndpointsAccessType) {
            case ADMIN_ONLY ->
            // management endpoints are locked behind an authorization
            // and are only available for users with the admin role
            managementAuthorizedUrl.hasRole(adminRole);
            case PRIVATE ->
            // management endpoints are locked behind an authorization, but are available to any role
            managementAuthorizedUrl.hasAnyRole(adminRole, userRole, PROFILE_SCOPE);
            case PUBLIC ->
            // management endpoints can be accessed without an authorization
            managementAuthorizedUrl.permitAll();
            default -> throw new IllegalStateException(String.format(
                    "Unexpected management endpoints access control type %s", managementEndpointsAccessType));
        }

        registry.anyRequest()
                .hasAnyRole(adminRole, userRole, PROFILE_SCOPE)
                .and()
                .oauth2ResourceServer()
                .jwt()
                .jwtAuthenticationConverter(getJwtAuthenticationConverter());
        // @formatter:on
    }

    // Converter creates list of "ROLE_*" (upper case) authorities for each "realm access" role
    // and "roles" role from JWT
    @SuppressWarnings("unchecked")
    private Converter<Jwt, AbstractAuthenticationToken> getJwtAuthenticationConverter() {
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Map<String, Object> realmAccess;
            realmAccess = (Map<String, Object>) jwt.getClaims().get("realm_access");

            Collection<GrantedAuthority> authority = new HashSet<>();
            if (realmAccess != null && realmAccess.containsKey("roles")) {
                authority.addAll(((List<String>) realmAccess.get("roles"))
                        .stream()
                                .map(roleName -> "ROLE_" + roleName.toUpperCase())
                                .map(SimpleGrantedAuthority::new)
                                .collect(Collectors.toList()));
            }

            if (jwt.getClaims().containsKey("scope")) {
                authority.addAll(
                        Arrays.stream(jwt.getClaims().get("scope").toString().split(" "))
                                .map(roleName -> "ROLE_" + roleName.toUpperCase())
                                .map(SimpleGrantedAuthority::new)
                                .collect(Collectors.toList()));
            }
            return authority;
        });
        return converter;
    }
}
