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

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Predicate;
import org.apache.commons.lang3.function.TriFunction;
import org.ehrbase.api.tenant.TenantAuthentication;
import org.ehrbase.api.tenant.TenantIdExtractionStrategy;
import org.ehrbase.tenant.DefaultTenantAuthentication;
import org.ehrbase.tenant.TokenSupport;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.AbstractOAuth2Token;

// @format:off
public abstract class AuthenticatedExtractionStrategy<A extends Authentication> implements TenantIdExtractionStrategy<String> {

    static class TenantAuthenticationAdapter implements MethodInterceptor {
        private static Method tenantIdCall;
        private static Method defTenantIdCall;
        private static Method authenticationCall;

        static {
            try {
                defTenantIdCall = TenantAuthentication.class.getDeclaredMethod("getDefaultTenantId");
                tenantIdCall = TenantAuthentication.class.getDeclaredMethod("getTenantId");
                authenticationCall = TenantAuthentication.class.getDeclaredMethod("getAuthentication");
            } catch (NoSuchMethodException | SecurityException e) {
                throw new IllegalStateException(e);
            }
        }

        private final Authentication authentication;
        private final String rawToken;
        private final String tenantId;

        TenantAuthenticationAdapter(Authentication authentication, String rawToken, String tenantId) {
            this.authentication = authentication;
            this.rawToken = rawToken;
            this.tenantId = tenantId;
        }

        public Object intercept(Object me, Method method, Object[] args, MethodProxy proxy) throws Throwable {
            if (TenantAuthenticationAdapter.defTenantIdCall.equals(method))
                return TenantAuthentication.getDefaultTenantId();
            else if (TenantAuthenticationAdapter.tenantIdCall.equals(method)) return tenantId;
            else if (TenantAuthenticationAdapter.authenticationCall.equals(method)) return rawToken;
            else return method.invoke(authentication, args);
        }
    }

    @SuppressWarnings("unchecked")
    private static final TriFunction<Authentication, String, String, TenantAuthentication<String>> TO_AUTH =
            (Authentication auth, String rawToken, String tenantId) -> {
                Enhancer enhancer = new Enhancer();
                enhancer.setSuperclass(AbstractAuthenticationToken.class);
                enhancer.setInterfaces(new Class[] {TenantAuthentication.class});
                enhancer.setCallback(new TenantAuthenticationAdapter(auth, rawToken, tenantId));
                Object created =
                        enhancer.create(new Class[] {Collection.class}, new Object[] {Collections.emptyList()});
                return (TenantAuthentication<String>) created;
            };

    public static class TokenAuthenticatedExtractionStrategy extends AuthenticatedExtractionStrategy<AbstractAuthenticationToken> {
        public TokenAuthenticatedExtractionStrategy() {
            super(auth -> auth instanceof AbstractAuthenticationToken);
        }

        public Optional<TenantAuthentication<String>> extractWithPrior(Optional<TenantAuthentication<?>> priorAuthentication, Object... args) {
            SecurityContext ctx = SecurityContextHolder.getContext();

            if (ctx.getAuthentication() instanceof AbstractAuthenticationToken auth) {
                if (auth.getCredentials() instanceof AbstractOAuth2Token token) {
                    Optional<String> optTenantId = TokenSupport.extractClaim(token.getTokenValue(), DefaultTenantAuthentication.TENANT_CLAIM);

                    if (optTenantId.isPresent())
                        return Optional.of(TO_AUTH.apply(auth, token.getTokenValue(), optTenantId.get()));
                    else if (priorAuthentication.isPresent()) {
                        // try to resolve tenantId from prior authentication
                        return Optional.of(TO_AUTH.apply(
                                auth,
                                token.getTokenValue(),
                                priorAuthentication.get().getTenantId()));
                    } else throw new IllegalStateException();
                } else {
                    return Optional.empty();
                }
            } else throw new IllegalStateException();
        }

        public int priority() {
            return 1000;
        }
    }

    public static class AuthenticationExtractionStrategy extends AuthenticatedExtractionStrategy<UsernamePasswordAuthenticationToken> {
        public AuthenticationExtractionStrategy() {
            super(auth -> auth instanceof UsernamePasswordAuthenticationToken);
        }

        public Optional<TenantAuthentication<String>> extractWithPrior(Optional<TenantAuthentication<?>> priorAuthentication, Object... args) {
            SecurityContext ctx = SecurityContextHolder.getContext();

            if (priorAuthentication.isPresent())
                return Optional.of(TO_AUTH.apply(
                        ctx.getAuthentication(),
                        priorAuthentication.get().getAuthentication().toString(),
                        priorAuthentication.get().getTenantId()));
            else throw new IllegalStateException();
        }

        public int priority() {
            return 900;
        }
    }

    private final Predicate<Authentication> predicate;

    protected AuthenticatedExtractionStrategy(Predicate<Authentication> predicate) {
        this.predicate = predicate;
    }

    public boolean accept(Object... args) {
        SecurityContext ctx = SecurityContextHolder.getContext();
        Authentication theAuthentication = ctx.getAuthentication();

        return null != theAuthentication && predicate.test(theAuthentication);
    }

    public Optional<TenantAuthentication<String>> extract(Object... args) {
        return extract(null, args);
    }
}
// @format:on
