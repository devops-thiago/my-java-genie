package br.com.arquivolivre.myjavagenie.exception;

/**
 * Base exception for document ingestion related errors.
 */
public class IngestionException extends RagSystemException {

    public IngestionException(String message) {
        super(message);
    }

    public IngestionException(String message, Throwable cause) {
        super(message, cause);
    }

    public IngestionException(Throwable cause) {
        super(cause);
    }
}
