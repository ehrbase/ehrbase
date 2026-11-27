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

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security")
public class SecurityProperties {

    /// Authentication type.
    private AuthTypes authType;

    /// Username.
    private String authUser;

    /// Password for the user.
    private String authPassword;

    /// Admin username.
    private String authAdminUser;

    /// Password for the admin user.
    private String authAdminPassword;

    /// Openehr user role name used with OAuth2 authentication type.
    private String[] oauth2OpenehrRoles;

    /// Admin role name used with OAuth2 authentication type.
    private String[] oauth2AdminRoles;

    /// Management role name used with OAuth2 authentication type.
    private String[] oauth2ManagementRoles;

    /// Access-only role name used with OAuth2 authentication type.
    private String[] oauth2AccessOnlyRoles;

    private List<EndpointAuthorization> additionalAuthorizations = new ArrayList<>();

    public AuthTypes getAuthType() {
        return authType;
    }

    public void setAuthType(AuthTypes authType) {
        this.authType = authType;
    }

    public String getAuthUser() {
        return authUser;
    }

    public void setAuthUser(String authUser) {
        this.authUser = authUser;
    }

    public String getAuthPassword() {
        return authPassword;
    }

    public void setAuthPassword(String authPassword) {
        this.authPassword = authPassword;
    }

    public String getAuthAdminUser() {
        return authAdminUser;
    }

    public void setAuthAdminUser(String authAdminUser) {
        this.authAdminUser = authAdminUser;
    }

    public String getAuthAdminPassword() {
        return authAdminPassword;
    }

    public void setAuthAdminPassword(String authAdminPassword) {
        this.authAdminPassword = authAdminPassword;
    }

    public String[] getOauth2OpenehrRoles() {
        return oauth2OpenehrRoles;
    }

    public void setOauth2OpenehrRoles(String[] oauth2OpenehrRoles) {
        this.oauth2OpenehrRoles = oauth2OpenehrRoles;
    }

    public String[] getOauth2AdminRoles() {
        return oauth2AdminRoles;
    }

    public void setOauth2AdminRoles(String[] oauth2AdminRoles) {
        this.oauth2AdminRoles = oauth2AdminRoles;
    }

    public String[] getOauth2ManagementRoles() {
        return oauth2ManagementRoles;
    }

    public void setOauth2ManagementRoles(String[] oauth2ManagementRoles) {
        this.oauth2ManagementRoles = oauth2ManagementRoles;
    }

    public String[] getOauth2AccessOnlyRoles() {
        return oauth2AccessOnlyRoles;
    }

    public void setOauth2AccessOnlyRoles(String[] oauth2AccessOnlyRoles) {
        this.oauth2AccessOnlyRoles = oauth2AccessOnlyRoles;
    }

    public List<EndpointAuthorization> getAdditionalAuthorizations() {
        return additionalAuthorizations;
    }

    public void setAdditionalAuthorizations(List<EndpointAuthorization> additionalAuthorizations) {
        this.additionalAuthorizations = additionalAuthorizations;
    }

    public enum AuthTypes {
        NONE,
        BASIC,
        OAUTH
    }

    /// Supported values for the `management.endpoints.web.access` property value.
    public enum AccessType {
        ADMIN_ONLY,
        PRIVATE,
        PUBLIC
    }

    /// Rules are bound from configuration (see [SecurityProperties#getAdditionalAuthorizations()]) and applied by
    /// the matching `SecurityConfig` implementation.
    ///
    /// @param authType the authentication type this rule applies to;
    ///                 a rule is only applied on the matching auth chain
    /// @param pathPattern the ant-style request path to secure
    /// @param roles the roles allowed to access the path. The keywords [#ADMIN] and [#USER] are replaced
    ///              with the admin/user role names configured for the authentication type; any other value is used
    ///              as a literal role name
    public record EndpointAuthorization(AuthTypes authType, String pathPattern, List<String> roles) {

        /// Keyword replaced with the admin role name configured for the authentication type.
        public static final String ADMIN = "ADMIN";

        /// Keyword replaced with the user role name configured for the authentication type.
        public static final String USER = "USER";
    }
}
