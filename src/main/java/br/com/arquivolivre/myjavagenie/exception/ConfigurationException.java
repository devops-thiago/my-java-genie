package br.com.arquivolivre.myjavagenie.exception;

/**
 * Base exception for configuration related errors.
 */
public class ConfigurationException extends RagSystemException {

    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConfigurationException(Throwable cause) {
        super(cause);
    }
}
