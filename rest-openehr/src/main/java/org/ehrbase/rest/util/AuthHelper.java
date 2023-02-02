package org.ehrbase.rest.util;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.authentication.AbstractAuthenticationToken;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.*;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

public class AuthHelper {

    private static final String BASIC = "Basic ";

    private AuthHelper() {}

    /**
     * Gets the currently authenticated username from the given HTTP request.
     *
     * @param request The HTTP request
     * @return The username if it exists, or an empty string if not
     */
    public static String getCurrentAuthenticatedUsername(HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();
        String username;

        // Check if the principal is null and get the principal from the SecurityContext if necessary
        if (principal == null) {
            principal = SecurityContextHolder.getContext().getAuthentication();
        }

        username = Optional.ofNullable(principal)
                .filter(AbstractAuthenticationToken.class::isInstance)
                .map(AbstractAuthenticationToken.class::cast)
                .map(AbstractAuthenticationToken::getPrincipal)
                .filter(DecodedJWT.class::isInstance)
                .map(DecodedJWT.class::cast)
                .map(DecodedJWT::getSubject)
                .orElse(EMPTY);

        // TODO:: for some  reason for basic auth not working principal.getName() default tenant jwt context is running
        if (isBlank(username) && request.getHeader(AUTHORIZATION) != null
                && request.getHeader(AUTHORIZATION).startsWith("Basic")) {
            username = getBasicAuthUsername(request);
        }

        // If the username is still blank, use the name from the Principal
        if (isBlank(username) && principal != null) {
            username = principal.getName();
        }

        return username;
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

        String encoded = (authorization.length() <= BASIC.length()) ? "" : authorization.substring(BASIC.length());
        String credentials = new String(base64Decode(encoded));

        int colonIndex = credentials.indexOf(":");
        if (colonIndex == -1) {
            return null;
        }

        return credentials.substring(0, colonIndex);
    }

    private static byte[] base64Decode(String value) {
        try {
            return Base64.getDecoder().decode(value);
        } catch (Exception ex) {
            return new byte[0];
        }
    }
}
