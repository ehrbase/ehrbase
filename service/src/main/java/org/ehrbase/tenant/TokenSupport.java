package org.ehrbase.tenant;

import java.util.Optional;

import org.apache.commons.codec.binary.Base64;

import com.auth0.jwt.JWT;
import com.auth0.jwt.impl.JWTParser;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.Payload;

public abstract class TokenSupport {
  
  public static Optional<String> extractClaim(String token, String claim) {
    DecodedJWT jwt = JWT.decode(token);
    Payload payload = new JWTParser().parsePayload(new String(Base64.decodeBase64(jwt.getPayload())));
    return Optional.ofNullable(payload.getClaim(claim).asString());
}

}
