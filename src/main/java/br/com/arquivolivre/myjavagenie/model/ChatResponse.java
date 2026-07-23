package br.com.arquivolivre.myjavagenie.model;

import java.util.List;

/** Response model for chat interactions. Extends QueryResponse with chat-specific fields. */
public class ChatResponse {
  private String sessionId;
  private String answer;
  private List<SourceReference> sources;
  private TokenUsageMetrics tokenUsage;
  private long responseTimeMs;

  public ChatResponse() {}

  public ChatResponse(
      String sessionId,
      String answer,
      List<SourceReference> sources,
      TokenUsageMetrics tokenUsage,
      long responseTimeMs) {
    this.sessionId = sessionId;
    this.answer = answer;
    this.sources = sources;
    this.tokenUsage = tokenUsage;
    this.responseTimeMs = responseTimeMs;
  }

  public static ChatResponse fromQueryResponse(QueryResponse queryResponse) {
    return new ChatResponse(
        queryResponse.getSessionId(),
        queryResponse.getAnswer(),
        queryResponse.getSources(),
        queryResponse.getTokenUsage(),
        queryResponse.getResponseTimeMs());
  }

  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  public String getAnswer() {
    return answer;
  }

  public void setAnswer(String answer) {
    this.answer = answer;
  }

  public List<SourceReference> getSources() {
    return sources;
  }

  public void setSources(List<SourceReference> sources) {
    this.sources = sources;
  }

  public TokenUsageMetrics getTokenUsage() {
    return tokenUsage;
  }

  public void setTokenUsage(TokenUsageMetrics tokenUsage) {
    this.tokenUsage = tokenUsage;
  }

  public long getResponseTimeMs() {
    return responseTimeMs;
  }

  public void setResponseTimeMs(long responseTimeMs) {
    this.responseTimeMs = responseTimeMs;
  }

  @Override
  public String toString() {
    return "ChatResponse{"
        + "sessionId='"
        + sessionId
        + '\''
        + ", answer='"
        + answer
        + '\''
        + ", sources="
        + sources
        + ", tokenUsage="
        + tokenUsage
        + ", responseTimeMs="
        + responseTimeMs
        + '}';
  }
}
