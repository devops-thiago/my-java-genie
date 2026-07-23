package br.com.arquivolivre.myjavagenie.controller;

import br.com.arquivolivre.myjavagenie.exception.LlmException;
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

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationException(
      MethodArgumentNotValidException ex, WebRequest request) {
    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult()
        .getFieldErrors()
        .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

    return ResponseEntity.badRequest()
        .body(
            new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed",
                "Request validation failed: " + errors,
                request.getDescription(false),
                LocalDateTime.now()));
  }

  @ExceptionHandler(LlmException.class)
  public ResponseEntity<ErrorResponse> handleLlmException(LlmException ex, WebRequest request) {
    logger.error("LLM error: {}", ex.getMessage(), ex);
    return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
        .body(
            new ErrorResponse(
                HttpStatus.BAD_GATEWAY.value(),
                "Language model error",
                ex.getMessage(),
                request.getDescription(false),
                LocalDateTime.now()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex, WebRequest request) {
    logger.error("Unexpected error: {}", ex.getMessage(), ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(
            new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal server error",
                "An unexpected error occurred.",
                request.getDescription(false),
                LocalDateTime.now()));
  }

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
