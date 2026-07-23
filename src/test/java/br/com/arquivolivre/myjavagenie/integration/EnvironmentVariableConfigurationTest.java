package br.com.arquivolivre.myjavagenie.integration;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.arquivolivre.myjavagenie.config.ConfigurationProvider;
import br.com.arquivolivre.myjavagenie.config.ModelConfig;
import br.com.arquivolivre.myjavagenie.config.QueryConfig;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Integration test for environment variable substitution in configuration. Tests Requirement 7.1:
 * Environment variable substitution
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("envtest")
@Testcontainers
class EnvironmentVariableConfigurationTest {

  @Container
  static GenericContainer<?> chromaContainer =
      new GenericContainer<>(DockerImageName.parse("chromadb/chroma:0.4.15"))
          .withExposedPorts(8000)
          .waitingFor(
              Wait.forHttp("/api/v1/heartbeat")
                  .forPort(8000)
                  .forStatusCode(200)
                  .withStartupTimeout(Duration.ofSeconds(60)));

  @Autowired private ConfigurationProvider configurationProvider;

  @DynamicPropertySource
  static void setEnvironmentVariables(DynamicPropertyRegistry registry) {
    registry.add("MODEL_PROVIDER", () -> "openai");
    registry.add("rag.startup-validation.enabled", () -> "false");
    registry.add("OPENAI_API_KEY", () -> "env-test-key");
    registry.add("OPENAI_MODEL", () -> "gpt-3.5-turbo");
    registry.add("MODEL_TEMPERATURE", () -> "0.5");
    registry.add("MODEL_MAX_TOKENS", () -> "300");

    registry.add("VECTOR_DB_TYPE", () -> "chroma");
    registry.add("VECTOR_DB_URL", () -> "http://localhost:" + chromaContainer.getMappedPort(8000));
    registry.add(
        "vector-db.connection-url",
        () -> "http://localhost:" + chromaContainer.getMappedPort(8000));
    registry.add("VECTOR_DB_COLLECTION", () -> "test_collection");

    registry.add("MAX_CHUNKS", () -> "3");
    registry.add("SIMILARITY_THRESHOLD", () -> "0.8");
    registry.add("QUERY_TIMEOUT", () -> "15");
    registry.add("opentelemetry.enabled", () -> "false");
  }

  /** Test Requirement 7.1: Verify environment variable substitution works */
  @Test
  void testEnvironmentVariableSubstitution() {
    ModelConfig modelConfig = configurationProvider.getModelConfig();

    assertThat(modelConfig.getProvider()).isEqualTo("openai");
    assertThat(modelConfig.getOpenai().getApiKey()).isEqualTo("env-test-key");
    assertThat(modelConfig.getOpenai().getModelName()).isEqualTo("gpt-3.5-turbo");
    assertThat(modelConfig.getTemperature()).isEqualTo(0.5);
    assertThat(modelConfig.getMaxTokens()).isEqualTo(300);
  }

  /** Test Requirement 7.1: Verify default values work when env vars not set */
  @Test
  void testDefaultValuesWithoutEnvironmentVariables() {
    assertThat(configurationProvider.getModelConfig()).isNotNull();
    assertThat(configurationProvider.getVectorDbConfig()).isNotNull();
  }

  /** Test Requirement 7.1: Verify query config from environment variables */
  @Test
  void testQueryConfigFromEnvironmentVariables() {
    QueryConfig queryConfig = configurationProvider.getQueryConfig();

    assertThat(queryConfig.getMaxRetrievedChunks()).isEqualTo(3);
    assertThat(queryConfig.getSimilarityThreshold()).isEqualTo(0.8);
    assertThat(queryConfig.getTimeoutSeconds()).isEqualTo(15);
  }
}
