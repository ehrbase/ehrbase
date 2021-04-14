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
  private String organizationClaim;
  private String patientClaim;
  // ehrStatus
  @Value("${abac.policy.ehrstatus.name}")
  private String policyEhrStatusName;
  @Value("${abac.policy.ehrstatus.parameters}")
  private String[] policyEhrStatusParameters;
  // ehr
  @Value("${abac.policy.ehr.name}")
  private String policyEhrName;
  @Value("${abac.policy.ehr.parameters}")
  private String[] policyEhrParameters;
  // composition
  @Value("${abac.policy.composition.name}")
  private String policyCompositionName;
  @Value("${abac.policy.composition.parameters}")
  private String[] policyCompositionParameters;
  // directory
  @Value("${abac.policy.directory.name}")
  private String policyDirectoryName;
  @Value("${abac.policy.directory.parameters}")
  private String[] policyDirectoryParameters;
  // contribution
  @Value("${abac.policy.contribution.name}")
  private String policyContributionName;
  @Value("${abac.policy.contribution.parameters}")
  private String[] policyContributionParameters;
  // query
  @Value("${abac.policy.query.name}")
  private String policyQueryName;
  @Value("${abac.policy.query.parameters}")
  private String[] policyQueryParameters;
  // definition
  @Value("${abac.policy.definition.name}")
  private String policyDefinitionName;
  @Value("${abac.policy.definition.parameters}")
  private String[] policyDefinitionParameters;

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

  public String getPolicyEhrStatusName() {
    return policyEhrStatusName;
  }

  public void setPolicyEhrStatusName(String policyEhrStatusName) {
    this.policyEhrStatusName = policyEhrStatusName;
  }

  public String[] getPolicyEhrStatusParameters() {
    return policyEhrStatusParameters;
  }

  public void setPolicyEhrStatusParameters(String[] policyEhrStatusParameters) {
    this.policyEhrStatusParameters = policyEhrStatusParameters;
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

  public String getPolicyDirectoryName() {
    return policyDirectoryName;
  }

  public void setPolicyDirectoryName(String policyDirectoryName) {
    this.policyDirectoryName = policyDirectoryName;
  }

  public String[] getPolicyDirectoryParameters() {
    return policyDirectoryParameters;
  }

  public void setPolicyDirectoryParameters(String[] policyDirectoryParameters) {
    this.policyDirectoryParameters = policyDirectoryParameters;
  }

  public String getPolicyContributionName() {
    return policyContributionName;
  }

  public void setPolicyContributionName(String policyContributionName) {
    this.policyContributionName = policyContributionName;
  }

  public String[] getPolicyContributionParameters() {
    return policyContributionParameters;
  }

  public void setPolicyContributionParameters(String[] policyContributionParameters) {
    this.policyContributionParameters = policyContributionParameters;
  }

  public String getPolicyQueryName() {
    return policyQueryName;
  }

  public void setPolicyQueryName(String policyQueryName) {
    this.policyQueryName = policyQueryName;
  }

  public String[] getPolicyQueryParameters() {
    return policyQueryParameters;
  }

  public void setPolicyQueryParameters(String[] policyQueryParameters) {
    this.policyQueryParameters = policyQueryParameters;
  }

  public String getPolicyDefinitionName() {
    return policyDefinitionName;
  }

  public void setPolicyDefinitionName(String policyDefinitionName) {
    this.policyDefinitionName = policyDefinitionName;
  }

  public String[] getPolicyDefinitionParameters() {
    return policyDefinitionParameters;
  }

  public void setPolicyDefinitionParameters(String[] policyDefinitionParameters) {
    this.policyDefinitionParameters = policyDefinitionParameters;
  }

  public String[] getPolicyCompositionParameters() {
    return policyCompositionParameters;
  }

  public void setPolicyCompositionParameters(String[] policyCompositionParameters) {
    this.policyCompositionParameters = policyCompositionParameters;
  }
}
