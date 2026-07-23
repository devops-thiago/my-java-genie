package br.com.arquivolivre.myjavagenie.model;

import java.util.ArrayList;
import java.util.List;

public class QueryResponse {
  private String answer;
  private List<SourceReference> sources = new ArrayList<>();
  private String searchQuery;
  private long responseTimeMs;

  public QueryResponse() {}

  public QueryResponse(
      String answer, List<SourceReference> sources, String searchQuery, long responseTimeMs) {
    this.answer = answer;
    this.sources = sources != null ? sources : new ArrayList<>();
    this.searchQuery = searchQuery;
    this.responseTimeMs = responseTimeMs;
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

  public String getSearchQuery() {
    return searchQuery;
  }

  public void setSearchQuery(String searchQuery) {
    this.searchQuery = searchQuery;
  }

  public long getResponseTimeMs() {
    return responseTimeMs;
  }

  public void setResponseTimeMs(long responseTimeMs) {
    this.responseTimeMs = responseTimeMs;
  }
}
