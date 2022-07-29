package org.ehrbase.tenant.extraction;

import java.util.Optional;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;

import org.ehrbase.tenant.TenantIdExtractionStrategy;

public class HttpServletRequestExtractionStrategy implements TenantIdExtractionStrategy {
  private static final String AUTOR_HEADER = "Authorization";
  private static final String TOKEN_PREFIX = "Bearer";
  
  
  @Override
  public boolean accept(Object... args) {
    return extractInternal(args).map(req -> true).orElse(false);
  }

  @Override
  public Optional<String> extract(Object... args) {
    Optional<String> req = extractInternal(args)
        .map(r -> r.getHeader(AUTOR_HEADER))
        .filter(h -> h != null && h.startsWith(TOKEN_PREFIX))
        .map(h -> h.substring(TOKEN_PREFIX.length()).trim());
    
    return req;
  }
  
  private Optional<HttpServletRequest> extractInternal(Object... args) {
    return Stream.of(args).filter(arg -> arg instanceof HttpServletRequest).map(HttpServletRequest.class::cast).findFirst();
  }

  @Override
  public int priority() {
    return 1;
  }

}
