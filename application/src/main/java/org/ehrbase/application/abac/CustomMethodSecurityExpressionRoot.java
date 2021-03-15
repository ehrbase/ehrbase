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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;

/**
 * Implementation of custom security expression, to be used in e.g. @PreAuthorize(..) to allow ABAC
 * requests.
 */
public class CustomMethodSecurityExpressionRoot extends SecurityExpressionRoot implements
    MethodSecurityExpressionOperations {

  static final String ORGANIZATION = "organization";
  static final String PATIENT = "patient";
  static final String TEMPLATE = "template";
  private final AbacConfig abacConfig;
  private Object filterObject;
  private Object returnObject;

  public CustomMethodSecurityExpressionRoot(Authentication authentication,
      AbacConfig abacConfig) {
    super(authentication);
    this.abacConfig = abacConfig;
  }

  /**
   * Custom SpEL expression to be used to check if the remote ABAC allows the operation by given
   * data.
   *
   * @param Organization
   * @param subject
   * @param composition
   * @return
   */
  public boolean checkAbacComposition(String Organization, UUID subject, Object composition)
      throws JsonProcessingException {

    // Extract and set parameters according to which parameters are configured
    List<String> policyParameters = new ArrayList<>(
        Arrays.asList(abacConfig.getPolicyCompositionParameters()));

    // TODO-505: add orga handling. extraction from "this.authentication"?
    if (policyParameters.contains(ORGANIZATION)) {

    }

    // TODO-505: any formatting or alike necessary?
    if (policyParameters.contains(PATIENT)) {

    }

    // extract template ID from composition
    String templateId = null;
    if (policyParameters.contains(TEMPLATE)) {
      // extract templateId from returnObject
      ObjectMapper mapper = new ObjectMapper();
      JsonNode actualObj = mapper.readTree(((ResponseEntity) composition).getBody().toString());
      templateId = actualObj.get("archetype_details").get("template_id").get("value")
          .asText();
    }

    // TODO-505: build and fire abac server request
    String requestUrl = abacConfig.getServer() + abacConfig.getPolicyCompositionName();

    return true;
  }

  @Override
  public Object getFilterObject() {
    return this.filterObject;
  }

  @Override
  public void setFilterObject(Object filterObject) {
    this.filterObject = filterObject;
  }

  @Override
  public Object getReturnObject() {
    return this.returnObject;
  }

  @Override
  public void setReturnObject(Object returnObject) {
    this.returnObject = returnObject;
  }

  @Override
  public Object getThis() {
    return this;
  }
}
