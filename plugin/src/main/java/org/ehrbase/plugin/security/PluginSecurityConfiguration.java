/*
 * Copyright (c) 2022 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.plugin.security;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.SourceLocation;
import org.aspectj.runtime.internal.AroundClosure;
import org.ehrbase.api.annotations.TenantAware;
import org.ehrbase.api.aspect.AnnotationAspect;
import org.ehrbase.api.aspect.AuthorizationAspect;
import org.ehrbase.api.aspect.TenantAspect;
import org.ehrbase.api.authorization.EhrbaseAuthorization;
import org.springframework.aop.Advisor;
import org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;

public class PluginSecurityConfiguration implements ApplicationContextAware {
    // @format:off
    @SuppressWarnings("rawtypes")
    private static class SignatureAdap implements org.aspectj.lang.reflect.MethodSignature {
      private final MethodInvocation invocation;
  
      SignatureAdap(MethodInvocation invocation) { this.invocation = invocation; }
  
      public String toShortString() { return invocation.getMethod().getName(); }
      public String toLongString() { return invocation.getMethod().toString(); }
      public String getName() { return toShortString(); }
      public int getModifiers() { return invocation.getMethod().getModifiers(); }
      public String getDeclaringTypeName() { return invocation.getMethod().getDeclaringClass().getTypeName(); }
      public Class getDeclaringType() { return invocation.getMethod().getDeclaringClass(); }
      public Class[] getParameterTypes() { return invocation.getMethod().getParameterTypes(); }
      public String[] getParameterNames() { throw new UnsupportedOperationException(); }
      public Class[] getExceptionTypes() { return invocation.getMethod().getExceptionTypes(); }
      public Class getReturnType() { return invocation.getMethod().getReturnType(); }
      public Method getMethod() { return invocation.getMethod(); }
    }  
      
    private static class ProceedingJoinPointAdapter implements ProceedingJoinPoint {
      private final MethodInvocation invocation;
  
      ProceedingJoinPointAdapter(MethodInvocation invocation) { this.invocation = invocation; }
  
      public String toShortString() { return invocation.toString(); }
      public String toLongString() { return invocation.toString(); }
      public Object getThis() { return invocation.getThis(); }
      public Object getTarget() { return invocation.getThis(); }
      public Object[] getArgs() { return invocation.getArguments(); }
      public Signature getSignature() { return new SignatureAdap(invocation); }
      public SourceLocation getSourceLocation() { throw new UnsupportedOperationException(); }
      public String getKind() { return "method execution"; }
      public StaticPart getStaticPart() { throw new UnsupportedOperationException(); }
      public void set$AroundClosure(AroundClosure arc) { throw new UnsupportedOperationException(); }
      public Object proceed() throws Throwable { return invocation.proceed(); }
  
      public Object proceed(Object[] args) throws Throwable {
        Method method = invocation.getMethod();
        Object obj = invocation.getThis();
        return method.invoke(obj, args);
      }
    }
    // @format:on

    private abstract static class AspectAdapter implements MethodInterceptor {
        private final AnnotationAspect aspect;

        AspectAdapter(AnnotationAspect aspect) {
            this.aspect = aspect;
        }

        protected AnnotationAspect getAspect() {
            return aspect;
        }

        public abstract Object invoke(MethodInvocation invocation) throws Throwable;
    }

    public static class AnyOfAspectsCondition extends AnyNestedCondition {
        public AnyOfAspectsCondition() {
            super(ConfigurationPhase.REGISTER_BEAN);
        }

        @ConditionalOnBean(value = AuthorizationAspect.class)
        static class OnAuthorization {}

        @ConditionalOnBean(value = TenantAspect.class)
        static class OnTenant {}
    }

    private ApplicationContext applicationContext;

    @Bean
    @Conditional(AnyOfAspectsCondition.class)
    public DefaultAdvisorAutoProxyCreator advisorAutoProxyCreator() {
        return new DefaultAdvisorAutoProxyCreator();
    }

    @Bean
    @ConditionalOnBean(value = {AuthorizationAspect.class, AuthorizationInfo.AuthorizationEnabled.class})
    public Advisor authorizationAspect() {
        ApplicationContext parentCtx = applicationContext.getParent();
        AuthorizationAspect theAspect = parentCtx.getBean(AuthorizationAspect.class);

        return new DefaultPointcutAdvisor(
                new AnnotationMatchingPointcut(null, EhrbaseAuthorization.class, true), new AspectAdapter(theAspect) {
                    public Object invoke(MethodInvocation invocation) throws Throwable {
                        return getAspect().action(new ProceedingJoinPointAdapter(invocation), null);
                    }
                });
    }

    @Bean
    @ConditionalOnBean(value = TenantAspect.class)
    public Advisor tenantAspect() {
        ApplicationContext parentCtx = applicationContext.getParent();
        TenantAspect theAspect = parentCtx.getBean(TenantAspect.class);

        return new DefaultPointcutAdvisor(
                new AnnotationMatchingPointcut(TenantAware.class, true), new AspectAdapter(theAspect) {
                    public Object invoke(MethodInvocation invocation) throws Throwable {
                        Method method = invocation.getMethod();
                        Class<?> declaringClass = method.getDeclaringClass();
                        TenantAware annotation =
                                Objects.requireNonNull(declaringClass.getAnnotation(TenantAware.class));
                        return getAspect().action(new ProceedingJoinPointAdapter(invocation), List.of(annotation));
                    }
                });
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
