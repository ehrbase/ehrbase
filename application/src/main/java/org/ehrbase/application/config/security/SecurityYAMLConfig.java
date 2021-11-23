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
package org.ehrbase.application.config.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "security")
public class SecurityYAMLConfig {

    // Roles, when not using OAuth2
    public static final String ADMIN = "ADMIN";
    public static final String USER = "USER";

    public enum AuthTypes {
        NONE, BASIC, OAUTH
    }

    private AuthTypes authType;
    private String authUser;
    private String authPassword;
    private String authAdminUser;
    private String authAdminPassword;
    private String oauth2UserRole;
    private String oauth2AdminRole;
    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String oauth2IssuerUri;

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

    public String getOauth2UserRole() { return oauth2UserRole; }

    public void setOauth2UserRole(String oauth2UserRole) { this.oauth2UserRole = oauth2UserRole.toUpperCase(); }

    public String getOauth2AdminRole() { return oauth2AdminRole; }

    public void setOauth2AdminRole(String oauth2AdminRole) { this.oauth2AdminRole = oauth2AdminRole.toUpperCase(); }

    public String getOauth2IssuerUri() {
        return oauth2IssuerUri;
    }

    public void setOauth2IssuerUri(String oauth2IssuerUri) {
        this.oauth2IssuerUri = oauth2IssuerUri;
    }
}
