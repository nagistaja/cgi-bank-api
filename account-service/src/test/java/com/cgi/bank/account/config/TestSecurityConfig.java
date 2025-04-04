package com.cgi.bank.account.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Test security configuration that disables security for integration tests.
 * This configuration permits all requests and disables CSRF protection.
 */
@Configuration
@EnableWebSecurity
@Profile("test")
@Order(1) // Higher priority than default config (which is Order(100) implicitly)
public class TestSecurityConfig {

    /**
     * Security filter chain for test environments.
     * Disables security checks to simplify testing.
     * Uses a specific securityMatcher for test-only endpoints to prevent conflict
     * with the main SecurityConfig.
     */
    @Bean
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/**")
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .httpBasic(Customizer.withDefaults());
            
        return http.build();
    }
} 