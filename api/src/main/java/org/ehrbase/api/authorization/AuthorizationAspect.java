package org.ehrbase.api.authorization;

import org.aspectj.lang.ProceedingJoinPoint;

public interface AuthorizationAspect {
  public void matchEhrbaseAuthorization();
  public void matchEhrbaseAuthorizations();
  public Object action(ProceedingJoinPoint pjp) throws Throwable;
}
