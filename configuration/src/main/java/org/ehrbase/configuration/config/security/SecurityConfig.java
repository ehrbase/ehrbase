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

import static org.ehrbase.configuration.config.security.SecurityProperties.AccessType;
import static org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher.pathPattern;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import java.util.List;
import org.ehrbase.configuration.config.security.SecurityProperties.AuthTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.actuate.context.ShutdownEndpoint;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;

/**
 * Common Security config interface that allows to secure the spring actuator endpoints in common way between basic-auth
 * and oauth2 authentication.
 */
public abstract sealed class SecurityConfig permits SecurityConfigNoOp, SecurityConfigBasicAuth, SecurityConfigOAuth2 {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Spring boot actuator properties
     */
    protected final WebEndpointProperties webEndpointProperties;
    /**
     * Extended property on spring actuator config that defines who can access the management endpoint.
     */
    @Value("${management.endpoints.web.access:ADMIN_ONLY}")
    protected SecurityProperties.AccessType managementEndpointsAccessType;

    protected SecurityConfig(WebEndpointProperties webEndpointProperties) {
        this.webEndpointProperties = webEndpointProperties;
    }

    protected record SecurityConfigParams(
            Class<? extends Filter> authFilterClass,
            AuthTypes authType,
            String adminRole,
            String userRole,
            List<String> mgmtRoles,
            List<String> otherRequestsRoles,
            List<SecurityProperties.EndpointAuthorization> additionalAuthorizations) {}

    protected abstract SecurityConfigParams securityConfigParams();

    protected HttpSecurity configureHttpSecurity(HttpSecurity http) throws Exception {
        SecurityConfigParams params = securityConfigParams();
        return http.addFilterBefore(new SecurityFilter(), params.authFilterClass())
                .authorizeHttpRequests(auth -> {

                    // Permit dispatcher types forward and error
                    auth = auth.dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR)
                            .permitAll();
                    // Permit welcome page and img
                    auth = auth.requestMatchers("/", "/img/**").permitAll();
                    // secure /rest/admin/** so that only admins can access it
                    auth = requestMatcherWithRoles(auth, "/rest/admin/**", params.adminRole());

                    auth = applyAdditionalAuthorizations(auth, params);

                    // secure /management/**
                    auth = configureManagementEndpointAccess(auth, params.adminRole(), params.mgmtRoles());
                    // secure all other requests using either user and/or admin roles
                    auth.anyRequest().hasAnyRole(params.otherRequestsRoles().toArray(String[]::new));
                })
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    }

    private static AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry
            requestMatcherWithRoles(
                    AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth,
                    String pattern,
                    String... roles) {
        return auth.requestMatchers(pathPattern(pattern)).hasAnyRole(roles);
    }

    /**
     * Configures management endpoints access
     */
    protected AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry
            configureManagementEndpointAccess(
                    AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth,
                    String adminRoleSupplier,
                    List<String> privateRolesSupplier) {

        logger.info("Management endpoint access type {}", managementEndpointsAccessType);

        var endpointRequestMatcher = EndpointRequest.toAnyEndpoint();

        return switch (managementEndpointsAccessType) {
            // management endpoints are locked behind an authorization
            // and are only available for users with the admin role
            case AccessType.ADMIN_ONLY ->
                auth.requestMatchers(endpointRequestMatcher).hasRole(adminRoleSupplier);
            // management endpoints are locked behind an authorization, but are available to any role
            case AccessType.PRIVATE ->
                auth.requestMatchers(endpointRequestMatcher).hasAnyRole(privateRolesSupplier.toArray(new String[] {}));
            // management endpoints can be accessed without an authorization
            case AccessType.PUBLIC ->
                auth.requestMatchers(endpointRequestMatcher.excluding(ShutdownEndpoint.class))
                        .permitAll();
        };
    }

    /**
     * Applies the rules for the given authentication type. Each matching rule secures its path pattern behind the
     * configured roles. The list can be shared across auth chains, rules for other auth types are ignored.
     * The {@link SecurityProperties.EndpointAuthorization#ADMIN} and
     * {@link SecurityProperties.EndpointAuthorization#USER} keywords are replaced with the admin/user role names
     * of the authentication type.
     */
    protected AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry
            applyAdditionalAuthorizations(
                    AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth,
                    SecurityConfigParams params) {

        for (SecurityProperties.EndpointAuthorization rule : params.additionalAuthorizations()) {
            if (rule.authType() == null || rule.authType() == params.authType()) {
                auth = requestMatcherWithRoles(auth, rule.pathPattern(), resolveRoles(rule.roles(), params));
            }
        }
        return auth;
    }

    private static String[] resolveRoles(List<String> roles, SecurityConfigParams params) {
        return roles.stream()
                .map(role -> switch (role) {
                    case SecurityProperties.EndpointAuthorization.ADMIN -> params.adminRole();
                    case SecurityProperties.EndpointAuthorization.USER -> params.userRole();
                    default -> role;
                })
                .toArray(String[]::new);
    }
}
