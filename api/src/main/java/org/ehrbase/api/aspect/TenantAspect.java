/*
 * Copyright (c) 2019 vitasystems GmbH and Hannover Medical School.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.api.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.ehrbase.api.annotations.TenantAware;
import org.ehrbase.api.tenant.ExtractionStrategyAware;
import org.ehrbase.api.tenant.TenantIdExtractionStrategy;

public interface TenantAspect extends ExtractionStrategyAware, AnnotationAspect {
//    public void matchTenantAnnotation(org.ehrbase.api.annotations.TenantAware tenantAnnotation);
//    public Object action(ProceedingJoinPoint pjp, TenantAware tenantAnnotation) throws Throwable;
    public <T> void addExtractionStrategy(TenantIdExtractionStrategy<T> strategy);
}
