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
package org.ehrbase.application.config.client;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import javax.net.ssl.SSLContext;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ResourceUtils;

/**
 * {@link Configuration} for Apache HTTP Client.
 */
@Configuration
@EnableConfigurationProperties(HttpClientProperties.class)
@SuppressWarnings("java:S6212")
public class HttpClientConfiguration {

    @Bean
    public HttpClient httpClient(HttpClientProperties properties)
            throws UnrecoverableKeyException, CertificateException, NoSuchAlgorithmException, KeyStoreException,
                    IOException, KeyManagementException {

        HttpClientBuilder builder = HttpClients.custom();

        if (properties.getSsl().isEnabled()) {
            builder.setSSLContext(buildSSLContext(properties.getSsl()));
            builder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
        }

        if (properties.getProxy().getHost() != null && properties.getProxy().getPort() != null) {
            builder.setProxy(new HttpHost(
                    properties.getProxy().getHost(), properties.getProxy().getPort()));

            if (properties.getProxy().getUsername() != null
                    && properties.getProxy().getPassword() != null) {
                UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(
                        properties.getProxy().getUsername(),
                        properties.getProxy().getPassword());
                BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(AuthScope.ANY, credentials);
                builder.setDefaultCredentialsProvider(credentialsProvider);
            }
        }

        return builder.build();
    }

    private SSLContext buildSSLContext(HttpClientProperties.Ssl properties)
            throws UnrecoverableKeyException, CertificateException, NoSuchAlgorithmException, KeyStoreException,
                    IOException, KeyManagementException {

        SSLContextBuilder builder = SSLContextBuilder.create();

        if (properties.getKeyStoreType() != null) {
            builder.setKeyStoreType(properties.getKeyStoreType());
        }
        builder.loadKeyMaterial(
                ResourceUtils.getFile(properties.getKeyStore()),
                properties.getKeyStorePassword().toCharArray(),
                properties.getKeyPassword().toCharArray());

        if (properties.getTrustStoreType() != null) {
            builder.setKeyStoreType(properties.getTrustStoreType());
        }
        builder.loadTrustMaterial(
                ResourceUtils.getFile(properties.getTrustStore()),
                properties.getTrustStorePassword().toCharArray(),
                TrustAllStrategy.INSTANCE);

        return builder.build();
    }
}
