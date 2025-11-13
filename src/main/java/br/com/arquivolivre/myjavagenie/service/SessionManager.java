package br.com.arquivolivre.myjavagenie.service;

import br.com.arquivolivre.myjavagenie.model.ChatSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing chat sessions in memory.
 * Handles session creation, retrieval, and expiration.
 */
@Service
public class SessionManager {
    private static final Logger logger = LoggerFactory.getLogger(SessionManager.class);

    private final Map<String, ChatSession> sessions = new ConcurrentHashMap<>();

    @Value("${chat.session.timeout-seconds:1800}")
    private long sessionTimeoutSeconds;

    /**
     * Gets an existing session or creates a new one.
     *
     * @param sessionId the session ID, or null to create a new session
     * @return the chat session
     */
    public ChatSession getOrCreateSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            ChatSession newSession = new ChatSession();
            sessions.put(newSession.getSessionId(), newSession);
            logger.info("Created new chat session: {}", newSession.getSessionId());
            return newSession;
        }

        ChatSession session = sessions.get(sessionId);
        if (session == null) {
            session = new ChatSession(sessionId);
            sessions.put(sessionId, session);
            logger.info("Created chat session with provided ID: {}", sessionId);
        } else {
            session.updateLastAccessedAt();
            logger.debug("Retrieved existing chat session: {}", sessionId);
        }

        return session;
    }

    /**
     * Gets an existing session by ID.
     *
     * @param sessionId the session ID
     * @return the chat session, or null if not found
     */
    public ChatSession getSession(String sessionId) {
        ChatSession session = sessions.get(sessionId);
        if (session != null) {
            session.updateLastAccessedAt();
        }
        return session;
    }

    /**
     * Removes a session.
     *
     * @param sessionId the session ID to remove
     */
    public void removeSession(String sessionId) {
        ChatSession removed = sessions.remove(sessionId);
        if (removed != null) {
            logger.info("Removed chat session: {}", sessionId);
        }
    }

    /**
     * Clears all sessions.
     */
    public void clearAllSessions() {
        int count = sessions.size();
        sessions.clear();
        logger.info("Cleared all {} chat sessions", count);
    }

    /**
     * Gets the number of active sessions.
     *
     * @return the session count
     */
    public int getSessionCount() {
        return sessions.size();
    }

    /**
     * Scheduled task to clean up expired sessions.
     * Runs every 5 minutes.
     */
    @Scheduled(fixedRate = 300000)
    public void cleanupExpiredSessions() {
        logger.debug("Running session cleanup task");

        int removedCount = 0;
        for (Map.Entry<String, ChatSession> entry : sessions.entrySet()) {
            if (entry.getValue().isExpired(sessionTimeoutSeconds)) {
                sessions.remove(entry.getKey());
                removedCount++;
                logger.info("Removed expired session: {}", entry.getKey());
            }
        }

        if (removedCount > 0) {
            logger.info("Cleaned up {} expired sessions. Active sessions: {}",
                    removedCount, sessions.size());
        }
    }
}
