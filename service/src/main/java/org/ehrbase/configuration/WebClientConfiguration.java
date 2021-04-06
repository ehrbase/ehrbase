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
package org.ehrbase.configuration;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.ResourceUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

/**
 * {@link Configuration} for WebClient.
 */
@Configuration
@EnableConfigurationProperties(HttpClientProperties.class)
public class WebClientConfiguration {

    @Bean
    public WebClient webClient(WebClient.Builder webClientBuilder) {
        return webClientBuilder.build();
    }

    @Bean
    public WebClientCustomizer httpClientCustomizer(HttpClientProperties properties) {
        return webClientBuilder -> {
            HttpClient httpClient;

            if (properties.getSsl().isEnabled()) {
                httpClient = HttpClient.create()
                        .secure(sslContextBuilder -> sslContextBuilder.sslContext(sslContext(properties.getSsl())));
            } else {
                httpClient = HttpClient.create();
            }

            webClientBuilder.clientConnector(new ReactorClientHttpConnector(httpClient));
        };
    }

    private SslContext sslContext(HttpClientProperties.Ssl ssl) {
        try {
            return SslContextBuilder.forClient()
                    .keyManager(getKeyManagerFactory(ssl))
                    .trustManager(getTrustManagerFactory(ssl))
                    .build();
        } catch (SSLException e) {
            throw new IllegalStateException(e);
        }
    }

    private KeyManagerFactory getKeyManagerFactory(HttpClientProperties.Ssl ssl) {
        try {
            KeyStore keyStore = loadKeyStore(ssl.getKeyStoreType(), ssl.getKeyStore(), ssl.getKeyStorePassword());
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            char[] keyPassword = (ssl.getKeyPassword() != null) ? ssl.getKeyPassword().toCharArray() : null;
            if (keyPassword == null && ssl.getKeyStorePassword() != null) {
                keyPassword = ssl.getKeyStorePassword().toCharArray();
            }
            keyManagerFactory.init(keyStore, keyPassword);
            return keyManagerFactory;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private TrustManagerFactory getTrustManagerFactory(HttpClientProperties.Ssl ssl) {
        try {
            KeyStore trustStore = loadKeyStore(ssl.getTrustStoreType(), ssl.getTrustStore(), ssl.getTrustStorePassword());
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);
            return trustManagerFactory;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private KeyStore loadKeyStore(String type, String location, String password) throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
        type = type != null ? type : "JKS";
        KeyStore keyStore = KeyStore.getInstance(type);
        keyStore.load(new FileInputStream(ResourceUtils.getFile(location)), password != null ? password.toCharArray() : null);
        return keyStore;
    }
}
