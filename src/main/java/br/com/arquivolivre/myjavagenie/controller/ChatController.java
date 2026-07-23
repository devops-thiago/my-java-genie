package br.com.arquivolivre.myjavagenie.controller;

import br.com.arquivolivre.myjavagenie.model.ChatMessage;
import br.com.arquivolivre.myjavagenie.model.ChatRequest;
import br.com.arquivolivre.myjavagenie.model.ChatResponse;
import br.com.arquivolivre.myjavagenie.service.ChatService;
import jakarta.validation.Valid;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
  private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

  private final ChatService chatService;

  public ChatController(ChatService chatService) {
    this.chatService = chatService;
  }

  @PostMapping("/query")
  public ResponseEntity<ChatResponse> query(@Valid @RequestBody ChatRequest request) {
    logger.info("Received chat query for session: {}", request.getSessionId());
    ChatResponse response =
        chatService.processMessage(
            request.getSessionId(), request.getMessage(), request.getWebSocketSessionId());
    return ResponseEntity.ok(response);
  }

  @GetMapping("/history")
  public ResponseEntity<List<ChatMessage>> getHistory(@RequestParam String sessionId) {
    List<ChatMessage> history = chatService.getHistory(sessionId);
    if (history.isEmpty() && !chatService.sessionExists(sessionId)) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(history);
  }

  @DeleteMapping("/history")
  public ResponseEntity<Void> clearHistory(@RequestParam String sessionId) {
    if (!chatService.clearHistory(sessionId)) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.noContent().build();
  }
}
