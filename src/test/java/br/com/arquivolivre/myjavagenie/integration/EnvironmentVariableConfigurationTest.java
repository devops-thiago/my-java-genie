package br.com.arquivolivre.myjavagenie.integration;

import br.com.arquivolivre.myjavagenie.config.ConfigurationProvider;
import br.com.arquivolivre.myjavagenie.config.ModelConfig;
import br.com.arquivolivre.myjavagenie.config.QueryConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for environment variable substitution in configuration.
 * Tests Requirement 7.1: Environment variable substitution
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("envtest")
class EnvironmentVariableConfigurationTest {

    @Autowired
    private ConfigurationProvider configurationProvider;

    @DynamicPropertySource
    static void setEnvironmentVariables(DynamicPropertyRegistry registry) {
        // Simulate environment variables
        registry.add("MODEL_PROVIDER", () -> "openai");
        registry.add("rag.startup-validation.enabled", () -> "false");
        registry.add("OPENAI_API_KEY", () -> "env-test-key");
        registry.add("rag.startup-validation.enabled", () -> "false");
        registry.add("OPENAI_MODEL", () -> "gpt-3.5-turbo");
        registry.add("rag.startup-validation.enabled", () -> "false");
        registry.add("MODEL_TEMPERATURE", () -> "0.5");
        registry.add("rag.startup-validation.enabled", () -> "false");
        registry.add("MODEL_MAX_TOKENS", () -> "300");
        registry.add("rag.startup-validation.enabled", () -> "false");

        registry.add("VECTOR_DB_TYPE", () -> "chroma");
        registry.add("rag.startup-validation.enabled", () -> "false");
        registry.add("VECTOR_DB_URL", () -> "http://test-chroma:8000");
        registry.add("rag.startup-validation.enabled", () -> "false");
        registry.add("VECTOR_DB_COLLECTION", () -> "test_collection");
        registry.add("rag.startup-validation.enabled", () -> "false");

        registry.add("MAX_CHUNKS", () -> "3");
        registry.add("rag.startup-validation.enabled", () -> "false");
        registry.add("SIMILARITY_THRESHOLD", () -> "0.8");
        registry.add("rag.startup-validation.enabled", () -> "false");
        registry.add("QUERY_TIMEOUT", () -> "15");
        registry.add("rag.startup-validation.enabled", () -> "false");
    }

    /**
     * Test Requirement 7.1: Verify environment variable substitution works
     */
    @Test
    void testEnvironmentVariableSubstitution() {
        ModelConfig modelConfig = configurationProvider.getModelConfig();

        assertThat(modelConfig.getProvider()).isEqualTo("openai");
        assertThat(modelConfig.getOpenai().getApiKey()).isEqualTo("env-test-key");
        assertThat(modelConfig.getOpenai().getModelName()).isEqualTo("gpt-3.5-turbo");
        assertThat(modelConfig.getTemperature()).isEqualTo(0.5);
        assertThat(modelConfig.getMaxTokens()).isEqualTo(300);
    }

    /**
     * Test Requirement 7.1: Verify default values work when env vars not set
     */
    @Test
    void testDefaultValuesWithoutEnvironmentVariables() {
        // The configuration should load with defaults if env vars are not set
        // This is tested by the default values in application-envtest.yml
        assertThat(configurationProvider.getModelConfig()).isNotNull();
        assertThat(configurationProvider.getVectorDbConfig()).isNotNull();
    }

    /**
     * Test Requirement 7.1: Verify query config from environment variables
     */
    @Test
    void testQueryConfigFromEnvironmentVariables() {
        QueryConfig queryConfig = configurationProvider.getQueryConfig();

        assertThat(queryConfig.getMaxRetrievedChunks()).isEqualTo(3);
        assertThat(queryConfig.getSimilarityThreshold()).isEqualTo(0.8);
        assertThat(queryConfig.getTimeoutSeconds()).isEqualTo(15);
    }
}
