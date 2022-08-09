package org.ehrbase.api.tenant;

import java.util.Optional;

public interface TenantIdExtractionStrategy<T> {
  public int priority();
  public boolean accept(Object...args);
  public Optional<TenantAuthentication<T>> extract(Object...args);
  
}
