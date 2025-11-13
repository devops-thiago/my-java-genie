package br.com.arquivolivre.myjavagenie.exception;

/**
 * Exception thrown when a vector database query operation fails.
 */
public class VectorDbQueryException extends VectorDbException {

    public VectorDbQueryException(String message) {
        super(message);
    }

    public VectorDbQueryException(String message, Throwable cause) {
        super(message, cause);
    }

    public static VectorDbQueryException forOperation(String operation, Throwable cause) {
        return new VectorDbQueryException(
                String.format("Vector database query failed during operation: %s", operation),
                cause
        );
    }
}
