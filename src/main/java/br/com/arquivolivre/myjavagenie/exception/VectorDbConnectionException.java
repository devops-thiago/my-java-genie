package br.com.arquivolivre.myjavagenie.exception;

/**
 * Exception thrown when connection to the vector database fails. This can occur during
 * initialization or when the database becomes unavailable.
 */
public class VectorDbConnectionException extends VectorDbException {

  public VectorDbConnectionException(String message) {
    super(message);
  }

  public VectorDbConnectionException(String message, Throwable cause) {
    super(message, cause);
  }

  public static VectorDbConnectionException forDatabase(
      String dbType, String connectionUrl, Throwable cause) {
    return new VectorDbConnectionException(
        String.format("Failed to connect to %s vector database at %s", dbType, connectionUrl),
        cause);
  }
}
