package br.com.arquivolivre.myjavagenie.service;

import br.com.arquivolivre.myjavagenie.model.*;
import br.com.arquivolivre.myjavagenie.websocket.ChatWebSocketHandler;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for handling chat interactions. Manages conversation flow and integrates with
 * QueryService.
 */
@Service
public class ChatService {
  private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

  private final QueryService queryService;
  private final SessionManager sessionManager;

  @Autowired(required = false)
  private ChatWebSocketHandler webSocketHandler;

  public ChatService(QueryService queryService, SessionManager sessionManager) {
    this.queryService = queryService;
    this.sessionManager = sessionManager;
  }

  /**
   * Processes a user message and generates a response.
   *
   * @param sessionId the session ID (null to create new session)
   * @param message the user's message
   * @return the query response with session information
   */
  public QueryResponse processMessage(String sessionId, String message) {
    return processMessage(sessionId, message, null);
  }

  /**
   * Processes a user message and generates a response with WebSocket status updates.
   *
   * @param sessionId the session ID (null to create new session)
   * @param message the user's message
   * @param webSocketSessionId the WebSocket session ID for status updates (optional)
   * @return the query response with session information
   */
  public QueryResponse processMessage(String sessionId, String message, String webSocketSessionId) {
    logger.info("Processing chat message for session: {}", sessionId);

    // Get or create session
    ChatSession session = sessionManager.getOrCreateSession(sessionId);

    // Add user message to session
    ChatMessage userMessage = new ChatMessage(ChatMessage.MessageRole.USER, message);
    session.addMessage(userMessage);
    logger.debug("Added user message to session {}: {}", session.getSessionId(), message);

    // Send embedding status
    sendStatusUpdate(
        webSocketSessionId,
        session.getSessionId(),
        QueryStatus.ProcessingStage.EMBEDDING,
        "Generating query embedding");

    // Send searching status
    sendStatusUpdate(
        webSocketSessionId,
        session.getSessionId(),
        QueryStatus.ProcessingStage.SEARCHING,
        "Searching for relevant documents");

    // Send generating status
    sendStatusUpdate(
        webSocketSessionId,
        session.getSessionId(),
        QueryStatus.ProcessingStage.GENERATING,
        "Generating response");

    // Process query
    QueryResponse response = queryService.processQuery(message);

    // Add assistant response to session
    ChatMessage assistantMessage =
        new ChatMessage(
            ChatMessage.MessageRole.ASSISTANT, response.getAnswer(), response.getSources());
    session.addMessage(assistantMessage);
    logger.debug("Added assistant response to session {}", session.getSessionId());

    // Update response with session ID
    QueryResponse finalResponse =
        new QueryResponse(
            response.getAnswer(),
            response.getSources(),
            response.getTokenUsage(),
            response.getResponseTimeMs(),
            session.getSessionId());

    // Send completion status
    if (webSocketSessionId != null && webSocketHandler != null) {
      ChatResponse chatResponse = ChatResponse.fromQueryResponse(finalResponse);
      QueryStatus completionStatus = new QueryStatus(session.getSessionId(), chatResponse);
      webSocketHandler.sendStatusUpdate(webSocketSessionId, completionStatus);
    }

    return finalResponse;
  }

  /** Sends a status update via WebSocket if available. */
  private void sendStatusUpdate(
      String webSocketSessionId,
      String chatSessionId,
      QueryStatus.ProcessingStage stage,
      String message) {
    if (webSocketSessionId != null && webSocketHandler != null) {
      QueryStatus status = new QueryStatus(chatSessionId, stage, message);
      webSocketHandler.sendStatusUpdate(webSocketSessionId, status);
    }
  }

  /**
   * Retrieves the conversation history for a session.
   *
   * @param sessionId the session ID
   * @return the list of messages, or empty list if session not found
   */
  public List<ChatMessage> getHistory(String sessionId) {
    logger.debug("Retrieving history for session: {}", sessionId);

    ChatSession session = sessionManager.getSession(sessionId);
    if (session == null) {
      logger.warn("Session not found: {}", sessionId);
      return List.of();
    }

    return session.getMessages();
  }

  /**
   * Clears the conversation history for a session.
   *
   * @param sessionId the session ID
   * @return true if session was found and cleared, false otherwise
   */
  public boolean clearHistory(String sessionId) {
    logger.info("Clearing history for session: {}", sessionId);

    ChatSession session = sessionManager.getSession(sessionId);
    if (session == null) {
      logger.warn("Session not found: {}", sessionId);
      return false;
    }

    session.clearMessages();
    logger.info("Cleared history for session: {}", sessionId);
    return true;
  }

  /**
   * Checks if a session exists.
   *
   * @param sessionId the session ID
   * @return true if session exists, false otherwise
   */
  public boolean sessionExists(String sessionId) {
    return sessionManager.getSession(sessionId) != null;
  }
}
