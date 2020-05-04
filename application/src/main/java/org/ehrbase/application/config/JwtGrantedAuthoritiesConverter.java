package org.ehrbase.application.config;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

public class JwtGrantedAuthoritiesConverter extends JwtAuthenticationConverter {

    @Override
    protected Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        Collection<GrantedAuthority> authorities =  super.extractAuthorities(jwt);

        List<String> claimsRealmAccess = jwt.getClaimAsStringList("realm_access");

        if (claimsRealmAccess != null && claimsRealmAccess.size() > 0) {

            String roles = claimsRealmAccess.get(0);

            if (roles != null && roles.contains("Admin")) {

                authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));

            }



        }

        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

        return authorities;
    }

}

