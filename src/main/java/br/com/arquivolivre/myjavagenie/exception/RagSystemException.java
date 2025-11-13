package br.com.arquivolivre.myjavagenie.exception;

/**
 * Base exception for all RAG system errors.
 * All custom exceptions in the system should extend this class.
 */
public class RagSystemException extends RuntimeException {

    public RagSystemException(String message) {
        super(message);
    }

    public RagSystemException(String message, Throwable cause) {
        super(message, cause);
    }

    public RagSystemException(Throwable cause) {
        super(cause);
    }
}
