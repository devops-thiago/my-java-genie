package br.com.arquivolivre.myjavagenie.service;

import br.com.arquivolivre.myjavagenie.config.ModelConfig;
import br.com.arquivolivre.myjavagenie.exception.InvalidConfigurationException;
import br.com.arquivolivre.myjavagenie.exception.ModelInitializationException;

/**
 * Factory interface for creating language model providers.
 * Implementations should instantiate the appropriate provider based on configuration.
 */
public interface LanguageModelFactory {

    /**
     * Creates a language model provider based on the provided configuration.
     *
     * @param config the model configuration specifying provider type and settings
     * @return a configured language model provider instance
     * @throws ModelInitializationException  if provider creation fails
     * @throws InvalidConfigurationException if configuration is invalid
     */
    LanguageModelProvider createProvider(ModelConfig config);
}
