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

import org.aopalliance.intercept.MethodInvocation;
import org.ehrbase.api.service.CompositionService;
import org.ehrbase.api.service.ContributionService;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class CustomMethodSecurityExpressionHandler extends DefaultMethodSecurityExpressionHandler {

  private final AbacConfig abacConfig;
  private final AuthenticationTrustResolver trustResolver =
      new AuthenticationTrustResolverImpl();
  private final CompositionService compositionService;
  private final ContributionService contributionService;

  @Lazy
  public CustomMethodSecurityExpressionHandler(AbacConfig abacConfig, CompositionService compositionService,
      ContributionService contributionService) {
    this.abacConfig = abacConfig;
    this.compositionService = compositionService;
    this.contributionService = contributionService;
  }

  @Override
  protected MethodSecurityExpressionOperations createSecurityExpressionRoot(
      Authentication authentication, MethodInvocation invocation) {
    CustomMethodSecurityExpressionRoot root =
        new CustomMethodSecurityExpressionRoot(authentication, abacConfig);
    root.setCompositionService(this.compositionService);
    root.setContributionService(this.contributionService);
    root.setPermissionEvaluator(getPermissionEvaluator());
    root.setTrustResolver(this.trustResolver);
    root.setRoleHierarchy(getRoleHierarchy());
    return root;
  }

}
