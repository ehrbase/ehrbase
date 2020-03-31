package org.ehrbase.application.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Formatter;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private SecurityYAMLConfig securityYAMLConfig;

    private Logger logger = LoggerFactory.getLogger(getClass());
    private Formatter formatter = new Formatter();

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {

        auth.inMemoryAuthentication()
                .withUser(securityYAMLConfig.getAuthUser())
                .password(passwordEncoder().encode(securityYAMLConfig.getAuthPassword()))
                .authorities("ROLE_USER");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        switch (securityYAMLConfig.getAuthType()) {
            case BASIC: {
                logger.info("Using basic authentication.");
                logger.info(formatter.format(
                        "Username: %s Password: %s", securityYAMLConfig.getAuthUser(), securityYAMLConfig.getAuthPassword()
                ).toString());
                http
                        .csrf().disable()
                        .authorizeRequests().anyRequest().authenticated()
                        .and()
                        .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                        .and()
                        .httpBasic();
                break;
            }
            case NONE:
            default: {
                logger.warn("Authentication disabled!");
                logger.warn("To enable security set security.authType to BASIC in yaml properties file.");
                http
                        .csrf().disable()
                        .authorizeRequests().anyRequest().permitAll();
                break;
            }
        }
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
