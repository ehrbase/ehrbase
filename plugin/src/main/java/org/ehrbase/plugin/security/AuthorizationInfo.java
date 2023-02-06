package org.ehrbase.plugin.security;

public interface AuthorizationInfo {
  public boolean isDisabled();
  
  public static class AuthorizationDisabled implements AuthorizationInfo {
    public boolean isDisabled() { return true; }
  }
  
  public static class AuthorizationEnabled implements AuthorizationInfo {
    public boolean isDisabled() { return false; }
  }
}
