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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.ehrbase.configuration.config.security.SecurityProperties.AuthTypes;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerProperties;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
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
        logger.debug("Using openehr roles: {}", (Object) securityProperties.getOauth2OpenehrRoles());
        logger.debug("Using admin roles: {}", (Object) securityProperties.getOauth2AdminRoles());
        logger.debug("Using management roles: {}", (Object) securityProperties.getOauth2ManagementRoles());
        logger.debug("Using access-only roles: {}", (Object) securityProperties.getOauth2AccessOnlyRoles());
    }

    @Override
    protected SecurityConfigParams securityConfigParams() {
        final String[] openEhrRoles = securityProperties.getOauth2OpenehrRoles();
        final String[] adminRoles = securityProperties.getOauth2AdminRoles();
        final String[] managementRoles = securityProperties.getOauth2ManagementRoles();
        final String[] accessOnlyRoles = securityProperties.getOauth2AccessOnlyRoles();

        String[] mgmtRoles = merge(adminRoles, managementRoles);
        String[] accessRoles = merge(adminRoles, openEhrRoles, accessOnlyRoles);
        return new SecurityConfigParams(
                BearerTokenAuthenticationFilter.class,
                AuthTypes.OAUTH,
                adminRoles,
                openEhrRoles,
                mgmtRoles,
                accessRoles,
                securityProperties.getAdditionalAuthorizations());
    }

    private String[] merge(String[]... arrays) {
        return Arrays.stream(arrays)
                .filter(Objects::nonNull)
                .flatMap(Arrays::stream)
                .distinct()
                .toArray(String[]::new);
    }

    @Override
    public HttpSecurity configureHttpSecurity(HttpSecurity http) throws Exception {
        return super.configureHttpSecurity(http)
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
            return authority;
        });
        return converter;
    }
}
