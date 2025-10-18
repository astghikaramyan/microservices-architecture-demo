package com.example.storageservice.config;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.util.List;

/**
 * Tries multiple JwtDecoders in order until one succeeds
 */
public class MultiJwtDecoder implements JwtDecoder {

  private final List<JwtDecoder> decoders;

  public MultiJwtDecoder(List<JwtDecoder> decoders) {
    this.decoders = decoders;
  }

  @Override
  public Jwt decode(String token) throws JwtException {
    JwtException lastException = null;
    for (JwtDecoder decoder : decoders) {
      try {
        return decoder.decode(token);
      } catch (JwtException e) {
        lastException = e; // remember last exception
      }
    }
    // if none worked, throw the last exception
    throw lastException != null ? lastException : new JwtException("No decoders configured");
  }
}
