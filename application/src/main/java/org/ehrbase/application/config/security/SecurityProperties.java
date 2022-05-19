/*
 * Copyright (c) 2020 vitasystems GmbH and Hannover Medical School.
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

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security")
public class SecurityProperties {

    // Roles, when not using OAuth2
    public static final String ADMIN = "ADMIN";

    public static final String USER = "USER";

    /**
     * Authentication type.
     */
    private AuthTypes authType;

    /**
     * Username.
     */
    private String authUser;

    /**
     * Password for the user.
     */
    private String authPassword;

    /**
     * Admin username.
     */
    private String authAdminUser;

    /**
     * Password for the admin user.
     */
    private String authAdminPassword;

    /**
     * User role name used with OAuth2 authentication type.
     */
    private String oauth2UserRole;

    /**
     * Admin role name used with OAuth2 authentication type.
     */
    private String oauth2AdminRole;

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

    public String getOauth2UserRole() {
        return oauth2UserRole;
    }

    public void setOauth2UserRole(String oauth2UserRole) {
        this.oauth2UserRole = oauth2UserRole.toUpperCase();
    }

    public String getOauth2AdminRole() {
        return oauth2AdminRole;
    }

    public void setOauth2AdminRole(String oauth2AdminRole) {
        this.oauth2AdminRole = oauth2AdminRole.toUpperCase();
    }

    public enum AuthTypes {
        NONE,
        BASIC,
        OAUTH
    }
}
