package br.com.arquivolivre.myjavagenie.controller;

import br.com.arquivolivre.myjavagenie.config.VectorDbConfig;
import br.com.arquivolivre.myjavagenie.repository.VectorRepository;
import br.com.arquivolivre.myjavagenie.service.LanguageModelProvider;
import br.com.arquivolivre.myjavagenie.util.LogSanitizer;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for health check endpoints. Provides system health status including availability
 * of language model and vector database.
 */
@RestController
@RequestMapping("/api")
public class HealthController {
  private static final Logger logger = LoggerFactory.getLogger(HealthController.class);

  private final LanguageModelProvider languageModel;
  private final VectorRepository vectorRepository;
  private final VectorDbConfig vectorDbConfig;

  public HealthController(
      LanguageModelProvider languageModel,
      VectorRepository vectorRepository,
      VectorDbConfig vectorDbConfig) {
    this.languageModel = languageModel;
    this.vectorRepository = vectorRepository;
    this.vectorDbConfig = vectorDbConfig;
  }

  /**
   * Health check endpoint that verifies the availability of system components.
   *
   * @return ResponseEntity containing health status and component details
   */
  @GetMapping("/health")
  public ResponseEntity<HealthResponse> health() {
    logger.debug("Health check requested");

    HealthResponse response = new HealthResponse();
    boolean allHealthy = true;

    // Check Language Model availability
    ComponentHealth languageModelHealth = checkLanguageModel();
    response.addComponent("languageModel", languageModelHealth);
    if (!languageModelHealth.isHealthy()) {
      allHealthy = false;
    }

    // Check Vector Database availability
    ComponentHealth vectorDbHealth = checkVectorDatabase();
    response.addComponent("vectorDatabase", vectorDbHealth);
    if (!vectorDbHealth.isHealthy()) {
      allHealthy = false;
    }

    // Set overall status
    response.setStatus(allHealthy ? "UP" : "DOWN");

    // Return appropriate HTTP status
    HttpStatus httpStatus = allHealthy ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;

    logger.info(
        "Health check completed: status={}, languageModel={}, vectorDb={}",
        LogSanitizer.sanitize(response.getStatus()),
        LogSanitizer.sanitize(languageModelHealth.getStatus()),
        LogSanitizer.sanitize(vectorDbHealth.getStatus()));

    return ResponseEntity.status(httpStatus).body(response);
  }

  /**
   * Checks the health of the language model.
   *
   * @return ComponentHealth with status and details
   */
  private ComponentHealth checkLanguageModel() {
    try {
      boolean available = languageModel.isAvailable();

      if (available) {
        return new ComponentHealth(
            "UP",
            true,
            Map.of(
                "provider",
                languageModel.getProviderName(),
                "message",
                "Language model is available"));
      } else {
        return new ComponentHealth(
            "DOWN",
            false,
            Map.of(
                "provider",
                languageModel.getProviderName(),
                "message",
                "Language model is not available"));
      }
    } catch (Exception e) {
      logger.error("Error checking language model health", e);
      return new ComponentHealth(
          "DOWN",
          false,
          Map.of(
              "provider",
              languageModel.getProviderName(),
              "message",
              "Error checking language model: " + e.getMessage()));
    }
  }

  /**
   * Checks the health of the vector database.
   *
   * @return ComponentHealth with status and details
   */
  private ComponentHealth checkVectorDatabase() {
    try {
      String collectionName = vectorDbConfig.collectionName();
      boolean exists = vectorRepository.collectionExists(collectionName);

      if (exists) {
        return new ComponentHealth(
            "UP",
            true,
            Map.of(
                "type",
                vectorDbConfig.type(),
                "collection",
                collectionName,
                "message",
                "Vector database is available and collection exists"));
      } else {
        return new ComponentHealth(
            "DOWN",
            false,
            Map.of(
                "type",
                vectorDbConfig.type(),
                "collection",
                collectionName,
                "message",
                "Collection does not exist"));
      }
    } catch (Exception e) {
      logger.error("Error checking vector database health", e);
      return new ComponentHealth(
          "DOWN",
          false,
          Map.of(
              "type",
              vectorDbConfig.type(),
              "message",
              "Error checking vector database: " + e.getMessage()));
    }
  }

  /** Health response model containing overall status and component details. */
  public static class HealthResponse {
    private String status;
    private Map<String, ComponentHealth> components;

    public HealthResponse() {
      this.components = new HashMap<>();
    }

    public String getStatus() {
      return status;
    }

    public void setStatus(String status) {
      this.status = status;
    }

    public Map<String, ComponentHealth> getComponents() {
      return components == null ? null : new HashMap<>(components);
    }

    public void setComponents(Map<String, ComponentHealth> components) {
      this.components = components == null ? new HashMap<>() : new HashMap<>(components);
    }

    public void addComponent(String name, ComponentHealth health) {
      this.components.put(name, health);
    }
  }

  /** Component health model containing status and details for a specific component. */
  public static class ComponentHealth {
    private String status;
    private boolean healthy;
    private Map<String, String> details;

    public ComponentHealth() {
      this.details = new HashMap<>();
    }

    public ComponentHealth(String status, boolean healthy, Map<String, String> details) {
      this.status = status;
      this.healthy = healthy;
      this.details = details != null ? new HashMap<>(details) : new HashMap<>();
    }

    public String getStatus() {
      return status;
    }

    public void setStatus(String status) {
      this.status = status;
    }

    public boolean isHealthy() {
      return healthy;
    }

    public void setHealthy(boolean healthy) {
      this.healthy = healthy;
    }

    public Map<String, String> getDetails() {
      return details == null ? null : new HashMap<>(details);
    }

    public void setDetails(Map<String, String> details) {
      this.details = details == null ? new HashMap<>() : new HashMap<>(details);
    }
  }
}
