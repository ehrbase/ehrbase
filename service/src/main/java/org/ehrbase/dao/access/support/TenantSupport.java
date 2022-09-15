package org.ehrbase.dao.access.support;

import java.util.Optional;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.tenant.TenantAuthentication;
import org.ehrbase.functional.Try;
import org.ehrbase.tenant.DefaultTenantAuthentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

public final class TenantSupport {
  private static final Logger LOGGER = LoggerFactory.getLogger(TenantSupport.class);
  
  private static final String WARN_NOT_TENEANT_IDENT = "No tenent identifier provided, falling back to default tenant identifier {}";
  
  public static String currentTenantIdentifier() {
    return Optional
      .ofNullable(SecurityContextHolder.getContext())
      .map(ctx ->
        ctx.getAuthentication()
      )
      .filter(auth -> auth != null)
      .filter(auth ->
        auth instanceof DefaultTenantAuthentication
      )
      .map(DefaultTenantAuthentication.class::cast)
      .map(tAuth ->
        tAuth.getTenantId()
      )
      .filter(StringUtils::isNotEmpty)
      .orElseGet(() -> {
        LOGGER.warn(WARN_NOT_TENEANT_IDENT, TenantAuthentication.getDefaultTenantId());
        return TenantAuthentication.getDefaultTenantId();
      });
  }
  
  private static final String ERR_TENANT_ID_MISSMATCH = "Provided tenant id[%s] does not match session tenant id[%s]";
  
  public static Try<String, InternalServerException> isValidTenantId(String tenantId, Supplier<String> currentTenant) {
    String currentTenantIdentifier = currentTenant.get();
    
    return currentTenantIdentifier.equals(tenantId)
        ? Try.success(tenantId)
        : Try.failure(new InternalServerException(String.format(ERR_TENANT_ID_MISSMATCH, tenantId, currentTenantIdentifier)));
  }
}
