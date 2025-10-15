package com.example.storageservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
  private static final String HS256_SECRET = "3nK1U1Rkj7v5yN5dxq5FZJx2yB3hG9K0mFqk5N8Vh8c=";
  private static final String JWKS_URI = "http://host.docker.internal:9000/.well-known/jwks.json";


  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationConverter jwtConverter) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth
            // endpoint patterns and HTTP method security
            .requestMatchers("/actuator/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/storages/**").hasAnyRole("USER", "ADMIN")
            .requestMatchers(HttpMethod.POST, "/storages/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.DELETE, "/storages/**").hasRole("ADMIN")
            .anyRequest().authenticated()
        )
    .oauth2ResourceServer(oauth2 -> oauth2
        .jwt(jwt -> jwt
            .decoder(delegatingJwtDecoder())
            .jwtAuthenticationConverter(jwtConverter)
        )
    );

    return http.build();
  }

  /**
   * DelegatingJwtDecoder tries multiple decoders in order until one succeeds
   */
  @Bean
  public JwtDecoder delegatingJwtDecoder() {
    List<JwtDecoder> decoders = new ArrayList<>();

    // 1️⃣ HS256 decoder (shared secret)
    NimbusJwtDecoder hsDecoder = NimbusJwtDecoder
        .withSecretKey(new javax.crypto.spec.SecretKeySpec(HS256_SECRET.getBytes(), "HmacSHA256"))
        .build();
    decoders.add(hsDecoder);

    // 2️⃣ RS256 decoder (JWKS from auth server)
    NimbusJwtDecoder rsDecoder = NimbusJwtDecoder.withJwkSetUri(JWKS_URI).build();
    decoders.add(rsDecoder);

    return new MultiJwtDecoder(decoders);
  }

  @Bean
  public JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(new RolesClaimConverter());
    return converter;
  }

  static class RolesClaimConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
      Object rolesObj = jwt.getClaims().get("roles");
      if (rolesObj instanceof Collection) {
        @SuppressWarnings("unchecked")
        Collection<String> roles = (Collection<String>) rolesObj;
        return roles.stream()
            .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList());
      }
      return Collections.emptyList();
    }
  }
}
