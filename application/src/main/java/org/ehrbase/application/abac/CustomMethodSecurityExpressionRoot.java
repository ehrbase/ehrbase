package org.ehrbase.application.abac;

import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;

public class CustomMethodSecurityExpressionRoot extends SecurityExpressionRoot implements MethodSecurityExpressionOperations {

  public CustomMethodSecurityExpressionRoot(Authentication authentication) {
    super(authentication);
  }

  /**
   * Custom SpEL expression to be used to check if the remote ABAC allows the operation by given data.
   * @param Organization
   * @param patient
   * @param template
   * @return
   */
  public boolean isAllowed(String Organization, String patient, String template) {
    // TODO add logic
    return false;
  }


  @Override
  public void setFilterObject(Object filterObject) {

  }

  @Override
  public Object getFilterObject() {
    return null;
  }

  @Override
  public void setReturnObject(Object returnObject) {

  }

  @Override
  public Object getReturnObject() {
    return null;
  }

  @Override
  public Object getThis() {
    return null;
  }
}
