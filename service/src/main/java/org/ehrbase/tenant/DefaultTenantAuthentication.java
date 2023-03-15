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
package org.ehrbase.tenant;

import com.auth0.jwt.JWT;
import com.auth0.jwt.impl.JWTParser;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.Payload;
import org.apache.commons.codec.binary.Base64;
import org.ehrbase.api.tenant.TenantAuthentication;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;

public class DefaultTenantAuthentication extends AbstractAuthenticationToken implements TenantAuthentication<String> {
    private static final long serialVersionUID = -187707458684929521L;
    public static final String TENANT_CLAIM = "tnt";
    private String name;

    public static <T> DefaultTenantAuthentication of(TenantAuthentication<T> auth, Converter<T, String> converter) {
        DefaultTenantAuthentication defAuth = new DefaultTenantAuthentication();
        defAuth.tenantId = auth.getTenantId();
        defAuth.name = auth.getName();
        defAuth.raw = converter.convert(auth.getAuthentication());
        defAuth.token = JWT.decode(defAuth.raw);
        return defAuth;
    }

    public static <T> DefaultTenantAuthentication of(String tenantId) {
        DefaultTenantAuthentication auth = new DefaultTenantAuthentication();
        auth.tenantId = tenantId;
        return auth;
    }

    public static <T> DefaultTenantAuthentication ofToken(String token) {
        return new DefaultTenantAuthentication(token);
    }

    private String tenantId;
    private DecodedJWT token;
    private String raw;
    private Payload payload;

    private DefaultTenantAuthentication() {
        super(null);
    }

    private DefaultTenantAuthentication(String token) {
        super(null);
        this.raw = token;
        this.token = JWT.decode(token);
        this.payload = new JWTParser().parsePayload(new String(Base64.decodeBase64(this.token.getPayload())));
        this.tenantId = payload.getClaim(TENANT_CLAIM).asString();
    }

    @Override
    public String getName() {
        return this.name;
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

    @Override
    public String getAuthentication() {
        return raw;
    }
}
