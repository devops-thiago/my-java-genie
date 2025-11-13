package br.com.arquivolivre.myjavagenie.exception;

/**
 * Base exception for vector database related errors.
 */
public class VectorDbException extends RagSystemException {

    public VectorDbException(String message) {
        super(message);
    }

    public VectorDbException(String message, Throwable cause) {
        super(message, cause);
    }

    public VectorDbException(Throwable cause) {
        super(cause);
    }
}
