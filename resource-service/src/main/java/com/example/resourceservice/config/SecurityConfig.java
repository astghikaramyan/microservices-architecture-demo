package com.example.resourceservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        // Allow gateway requests to resource endpoints
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/resources/**").permitAll()
            .requestMatchers("/actuator/**").permitAll()
            .anyRequest().authenticated()
        )
        // Disable form login (no 302 redirects)
        .formLogin(form -> form.disable())
        // Disable CSRF for non-browser clients
        .csrf(csrf -> csrf.disable())
        // Enable OAuth2 client for outgoing requests
        .oauth2Client(oauth2 -> {});

    return http.build();
  }
}
