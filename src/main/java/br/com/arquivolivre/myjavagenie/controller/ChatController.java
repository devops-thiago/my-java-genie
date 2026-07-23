package br.com.arquivolivre.myjavagenie.controller;

import br.com.arquivolivre.myjavagenie.model.ChatMessage;
import br.com.arquivolivre.myjavagenie.model.ChatRequest;
import br.com.arquivolivre.myjavagenie.model.ChatResponse;
import br.com.arquivolivre.myjavagenie.model.QueryResponse;
import br.com.arquivolivre.myjavagenie.service.ChatService;
import jakarta.validation.Valid;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for chat interactions. Provides endpoints for querying, retrieving history, and
 * clearing sessions.
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {
  private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

  private final ChatService chatService;

  public ChatController(ChatService chatService) {
    this.chatService = chatService;
  }

  /**
   * Processes a chat query.
   *
   * @param request the chat request containing sessionId and message
   * @return the chat response with answer and sources
   */
  @PostMapping("/query")
  public ResponseEntity<ChatResponse> query(@Valid @RequestBody ChatRequest request) {
    logger.info("Received chat query for session: {}", request.getSessionId());

    try {
      QueryResponse queryResponse =
          chatService.processMessage(
              request.getSessionId(), request.getMessage(), request.getWebSocketSessionId());

      ChatResponse response = ChatResponse.fromQueryResponse(queryResponse);
      logger.info("Chat query processed successfully for session: {}", response.getSessionId());

      return ResponseEntity.ok(response);
    } catch (Exception e) {
      logger.error("Error processing chat query", e);
      throw e;
    }
  }

  /**
   * Retrieves conversation history for a session.
   *
   * @param sessionId the session ID
   * @return the list of messages in the conversation
   */
  @GetMapping("/history")
  public ResponseEntity<List<ChatMessage>> getHistory(@RequestParam String sessionId) {
    logger.info("Retrieving history for session: {}", sessionId);

    List<ChatMessage> history = chatService.getHistory(sessionId);

    if (history.isEmpty() && !chatService.sessionExists(sessionId)) {
      logger.warn("Session not found: {}", sessionId);
      return ResponseEntity.notFound().build();
    }

    logger.info("Retrieved {} messages for session: {}", history.size(), sessionId);
    return ResponseEntity.ok(history);
  }

  /**
   * Clears conversation history for a session.
   *
   * @param sessionId the session ID
   * @return 204 No Content if successful, 404 Not Found if session doesn't exist
   */
  @DeleteMapping("/history")
  public ResponseEntity<Void> clearHistory(@RequestParam String sessionId) {
    logger.info("Clearing history for session: {}", sessionId);

    boolean cleared = chatService.clearHistory(sessionId);

    if (!cleared) {
      logger.warn("Session not found: {}", sessionId);
      return ResponseEntity.notFound().build();
    }

    logger.info("History cleared for session: {}", sessionId);
    return ResponseEntity.noContent().build();
  }
}
