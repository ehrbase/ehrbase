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

import org.aopalliance.intercept.MethodInterceptor;
import org.ehrbase.api.service.CompositionService;
import org.springframework.aop.interceptor.SimpleTraceInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.method.MethodSecurityMetadataSource;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.GlobalMethodSecurityConfiguration;

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class MethodSecurityConfig extends GlobalMethodSecurityConfiguration {

  private final AbacConfig abacConfig;
  private final CompositionService compositionService;

  public MethodSecurityConfig(AbacConfig abacConfig,
      CompositionService compositionService) {
    this.abacConfig = abacConfig;
    this.compositionService = compositionService;
  }

  @Value("${abac.disabled}")
  private boolean abacDisabled;

  /**
   * Method will overwrite the security method interceptor by a dummy one when ABAC is disabled.
   * This results in behavior as if "@EnableGlobalMethodSecurity(prePostEnabled = true)" wouldn't be
   * set. As a result the @PreAuthorize (etc) annotations will be ignored.
   * <p> See: https://stackoverflow.com/a/65610687
   */
  @Override
  public MethodInterceptor methodSecurityInterceptor(
      MethodSecurityMetadataSource methodSecurityMetadataSource) {
    return abacDisabled ? new SimpleTraceInterceptor()
        : super.methodSecurityInterceptor(methodSecurityMetadataSource);
  }

  /**
   * Registration of custom SpEL expressions, here to include ABAC checks.
   */
  @Override
  protected MethodSecurityExpressionHandler createExpressionHandler() {
    CustomMethodSecurityExpressionHandler expressionHandler =
        new CustomMethodSecurityExpressionHandler(abacConfig, compositionService);
    //expressionHandler.setPermissionEvaluator(new AbacPermissionEvaluator());  // TODO-505: remove
    return expressionHandler;
  }

}
