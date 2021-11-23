/*
 * Copyright (c) 2021 Vitasystems GmbH and Jake Smolka (Hannover Medical School).
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
package org.ehrbase.application.config.security;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

@ConditionalOnProperty(name = "security.authType", havingValue = "OAUTH")
@Configuration
@EnableWebSecurity
public class OAuth2SecurityConfig extends WebSecurityConfigurerAdapter {
  // OAuth scope to allow as normal user
  public static final String PROFILE_SCOPE = "PROFILE";

  private final SecurityYAMLConfig securityYAMLConfig;

  private final Logger logger = LoggerFactory.getLogger(getClass());

  public OAuth2SecurityConfig(SecurityYAMLConfig securityYAMLConfig) {
    this.securityYAMLConfig = securityYAMLConfig;
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    String userRole = securityYAMLConfig.getOauth2UserRole();
    String adminRole = securityYAMLConfig.getOauth2AdminRole();

    logger.info("Using OAuth2 authentication.");
    logger.info("Using issuer URI: {}", securityYAMLConfig.getOauth2IssuerUri());
    logger.info("Using user role: {}", userRole);
    logger.info("Using admin role: {}", adminRole);

    http.cors()
        .and()
        .authorizeRequests()
        // Specific routes with ../admin/.. and actuator /management/.. endpoints require admin role
        .antMatchers("/rest/admin/**", "/management/**")
        .hasRole(adminRole)
        // Everything else is open to all users of role admin and user
        .antMatchers("/**")
        .hasAnyRole(adminRole, userRole, PROFILE_SCOPE)
        .and()
        .oauth2ResourceServer()
        .jwt()
        .jwtAuthenticationConverter(getJwtAuthenticationConverter());
  }

  // Converter creates list of "ROLE_*" (upper case) authorities for each "realm access" role
  // and "roles" role from JWT
  private Converter<Jwt, AbstractAuthenticationToken> getJwtAuthenticationConverter() {
    var converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(
        jwt -> {
          Map<String, Object> realmAccess;
          realmAccess = (Map<String, Object>) jwt.getClaims().get("realm_access");

          Collection<GrantedAuthority> authority = new HashSet<>();
          if (realmAccess != null && realmAccess.containsKey("roles")) {
            authority.addAll(((List<String>) realmAccess.get("roles"))
                .stream()
                .map(roleName -> "ROLE_" + roleName.toUpperCase())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList()));
          }

          //Collection<GrantedAuthority> finalAuthority = authority;
          if (jwt.getClaims().containsKey("scope")) {
            authority.addAll(Arrays.stream(jwt.getClaims().get("scope").toString().split(" "))
                .map(roleName -> "ROLE_" + roleName.toUpperCase())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList()));
          }
          return authority;
        });
    return converter;
  }
}
