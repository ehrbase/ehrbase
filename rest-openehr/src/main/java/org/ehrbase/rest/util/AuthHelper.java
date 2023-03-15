/*
 * Copyright (c) 2023 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.rest.util;

import static org.apache.commons.lang3.StringUtils.*;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

public class AuthHelper {

    private static final String AUTHENTICATION_SCHEME_BASIC = "Basic";

    private AuthHelper() {}

    /**
     * Gets the currently authenticated username from the given HTTP request.
     *
     * @param request The HTTP request
     * @return The username if it exists, or an empty string if not
     */
    public static String getCurrentAuthenticatedUsername(HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();
        return getJwtSubject(principal).orElseGet(() -> Optional.of(request)
                .map(AuthHelper::getBasicAuthUsername)
                .filter(StringUtils::isNotBlank)
                .orElseGet(() ->
                        Optional.ofNullable(principal).map(Principal::getName).orElse(null)));
    }

    private static Optional<String> getJwtSubject(Principal principal) {
        return Optional.ofNullable(principal)
                .filter(AbstractAuthenticationToken.class::isInstance)
                .map(AbstractAuthenticationToken.class::cast)
                .map(AbstractAuthenticationToken::getPrincipal)
                .filter(DecodedJWT.class::isInstance)
                .map(DecodedJWT.class::cast)
                .map(DecodedJWT::getSubject)
                .filter(StringUtils::isNotBlank);
    }

    /**
     * Extracts the requested claim from the token's claims.
     *
     * @param token Token
     * @param requestedClaim The claim to be retrieved
     * @return The value of the requested claim
     */
    public static String getRequestedJwtClaim(AbstractAuthenticationToken token, String requestedClaim) {
        String claim = EMPTY;

        Object principal = token.getCredentials();
        if (principal instanceof Jwt jwt) {
            Map<String, Object> claims = jwt.getClaims();
            if (claims != null && claims.containsKey(requestedClaim)) {
                claim = claims.get(requestedClaim).toString();
            }
        } else if (principal instanceof DecodedJWT jwt) {
            Map<String, Claim> claims = jwt.getClaims();
            if (claims != null && claims.containsKey(requestedClaim)) {
                claim = claims.get(requestedClaim).asString();
            }
        } else {
            throw new IllegalArgumentException("Invalid authentication, no claims available.");
        }

        return claim;
    }

    private static String getBasicAuthUsername(HttpServletRequest request) {
        String authorization = request.getHeader(AUTHORIZATION);
        if (authorization == null || !startsWithIgnoreCase(authorization, "basic ")) {
            return null;
        }

        authorization = authorization.trim();
        if (!startsWithIgnoreCase(authorization, AUTHENTICATION_SCHEME_BASIC)) {
            return null;
        }

        if (authorization.equalsIgnoreCase(AUTHENTICATION_SCHEME_BASIC)) {
            return null;
        }

        byte[] base64Token = authorization.substring(6).getBytes(StandardCharsets.UTF_8);
        String credentials = new String(base64Decode(base64Token), StandardCharsets.UTF_8);

        int colonIndex = credentials.indexOf(":");
        if (colonIndex == -1) {
            return null;
        }

        return credentials.substring(0, colonIndex);
    }

    private static byte[] base64Decode(byte[] value) {
        try {
            return Base64.getDecoder().decode(value);
        } catch (Exception ex) {
            return new byte[0];
        }
    }
}
