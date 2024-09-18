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

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;

import jakarta.servlet.DispatcherType;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.stereotype.Component;

/**
 * {@link Component} for OAuth2 authentication.
 */
@Component
@ConditionalOnProperty(prefix = "security", name = "auth-type", havingValue = "oauth")
public final class SecurityConfigOAuth2 extends SecurityConfig {

    public static final String PROFILE_SCOPE = "PROFILE";

    private final SecurityProperties securityProperties;

    private final OAuth2ResourceServerProperties oAuth2Properties;

    public SecurityConfigOAuth2(
            SecurityProperties securityProperties,
            OAuth2ResourceServerProperties oAuth2Properties,
            WebEndpointProperties webEndpointProperties) {
        super(webEndpointProperties);
        this.securityProperties = securityProperties;
        this.oAuth2Properties = oAuth2Properties;
    }

    @PostConstruct
    public void initialize() {
        logger.info("Using OAuth2 authentication");
        logger.debug("Using issuer URI: {}", oAuth2Properties.getJwt().getIssuerUri());
        logger.debug("Using user role: {}", securityProperties.getOauth2UserRole());
        logger.debug("Using admin role: {}", securityProperties.getOauth2AdminRole());
    }

    @Override
    public HttpSecurity configureHttpSecurity(HttpSecurity http) throws Exception {

        final String userRole = securityProperties.getOauth2UserRole();
        final String adminRole = securityProperties.getOauth2AdminRole();

        return http.addFilterBefore(new SecurityFilter(), BearerTokenAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> {

                    // Permit dispatcher types forward and error
                    auth.dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR).permitAll();

                    // Permit welcome page and img
                    auth.requestMatchers("/", "/img/**").permitAll();

                    // secure /rest/admin/** so that only admins can access it
                    auth = auth.requestMatchers(antMatcher("/rest/admin/**")).hasRole(adminRole);

                    // secure /management/**
                    auth = configureManagementEndpointAccess(
                            auth, adminRole, List.of(adminRole, userRole, PROFILE_SCOPE));

                    // secure all other requests using either user and/or admin roles
                    auth.anyRequest().hasAnyRole(adminRole, userRole, PROFILE_SCOPE);
                })
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(
                        server -> server.jwt(jwt -> jwt.jwtAuthenticationConverter(getJwtAuthenticationConverter())));
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
                                .toList());
            }

            if (jwt.getClaims().containsKey("scope")) {
                authority.addAll(
                        Arrays.stream(jwt.getClaims().get("scope").toString().split(" "))
                                .map(roleName -> "ROLE_" + roleName.toUpperCase())
                                .map(SimpleGrantedAuthority::new)
                                .toList());
            }
            return authority;
        });
        return converter;
    }
}
