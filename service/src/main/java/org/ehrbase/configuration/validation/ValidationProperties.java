package org.ehrbase.configuration.validation;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * {@link ConfigurationProperties} for external terminology validation.
 */
@ConfigurationProperties(prefix = "validation.external-terminology")
public class ValidationProperties {

    private boolean enabled = false;

    private boolean failOnError = false;

    private final Map<String, Provider> provider = new HashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isFailOnError() {
        return failOnError;
    }

    public void setFailOnError(boolean failOnError) {
        this.failOnError = failOnError;
    }

    public Map<String, Provider> getProvider() {
        return provider;
    }

    public enum ProviderType {

        FHIR
    }

    public static class Provider {

        private ProviderType type;

        private String url;

        public ProviderType getType() {
            return type;
        }

        public void setType(ProviderType type) {
            this.type = type;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
}
