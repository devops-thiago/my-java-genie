package br.com.arquivolivre.myjavagenie.service;

import br.com.arquivolivre.myjavagenie.model.ChatSession;

/**
 * Read-oriented abstraction over chat-session lookup that conversation flows depend on.
 * Deliberately exposes only the session-access operations its consumers need (Interface
 * Segregation); session removal and bulk clearing remain on the concrete {@link SessionManager}
 * implementation.
 */
public interface SessionRegistry {

  /**
   * Gets an existing session or creates a new one.
   *
   * @param sessionId the session ID, or {@code null}/blank to create a new session
   * @return the chat session
   */
  ChatSession getOrCreateSession(String sessionId);

  /**
   * Gets an existing session by ID.
   *
   * @param sessionId the session ID
   * @return the chat session, or {@code null} if not found
   */
  ChatSession getSession(String sessionId);
}
