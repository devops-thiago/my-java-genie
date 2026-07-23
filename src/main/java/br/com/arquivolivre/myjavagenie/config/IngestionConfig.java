package br.com.arquivolivre.myjavagenie.config;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "ingestion")
@Validated
public class IngestionConfig {

  @Positive private int chunkSize = 1000;

  @Positive private int chunkOverlap = 200;

  public int getChunkSize() {
    return chunkSize;
  }

  public void setChunkSize(int chunkSize) {
    this.chunkSize = chunkSize;
  }

  public int getChunkOverlap() {
    return chunkOverlap;
  }

  public void setChunkOverlap(int chunkOverlap) {
    this.chunkOverlap = chunkOverlap;
  }
}
