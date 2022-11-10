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
package org.ehrbase.tenant;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.ehrbase.api.annotations.TenantAware;
import org.ehrbase.api.tenant.ExtractionStrategyAware;
import org.ehrbase.api.tenant.TenantAuthentication;
import org.ehrbase.api.tenant.TenantIdExtractionStrategy;
import org.springframework.aop.aspectj.MethodInvocationProceedingJoinPoint;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@Aspect
public class TenantAspect implements ExtractionStrategyAware {
    private List<TenantIdExtractionStrategy<?>> extractionStrategies;

    public TenantAspect() {
        this(new ArrayList<>());
    }

    private static final Comparator<TenantIdExtractionStrategy<?>> PRIORITY_SORT =
            (s1, s2) -> s1.priority() - s2.priority();

    public TenantAspect(List<TenantIdExtractionStrategy<?>> extractionStrategies) {
        extractionStrategies.sort(PRIORITY_SORT);
        this.extractionStrategies = extractionStrategies;
    }

    public <T> void addExtractionStrategy(TenantIdExtractionStrategy<T> strategy) {
        extractionStrategies.add(strategy);
        extractionStrategies.sort(PRIORITY_SORT);
    }

    @Pointcut(value = "@within(tenantAnnotation)")
    public void matchTenantAnnotation(org.ehrbase.api.annotations.TenantAware tenantAnnotation) {}

    private static final String ERR_NON_TENANT_ID = "Fatal error, no tenant id avaliable";

    @Around("matchTenantAnnotation(tenantAnnotation)")
    public Object securedCall(ProceedingJoinPoint pjp, TenantAware tenantAnnotation) throws Throwable {
        if (isMethodTenantAware(pjp, tenantAnnotation)) {
            Object[] args = pjp.getArgs();
            TenantAuthentication<?> tenant = Objects.requireNonNull(extract(args), ERR_NON_TENANT_ID);
            SecurityContext ctx = SecurityContextHolder.getContext();
            ctx.setAuthentication(DefaultTenantAuthentication.of(tenant));
        }
        return pjp.proceed();
    }

    private boolean isMethodTenantAware(ProceedingJoinPoint pjp, TenantAware tenantAnnotation) {
        if (pjp instanceof MethodInvocationProceedingJoinPoint
                && ((MethodInvocationProceedingJoinPoint) pjp).getSignature() instanceof MethodSignature) {
            MethodInvocationProceedingJoinPoint mijp = (MethodInvocationProceedingJoinPoint) pjp;
            MethodSignature signature = (MethodSignature) mijp.getSignature();

            List<String> allVariants = List.of(
                    signature.getMethod().getName(),
                    signature.toShortString(),
                    signature.toLongString(),
                    signature.toString());

            for (String exclude : tenantAnnotation.exclude()) if (allVariants.contains(exclude)) return false;
        }

        return true;
    }

    private TenantAuthentication<?> extract(Object... args) {
        return extractionStrategies.stream()
                .filter(s -> s.accept(args))
                .map(s -> s.extract(args))
                .filter(opt -> opt.isPresent())
                .map(opt -> opt.get())
                .reduce(null, (str1, str2) -> str2);
    }
}
