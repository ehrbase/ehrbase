/*
 * Copyright (c) 2021 Vitasystems GmbH.
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

package org.ehrbase.configuration.client;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ResourceUtils;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

/**
 * {@link Configuration} for Apache HTTP Client.
 */
@Configuration
public class HttpClientConfiguration {

    @Bean
    public HttpClient httpClient(ClientProperties properties) throws UnrecoverableKeyException, CertificateException,
            NoSuchAlgorithmException, KeyStoreException, IOException, KeyManagementException {

        HttpClientBuilder builder = HttpClients.custom();

        if (properties.getSsl().isEnabled()) {
            builder.setSSLContext(buildSSLContext(properties.getSsl()));
            builder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
        }

        return builder.build();
    }

    private SSLContext buildSSLContext(ClientProperties.Ssl properties) throws UnrecoverableKeyException, CertificateException,
            NoSuchAlgorithmException, KeyStoreException, IOException, KeyManagementException {

        SSLContextBuilder builder = SSLContextBuilder.create();

        if (properties.getKeyStoreType() != null) {
            builder.setKeyStoreType(properties.getKeyStoreType());
        }
        builder.loadKeyMaterial(ResourceUtils.getFile(properties.getKeyStore()),
                properties.getKeyStorePassword().toCharArray(),
                properties.getKeyPassword().toCharArray());

        if (properties.getTrustStoreType() != null) {
            builder.setKeyStoreType(properties.getTrustStoreType());
        }
        builder.loadTrustMaterial(ResourceUtils.getFile(properties.getTrustStore()),
                properties.getTrustStorePassword().toCharArray(), TrustAllStrategy.INSTANCE);

        return builder.build();
    }
}
