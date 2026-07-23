package br.com.arquivolivre.myjavagenie.controller;

import br.com.arquivolivre.myjavagenie.model.ChatMessage;
import br.com.arquivolivre.myjavagenie.model.ChatRequest;
import br.com.arquivolivre.myjavagenie.model.ChatResponse;
import br.com.arquivolivre.myjavagenie.model.QueryResponse;
import br.com.arquivolivre.myjavagenie.service.ChatService;
import br.com.arquivolivre.myjavagenie.util.LogSanitizer;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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

  @SuppressFBWarnings(
      value = "EI_EXPOSE_REP2",
      justification =
          "chatService is a Spring-managed singleton injected by constructor, never mutated through"
              + " this field. SpotBugs reports it only because ChatService exposes boolean-returning"
              + " query methods (sessionExists/clearHistory) that match its collection-mutator"
              + " heuristic; this is a documented false positive for standard dependency injection.")
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
    logger.info(
        "Received chat query for session: {}", LogSanitizer.sanitize(request.getSessionId()));

    try {
      QueryResponse queryResponse =
          chatService.processMessage(
              request.getSessionId(), request.getMessage(), request.getWebSocketSessionId());

      ChatResponse response = ChatResponse.fromQueryResponse(queryResponse);
      logger.info(
          "Chat query processed successfully for session: {}",
          LogSanitizer.sanitize(response.getSessionId()));

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
    logger.info("Retrieving history for session: {}", LogSanitizer.sanitize(sessionId));

    List<ChatMessage> history = chatService.getHistory(sessionId);

    if (history.isEmpty() && !chatService.sessionExists(sessionId)) {
      logger.warn("Session not found: {}", LogSanitizer.sanitize(sessionId));
      return ResponseEntity.notFound().build();
    }

    logger.info(
        "Retrieved {} messages for session: {}",
        LogSanitizer.sanitize(history.size()),
        LogSanitizer.sanitize(sessionId));
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
    logger.info("Clearing history for session: {}", LogSanitizer.sanitize(sessionId));

    boolean cleared = chatService.clearHistory(sessionId);

    if (!cleared) {
      logger.warn("Session not found: {}", LogSanitizer.sanitize(sessionId));
      return ResponseEntity.notFound().build();
    }

    logger.info("History cleared for session: {}", LogSanitizer.sanitize(sessionId));
    return ResponseEntity.noContent().build();
  }
}
