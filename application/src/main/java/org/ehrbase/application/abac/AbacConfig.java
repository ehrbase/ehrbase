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

import java.net.URI;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
}
