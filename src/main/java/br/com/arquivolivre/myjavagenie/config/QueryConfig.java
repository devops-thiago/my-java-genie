package br.com.arquivolivre.myjavagenie.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Configuration properties for query processing settings. */
@ConfigurationProperties(prefix = "query")
@Validated
public class QueryConfig {

  @NotNull(message = "Max retrieved chunks must be specified")
  @Positive(message = "Max retrieved chunks must be positive")
  private Integer maxRetrievedChunks;

  @NotNull(message = "Similarity threshold must be specified")
  @DecimalMin(value = "0.0", message = "Similarity threshold must be at least 0.0")
  @DecimalMax(value = "1.0", message = "Similarity threshold must be at most 1.0")
  private Double similarityThreshold;

  @NotNull(message = "Timeout seconds must be specified")
  @Positive(message = "Timeout seconds must be positive")
  private Integer timeoutSeconds;

  private Boolean enableCache;

  private Integer cacheTtlMinutes;

  // Getters and Setters

  public Integer getMaxRetrievedChunks() {
    return maxRetrievedChunks;
  }

  public void setMaxRetrievedChunks(Integer maxRetrievedChunks) {
    this.maxRetrievedChunks = maxRetrievedChunks;
  }

  public Double getSimilarityThreshold() {
    return similarityThreshold;
  }

  public void setSimilarityThreshold(Double similarityThreshold) {
    this.similarityThreshold = similarityThreshold;
  }

  public Integer getTimeoutSeconds() {
    return timeoutSeconds;
  }

  public void setTimeoutSeconds(Integer timeoutSeconds) {
    this.timeoutSeconds = timeoutSeconds;
  }

  public Boolean getEnableCache() {
    return enableCache;
  }

  public void setEnableCache(Boolean enableCache) {
    this.enableCache = enableCache;
  }

  public Integer getCacheTtlMinutes() {
    return cacheTtlMinutes;
  }

  public void setCacheTtlMinutes(Integer cacheTtlMinutes) {
    this.cacheTtlMinutes = cacheTtlMinutes;
  }
}
