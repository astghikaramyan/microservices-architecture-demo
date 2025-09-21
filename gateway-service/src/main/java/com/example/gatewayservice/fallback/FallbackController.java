package com.example.gatewayservice.fallback;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FallbackController {

  // Song service fallback
  @RequestMapping("/fallback/songs")
  public ResponseEntity<Map<String, String>> songServiceFallback() {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(Map.of(
            "error", "Song service is currently unavailable. Please try again later."
        ));
  }

  // Resource service fallback
  @RequestMapping("/fallback/resources")
  public ResponseEntity<Map<String, String>> resourceServiceFallback() {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(Map.of(
            "error", "Resource service is currently unavailable. Please try again later."
        ));
  }

  // Undefined route fallback
  @RequestMapping("/fallback")
  public ResponseEntity<Map<String, String>> fallback() {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(Map.of(
            "error", "Route not found",
            "message", "The requested endpoint does not exist."
        ));
  }
}
