package br.com.arquivolivre.myjavagenie.config;

import br.com.arquivolivre.myjavagenie.repository.VectorRepository;
import br.com.arquivolivre.myjavagenie.repository.VectorRepositoryFactory;
import br.com.arquivolivre.myjavagenie.service.*;
import io.opentelemetry.api.trace.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Main configuration class for the RAG System. Defines all major component beans with proper
 * dependency injection. Uses @ConditionalOnProperty for optional features.
 */
@Configuration
@EnableConfigurationProperties({
  ModelConfig.class,
  VectorDbConfig.class,
  IngestionConfig.class,
  QueryConfig.class
})
public class RagSystemConfiguration {

  private static final Logger logger = LoggerFactory.getLogger(RagSystemConfiguration.class);

  /**
   * Creates the LanguageModelFactory bean. This factory is responsible for creating language model
   * providers based on configuration.
   */
  @Bean
  public LanguageModelFactory languageModelFactory() {
    logger.info("Initializing LanguageModelFactory bean");
    return new DefaultLanguageModelFactory();
  }

  /**
   * Creates the LanguageModelProvider bean. This is the actual language model provider instance
   * used throughout the application.
   *
   * @param languageModelFactory the factory to create the provider
   * @param modelConfig the model configuration
   * @return configured LanguageModelProvider
   */
  @Bean
  public LanguageModelProvider languageModelProvider(
      LanguageModelFactory languageModelFactory, ModelConfig modelConfig) {
    logger.info(
        "Initializing LanguageModelProvider bean for provider: {}", modelConfig.getProvider());
    return languageModelFactory.createProvider(modelConfig);
  }

  /**
   * Creates the EmbeddingModelProvider bean. This provider generates embeddings for text chunks and
   * queries.
   */
  @Bean
  public EmbeddingModelProvider embeddingModelProvider() {
    logger.info("Initializing EmbeddingModelProvider bean");
    return new DefaultEmbeddingModelProvider();
  }

  /**
   * Creates the VectorRepositoryFactory bean. This factory creates vector repository instances
   * based on configuration.
   */
  @Bean
  public VectorRepositoryFactory vectorRepositoryFactory() {
    logger.info("Initializing VectorRepositoryFactory bean");
    return new VectorRepositoryFactory();
  }

  /**
   * Creates the VectorRepository bean. This is the actual vector database repository instance used
   * throughout the application.
   *
   * @param vectorRepositoryFactory the factory to create the repository
   * @param vectorDbConfig the vector database configuration
   * @return configured VectorRepository
   */
  @Bean
  public VectorRepository vectorRepository(
      VectorRepositoryFactory vectorRepositoryFactory, VectorDbConfig vectorDbConfig) {
    logger.info("Initializing VectorRepository bean for type: {}", vectorDbConfig.getType());
    return vectorRepositoryFactory.createRepository(vectorDbConfig);
  }

  /**
   * Creates the DocumentProcessor bean. This processor chunks documents into smaller pieces for
   * embedding and retrieval.
   *
   * @param ingestionConfig the ingestion configuration
   * @return configured DocumentProcessor
   */
  @Bean
  public DocumentProcessor documentProcessor(IngestionConfig ingestionConfig) {
    logger.info(
        "Initializing DocumentProcessor bean with chunk size: {}, overlap: {}",
        ingestionConfig.getChunkSize(),
        ingestionConfig.getChunkOverlap());
    return new RecursiveCharacterSplitter(ingestionConfig);
  }

  /** Creates the DocumentLoader bean. This loader reads documents from the filesystem. */
  @Bean
  public DocumentLoader documentLoader() {
    logger.info("Initializing DocumentLoader bean");
    return new DocumentLoader();
  }

  /**
   * Creates the RetrievalEngine bean. This engine retrieves relevant document chunks based on query
   * similarity.
   *
   * @param vectorRepository the vector repository for similarity search
   * @param embeddingModelProvider the embedding model for query embedding
   * @param queryConfig the query configuration
   * @return configured RetrievalEngine
   */
  @Bean
  public RetrievalEngine retrievalEngine(
      VectorRepository vectorRepository,
      EmbeddingModelProvider embeddingModelProvider,
      QueryConfig queryConfig,
      @Autowired(required = false) Tracer tracer) {
    logger.info(
        "Initializing RetrievalEngine bean with max chunks: {}, threshold: {}",
        queryConfig.getMaxRetrievedChunks(),
        queryConfig.getSimilarityThreshold());
    return new RetrievalEngine(vectorRepository, embeddingModelProvider, queryConfig, tracer);
  }

  /**
   * Creates the PromptBuilder bean. This builder constructs prompts for the language model with
   * retrieved context.
   */
  @Bean
  public PromptBuilder promptBuilder() {
    logger.info("Initializing PromptBuilder bean");
    return new PromptBuilder();
  }

  /**
   * Creates the TokenUsageTracker bean. This tracker monitors and logs token consumption for cost
   * analysis.
   */
  @Bean
  public TokenUsageTracker tokenUsageTracker() {
    logger.info("Initializing TokenUsageTracker bean");
    return new TokenUsageTracker();
  }

  /**
   * Creates the ConfigurationProvider bean. This provider wraps all configuration properties and
   * provides validation.
   *
   * @param modelConfig the model configuration
   * @param vectorDbConfig the vector database configuration
   * @param ingestionConfig the ingestion configuration
   * @param queryConfig the query configuration
   * @return configured ConfigurationProvider
   */
  @Bean
  public ConfigurationProvider configurationProvider(
      ModelConfig modelConfig,
      VectorDbConfig vectorDbConfig,
      IngestionConfig ingestionConfig,
      QueryConfig queryConfig) {
    logger.info("Initializing ConfigurationProvider bean");
    return new SpringConfigurationProvider(
        modelConfig, vectorDbConfig, ingestionConfig, queryConfig);
  }

  /**
   * Optional bean for development mode features. Only created when 'rag.dev-mode.enabled' property
   * is set to true.
   */
  @Bean
  @ConditionalOnProperty(
      name = "rag.dev-mode.enabled",
      havingValue = "true",
      matchIfMissing = false)
  public DevModeConfiguration devModeConfiguration() {
    logger.info("Development mode enabled - initializing DevModeConfiguration bean");
    return new DevModeConfiguration();
  }

  /**
   * Inner class for development mode specific configuration. This can include features like verbose
   * logging, test data generation, etc.
   */
  public static class DevModeConfiguration {
    private static final Logger devLogger = LoggerFactory.getLogger(DevModeConfiguration.class);

    public DevModeConfiguration() {
      devLogger.warn("=== DEVELOPMENT MODE ACTIVE ===");
      devLogger.warn("This mode should NOT be used in production");
      devLogger.warn("Additional logging and debugging features are enabled");
    }
  }
}
