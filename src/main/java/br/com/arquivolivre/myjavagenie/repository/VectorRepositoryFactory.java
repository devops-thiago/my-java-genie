package br.com.arquivolivre.myjavagenie.repository;

import br.com.arquivolivre.myjavagenie.config.VectorDbConfig;
import br.com.arquivolivre.myjavagenie.exception.InvalidConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Factory for creating VectorRepository instances based on configuration. Supports ChromaDB
 * initially, with extensibility for pgvector and Qdrant.
 */
@Component
public class VectorRepositoryFactory {

  private static final Logger logger = LoggerFactory.getLogger(VectorRepositoryFactory.class);

  /**
   * Creates a VectorRepository instance based on the provided configuration.
   *
   * @param config the vector database configuration
   * @return a VectorRepository implementation
   * @throws InvalidConfigurationException if the database type is not supported or configuration is
   *     invalid
   */
  public VectorRepository createRepository(VectorDbConfig config) {
    if (config == null) {
      throw new InvalidConfigurationException("VectorDbConfig cannot be null");
    }

    String dbType = config.getType();
    if (dbType == null || dbType.trim().isEmpty()) {
      throw new InvalidConfigurationException("Vector database type must be specified");
    }

    logger.info("Creating vector repository for type: {}", dbType);

    switch (dbType.toLowerCase()) {
      case "chroma":
      case "chromadb":
        return createChromaRepository(config);

      case "pgvector":
      case "postgres":
        throw new InvalidConfigurationException(
            "pgvector support is not yet implemented. Currently supported: chroma");

      case "qdrant":
        throw new InvalidConfigurationException(
            "Qdrant support is not yet implemented. Currently supported: chroma");

      default:
        throw new InvalidConfigurationException(
            String.format("Unsupported vector database type: %s. Supported types: chroma", dbType));
    }
  }

  /** Creates a ChromaDB repository instance. */
  private VectorRepository createChromaRepository(VectorDbConfig config) {
    validateConnectionUrl(config);
    validateCollectionName(config);

    logger.info("Initializing ChromaDB repository at: {}", config.getConnectionUrl());
    return new ChromaVectorRepository(config);
  }

  /** Validates that the connection URL is properly configured. */
  private void validateConnectionUrl(VectorDbConfig config) {
    String url = config.getConnectionUrl();
    if (url == null || url.trim().isEmpty()) {
      throw new InvalidConfigurationException("Vector database connection URL must be specified");
    }
  }

  /** Validates that the collection name is properly configured. */
  private void validateCollectionName(VectorDbConfig config) {
    String collectionName = config.getCollectionName();
    if (collectionName == null || collectionName.trim().isEmpty()) {
      throw new InvalidConfigurationException("Vector database collection name must be specified");
    }
  }
}
