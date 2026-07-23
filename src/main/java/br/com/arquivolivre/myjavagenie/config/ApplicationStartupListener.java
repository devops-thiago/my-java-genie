package br.com.arquivolivre.myjavagenie.config;

import br.com.arquivolivre.myjavagenie.exception.ConfigurationException;
import br.com.arquivolivre.myjavagenie.exception.ModelInitializationException;
import br.com.arquivolivre.myjavagenie.exception.VectorDbConnectionException;
import br.com.arquivolivre.myjavagenie.repository.VectorRepository;
import br.com.arquivolivre.myjavagenie.repository.VectorRepositoryFactory;
import br.com.arquivolivre.myjavagenie.service.DefaultEmbeddingModelProvider;
import br.com.arquivolivre.myjavagenie.service.EmbeddingModelProvider;
import br.com.arquivolivre.myjavagenie.service.LanguageModelFactory;
import br.com.arquivolivre.myjavagenie.service.LanguageModelProvider;
import br.com.arquivolivre.myjavagenie.util.LogSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * Listener that runs on application startup to verify configuration and initialize components.
 * Ensures all critical components are properly configured and available before the application
 * starts serving requests.
 */
@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    name = "rag.startup-validation.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class ApplicationStartupListener implements ApplicationListener<ApplicationReadyEvent> {

  private static final Logger logger = LoggerFactory.getLogger(ApplicationStartupListener.class);

  private final ConfigurationProvider configurationProvider;
  private final LanguageModelFactory languageModelFactory;
  private final VectorRepositoryFactory vectorRepositoryFactory;

  public ApplicationStartupListener(
      ConfigurationProvider configurationProvider,
      LanguageModelFactory languageModelFactory,
      VectorRepositoryFactory vectorRepositoryFactory) {
    this.configurationProvider = configurationProvider;
    this.languageModelFactory = languageModelFactory;
    this.vectorRepositoryFactory = vectorRepositoryFactory;
  }

  @Override
  public void onApplicationEvent(ApplicationReadyEvent event) {
    logger.info("=== Starting Java RAG System Initialization ===");

    try {
      // Step 1: Verify configuration is valid
      verifyConfiguration();

      // Step 2: Initialize and verify Language Model Provider
      initializeLanguageModel();

      // Step 3: Initialize and verify Embedding Model Provider
      initializeEmbeddingModel();

      // Step 4: Initialize and verify Vector Repository
      initializeVectorRepository();

      // Step 5: Log startup summary
      logStartupSummary();

      logger.info("=== Java RAG System Initialization Complete ===");

    } catch (ConfigurationException e) {
      logger.error("Configuration validation failed: {}", LogSanitizer.sanitize(e.getMessage()));
      throw new IllegalStateException("Application startup failed due to invalid configuration", e);
    } catch (ModelInitializationException e) {
      logger.error(
          "Language model initialization failed: {}", LogSanitizer.sanitize(e.getMessage()));
      throw new IllegalStateException(
          "Application startup failed due to model initialization error", e);
    } catch (VectorDbConnectionException e) {
      logger.error("Vector database connection failed: {}", LogSanitizer.sanitize(e.getMessage()));
      throw new IllegalStateException(
          "Application startup failed due to vector database connection error", e);
    } catch (Exception e) {
      logger.error("Unexpected error during application initialization", e);
      throw new IllegalStateException("Application startup failed due to unexpected error", e);
    }
  }

  /** Verifies that all configuration is valid. */
  private void verifyConfiguration() {
    logger.info("Step 1: Verifying configuration...");

    try {
      configurationProvider.validateConfiguration();
      logger.info("✓ Configuration validation successful");
    } catch (ConfigurationException e) {
      logger.error("✗ Configuration validation failed: {}", LogSanitizer.sanitize(e.getMessage()));
      throw e;
    }
  }

  /** Initializes the language model provider and verifies connectivity. */
  private void initializeLanguageModel() {
    logger.info("Step 2: Initializing Language Model Provider...");

    try {
      ModelConfig modelConfig = configurationProvider.getModelConfig();
      LanguageModelProvider provider = languageModelFactory.createProvider(modelConfig);

      logger.info("Language Model Provider: {}", LogSanitizer.sanitize(provider.getProviderName()));

      // Verify connectivity
      if (provider.isAvailable()) {
        logger.info("✓ Language Model is available and ready");
      } else {
        throw new ModelInitializationException(
            "Language Model provider initialized but is not available. Check connectivity and configuration.");
      }

    } catch (ModelInitializationException e) {
      logger.error(
          "✗ Language Model initialization failed: {}", LogSanitizer.sanitize(e.getMessage()));
      throw e;
    } catch (Exception e) {
      logger.error("✗ Unexpected error during Language Model initialization", e);
      throw new ModelInitializationException("Failed to initialize Language Model", e);
    }
  }

  /** Initializes the embedding model provider. */
  private void initializeEmbeddingModel() {
    logger.info("Step 3: Initializing Embedding Model Provider...");

    try {
      EmbeddingModelProvider embeddingProvider = new DefaultEmbeddingModelProvider();
      logger.info(
          "Embedding Model Dimensions: {}",
          LogSanitizer.sanitize(embeddingProvider.getDimensions()));
      logger.info("✓ Embedding Model initialized successfully");

    } catch (Exception e) {
      logger.error(
          "✗ Embedding Model initialization failed: {}", LogSanitizer.sanitize(e.getMessage()));
      throw new ModelInitializationException("Failed to initialize Embedding Model", e);
    }
  }

  /**
   * Initializes the vector repository and verifies connectivity. Creates the collection if it
   * doesn't exist.
   */
  private void initializeVectorRepository() {
    logger.info("Step 4: Initializing Vector Repository...");

    try {
      VectorDbConfig vectorDbConfig = configurationProvider.getVectorDbConfig();
      VectorRepository repository = vectorRepositoryFactory.createRepository(vectorDbConfig);

      logger.info("Vector Database Type: {}", LogSanitizer.sanitize(vectorDbConfig.type()));
      logger.info("Collection Name: {}", LogSanitizer.sanitize(vectorDbConfig.collectionName()));

      // Check if collection exists, create if it doesn't
      String collectionName = vectorDbConfig.collectionName();
      if (!repository.collectionExists(collectionName)) {
        logger.info(
            "Collection '{}' does not exist, creating...", LogSanitizer.sanitize(collectionName));

        // Use embedding dimensions from the embedding model
        EmbeddingModelProvider embeddingProvider = new DefaultEmbeddingModelProvider();
        int dimensions = embeddingProvider.getDimensions();

        repository.createCollection(collectionName, dimensions);
        logger.info(
            "✓ Collection '{}' created successfully", LogSanitizer.sanitize(collectionName));
      } else {
        logger.info("✓ Collection '{}' already exists", LogSanitizer.sanitize(collectionName));
      }

      logger.info("✓ Vector Repository initialized and ready");

    } catch (VectorDbConnectionException e) {
      logger.error(
          "✗ Vector Repository initialization failed: {}", LogSanitizer.sanitize(e.getMessage()));
      throw e;
    } catch (Exception e) {
      logger.error("✗ Unexpected error during Vector Repository initialization", e);
      throw new VectorDbConnectionException("Failed to initialize Vector Repository", e);
    }
  }

  /** Logs a summary of the startup configuration. */
  private void logStartupSummary() {
    logger.info("=== Configuration Summary ===");

    ModelConfig modelConfig = configurationProvider.getModelConfig();
    logger.info("Model Provider: {}", LogSanitizer.sanitize(modelConfig.provider()));
    logger.info("Model Temperature: {}", LogSanitizer.sanitize(modelConfig.temperature()));
    logger.info("Model Max Tokens: {}", LogSanitizer.sanitize(modelConfig.maxTokens()));

    VectorDbConfig vectorDbConfig = configurationProvider.getVectorDbConfig();
    logger.info("Vector DB Type: {}", LogSanitizer.sanitize(vectorDbConfig.type()));
    logger.info("Vector DB URL: {}", LogSanitizer.sanitize(vectorDbConfig.connectionUrl()));
    logger.info("Collection Name: {}", LogSanitizer.sanitize(vectorDbConfig.collectionName()));

    IngestionConfig ingestionConfig = configurationProvider.getIngestionConfig();
    logger.info("Chunk Size: {}", LogSanitizer.sanitize(ingestionConfig.chunkSize()));
    logger.info("Chunk Overlap: {}", LogSanitizer.sanitize(ingestionConfig.chunkOverlap()));
    logger.info("Batch Size: {}", LogSanitizer.sanitize(ingestionConfig.batchSize()));

    QueryConfig queryConfig = configurationProvider.getQueryConfig();
    logger.info(
        "Max Retrieved Chunks: {}", LogSanitizer.sanitize(queryConfig.maxRetrievedChunks()));
    logger.info(
        "Similarity Threshold: {}", LogSanitizer.sanitize(queryConfig.similarityThreshold()));
    logger.info("Query Timeout: {} seconds", LogSanitizer.sanitize(queryConfig.timeoutSeconds()));
  }
}
