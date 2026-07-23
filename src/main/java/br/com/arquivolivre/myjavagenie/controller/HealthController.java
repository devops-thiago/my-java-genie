package br.com.arquivolivre.myjavagenie.controller;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

  @GetMapping("/health")
  public ResponseEntity<Map<String, String>> health() {
    return ResponseEntity.ok(Map.of("status", "UP"));
  }
}
