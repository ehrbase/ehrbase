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

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.ehrbase.api.annotations.TenantAware;
import org.ehrbase.api.tenant.TenantAuthentication;
import org.ehrbase.api.tenant.TenantIdExtractionStrategy;
import org.springframework.aop.aspectj.MethodInvocationProceedingJoinPoint;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@Aspect
public class DefaultTenantAspect implements org.ehrbase.api.aspect.TenantAspect {
    private static final List<Class<? extends Annotation>> MATCHED_ANNOTATIONS = List.of(TenantAware.class);
    private final List<TenantIdExtractionStrategy<?>> extractionStrategies;

    public DefaultTenantAspect() {
        this(new ArrayList<>());
    }

    private static final Comparator<TenantIdExtractionStrategy<?>> PRIORITY_SORT =
            Comparator.comparingInt(TenantIdExtractionStrategy::priority);

    public DefaultTenantAspect(List<TenantIdExtractionStrategy<?>> extractionStrategies) {
        extractionStrategies.sort(PRIORITY_SORT);
        this.extractionStrategies = extractionStrategies;
    }

    public <T> void addExtractionStrategy(TenantIdExtractionStrategy<T> strategy) {
        extractionStrategies.add(strategy);
        extractionStrategies.sort(PRIORITY_SORT);
    }

    @Pointcut(value = "@within(tenantAnnotation)")
    public void matchTenantAnnotation(org.ehrbase.api.annotations.TenantAware tenantAnnotation) {}

    private static final String ERR_NON_TENANT_ID = "Fatal error, no tenant id available";

    /*
     * currently we support only TenantAuthentication<String>. when more
     * is needed we must implement a more sophisticated conversion.
     */
    @Around("matchTenantAnnotation(tenantAnnotation)")
    public Object action(ProceedingJoinPoint pjp, TenantAware tenantAnnotation) throws Throwable {
        return action(pjp, List.of(tenantAnnotation));
    }

    @Override
    public Object action(ProceedingJoinPoint pjp, List<Annotation> annotations) throws Throwable {
        annotations.stream()
                .filter(TenantAware.class::isInstance)
                .map(TenantAware.class::cast)
                .findFirst()
                .ifPresent(tenantAnnotation -> {
                    if (isMethodTenantAware(pjp, tenantAnnotation)) {
                        Object[] args = pjp.getArgs();
                        TenantAuthentication<?> tenant = Objects.requireNonNull(extract(args), ERR_NON_TENANT_ID);
                        SecurityContext ctx = SecurityContextHolder.getContext();
                        ctx.setAuthentication(DefaultTenantAuthentication.of(tenant, Object::toString));
                    }
                });
        return pjp.proceed();
    }

    private boolean isMethodTenantAware(ProceedingJoinPoint pjp, TenantAware tenantAnnotation) {
        if (tenantAnnotation.exclude().length != 0
                && pjp instanceof MethodInvocationProceedingJoinPoint mijp
                && mijp.getSignature() instanceof MethodSignature signature) {

            List<String> allVariants = List.of(
                    signature.getMethod().getName(),
                    signature.toShortString(),
                    signature.toLongString(),
                    signature.toString());

            for (String exclude : tenantAnnotation.exclude()) {
                if (allVariants.contains(exclude)) {
                    return false;
                }
            }
        }

        return true;
    }

    private TenantAuthentication<?> extract(Object... args) {
        Optional<TenantAuthentication<?>> priorAuth = Optional.empty();

        for (TenantIdExtractionStrategy<?> strg : extractionStrategies) {
            if (!strg.accept(args)) continue;

            Optional<? extends TenantAuthentication<?>> extract = strg.extractWithPrior(priorAuth, args);
            if (!extract.isPresent()) continue;

            priorAuth = (Optional<TenantAuthentication<?>>) extract;
        }

        return priorAuth.get();
    }

    @Override
    public List<Class<? extends Annotation>> matchAnnotations() {
        return MATCHED_ANNOTATIONS;
    }
}
