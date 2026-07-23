package br.com.arquivolivre.myjavagenie.exception;

/**
 * Exception thrown when a language model fails to initialize. This typically occurs during startup
 * when the model provider cannot be configured or connected.
 */
public class ModelInitializationException extends ModelException {

  public ModelInitializationException(String message) {
    super(message);
  }

  public ModelInitializationException(String message, Throwable cause) {
    super(message, cause);
  }

  public static ModelInitializationException forProvider(String providerName, Throwable cause) {
    return new ModelInitializationException(
        String.format("Failed to initialize language model provider: %s", providerName), cause);
  }
}
