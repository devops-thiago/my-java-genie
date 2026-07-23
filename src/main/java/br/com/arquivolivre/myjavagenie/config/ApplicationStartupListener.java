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
      logger.error("Configuration validation failed: {}", e.getMessage());
      throw new IllegalStateException("Application startup failed due to invalid configuration", e);
    } catch (ModelInitializationException e) {
      logger.error("Language model initialization failed: {}", e.getMessage());
      throw new IllegalStateException(
          "Application startup failed due to model initialization error", e);
    } catch (VectorDbConnectionException e) {
      logger.error("Vector database connection failed: {}", e.getMessage());
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
      logger.error("✗ Configuration validation failed: {}", e.getMessage());
      throw e;
    }
  }

  /** Initializes the language model provider and verifies connectivity. */
  private void initializeLanguageModel() {
    logger.info("Step 2: Initializing Language Model Provider...");

    try {
      ModelConfig modelConfig = configurationProvider.getModelConfig();
      LanguageModelProvider provider = languageModelFactory.createProvider(modelConfig);

      logger.info("Language Model Provider: {}", provider.getProviderName());

      // Verify connectivity
      if (provider.isAvailable()) {
        logger.info("✓ Language Model is available and ready");
      } else {
        throw new ModelInitializationException(
            "Language Model provider initialized but is not available. Check connectivity and configuration.");
      }

    } catch (ModelInitializationException e) {
      logger.error("✗ Language Model initialization failed: {}", e.getMessage());
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
      logger.info("Embedding Model Dimensions: {}", embeddingProvider.getDimensions());
      logger.info("✓ Embedding Model initialized successfully");

    } catch (Exception e) {
      logger.error("✗ Embedding Model initialization failed: {}", e.getMessage());
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

      logger.info("Vector Database Type: {}", vectorDbConfig.getType());
      logger.info("Collection Name: {}", vectorDbConfig.getCollectionName());

      // Check if collection exists, create if it doesn't
      String collectionName = vectorDbConfig.getCollectionName();
      if (!repository.collectionExists(collectionName)) {
        logger.info("Collection '{}' does not exist, creating...", collectionName);

        // Use embedding dimensions from the embedding model
        EmbeddingModelProvider embeddingProvider = new DefaultEmbeddingModelProvider();
        int dimensions = embeddingProvider.getDimensions();

        repository.createCollection(collectionName, dimensions);
        logger.info("✓ Collection '{}' created successfully", collectionName);
      } else {
        logger.info("✓ Collection '{}' already exists", collectionName);
      }

      logger.info("✓ Vector Repository initialized and ready");

    } catch (VectorDbConnectionException e) {
      logger.error("✗ Vector Repository initialization failed: {}", e.getMessage());
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
    logger.info("Model Provider: {}", modelConfig.getProvider());
    logger.info("Model Temperature: {}", modelConfig.getTemperature());
    logger.info("Model Max Tokens: {}", modelConfig.getMaxTokens());

    VectorDbConfig vectorDbConfig = configurationProvider.getVectorDbConfig();
    logger.info("Vector DB Type: {}", vectorDbConfig.getType());
    logger.info("Vector DB URL: {}", vectorDbConfig.getConnectionUrl());
    logger.info("Collection Name: {}", vectorDbConfig.getCollectionName());

    IngestionConfig ingestionConfig = configurationProvider.getIngestionConfig();
    logger.info("Chunk Size: {}", ingestionConfig.getChunkSize());
    logger.info("Chunk Overlap: {}", ingestionConfig.getChunkOverlap());
    logger.info("Batch Size: {}", ingestionConfig.getBatchSize());

    QueryConfig queryConfig = configurationProvider.getQueryConfig();
    logger.info("Max Retrieved Chunks: {}", queryConfig.getMaxRetrievedChunks());
    logger.info("Similarity Threshold: {}", queryConfig.getSimilarityThreshold());
    logger.info("Query Timeout: {} seconds", queryConfig.getTimeoutSeconds());
  }
}
