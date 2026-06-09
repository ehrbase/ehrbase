/*
 * Copyright (c) 2026 vitasystems GmbH.
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
package org.ehrbase.service.validation;

public class ExternalTerminologyProviderProperties {

    public static class Retry {
        private int attempts = 3;
        private int initialBackoffMillis = 100;

        public int getAttempts() {
            return attempts;
        }

        public void setAttempts(final int attempts) {
            this.attempts = attempts;
        }

        public int getInitialBackoffMillis() {
            return initialBackoffMillis;
        }

        public void setInitialBackoffMillis(final int initialBackoffMillis) {
            this.initialBackoffMillis = initialBackoffMillis;
        }
    }

    private String oauth2Client;
    private ProviderType type;
    private String url;
    private int maxConnections = 50;
    private int maxPendingConnections = 100;
    private int maxConnectionIdleTimeSeconds = 240;
    private int maxConnectionLifeTimeSeconds = 600;
    private int responseTimeoutSeconds = 10;
    private Retry retry = new Retry();
    private boolean enableMetrics = true;

    public ExternalTerminologyProviderProperties() {}

    public ExternalTerminologyProviderProperties(final ProviderType type, final String url, boolean enableMetrics) {
        this.type = type;
        this.url = url;
        this.enableMetrics = enableMetrics;
    }

    public String getOauth2Client() {
        return oauth2Client;
    }

    public void setOauth2Client(String oauth2Client) {
        this.oauth2Client = oauth2Client;
    }

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

    public int getMaxConnectionIdleTimeSeconds() {
        return maxConnectionIdleTimeSeconds;
    }

    public void setMaxConnectionIdleTimeSeconds(final int maxConnectionIdleTimeSeconds) {
        this.maxConnectionIdleTimeSeconds = maxConnectionIdleTimeSeconds;
    }

    public int getMaxConnectionLifeTimeSeconds() {
        return maxConnectionLifeTimeSeconds;
    }

    public void setMaxConnectionLifeTimeSeconds(final int maxConnectionLifeTimeSeconds) {
        this.maxConnectionLifeTimeSeconds = maxConnectionLifeTimeSeconds;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(final int maxConnections) {
        this.maxConnections = maxConnections;
    }

    public int getMaxPendingConnections() {
        return maxPendingConnections;
    }

    public void setMaxPendingConnections(final int maxPendingConnections) {
        this.maxPendingConnections = maxPendingConnections;
    }

    public int getResponseTimeoutSeconds() {
        return responseTimeoutSeconds;
    }

    public void setResponseTimeoutSeconds(final int responseTimeoutSeconds) {
        this.responseTimeoutSeconds = responseTimeoutSeconds;
    }

    public Retry getRetry() {
        return retry;
    }

    public void setRetry(final Retry retry) {
        this.retry = retry;
    }

    public boolean isEnableMetrics() {
        return enableMetrics;
    }

    public void setEnableMetrics(final boolean enableMetrics) {
        this.enableMetrics = enableMetrics;
    }

    public enum ProviderType {
        FHIR
    }
}
