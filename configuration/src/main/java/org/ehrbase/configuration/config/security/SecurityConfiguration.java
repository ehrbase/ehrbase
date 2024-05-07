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

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.oauth2.client.ClientsConfiguredCondition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.web.SecurityFilterChain;

/**
 * {@link Configuration} for secured endpoint authentication.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({SecurityProperties.class})
@Import({SecurityConfigNoOp.class, SecurityConfigOAuth2.class, SecurityConfigBasicAuth.class})
@EnableWebSecurity
public class SecurityConfiguration {

    private final Logger logger = LoggerFactory.getLogger(SecurityConfiguration.class);

    private final SecurityConfig securityConfig;

    @Value("${ehrbase.security.management.endpoints.web.csrf-validation-enabled:true}")
    protected boolean managementEndpointsCSRFValidationEnabled;

    public SecurityConfiguration(SecurityConfig securityConfig) {
        this.securityConfig = securityConfig;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        return securityConfig
                .configureHttpSecurity(http)
                // CORS will be always enabled
                .cors(Customizer.withDefaults())
                // Exclude apis from CSRF protection, to allow POST, PUT, DELETE, because there are used by client
                // implementation and not only restricted to a browser access.
                .csrf(csrf -> {
                    csrf.ignoringRequestMatchers(
                            antMatcher("/rest/**"), // allow full access to the rest api
                            antMatcher("/plugin/**"), // allow full access to plugin apis
                            antMatcher("/error/**") // ensure we have access to error re-routing
                            );
                    // disable csrf in case 'management.endpoints.web.csrf-validation-enabled=false' is defined
                    if (!managementEndpointsCSRFValidationEnabled) {
                        logger.info("Management endpoint csrf security is disabled");
                        String path = StringUtils.removeEnd(securityConfig.webEndpointProperties.getBasePath(), "/");
                        csrf.ignoringRequestMatchers(antMatcher(path + "/**"));
                    }
                })
                .build();
    }

    @Bean
    @Conditional({ClientsConfiguredCondition.class})
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegRep, OAuth2AuthorizedClientRepository authrClientRep) {
        OAuth2AuthorizedClientProvider authrClientProvider = OAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials()
                .build();

        DefaultOAuth2AuthorizedClientManager authrClientMngr =
                new DefaultOAuth2AuthorizedClientManager(clientRegRep, authrClientRep);
        authrClientMngr.setAuthorizedClientProvider(authrClientProvider);
        return authrClientMngr;
    }
}
