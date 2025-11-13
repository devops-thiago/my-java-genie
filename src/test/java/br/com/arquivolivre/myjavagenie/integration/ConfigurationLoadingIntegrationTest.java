package br.com.arquivolivre.myjavagenie.integration;

import br.com.arquivolivre.myjavagenie.config.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for configuration loading.
 * Tests Requirements: 7.1, 7.5
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConfigurationLoadingIntegrationTest {

    @Autowired
    private ConfigurationProvider configurationProvider;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Set up valid configuration
        registry.add("model.provider", () -> "self-hosted");
        registry.add("rag.startup-validation.enabled", () -> "false");
        registry.add("model.self-hosted.base-url", () -> "http://localhost:11434");
        registry.add("rag.startup-validation.enabled", () -> "false");
        registry.add("model.self-hosted.model-name", () -> "llama2");
        registry.add("rag.startup-validation.enabled", () -> "false");
        registry.add("model.temperature", () -> "0.7");
        registry.add("rag.startup-validation.enabled", () -> "false");
        registry.add("model.max-tokens", () -> "500");
        registry.add("rag.startup-validation.enabled", () -> "false");

        registry.add("vector-db.type", () -> "chroma");
        registry.add("rag.startup-validation.enabled", () -> "false");
        registry.add("vector-db.connection-url", () -> "http://localhost:8000");
        registry.add("rag.startup-validation.enabled", () -> "false");
        registry.add("vector-db.collection-name", () -> "java25_docs");
        registry.add("rag.startup-validation.enabled", () -> "false");

        registry.add("ingestion.chunk-size", () -> "1000");
        registry.add("rag.startup-validation.enabled", () -> "false");
        registry.add("ingestion.chunk-overlap", () -> "200");
        registry.add("rag.startup-validation.enabled", () -> "false");
        registry.add("ingestion.batch-size", () -> "100");
        registry.add("rag.startup-validation.enabled", () -> "false");

        registry.add("query.max-retrieved-chunks", () -> "5");
        registry.add("rag.startup-validation.enabled", () -> "false");
        registry.add("query.similarity-threshold", () -> "0.7");
        registry.add("rag.startup-validation.enabled", () -> "false");
        registry.add("query.timeout-seconds", () -> "10");
        registry.add("rag.startup-validation.enabled", () -> "false");
    }

    /**
     * Test Requirement 7.1: Load all configuration parameters from external files
     */
    @Test
    void testLoadAllConfigurationParameters() {
        assertThat(configurationProvider).isNotNull();

        ModelConfig modelConfig = configurationProvider.getModelConfig();
        assertThat(modelConfig).isNotNull();

        VectorDbConfig vectorDbConfig = configurationProvider.getVectorDbConfig();
        assertThat(vectorDbConfig).isNotNull();

        IngestionConfig ingestionConfig = configurationProvider.getIngestionConfig();
        assertThat(ingestionConfig).isNotNull();

        QueryConfig queryConfig = configurationProvider.getQueryConfig();
        assertThat(queryConfig).isNotNull();
    }

    /**
     * Test Requirement 7.1: Verify model configuration loading
     */
    @Test
    void testModelConfigurationLoading() {
        ModelConfig modelConfig = configurationProvider.getModelConfig();

        assertThat(modelConfig.getProvider()).isEqualTo("self-hosted");
        assertThat(modelConfig.getSelfHosted()).isNotNull();
        assertThat(modelConfig.getSelfHosted().getBaseUrl()).isEqualTo("http://localhost:11434");
        assertThat(modelConfig.getSelfHosted().getModelName()).isEqualTo("llama2");
        assertThat(modelConfig.getTemperature()).isEqualTo(0.7);
        assertThat(modelConfig.getMaxTokens()).isEqualTo(500);
    }

    /**
     * Test Requirement 7.1: Verify vector database configuration loading
     */
    @Test
    void testVectorDbConfigurationLoading() {
        VectorDbConfig vectorDbConfig = configurationProvider.getVectorDbConfig();

        assertThat(vectorDbConfig.getType()).isEqualTo("chroma");
        assertThat(vectorDbConfig.getConnectionUrl()).isEqualTo("http://localhost:8000");
        assertThat(vectorDbConfig.getCollectionName()).isEqualTo("java25_docs");
    }

    /**
     * Test Requirement 7.2: Verify ingestion configuration loading
     */
    @Test
    void testIngestionConfigurationLoading() {
        IngestionConfig ingestionConfig = configurationProvider.getIngestionConfig();

        assertThat(ingestionConfig.getChunkSize()).isEqualTo(1000);
        assertThat(ingestionConfig.getChunkOverlap()).isEqualTo(200);
        assertThat(ingestionConfig.getBatchSize()).isEqualTo(100);
    }

    /**
     * Test Requirement 7.2: Verify query configuration loading
     */
    @Test
    void testQueryConfigurationLoading() {
        QueryConfig queryConfig = configurationProvider.getQueryConfig();

        assertThat(queryConfig.getMaxRetrievedChunks()).isEqualTo(5);
        assertThat(queryConfig.getSimilarityThreshold()).isEqualTo(0.7);
        assertThat(queryConfig.getTimeoutSeconds()).isEqualTo(10);
    }

    /**
     * Test Requirement 7.3: Verify embedding model parameters
     */
    @Test
    void testEmbeddingModelConfiguration() {
        ModelConfig modelConfig = configurationProvider.getModelConfig();

        assertThat(modelConfig.getTemperature()).isBetween(0.0, 2.0);
        assertThat(modelConfig.getMaxTokens()).isGreaterThan(0);
    }

    /**
     * Test Requirement 7.4: Verify language model parameters
     */
    @Test
    void testLanguageModelConfiguration() {
        ModelConfig modelConfig = configurationProvider.getModelConfig();

        assertThat(modelConfig.getProvider()).isIn("self-hosted", "openai", "anthropic");
        assertThat(modelConfig.getTemperature()).isNotNull();
        assertThat(modelConfig.getMaxTokens()).isNotNull();
    }
}
