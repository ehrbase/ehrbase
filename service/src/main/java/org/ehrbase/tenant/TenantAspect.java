package org.ehrbase.tenant;

import java.util.List;
import java.util.Objects;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.ehrbase.api.annotations.TenantAware;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

@Aspect
public class TenantAspect {
  private final List<TenantIdExtractionStrategy> extractionStrategies; 
  
  public TenantAspect(List<TenantIdExtractionStrategy> extractionStrategies) {
    extractionStrategies.sort((s1, s2) -> s1.priority() - s2.priority());
    this.extractionStrategies = extractionStrategies;
  }
  
  @Pointcut(value = "@annotation(tenantAnnotation)")
  public void matchTenantAnnotation(
      org.ehrbase.api.annotations.TenantAware tenantAnnotation) {}

  private static final String ERR_NON_TENANT_ID = "Fatal error, no tenant id avaliable";
  
  @Around("matchTenantAnnotation(tenantAnnotation)")
  public Object securedCall(ProceedingJoinPoint pjp, TenantAware tenantAnnotation) throws Throwable {
    Object[] args = pjp.getArgs();
    
    String extract = Objects.requireNonNull(extract(args), ERR_NON_TENANT_ID);
    
    SecurityContext secCtx = new SecurityContextImpl();
    secCtx.setAuthentication(new TenantAuthentication(extract));
    SecurityContextHolder.setContext(secCtx);
    
    return pjp.proceed();
  }
  
  private String extract(Object...args) {
    return extractionStrategies.stream()
      .filter(s -> s.accept(args))
      .map(s -> s.extract(args))
      .filter(opt -> opt.isPresent())
      .map(opt -> opt.get())
      .reduce("", (str1, str2) -> str2);
  }
}
