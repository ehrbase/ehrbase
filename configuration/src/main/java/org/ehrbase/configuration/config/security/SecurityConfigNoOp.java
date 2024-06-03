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

import java.util.List;
import javax.annotation.PostConstruct;
import org.ehrbase.service.IAuthenticationFacade;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * {@link Component} used when security is disabled.
 */
@Component
@ConditionalOnProperty(prefix = "security", name = "auth-type", havingValue = "none", matchIfMissing = true)
public final class SecurityConfigNoOp extends SecurityConfig {

    public SecurityConfigNoOp(WebEndpointProperties webEndpointProperties) {
        super(webEndpointProperties);
    }

    @PostConstruct
    public void initialize() {
        logger.warn("Security is disabled. Configure 'security.auth-type' to disable this warning.");
    }

    @Bean
    @Primary
    public IAuthenticationFacade anonymousAuthentication() {
        var filter = new AnonymousAuthenticationFilter("key");
        return () -> new AnonymousAuthenticationToken("key", filter.getPrincipal(), filter.getAuthorities());
    }

    /**
     * We already log our own warning in the {@link #initialize()} post construction.
     *
     * Here we suppress spring warning during
     * {@link org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration}
     * initialization:
     * <code>
     * Using generated security password: {SOME UUID}
     *
     * This generated password is for development use only. Your security configuration must be updated before running your application in production.
     * </code>
     *
     * The reason for this warning is that <code>spring.security.user.password</code> is not configured and no auth
     * is used at all. In such cases the <code>spring.security.user.password</code> will be a generated
     * <code>UUID4</code> {@link  org.springframework.boot.autoconfigure.security.SecurityProperties.User}. But  we start
     * the app with security enabled to be able to use an external oauth2 client.
     */
    @Bean
    public InMemoryUserDetailsManager inMemoryUserDetailsManager(
            org.springframework.boot.autoconfigure.security.SecurityProperties properties) {
        final org.springframework.boot.autoconfigure.security.SecurityProperties.User user = properties.getUser();
        final List<String> roles = user.getRoles();
        return new InMemoryUserDetailsManager(User.withUsername(user.getName())
                .password(user.getPassword())
                .roles(StringUtils.toStringArray(roles))
                .build());
    }

    /**
     * Configure our used security chain by removing the  default <code>httpBasic</cpde> config as well as
     * <code>logout</code> config.
     *
     * Use <code>@EnableWebSecurity(debug = true)</code> on {@link SecurityConfiguration} to enable debug output and
     * verify the actual used filter chain.
     */
    @Override
    public HttpSecurity configureHttpSecurity(HttpSecurity http) throws Exception {
        return http
                // there is no basic auth available -> so let's remove them completely from the filter chain
                .httpBasic(AbstractHttpConfigurer::disable)
                // without login -> logout makes no sense
                .logout(AbstractHttpConfigurer::disable);
    }
}
