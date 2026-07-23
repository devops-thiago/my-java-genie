package br.com.arquivolivre.myjavagenie.websocket;

import br.com.arquivolivre.myjavagenie.model.QueryStatus;
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

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {
  private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketHandler.class);

  private final Map<String, WebSocketSession> sessionsByWsId = new ConcurrentHashMap<>();
  private final Map<String, WebSocketSession> sessionsByChatId = new ConcurrentHashMap<>();
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public void afterConnectionEstablished(WebSocketSession session) {
    sessionsByWsId.put(session.getId(), session);
    String chatSessionId = extractChatSessionId(session);
    if (chatSessionId != null && !chatSessionId.isBlank()) {
      sessionsByChatId.put(chatSessionId, session);
      logger.info("WebSocket connected wsId={} chatSessionId={}", session.getId(), chatSessionId);
    } else {
      logger.info("WebSocket connected wsId={}", session.getId());
    }
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
    sessionsByWsId.remove(session.getId());
    sessionsByChatId.entrySet().removeIf(entry -> entry.getValue().getId().equals(session.getId()));
    logger.info("WebSocket closed: {} status={}", session.getId(), status);
  }

  @Override
  protected void handleTextMessage(WebSocketSession session, TextMessage message) {
    logger.debug("Received WebSocket message from {}: {}", session.getId(), message.getPayload());
  }

  public void sendStatusUpdate(String webSocketSessionId, QueryStatus status) {
    if (webSocketSessionId == null || webSocketSessionId.isBlank()) {
      return;
    }
    WebSocketSession session = sessionsByWsId.get(webSocketSessionId);
    if (session == null) {
      session = sessionsByChatId.get(webSocketSessionId);
    }
    send(session, webSocketSessionId, status);
  }

  public void sendStatusUpdateForChatSession(String chatSessionId, QueryStatus status) {
    if (chatSessionId == null || chatSessionId.isBlank()) {
      return;
    }
    send(sessionsByChatId.get(chatSessionId), chatSessionId, status);
  }

  private void send(WebSocketSession session, String key, QueryStatus status) {
    if (session == null || !session.isOpen()) {
      logger.debug("No open WebSocket session for key {}", key);
      return;
    }
    try {
      session.sendMessage(new TextMessage(objectMapper.writeValueAsString(status)));
      logger.debug("Sent status {} to {}", status.getStatus(), key);
    } catch (IOException e) {
      logger.error("Error sending status update to {}", key, e);
    }
  }

  private String extractChatSessionId(WebSocketSession session) {
    URI uri = session.getUri();
    if (uri == null) {
      return null;
    }
    return UriComponentsBuilder.fromUri(uri).build().getQueryParams().getFirst("sessionId");
  }
}
