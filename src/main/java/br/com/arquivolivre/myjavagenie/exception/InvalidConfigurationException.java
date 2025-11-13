package br.com.arquivolivre.myjavagenie.exception;

/**
 * Exception thrown when configuration validation fails.
 * This indicates that the provided configuration is invalid or incomplete.
 */
public class InvalidConfigurationException extends ConfigurationException {

    public InvalidConfigurationException(String message) {
        super(message);
    }

    public InvalidConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

    public static InvalidConfigurationException forKey(String configKey, String reason) {
        return new InvalidConfigurationException(
                String.format("Invalid configuration for '%s': %s", configKey, reason)
        );
    }
}
