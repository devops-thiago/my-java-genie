package br.com.arquivolivre.myjavagenie.config;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Immutable configuration properties for document ingestion settings.
 *
 * <p>Populated through Spring Boot constructor binding; construct directly with the canonical
 * constructor in tests. The {@code supportedFormats} list is defensively copied on both
 * construction and access so the configuration cannot be mutated through a shared reference.
 */
@ConfigurationProperties(prefix = "ingestion")
@Validated
public record IngestionConfig(
    @NotNull(message = "Chunk size must be specified")
        @Positive(message = "Chunk size must be positive")
        Integer chunkSize,
    @NotNull(message = "Chunk overlap must be specified")
        @PositiveOrZero(message = "Chunk overlap must be zero or positive")
        Integer chunkOverlap,
    @NotNull(message = "Batch size must be specified")
        @Positive(message = "Batch size must be positive")
        Integer batchSize,
    List<String> supportedFormats) {

  public IngestionConfig {
    supportedFormats = supportedFormats == null ? null : List.copyOf(supportedFormats);
  }

  @Override
  public List<String> supportedFormats() {
    return supportedFormats == null ? null : List.copyOf(supportedFormats);
  }
}
