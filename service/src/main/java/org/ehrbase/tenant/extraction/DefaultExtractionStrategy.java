/*
 * Copyright (c) 2019 vitasystems GmbH and Hannover Medical School.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.tenant.extraction;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.SignatureGenerationException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import java.util.Optional;
import org.apache.commons.codec.binary.Base64;
import org.ehrbase.api.tenant.TenantAuthentication;
import org.ehrbase.api.tenant.TenantIdExtractionStrategy;
import org.ehrbase.tenant.DefaultTenantAuthentication;

public class DefaultExtractionStrategy implements TenantIdExtractionStrategy<String> {

    private static final Optional<TenantAuthentication<String>> DEFAULT_TENANT_AUTH =
            Optional.of(DefaultTenantAuthentication.ofToken(JWT.create()
                    .withClaim("tnt", TenantAuthentication.getDefaultTenantId())
                    .sign(new NoneAlgorithm())));

    @Override
    public boolean accept(Object... args) {
        return true;
    }

    @Override
    public Optional<TenantAuthentication<String>> extract(Object... args) {
        return DEFAULT_TENANT_AUTH;
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
