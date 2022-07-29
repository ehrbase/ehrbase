package org.ehrbase.tenant;

import java.util.Optional;

public interface TenantIdExtractionStrategy {
  public int priority();
  public boolean accept(Object...args);
  public Optional<String> extract(Object...args);
}
