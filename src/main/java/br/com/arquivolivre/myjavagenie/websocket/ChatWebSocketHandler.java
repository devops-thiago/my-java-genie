package br.com.arquivolivre.myjavagenie.websocket;

import br.com.arquivolivre.myjavagenie.model.QueryStatus;
import br.com.arquivolivre.myjavagenie.util.LogSanitizer;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * WebSocket handler for real-time chat updates. Manages WebSocket connections and sends query
 * status updates to clients.
 *
 * <p>Clients may register with a stable id via {@code /ws/chat?sessionId=...} (used by the chat
 * UI). When omitted, the Spring WebSocket session id is used.
 */
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {
  private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketHandler.class);
  private static final String CLIENT_SESSION_ATTR = "clientSessionId";

  private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    String sessionId = resolveClientSessionId(session);
    session.getAttributes().put(CLIENT_SESSION_ATTR, sessionId);
    sessions.put(sessionId, session);
    logger.info("WebSocket connection established: {}", LogSanitizer.sanitize(sessionId));
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
    String sessionId = (String) session.getAttributes().get(CLIENT_SESSION_ATTR);
    if (sessionId == null) {
      sessionId = session.getId();
    }
    sessions.remove(sessionId);
    logger.info(
        "WebSocket connection closed: {} with status: {}",
        LogSanitizer.sanitize(sessionId),
        LogSanitizer.sanitize(status));
  }

  @Override
  protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    logger.debug(
        "Received WebSocket message from {}: {}",
        LogSanitizer.sanitize(session.getId()),
        LogSanitizer.sanitize(message.getPayload()));
    // Messages from client can be handled here if needed
  }

  /**
   * Sends a query status update to a specific WebSocket session.
   *
   * @param webSocketSessionId the WebSocket session ID
   * @param status the query status to send
   */
  public void sendStatusUpdate(String webSocketSessionId, QueryStatus status) {
    WebSocketSession session = sessions.get(webSocketSessionId);
    if (session != null && session.isOpen()) {
      try {
        String json = objectMapper.writeValueAsString(status);
        session.sendMessage(new TextMessage(json));
        logger.debug(
            "Sent status update to session {}: {}",
            LogSanitizer.sanitize(webSocketSessionId),
            LogSanitizer.sanitize(status.getStage()));
      } catch (IOException e) {
        logger.error(
            "Error sending status update to session {}",
            LogSanitizer.sanitize(webSocketSessionId),
            e);
      }
    } else {
      logger.warn(
          "WebSocket session not found or closed: {}", LogSanitizer.sanitize(webSocketSessionId));
    }
  }

  /**
   * Broadcasts a query status update to all connected sessions.
   *
   * @param status the query status to broadcast
   */
  public void broadcastStatusUpdate(QueryStatus status) {
    String json;
    try {
      json = objectMapper.writeValueAsString(status);
    } catch (IOException e) {
      logger.error("Error serializing status update", e);
      return;
    }

    sessions
        .values()
        .forEach(
            session -> {
              if (session.isOpen()) {
                try {
                  session.sendMessage(new TextMessage(json));
                } catch (IOException e) {
                  logger.error(
                      "Error broadcasting to session {}",
                      LogSanitizer.sanitize(session.getId()),
                      e);
                }
              }
            });

    logger.debug(
        "Broadcasted status update to {} sessions: {}",
        LogSanitizer.sanitize(sessions.size()),
        LogSanitizer.sanitize(status.getStage()));
  }

  /**
   * Gets the number of active WebSocket connections.
   *
   * @return the connection count
   */
  public int getConnectionCount() {
    return sessions.size();
  }

  private static String resolveClientSessionId(WebSocketSession session) {
    URI uri = session.getUri();
    if (uri != null) {
      String sessionId =
          UriComponentsBuilder.fromUri(uri).build().getQueryParams().getFirst("sessionId");
      if (sessionId != null && !sessionId.isBlank()) {
        return sessionId;
      }
    }
    return session.getId();
  }
}
