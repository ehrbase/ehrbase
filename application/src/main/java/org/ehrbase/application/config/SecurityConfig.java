package org.ehrbase.application.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Value("${DISABLE_SECURITY:false}")
    private boolean disableSecurity;

    @Value("${AUTH_USER:ehrbase-user}")
    private String authUser;

    @Value("${AUTH_PASSWORD:SuperSecretPassword}")
    private String authPassword;

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {

        auth.inMemoryAuthentication()
                .withUser(authUser).password(passwordEncoder().encode(authPassword))
                .authorities("ROLE_USER");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {

        if (disableSecurity) {
            http.authorizeRequests()
                    .antMatchers("/**").permitAll()
                    .and()
                    .csrf().disable();
        } else {
            http.authorizeRequests()
                    .antMatchers("/**")
                    .authenticated()
                    .and()
                    .httpBasic();
        }
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
