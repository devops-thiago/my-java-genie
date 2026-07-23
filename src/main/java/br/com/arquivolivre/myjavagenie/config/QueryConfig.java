package br.com.arquivolivre.myjavagenie.config;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "query")
@Validated
public class QueryConfig {

  @Positive private int maxRetrievedChunks = 5;

  private double similarityThreshold = 0.5;

  public int getMaxRetrievedChunks() {
    return maxRetrievedChunks;
  }

  public void setMaxRetrievedChunks(int maxRetrievedChunks) {
    this.maxRetrievedChunks = maxRetrievedChunks;
  }

  public double getSimilarityThreshold() {
    return similarityThreshold;
  }

  public void setSimilarityThreshold(double similarityThreshold) {
    this.similarityThreshold = similarityThreshold;
  }
}
