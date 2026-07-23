package br.com.arquivolivre.myjavagenie.exception;

/** Exception thrown when a language model operation exceeds the configured timeout. */
public class ModelTimeoutException extends ModelException {

  public ModelTimeoutException(String message) {
    super(message);
  }

  public ModelTimeoutException(String message, Throwable cause) {
    super(message, cause);
  }

  public static ModelTimeoutException afterSeconds(long timeoutSeconds) {
    return new ModelTimeoutException(
        String.format("Language model operation timed out after %d seconds", timeoutSeconds));
  }
}
