package br.com.arquivolivre.myjavagenie.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Immutable configuration properties for vector database settings. Supports ChromaDB, pgvector, and
 * Qdrant.
 *
 * <p>Populated through Spring Boot constructor binding; construct directly with the canonical
 * constructor in tests.
 */
@ConfigurationProperties(prefix = "vector-db")
@Validated
public record VectorDbConfig(
    @NotBlank(message = "Vector database type must be specified") String type,
    @NotBlank(message = "Connection URL must be specified") String connectionUrl,
    @NotBlank(message = "Collection name must be specified") String collectionName,
    @Valid ChromaSettings chroma,
    @Valid PgVectorSettings pgvector,
    @Valid QdrantSettings qdrant) {

  /** Configuration for ChromaDB. */
  public record ChromaSettings(String tenant, String database) {}

  /** Configuration for PostgreSQL with pgvector extension. */
  public record PgVectorSettings(
      @NotBlank(message = "PostgreSQL host must be specified") String host,
      @Positive(message = "PostgreSQL port must be positive") Integer port,
      @NotBlank(message = "PostgreSQL database must be specified") String database,
      @NotBlank(message = "PostgreSQL username must be specified") String username,
      String password,
      String schema,
      @NotBlank(message = "PostgreSQL table name must be specified") String tableName) {}

  /** Configuration for Qdrant vector database. */
  public record QdrantSettings(String apiKey, Boolean useTls) {}
}
