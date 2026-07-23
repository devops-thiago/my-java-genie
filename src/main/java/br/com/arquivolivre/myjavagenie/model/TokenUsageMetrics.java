package br.com.arquivolivre.myjavagenie.model;

public class TokenUsageMetrics {
  private int promptTokens;
  private int completionTokens;
  private int totalTokens;

  public TokenUsageMetrics() {}

  public TokenUsageMetrics(int promptTokens, int completionTokens, int totalTokens) {
    this.promptTokens = promptTokens;
    this.completionTokens = completionTokens;
    this.totalTokens = totalTokens;
  }

  public int getPromptTokens() {
    return promptTokens;
  }

  public void setPromptTokens(int promptTokens) {
    this.promptTokens = promptTokens;
  }

  public int getCompletionTokens() {
    return completionTokens;
  }

  public void setCompletionTokens(int completionTokens) {
    this.completionTokens = completionTokens;
  }

  public int getTotalTokens() {
    return totalTokens;
  }

  public void setTotalTokens(int totalTokens) {
    this.totalTokens = totalTokens;
  }
}
