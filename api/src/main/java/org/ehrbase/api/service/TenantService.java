package org.ehrbase.api.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.ehrbase.api.tenant.Tenant;

public interface TenantService extends BaseService {
  public String getCurrentTenantIdentifier();
  UUID create(Tenant tenant);
  Tenant update(Tenant tenant);
  Optional<Tenant> findBy(String tenantId);
  List<Tenant> getAll();
}
 