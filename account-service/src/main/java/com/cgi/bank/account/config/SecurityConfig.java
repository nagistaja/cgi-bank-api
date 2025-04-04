package com.cgi.bank.account.config;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for the application.
 * Configures basic security settings like CSRF and endpoint access.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Configures security settings for the application.
     * - Disables CSRF for REST API
     * - Configures stateless session management
     * - Allows open access to API endpoints for now
     * - Secures actuator endpoints
     *
     * @param http the HttpSecurity to configure
     * @return the configured SecurityFilterChain
     * @throws Exception if an error occurs during configuration
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // Disable CSRF for REST API
                .csrf(AbstractHttpConfigurer::disable)
                // Configure stateless session management - for horizontal scaling
                .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Permit all just for testing purposes 
                        .requestMatchers("/api/v1/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/webjars/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/actuator/**").authenticated()
                        .anyRequest().authenticated())
                .formLogin(AbstractHttpConfigurer::disable)
                .build();
    }
} 