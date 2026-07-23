package br.com.arquivolivre.myjavagenie.controller;

import br.com.arquivolivre.myjavagenie.model.IngestionResult;
import br.com.arquivolivre.myjavagenie.service.IngestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ingest")
public class IngestionController {

  private final IngestionService ingestionService;

  public IngestionController(IngestionService ingestionService) {
    this.ingestionService = ingestionService;
  }

  @PostMapping
  public ResponseEntity<IngestionResult> ingest(
      @RequestParam(defaultValue = "docs/specs/primitive-types-in-patterns-instanceof-switch-jls.html") String path) {
    return ResponseEntity.ok(ingestionService.ingest(path));
  }
}
