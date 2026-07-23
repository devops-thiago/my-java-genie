package br.com.arquivolivre.myjavagenie.exception;

/** Base exception for language model related errors. */
public class ModelException extends RagSystemException {

  public ModelException(String message) {
    super(message);
  }

  public ModelException(String message, Throwable cause) {
    super(message, cause);
  }

  public ModelException(Throwable cause) {
    super(cause);
  }
}
