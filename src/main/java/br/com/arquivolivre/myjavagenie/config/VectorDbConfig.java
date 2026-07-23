package br.com.arquivolivre.myjavagenie.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "vector-db")
@Validated
public class VectorDbConfig {

  @NotBlank private String connectionUrl;

  @NotBlank private String collectionName;

  public String getConnectionUrl() {
    return connectionUrl;
  }

  public void setConnectionUrl(String connectionUrl) {
    this.connectionUrl = connectionUrl;
  }

  public String getCollectionName() {
    return collectionName;
  }

  public void setCollectionName(String collectionName) {
    this.collectionName = collectionName;
  }
}
