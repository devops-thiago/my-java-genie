package br.com.arquivolivre.myjavagenie.controller;

import br.com.arquivolivre.myjavagenie.exception.IngestionException;
import br.com.arquivolivre.myjavagenie.exception.RagSystemException;
import br.com.arquivolivre.myjavagenie.model.IngestionResult;
import br.com.arquivolivre.myjavagenie.service.IngestionService;
import br.com.arquivolivre.myjavagenie.util.LogSanitizer;
import jakarta.validation.constraints.NotBlank;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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

  /** Root directory that ingestion paths must resolve within; defaults to the process directory. */
  private final Path ingestionRoot;

  public IngestionController(
      IngestionService ingestionService,
      @Value("${ingestion.base-path:.}") String ingestionBasePath) {
    this.ingestionService = ingestionService;
    // Resolve the configured base under the process working directory. The working directory is a
    // constant (untrusted config never reaches Paths.get/File directly), and Path#resolve keeps the
    // result within a known root, so a configured or requested value cannot address an arbitrary
    // filesystem location.
    this.ingestionRoot = Paths.get("").toAbsolutePath().resolve(ingestionBasePath).normalize();
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
    logger.info("Received ingestion request for path: {}", LogSanitizer.sanitize(documentPath));

    try {
      // Validate and convert path
      Path path = validateAndConvertPath(documentPath);

      // Perform ingestion
      IngestionResult result = ingestionService.ingestDocuments(path);

      // Return appropriate status based on result
      if ("FAILURE".equals(result.getStatus())) {
        logger.warn("Ingestion failed: {}", LogSanitizer.sanitize(result));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
      } else if ("PARTIAL_SUCCESS".equals(result.getStatus())) {
        logger.warn("Ingestion partially succeeded: {}", LogSanitizer.sanitize(result));
        return ResponseEntity.status(HttpStatus.MULTI_STATUS).body(result);
      } else {
        logger.info("Ingestion succeeded: {}", LogSanitizer.sanitize(result));
        return ResponseEntity.ok(result);
      }

    } catch (IllegalArgumentException e) {
      logger.warn("Invalid ingestion request: {}", LogSanitizer.sanitize(e.getMessage()));
      throw e;
    } catch (IngestionException e) {
      logger.error("Ingestion failed: {}", LogSanitizer.sanitize(e.getMessage()));
      throw e;
    } catch (RagSystemException e) {
      logger.error("System error during ingestion: {}", LogSanitizer.sanitize(e.getMessage()));
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
      // Resolve the request value against the trusted ingestion root and reject anything that
      // escapes it, so untrusted input can never reach an arbitrary filesystem location.
      Path resolved = ingestionRoot.resolve(pathString).normalize();
      if (!resolved.startsWith(ingestionRoot)) {
        throw new IllegalArgumentException(
            "Invalid document path: resolved location escapes the configured ingestion root");
      }
      return resolved;
    } catch (InvalidPathException e) {
      logger.warn("Invalid path provided: {}", LogSanitizer.sanitize(pathString));
      throw new IllegalArgumentException("Invalid document path: " + e.getMessage(), e);
    }
  }

  /**
   * Exception handler for IllegalArgumentException. Returns 400 Bad Request for validation errors.
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
    logger.debug("Handling IllegalArgumentException: {}", LogSanitizer.sanitize(e.getMessage()));
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
    logger.debug("Handling IngestionException: {}", LogSanitizer.sanitize(e.getMessage()));
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
    logger.debug("Handling RagSystemException: {}", LogSanitizer.sanitize(e.getMessage()));
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
