package br.com.arquivolivre.myjavagenie.exception;

/** Exception thrown when required configuration is missing. */
public class MissingConfigurationException extends ConfigurationException {

  public MissingConfigurationException(String message) {
    super(message);
  }

  public MissingConfigurationException(String message, Throwable cause) {
    super(message, cause);
  }

  public static MissingConfigurationException forKey(String configKey) {
    return new MissingConfigurationException(
        String.format("Required configuration is missing: %s", configKey));
  }

  public static MissingConfigurationException forKey(String configKey, Throwable cause) {
    return new MissingConfigurationException(
        String.format("Required configuration is missing: %s", configKey), cause);
  }
}
