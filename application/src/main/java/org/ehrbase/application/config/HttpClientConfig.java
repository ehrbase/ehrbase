/*
 * Copyright (c) 2021 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.application.config;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "httpclient")
public class HttpClientConfig {

    private HttpClient client;

    private URI proxy;
    private int proxyPort;

    /**
     * General HTTP client with central configuration.
     */
    public HttpClient getClient() {
        if (this.client == null) {
            var builder = HttpClient.newBuilder()
                    .version(Version.HTTP_2)
                    .followRedirects(Redirect.NEVER)
                    .connectTimeout(Duration.ofSeconds(20));

            if (proxy != null && proxyPort != 0) {
                builder.proxy(ProxySelector.of(new InetSocketAddress(proxy.toString(), proxyPort)));
            }

            // TODO: allow configuration of authentication
            // builder.authenticator(Authenticator.getDefault());

            this.client = builder.build();
        }
        return client;
    }

    public URI getProxy() {
        return proxy;
    }

    public void setProxy(URI proxy) {
        this.proxy = proxy;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }
}
