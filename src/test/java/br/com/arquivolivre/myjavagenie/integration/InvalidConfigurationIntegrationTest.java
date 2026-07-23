package br.com.arquivolivre.myjavagenie.integration;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.arquivolivre.myjavagenie.config.ModelConfig;
import br.com.arquivolivre.myjavagenie.config.QueryConfig;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Validation checks for invalid configuration values. Tests Requirement 7.5 without mutating
 * JVM-wide system properties (which would poison later SpringBoot tests).
 */
class InvalidConfigurationIntegrationTest {

  private Validator validator;

  @BeforeEach
  void setUp() {
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  /** Test Requirement 7.5: blank provider is rejected */
  @Test
  void testInvalidModelProviderConfiguration() {
    ModelConfig config = new ModelConfig();
    config.setProvider(" ");
    config.setTemperature(0.7);
    config.setMaxTokens(100);

    Set<ConstraintViolation<ModelConfig>> violations = validator.validate(config);
    assertThat(violations).isNotEmpty();
  }

  /** Test Requirement 7.5: Missing required configuration */
  @Test
  void testMissingRequiredConfiguration() {
    ModelConfig config = new ModelConfig();
    config.setTemperature(0.7);
    config.setMaxTokens(100);

    Set<ConstraintViolation<ModelConfig>> violations = validator.validate(config);
    assertThat(violations).isNotEmpty();
  }

  /** Test Requirement 7.5: Invalid numeric configuration values */
  @Test
  void testInvalidNumericConfiguration() {
    QueryConfig queryConfig = new QueryConfig();
    queryConfig.setMaxRetrievedChunks(-1);
    queryConfig.setSimilarityThreshold(5.0);
    queryConfig.setTimeoutSeconds(10);

    Set<ConstraintViolation<QueryConfig>> violations = validator.validate(queryConfig);
    assertThat(violations).isNotEmpty();
  }
}
