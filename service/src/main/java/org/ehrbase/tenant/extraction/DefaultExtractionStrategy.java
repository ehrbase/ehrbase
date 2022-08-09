package org.ehrbase.tenant.extraction;

import java.util.Optional;

import org.apache.commons.codec.binary.Base64;
import org.ehrbase.api.tenant.TenantAuthentication;
import org.ehrbase.api.tenant.TenantIdExtractionStrategy;
import org.ehrbase.tenant.DefaultTenantAuthentication;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.SignatureGenerationException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

public class DefaultExtractionStrategy implements TenantIdExtractionStrategy<String> {

  @Override
  public boolean accept(Object... args) {
    return true;
  }

  @Override
  public Optional<TenantAuthentication<String>> extract(Object... args) {
    String token = JWT.create().withClaim("tnt", TenantAuthentication.getDefaultTenantId()).sign(new NoneAlgorithm());
    return Optional.of(new DefaultTenantAuthentication(token));
  }

  @Override
  public int priority() {
    return 0;
  }

  private static class NoneAlgorithm extends Algorithm {

    NoneAlgorithm() {
      super("none", "none");
    }

    @Override
    public void verify(DecodedJWT jwt) throws SignatureVerificationException {
      byte[] signatureBytes = Base64.decodeBase64(jwt.getSignature());
      if (signatureBytes.length > 0) {
        throw new SignatureVerificationException(this);
      }
    }

    @Override
    public byte[] sign(byte[] contentBytes) throws SignatureGenerationException {
      return new byte[0];
    }
  }
}
