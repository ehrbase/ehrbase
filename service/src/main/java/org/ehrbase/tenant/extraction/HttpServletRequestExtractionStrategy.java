package org.ehrbase.tenant.extraction;

import java.util.Optional;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;

import org.ehrbase.api.tenant.TenantAuthentication;
import org.ehrbase.api.tenant.TenantIdExtractionStrategy;
import org.ehrbase.tenant.DefaultTenantAuthentication;

import com.auth0.jwt.JWT;
import com.auth0.jwt.impl.NullClaim;
import com.auth0.jwt.interfaces.DecodedJWT;

public class HttpServletRequestExtractionStrategy implements TenantIdExtractionStrategy<String> {
  private static final String AUTOR_HEADER = "Authorization";
  private static final String TOKEN_PREFIX = "Bearer";
  
  @Override
  public boolean accept(Object... args) {
    return extractInternal(args).map(req -> true).orElse(false);
  }

  @Override
  public Optional<TenantAuthentication<String>> extract(Object... args) {
    return extractJwt(args)
    .flatMap(token -> containsTnT(token))
    .map(token -> new DefaultTenantAuthentication(token));
  }
  
  private Optional<String> extractJwt(Object...args) {
    return extractInternal(args)
    .map(r -> r.getHeader(AUTOR_HEADER))
    .filter(h -> h != null && h.startsWith(TOKEN_PREFIX))
    .map(h -> h.substring(TOKEN_PREFIX.length()).trim());
  }
  
  private Optional<String> containsTnT(String token) {
    DecodedJWT decode = JWT.decode(token);
    return decode.getClaim("tnt") instanceof NullClaim ? Optional.empty() : Optional.of(token);
  } 
  
  private Optional<HttpServletRequest> extractInternal(Object... args) {
    return Stream.of(args).filter(arg -> arg instanceof HttpServletRequest).map(HttpServletRequest.class::cast).findFirst();
  }
  
  @Override
  public int priority() {
    return 1;
  }
}
