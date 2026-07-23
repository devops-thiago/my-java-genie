package br.com.arquivolivre.myjavagenie.service;

import br.com.arquivolivre.myjavagenie.model.ChatSession;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class SessionManager {
  private static final Logger logger = LoggerFactory.getLogger(SessionManager.class);

  private final Map<String, ChatSession> sessions = new ConcurrentHashMap<>();

  @Value("${chat.session.timeout-seconds:1800}")
  private long sessionTimeoutSeconds;

  public ChatSession getOrCreateSession(String sessionId) {
    if (sessionId == null || sessionId.isBlank()) {
      ChatSession newSession = new ChatSession();
      sessions.put(newSession.getSessionId(), newSession);
      logger.info("Created new chat session: {}", newSession.getSessionId());
      return newSession;
    }

    return sessions.compute(
        sessionId,
        (id, existing) -> {
          if (existing == null) {
            logger.info("Created chat session with provided ID: {}", id);
            return new ChatSession(id);
          }
          existing.updateLastAccessedAt();
          return existing;
        });
  }

  public ChatSession getSession(String sessionId) {
    ChatSession session = sessions.get(sessionId);
    if (session != null) {
      session.updateLastAccessedAt();
    }
    return session;
  }

  @Scheduled(fixedRate = 300000)
  public void cleanupExpiredSessions() {
    int removed = 0;
    for (Map.Entry<String, ChatSession> entry : sessions.entrySet()) {
      if (entry.getValue().isExpired(sessionTimeoutSeconds)) {
        sessions.remove(entry.getKey());
        removed++;
      }
    }
    if (removed > 0) {
      logger.info("Cleaned up {} expired sessions. Active: {}", removed, sessions.size());
    }
  }
}
