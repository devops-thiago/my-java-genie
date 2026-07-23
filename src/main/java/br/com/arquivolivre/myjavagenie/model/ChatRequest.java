package br.com.arquivolivre.myjavagenie.model;

import jakarta.validation.constraints.NotBlank;

/** Request model for chat interactions. */
public class ChatRequest {
  private String sessionId;

  @NotBlank(message = "Message cannot be blank")
  private String message;

  private String webSocketSessionId;

  public ChatRequest() {}

  public ChatRequest(String sessionId, String message) {
    this.sessionId = sessionId;
    this.message = message;
  }

  public ChatRequest(String sessionId, String message, String webSocketSessionId) {
    this.sessionId = sessionId;
    this.message = message;
    this.webSocketSessionId = webSocketSessionId;
  }

  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getWebSocketSessionId() {
    return webSocketSessionId;
  }

  public void setWebSocketSessionId(String webSocketSessionId) {
    this.webSocketSessionId = webSocketSessionId;
  }

  @Override
  public String toString() {
    return "ChatRequest{"
        + "sessionId='"
        + sessionId
        + '\''
        + ", message='"
        + message
        + '\''
        + ", webSocketSessionId='"
        + webSocketSessionId
        + '\''
        + '}';
  }
}
