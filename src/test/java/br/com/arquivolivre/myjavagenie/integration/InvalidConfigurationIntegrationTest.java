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
    ModelConfig config = new ModelConfig(" ", null, null, null, null, 0.7, 100);

    Set<ConstraintViolation<ModelConfig>> violations = validator.validate(config);
    assertThat(violations).isNotEmpty();
  }

  /** Test Requirement 7.5: Missing required configuration */
  @Test
  void testMissingRequiredConfiguration() {
    ModelConfig config = new ModelConfig(null, null, null, null, null, 0.7, 100);

    Set<ConstraintViolation<ModelConfig>> violations = validator.validate(config);
    assertThat(violations).isNotEmpty();
  }

  /** Test Requirement 7.5: Invalid numeric configuration values */
  @Test
  void testInvalidNumericConfiguration() {
    QueryConfig queryConfig = new QueryConfig(-1, 5.0, 10, null, null);

    Set<ConstraintViolation<QueryConfig>> violations = validator.validate(queryConfig);
    assertThat(violations).isNotEmpty();
  }
}
