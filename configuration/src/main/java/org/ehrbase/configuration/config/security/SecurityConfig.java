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

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.context.ShutdownEndpoint;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

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

    protected abstract HttpSecurity configureHttpSecurity(HttpSecurity http) throws Exception;

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
}
