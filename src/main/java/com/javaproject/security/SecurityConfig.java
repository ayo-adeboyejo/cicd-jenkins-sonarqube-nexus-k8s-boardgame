package com.javaproject.security;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Spring Security configuration.
 * Defines authentication, authorisation rules, and security beans.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger logger =
            LoggerFactory.getLogger(SecurityConfig.class);

    private static final String ROLE_USER    = "USER";
    private static final String ROLE_MANAGER = "MANAGER";
    private static final String LOGIN_PAGE   = "/login";
    private static final String SUCCESS_URL  = "/secured";

    private final DataSource dataSource;
    private final LoggingAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(DataSource dataSource,
            LoggingAccessDeniedHandler accessDeniedHandler) {
        this.dataSource = dataSource;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    /**
     * BCrypt password encoder bean.
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * JDBC-backed user details management bean.
     */
    @Bean
    public JdbcUserDetailsManager jdbcUserDetailsManager() {
        JdbcUserDetailsManager manager = new JdbcUserDetailsManager();
        manager.setDataSource(dataSource);
        return manager;
    }

    /**
     * Security filter chain.
     * CSRF is enabled for all paths except H2 console and REST API endpoints
     * which do not support CSRF tokens.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(new AntPathRequestMatcher("/user/**"))
                    .hasAnyRole(ROLE_USER, ROLE_MANAGER)
                .requestMatchers(new AntPathRequestMatcher("/secured/**"))
                    .hasAnyRole(ROLE_USER, ROLE_MANAGER)
                .requestMatchers(new AntPathRequestMatcher("/manager/**"))
                    .hasRole(ROLE_MANAGER)
                .requestMatchers(new AntPathRequestMatcher("/h2-console/**"))
                    .permitAll()
                .requestMatchers(new AntPathRequestMatcher("/boardgames/**"))
                    .permitAll()
                .anyRequest()
                    .permitAll()
            )
            .formLogin(form -> form
                .loginPage(LOGIN_PAGE)
                .defaultSuccessUrl(SUCCESS_URL)
            )
            .logout(logout -> logout
                .invalidateHttpSession(true)
                .clearAuthentication(true)
            )
            .exceptionHandling(ex -> ex
                .accessDeniedHandler(accessDeniedHandler)
            )
            .csrf(csrf -> csrf
                // Disable CSRF only for paths that do not support it:
                // H2 console does not send CSRF tokens
                // REST API endpoints are called by non-browser clients
                .ignoringRequestMatchers(
                    new AntPathRequestMatcher("/h2-console/**"),
                    new AntPathRequestMatcher("/boardgames/**")
                )
            )
            .headers(headers -> headers
                .frameOptions(frame -> frame.disable())
            );

        return http.build();
    }
}