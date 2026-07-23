package br.com.arquivolivre.myjavagenie.exception;

/**
 * Exception thrown when a language model invocation fails. This occurs when the model is
 * initialized but fails during text generation.
 */
public class ModelInvocationException extends ModelException {

  public ModelInvocationException(String message) {
    super(message);
  }

  public ModelInvocationException(String message, Throwable cause) {
    super(message, cause);
  }

  public static ModelInvocationException forOperation(
      String providerName, String operation, Throwable cause) {
    return new ModelInvocationException(
        String.format(
            "Failed to invoke %s on language model provider: %s", operation, providerName),
        cause);
  }
}
