package br.com.arquivolivre.myjavagenie.model;

import java.util.ArrayList;
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
    this.sources = sources == null ? null : new ArrayList<>(sources);
    this.tokenUsage = TokenUsageMetrics.copyOf(tokenUsage);
    this.responseTimeMs = responseTimeMs;
  }

  /** Copy constructor used for defensive copies. */
  public ChatResponse(ChatResponse other) {
    this(other.sessionId, other.answer, other.sources, other.tokenUsage, other.responseTimeMs);
  }

  /** Returns a defensive copy of the given response, or {@code null} if it is null. */
  public static ChatResponse copyOf(ChatResponse response) {
    return response == null ? null : new ChatResponse(response);
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
    return sources == null ? null : new ArrayList<>(sources);
  }

  public void setSources(List<SourceReference> sources) {
    this.sources = sources == null ? null : new ArrayList<>(sources);
  }

  public TokenUsageMetrics getTokenUsage() {
    return TokenUsageMetrics.copyOf(tokenUsage);
  }

  public void setTokenUsage(TokenUsageMetrics tokenUsage) {
    this.tokenUsage = TokenUsageMetrics.copyOf(tokenUsage);
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
