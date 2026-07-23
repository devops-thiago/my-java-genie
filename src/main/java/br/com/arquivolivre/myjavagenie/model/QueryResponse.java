package br.com.arquivolivre.myjavagenie.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Response model for user queries. Contains the generated answer, source references, token usage
 * metrics, and response time.
 */
public class QueryResponse {
  private String answer;
  private List<SourceReference> sources;
  private TokenUsageMetrics tokenUsage;
  private long responseTimeMs;
  private String sessionId;

  public QueryResponse() {
    this.sources = new ArrayList<>();
  }

  public QueryResponse(
      String answer,
      List<SourceReference> sources,
      TokenUsageMetrics tokenUsage,
      long responseTimeMs) {
    this.answer = answer;
    this.sources = sources != null ? new ArrayList<>(sources) : new ArrayList<>();
    this.tokenUsage = TokenUsageMetrics.copyOf(tokenUsage);
    this.responseTimeMs = responseTimeMs;
  }

  public QueryResponse(
      String answer,
      List<SourceReference> sources,
      TokenUsageMetrics tokenUsage,
      long responseTimeMs,
      String sessionId) {
    this.answer = answer;
    this.sources = sources != null ? new ArrayList<>(sources) : new ArrayList<>();
    this.tokenUsage = TokenUsageMetrics.copyOf(tokenUsage);
    this.responseTimeMs = responseTimeMs;
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

  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    QueryResponse that = (QueryResponse) o;
    return responseTimeMs == that.responseTimeMs
        && Objects.equals(answer, that.answer)
        && Objects.equals(sources, that.sources)
        && Objects.equals(tokenUsage, that.tokenUsage)
        && Objects.equals(sessionId, that.sessionId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(answer, sources, tokenUsage, responseTimeMs, sessionId);
  }

  @Override
  public String toString() {
    return "QueryResponse{"
        + "answer='"
        + answer
        + '\''
        + ", sources="
        + sources
        + ", tokenUsage="
        + tokenUsage
        + ", responseTimeMs="
        + responseTimeMs
        + ", sessionId='"
        + sessionId
        + '\''
        + '}';
  }
}
