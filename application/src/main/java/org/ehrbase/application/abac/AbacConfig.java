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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "abac")
public class AbacConfig {

  private URI server;
  @Value("${abac.policy.ehr.name}")
  private String policyEhrName;
  @Value("${abac.policy.ehr.parameters}")
  private String[] policyEhrParameters;
  @Value("${abac.policy.composition.name}")
  private String policyCompositionName;
  @Value("${abac.policy.composition.parameters}")
  private String[] policyCompositionParameters;

  // TODO-505: Add remaining properties

  public URI getServer() {
    return server;
  }

  public void setServer(URI server) {
    this.server = server;
  }

  public String getPolicyEhrName() {
    return policyEhrName;
  }

  public void setPolicyEhrName(String policyEhrName) {
    this.policyEhrName = policyEhrName;
  }

  public String[] getPolicyEhrParameters() {
    return policyEhrParameters;
  }

  public void setPolicyEhrParameters(String[] policyEhrParameters) {
    this.policyEhrParameters = policyEhrParameters;
  }

  public String getPolicyCompositionName() {
    return policyCompositionName;
  }

  public void setPolicyCompositionName(String policyCompositionName) {
    this.policyCompositionName = policyCompositionName;
  }

  public String[] getPolicyCompositionParameters() {
    return policyCompositionParameters;
  }

  public void setPolicyCompositionParameters(String[] policyCompositionParameters) {
    this.policyCompositionParameters = policyCompositionParameters;
  }
}
