package br.com.arquivolivre.myjavagenie.controller;

import br.com.arquivolivre.myjavagenie.exception.ModelInvocationException;
import br.com.arquivolivre.myjavagenie.exception.ModelTimeoutException;
import br.com.arquivolivre.myjavagenie.exception.RagSystemException;
import br.com.arquivolivre.myjavagenie.exception.VectorDbException;
import br.com.arquivolivre.myjavagenie.model.QueryRequest;
import br.com.arquivolivre.myjavagenie.model.QueryResponse;
import br.com.arquivolivre.myjavagenie.service.QueryService;
import br.com.arquivolivre.myjavagenie.util.LogSanitizer;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for handling query requests. Provides endpoint for users to ask questions about
 * Java 25 documentation.
 */
@RestController
@RequestMapping("/api")
@Validated
public class QueryController {
  private static final Logger logger = LoggerFactory.getLogger(QueryController.class);

  private final QueryService queryService;

  public QueryController(QueryService queryService) {
    this.queryService = queryService;
  }

  /**
   * Processes a user query and returns an answer with sources.
   *
   * @param request the query request containing the user's question
   * @return ResponseEntity containing the query response
   */
  @PostMapping("/query")
  public ResponseEntity<QueryResponse> query(@Valid @RequestBody QueryRequest request) {
    logger.info("Received query request");

    try {
      QueryResponse response = queryService.processQuery(request.getQuestion());
      return ResponseEntity.ok(response);
    } catch (IllegalArgumentException e) {
      logger.warn("Invalid query request: {}", LogSanitizer.sanitize(e.getMessage()));
      throw e;
    } catch (ModelTimeoutException e) {
      logger.error("Query timed out: {}", LogSanitizer.sanitize(e.getMessage()));
      throw e;
    } catch (ModelInvocationException e) {
      logger.error("Model invocation failed: {}", LogSanitizer.sanitize(e.getMessage()));
      throw e;
    } catch (VectorDbException e) {
      logger.error("Vector database error: {}", LogSanitizer.sanitize(e.getMessage()));
      throw e;
    } catch (RagSystemException e) {
      logger.error("RAG system error: {}", LogSanitizer.sanitize(e.getMessage()));
      throw e;
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
   * Exception handler for ModelTimeoutException. Returns 504 Gateway Timeout when model generation
   * times out.
   */
  @ExceptionHandler(ModelTimeoutException.class)
  public ResponseEntity<ErrorResponse> handleModelTimeout(ModelTimeoutException e) {
    logger.debug("Handling ModelTimeoutException: {}", LogSanitizer.sanitize(e.getMessage()));
    ErrorResponse error =
        new ErrorResponse(HttpStatus.GATEWAY_TIMEOUT.value(), "Request timeout", e.getMessage());
    return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(error);
  }

  /**
   * Exception handler for ModelInvocationException. Returns 503 Service Unavailable when model
   * invocation fails.
   */
  @ExceptionHandler(ModelInvocationException.class)
  public ResponseEntity<ErrorResponse> handleModelInvocation(ModelInvocationException e) {
    logger.debug("Handling ModelInvocationException: {}", LogSanitizer.sanitize(e.getMessage()));
    ErrorResponse error =
        new ErrorResponse(
            HttpStatus.SERVICE_UNAVAILABLE.value(), "Language model unavailable", e.getMessage());
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
  }

  /**
   * Exception handler for VectorDbException. Returns 503 Service Unavailable when vector database
   * operations fail.
   */
  @ExceptionHandler(VectorDbException.class)
  public ResponseEntity<ErrorResponse> handleVectorDbException(VectorDbException e) {
    logger.debug("Handling VectorDbException: {}", LogSanitizer.sanitize(e.getMessage()));
    ErrorResponse error =
        new ErrorResponse(
            HttpStatus.SERVICE_UNAVAILABLE.value(), "Vector database unavailable", e.getMessage());
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
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
