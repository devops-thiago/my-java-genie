package br.com.arquivolivre.myjavagenie.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ChatSession {
  private final String sessionId;
  private final List<ChatMessage> messages;
  private final Instant createdAt;
  private Instant lastAccessedAt;

  public ChatSession() {
    this(UUID.randomUUID().toString());
  }

  public ChatSession(String sessionId) {
    this.sessionId = sessionId;
    this.messages = new ArrayList<>();
    this.createdAt = Instant.now();
    this.lastAccessedAt = Instant.now();
  }

  public String getSessionId() {
    return sessionId;
  }

  public List<ChatMessage> getMessages() {
    return new ArrayList<>(messages);
  }

  public void addMessage(ChatMessage message) {
    this.messages.add(message);
    this.lastAccessedAt = Instant.now();
  }

  public void clearMessages() {
    this.messages.clear();
    this.lastAccessedAt = Instant.now();
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getLastAccessedAt() {
    return lastAccessedAt;
  }

  public void updateLastAccessedAt() {
    this.lastAccessedAt = Instant.now();
  }

  public boolean isExpired(long timeoutSeconds) {
    return Instant.now().isAfter(lastAccessedAt.plusSeconds(timeoutSeconds));
  }
}
