package org.ehrbase.api.tenant;

public interface TenantAuthentication<T> {
  public static final String DEFAULT_TENANT_ID = "1f332a66-0e57-11ed-861d-0242ac120002";

  public String getTenantId();

  public static String getDefaultTenantId() {
    return DEFAULT_TENANT_ID;
  }
  
  public T getAuthentication();
}
