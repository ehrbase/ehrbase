package org.ehrbase.application.abac;

import org.aopalliance.intercept.MethodInterceptor;
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
        new CustomMethodSecurityExpressionHandler();
    expressionHandler.setPermissionEvaluator(new AbacPermissionEvaluator());
    return expressionHandler;
  }

}
