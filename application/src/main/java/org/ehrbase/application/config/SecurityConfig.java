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

package org.ehrbase.application.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.*;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

  // Roles, independent of auth type
  public static final String ADMIN = "ADMIN";
  public static final String USER = "USER";

  private final SecurityYAMLConfig securityYAMLConfig;

  private final Logger logger = LoggerFactory.getLogger(getClass());

  public SecurityConfig(SecurityYAMLConfig securityYAMLConfig) {
    this.securityYAMLConfig = securityYAMLConfig;
  }

  @Autowired
  public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {

    // For Basic Auth: assigns specific roles to specific users. Enables conditional handling in
    // configure()
    auth.inMemoryAuthentication()
        .withUser(securityYAMLConfig.getAuthUser())
        .password(passwordEncoder().encode(securityYAMLConfig.getAuthPassword()))
        .roles(USER)
        .and()
        .withUser(securityYAMLConfig.getAuthAdminUser())
        .password(passwordEncoder().encode(securityYAMLConfig.getAuthAdminPassword()))
        .roles(ADMIN);
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    switch (securityYAMLConfig.getAuthType()) {
      case BASIC:
        logger.info("Using basic authentication.");

        http.cors()
            .and()
            .csrf()
            .disable()
            .authorizeRequests()
            // Specific routes with ../admin/.. and actuator /status/.. endpoints require admin role
            .antMatchers("/rest/openehr/v1/admin/**", "/status/**")
            .hasRole(ADMIN)
            // Everything else is open to all users of role admin and user
            .antMatchers("/**")
            .hasAnyRole(ADMIN, USER)
            .and()
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .httpBasic();
        break;
      case OAUTH:
        logger.info("Using OAuth2 authentication.");
        logger.info("Using issuer URI: {}", securityYAMLConfig.getOauth2IssuerUri());
        http.cors()
            .and()
            .authorizeRequests()
            // Specific routes with ../admin/.. and actuator /status/.. endpoints require admin role
            .antMatchers("/rest/openehr/v1/admin/**", "/status/**")
            .hasRole(ADMIN)
            // Everything else is open to all users of role admin and user
            .antMatchers("/**")
            // TODO-505: remove PoC hard coded roles
            .hasAnyRole(ADMIN, USER, "PROFILE", "OFFLINE_ACCESS", "VIEW-PROFILE", "UMA_AUTHORIZATION", "UMA_PROTECTION")
            .and()
            .oauth2ResourceServer()
            .jwt()
            .jwtAuthenticationConverter(getJwtAuthenticationConverter());
        break;
      case NONE:
      default:
        logger.warn("Authentication disabled!");
        logger.warn(
            "To enable security set security.authType to BASIC or OAUTH in yaml properties file.");
        http.cors().and().csrf().disable().authorizeRequests().anyRequest().permitAll();
        break;
    }
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  // Converter creates list of "ROLE_*" (upper case) authorities for each realm access role from JWT
  private Converter<Jwt, AbstractAuthenticationToken> getJwtAuthenticationConverter() {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(
        jwt -> {
          /*Map<String, Object> realmAccess =
              (Map<String, Object>) jwt.getClaims().get("realm_access");
          // TODO-505: fix or remove PoC handling
          if (realmAccess == null)
            realmAccess = new HashMap<>();
          Map<String, Object> resourceAccess = (Map<String, Object>) jwt.getClaims()
              .get("resource_access");
          realmAccess.putAll((Map<String, Object>) resourceAccess.get("demographics-service"));
          return ((List<String>) realmAccess.get("roles"))
              .stream()
                  .map(roleName -> "ROLE_" + roleName.toUpperCase())
                  .map(SimpleGrantedAuthority::new)
                  .collect(Collectors.toList());*/

          return Arrays.stream(jwt.getClaims().get("scope").toString().split(" "))
              .map(roleName -> "ROLE_" + roleName.toUpperCase())
              .map(SimpleGrantedAuthority::new)
              .collect(Collectors.toList());
        });
    return converter;
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();

    configuration.applyPermitDefaultValues();
    // Allow all origins to access EHRbase
    configuration.setAllowedOrigins(Collections.singletonList("*"));
    // Allowed HTTP methods
    configuration.setAllowedMethods(
        Arrays.asList("GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS"));
    // Allow credentials to be transmitted
    configuration.setAllowCredentials(true);
    // Exposed headers that can be read by clients. Includes also all safe-listed headers
    // See https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Access-Control-Expose-Headers
    configuration.setExposedHeaders(
        Arrays.asList(
            "Access-Control-Allow-Methods",
            "Access-Control-Allow-Origin",
            "ETag",
            "Content-Type",
            "Last-Modified",
            "Location",
            "WWW-Authenticate"));

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    // Apply for all paths
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
