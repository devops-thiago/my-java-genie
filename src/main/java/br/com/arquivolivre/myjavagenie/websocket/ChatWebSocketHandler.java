package br.com.arquivolivre.myjavagenie.websocket;

import br.com.arquivolivre.myjavagenie.model.QueryStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket handler for real-time chat updates.
 * Manages WebSocket connections and sends query status updates to clients.
 */
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        sessions.put(sessionId, session);
        logger.info("WebSocket connection established: {}", sessionId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        sessions.remove(sessionId);
        logger.info("WebSocket connection closed: {} with status: {}", sessionId, status);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        logger.debug("Received WebSocket message from {}: {}", session.getId(), message.getPayload());
        // Messages from client can be handled here if needed
    }

    /**
     * Sends a query status update to a specific WebSocket session.
     *
     * @param webSocketSessionId the WebSocket session ID
     * @param status             the query status to send
     */
    public void sendStatusUpdate(String webSocketSessionId, QueryStatus status) {
        WebSocketSession session = sessions.get(webSocketSessionId);
        if (session != null && session.isOpen()) {
            try {
                String json = objectMapper.writeValueAsString(status);
                session.sendMessage(new TextMessage(json));
                logger.debug("Sent status update to session {}: {}", webSocketSessionId, status.getStage());
            } catch (IOException e) {
                logger.error("Error sending status update to session {}", webSocketSessionId, e);
            }
        } else {
            logger.warn("WebSocket session not found or closed: {}", webSocketSessionId);
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

        sessions.values().forEach(session -> {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(json));
                } catch (IOException e) {
                    logger.error("Error broadcasting to session {}", session.getId(), e);
                }
            }
        });

        logger.debug("Broadcasted status update to {} sessions: {}", sessions.size(), status.getStage());
    }

    /**
     * Gets the number of active WebSocket connections.
     *
     * @return the connection count
     */
    public int getConnectionCount() {
        return sessions.size();
    }
}
