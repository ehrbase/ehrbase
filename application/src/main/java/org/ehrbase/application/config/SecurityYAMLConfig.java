package org.ehrbase.application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "security")
public class SecurityYAMLConfig {

    public enum AuthTypes {
        NONE, BASIC
    }

    private AuthTypes authType;
    private String authUser;
    private String authPassword;

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
}
