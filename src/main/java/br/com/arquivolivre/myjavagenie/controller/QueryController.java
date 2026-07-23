package br.com.arquivolivre.myjavagenie.controller;

import br.com.arquivolivre.myjavagenie.model.QueryResponse;
import br.com.arquivolivre.myjavagenie.service.QueryService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/query")
public class QueryController {

  private final QueryService queryService;

  public QueryController(QueryService queryService) {
    this.queryService = queryService;
  }

  @PostMapping
  public ResponseEntity<QueryResponse> query(@RequestBody Map<String, String> body) {
    String question = body.get("question");
    if (question == null || question.isBlank()) {
      throw new IllegalArgumentException("question is required");
    }
    return ResponseEntity.ok(queryService.query(question));
  }
}
