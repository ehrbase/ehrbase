/*
 * Copyright (c) 2021 Jake Smolka (Hannover Medical School) and Vitasystems GmbH.
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

package org.ehrbase.application.abac;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Map;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.application.config.HttpClientConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@ConditionalOnProperty(name = "abac.enabled")
@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "abac")
public class AbacConfig {

  public enum AbacType {
    EHR, EHR_STATUS, COMPOSITION, CONTRIBUTION, QUERY
  }

  public enum PolicyParameter {
    ORGANIZATION, PATIENT, TEMPLATE
  }

  static class Policy {
    private String name;
    private PolicyParameter[] parameters;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public PolicyParameter[] getParameters() {
      return parameters;
    }

    public void setParameters(PolicyParameter[] parameters) {
      this.parameters = parameters;
    }
  }

  private URI server;
  private String organizationClaim;
  private String patientClaim;
  private Map<AbacType, Policy> policy;

  private final HttpClientConfig httpClientConfig;

  public AbacConfig(HttpClientConfig httpClientConfig) {
    this.httpClientConfig = httpClientConfig;
  }

  @Bean
  public AbacCheck abacCheck() {
    return new AbacCheck(this.httpClientConfig);
  }

  public URI getServer() {
    return server;
  }

  public void setServer(URI server) {
    this.server = server;
  }

  public String getOrganizationClaim() {
    return organizationClaim;
  }

  public void setOrganizationClaim(String organizationClaim) {
    this.organizationClaim = organizationClaim;
  }

  public String getPatientClaim() {
    return patientClaim;
  }

  public void setPatientClaim(String patientClaim) {
    this.patientClaim = patientClaim;
  }

  public Map<AbacType, Policy> getPolicy() {
    return policy;
  }

  public void setPolicy(
      Map<AbacType, Policy> policy) {
    this.policy = policy;
  }

  /*
  This class has only some extracted methods to handle ABAC server connection and requests.
  It is mainly a separate class so it can be overwritten by a MockBean in the context of tests.
 */
  public static class AbacCheck {

    private final HttpClientConfig httpClientConfig;

    public AbacCheck(HttpClientConfig httpClientConfig) {
      this.httpClientConfig = httpClientConfig;
    }

    /**
     * Helper to build and send the actual HTTP request to the ABAC server.
     *
     * @param url     URL for ABAC server request
     * @param bodyMap Map of attributes for the request
     * @return HTTP response
     * @throws IOException          On error during attribute or HTTP handling
     * @throws InterruptedException On error during HTTP handling
     */
    public boolean  execute(String url, Map<String, String> bodyMap)
        throws IOException, InterruptedException {
      return evaluateResponse(send(url, bodyMap));
    }

    private HttpResponse<?> send(String url, Map<String, String> bodyMap)
        throws IOException, InterruptedException {
      // convert bodyMap to JSON
      ObjectMapper objectMapper = new ObjectMapper();
      String requestBody = objectMapper
          .writerWithDefaultPrettyPrinter()
          .writeValueAsString(bodyMap);

      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .header("Content-Type", "application/json")
          .POST(BodyPublishers.ofString(requestBody))
          .build();

      try {
        return httpClientConfig.getClient().send(request, BodyHandlers.ofString());
      } catch (InterruptedException e) {
        throw e;
      } catch (Exception e) {
        throw new InternalServerException("ABAC: Connection with ABAC server failed. Check configuration. Error: " + e.getMessage());
      }
    }

    private boolean evaluateResponse(HttpResponse<?> response) {
      return response.statusCode() == 200;
    }
  }
}
