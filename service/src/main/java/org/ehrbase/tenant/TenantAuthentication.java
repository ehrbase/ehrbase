package org.ehrbase.tenant;


import org.apache.commons.codec.binary.Base64;
import org.springframework.security.authentication.AbstractAuthenticationToken;

import com.auth0.jwt.JWT;
import com.auth0.jwt.impl.JWTParser;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.Payload;

public class TenantAuthentication extends AbstractAuthenticationToken {
  private static final long serialVersionUID = -187707458684929521L;

  public static final String TENANT_CLAIM = "tnt";
  
  private final String tenantId;
  private final DecodedJWT token;
  private final Payload payload;
  
  public TenantAuthentication(String token) {
    super(null);
    this.token = JWT.decode(token);
    this.payload = new JWTParser().parsePayload(new String(Base64.decodeBase64(this.token.getPayload())));
    this.tenantId = payload.getClaim(TENANT_CLAIM).asString();
  }
  
  public String getTenantId() {
    return tenantId;
  }
  
  @Override
  public Object getCredentials() {
    return token;
  }

  @Override
  public Object getPrincipal() {
    return token;
  }
}
