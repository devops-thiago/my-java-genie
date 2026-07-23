package br.com.arquivolivre.myjavagenie.controller;

import br.com.arquivolivre.myjavagenie.exception.IngestionException;
import br.com.arquivolivre.myjavagenie.exception.RagSystemException;
import br.com.arquivolivre.myjavagenie.model.IngestionResult;
import br.com.arquivolivre.myjavagenie.service.IngestionService;
import jakarta.validation.constraints.NotBlank;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for handling document ingestion requests. Provides endpoint for administrators to
 * ingest Java 25 documentation.
 *
 * <p>Note: In production, this endpoint should be secured with authentication/authorization.
 */
@RestController
@RequestMapping("/api")
@Validated
public class IngestionController {
  private static final Logger logger = LoggerFactory.getLogger(IngestionController.class);

  private final IngestionService ingestionService;

  public IngestionController(IngestionService ingestionService) {
    this.ingestionService = ingestionService;
  }

  /**
   * Ingests documents from the specified path.
   *
   * <p>TODO: Add authentication/authorization (e.g., @PreAuthorize("hasRole('ADMIN')"))
   *
   * @param documentPath the path to the directory or file containing documents to ingest
   * @return ResponseEntity containing the ingestion result
   */
  @PostMapping("/ingest")
  public ResponseEntity<IngestionResult> ingest(
      @RequestParam @NotBlank(message = "Document path cannot be blank") String documentPath) {
    logger.info("Received ingestion request for path: {}", documentPath);

    try {
      // Validate and convert path
      Path path = validateAndConvertPath(documentPath);

      // Perform ingestion
      IngestionResult result = ingestionService.ingestDocuments(path);

      // Return appropriate status based on result
      if ("FAILURE".equals(result.getStatus())) {
        logger.warn("Ingestion failed: {}", result);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
      } else if ("PARTIAL_SUCCESS".equals(result.getStatus())) {
        logger.warn("Ingestion partially succeeded: {}", result);
        return ResponseEntity.status(HttpStatus.MULTI_STATUS).body(result);
      } else {
        logger.info("Ingestion succeeded: {}", result);
        return ResponseEntity.ok(result);
      }

    } catch (IllegalArgumentException e) {
      logger.warn("Invalid ingestion request: {}", e.getMessage());
      throw e;
    } catch (IngestionException e) {
      logger.error("Ingestion failed: {}", e.getMessage());
      throw e;
    } catch (RagSystemException e) {
      logger.error("System error during ingestion: {}", e.getMessage());
      throw e;
    }
  }

  /**
   * Validates and converts a string path to a Path object.
   *
   * @param pathString the path string to validate
   * @return Path object
   * @throws IllegalArgumentException if path is invalid
   */
  private Path validateAndConvertPath(String pathString) {
    try {
      Path path = Paths.get(pathString);

      // Additional validation could be added here:
      // - Check if path exists
      // - Check if path is readable
      // - Check if path is within allowed directories

      return path;
    } catch (InvalidPathException e) {
      logger.warn("Invalid path provided: {}", pathString);
      throw new IllegalArgumentException("Invalid document path: " + e.getMessage(), e);
    }
  }

  /**
   * Exception handler for IllegalArgumentException. Returns 400 Bad Request for validation errors.
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
    logger.debug("Handling IllegalArgumentException: {}", e.getMessage());
    ErrorResponse error =
        new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Invalid request", e.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  /**
   * Exception handler for IngestionException. Returns 500 Internal Server Error when ingestion
   * fails.
   */
  @ExceptionHandler(IngestionException.class)
  public ResponseEntity<ErrorResponse> handleIngestionException(IngestionException e) {
    logger.debug("Handling IngestionException: {}", e.getMessage());
    ErrorResponse error =
        new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(), "Ingestion failed", e.getMessage());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
  }

  /**
   * Exception handler for general RagSystemException. Returns 500 Internal Server Error for
   * unexpected system errors.
   */
  @ExceptionHandler(RagSystemException.class)
  public ResponseEntity<ErrorResponse> handleRagSystemException(RagSystemException e) {
    logger.debug("Handling RagSystemException: {}", e.getMessage());
    ErrorResponse error =
        new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "System error", e.getMessage());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
  }

  /** Error response model for API errors. */
  public static class ErrorResponse {
    private int status;
    private String error;
    private String message;

    public ErrorResponse() {}

    public ErrorResponse(int status, String error, String message) {
      this.status = status;
      this.error = error;
      this.message = message;
    }

    public int getStatus() {
      return status;
    }

    public void setStatus(int status) {
      this.status = status;
    }

    public String getError() {
      return error;
    }

    public void setError(String error) {
      this.error = error;
    }

    public String getMessage() {
      return message;
    }

    public void setMessage(String message) {
      this.message = message;
    }
  }
}
