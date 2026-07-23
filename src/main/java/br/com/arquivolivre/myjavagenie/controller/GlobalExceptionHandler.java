package br.com.arquivolivre.myjavagenie.controller;

import br.com.arquivolivre.myjavagenie.exception.*;
import br.com.arquivolivre.myjavagenie.util.LogSanitizer;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

/**
 * Global exception handler for all REST controllers. Provides centralized error handling and
 * logging with detailed stack traces.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /** Handles validation errors from request body validation. */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationException(
      MethodArgumentNotValidException ex, WebRequest request) {

    logger.error(
        "Validation error on request to {}: {}",
        LogSanitizer.sanitize(request.getDescription(false)),
        LogSanitizer.sanitize(ex.getMessage()));

    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult()
        .getFieldErrors()
        .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

    ErrorResponse errorResponse =
        new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Validation failed",
            "Request validation failed: " + errors,
            request.getDescription(false),
            LocalDateTime.now());

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
  }

  /** Handles IllegalArgumentException for invalid request parameters. */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
      IllegalArgumentException ex, WebRequest request) {

    logger.error(
        "Invalid argument on request to {}: {}",
        LogSanitizer.sanitize(request.getDescription(false)),
        LogSanitizer.sanitize(ex.getMessage()),
        ex);

    ErrorResponse errorResponse =
        new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Invalid request",
            ex.getMessage(),
            request.getDescription(false),
            LocalDateTime.now());

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
  }

  /** Handles ModelTimeoutException when model generation times out. */
  @ExceptionHandler(ModelTimeoutException.class)
  public ResponseEntity<ErrorResponse> handleModelTimeoutException(
      ModelTimeoutException ex, WebRequest request) {

    logger.error(
        "Model timeout on request to {}: {}",
        LogSanitizer.sanitize(request.getDescription(false)),
        LogSanitizer.sanitize(ex.getMessage()),
        ex);

    ErrorResponse errorResponse =
        new ErrorResponse(
            HttpStatus.GATEWAY_TIMEOUT.value(),
            "Request timeout",
            ex.getMessage(),
            request.getDescription(false),
            LocalDateTime.now());

    return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(errorResponse);
  }

  /** Handles ModelInvocationException when model invocation fails. */
  @ExceptionHandler(ModelInvocationException.class)
  public ResponseEntity<ErrorResponse> handleModelInvocationException(
      ModelInvocationException ex, WebRequest request) {

    logger.error(
        "Model invocation failed on request to {}: {}",
        LogSanitizer.sanitize(request.getDescription(false)),
        LogSanitizer.sanitize(ex.getMessage()),
        ex);

    ErrorResponse errorResponse =
        new ErrorResponse(
            HttpStatus.SERVICE_UNAVAILABLE.value(),
            "Language model unavailable",
            ex.getMessage(),
            request.getDescription(false),
            LocalDateTime.now());

    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
  }

  /** Handles ModelInitializationException when model initialization fails. */
  @ExceptionHandler(ModelInitializationException.class)
  public ResponseEntity<ErrorResponse> handleModelInitializationException(
      ModelInitializationException ex, WebRequest request) {

    logger.error(
        "Model initialization failed on request to {}: {}",
        LogSanitizer.sanitize(request.getDescription(false)),
        LogSanitizer.sanitize(ex.getMessage()),
        ex);

    ErrorResponse errorResponse =
        new ErrorResponse(
            HttpStatus.SERVICE_UNAVAILABLE.value(),
            "Language model initialization failed",
            ex.getMessage(),
            request.getDescription(false),
            LocalDateTime.now());

    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
  }

  /** Handles VectorDbException when vector database operations fail. */
  @ExceptionHandler(VectorDbException.class)
  public ResponseEntity<ErrorResponse> handleVectorDbException(
      VectorDbException ex, WebRequest request) {

    logger.error(
        "Vector database error on request to {}: {}",
        LogSanitizer.sanitize(request.getDescription(false)),
        LogSanitizer.sanitize(ex.getMessage()),
        ex);

    ErrorResponse errorResponse =
        new ErrorResponse(
            HttpStatus.SERVICE_UNAVAILABLE.value(),
            "Vector database unavailable",
            ex.getMessage(),
            request.getDescription(false),
            LocalDateTime.now());

    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
  }

  /** Handles IngestionException when document ingestion fails. */
  @ExceptionHandler(IngestionException.class)
  public ResponseEntity<ErrorResponse> handleIngestionException(
      IngestionException ex, WebRequest request) {

    logger.error(
        "Ingestion failed on request to {}: {}",
        LogSanitizer.sanitize(request.getDescription(false)),
        LogSanitizer.sanitize(ex.getMessage()),
        ex);

    ErrorResponse errorResponse =
        new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Ingestion failed",
            ex.getMessage(),
            request.getDescription(false),
            LocalDateTime.now());

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
  }

  /** Handles ConfigurationException when configuration is invalid. */
  @ExceptionHandler(ConfigurationException.class)
  public ResponseEntity<ErrorResponse> handleConfigurationException(
      ConfigurationException ex, WebRequest request) {

    logger.error(
        "Configuration error on request to {}: {}",
        LogSanitizer.sanitize(request.getDescription(false)),
        LogSanitizer.sanitize(ex.getMessage()),
        ex);

    ErrorResponse errorResponse =
        new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Configuration error",
            ex.getMessage(),
            request.getDescription(false),
            LocalDateTime.now());

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
  }

  /** Handles general RagSystemException. */
  @ExceptionHandler(RagSystemException.class)
  public ResponseEntity<ErrorResponse> handleRagSystemException(
      RagSystemException ex, WebRequest request) {

    logger.error(
        "RAG system error on request to {}: {}",
        LogSanitizer.sanitize(request.getDescription(false)),
        LogSanitizer.sanitize(ex.getMessage()),
        ex);

    ErrorResponse errorResponse =
        new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "System error",
            ex.getMessage(),
            request.getDescription(false),
            LocalDateTime.now());

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
  }

  /** Handles all other unexpected exceptions. */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex, WebRequest request) {

    logger.error(
        "Unexpected error on request to {}: {}",
        LogSanitizer.sanitize(request.getDescription(false)),
        LogSanitizer.sanitize(ex.getMessage()),
        ex);

    ErrorResponse errorResponse =
        new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal server error",
            "An unexpected error occurred. Please contact support if the problem persists.",
            request.getDescription(false),
            LocalDateTime.now());

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
  }

  /** Enhanced error response model with timestamp and path information. */
  public static class ErrorResponse {
    private int status;
    private String error;
    private String message;
    private String path;
    private LocalDateTime timestamp;

    public ErrorResponse() {}

    public ErrorResponse(
        int status, String error, String message, String path, LocalDateTime timestamp) {
      this.status = status;
      this.error = error;
      this.message = message;
      this.path = path;
      this.timestamp = timestamp;
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

    public String getPath() {
      return path;
    }

    public void setPath(String path) {
      this.path = path;
    }

    public LocalDateTime getTimestamp() {
      return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
      this.timestamp = timestamp;
    }
  }
}
