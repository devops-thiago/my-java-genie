package br.com.arquivolivre.myjavagenie.config;

import br.com.arquivolivre.myjavagenie.exception.ConfigurationException;

/**
 * Interface for providing access to application configuration.
 * This abstraction allows for different configuration sources and testing.
 */
public interface ConfigurationProvider {

    /**
     * Get the language model configuration.
     *
     * @return ModelConfig instance
     */
    ModelConfig getModelConfig();

    /**
     * Get the vector database configuration.
     *
     * @return VectorDbConfig instance
     */
    VectorDbConfig getVectorDbConfig();

    /**
     * Get the document ingestion configuration.
     *
     * @return IngestionConfig instance
     */
    IngestionConfig getIngestionConfig();

    /**
     * Get the query processing configuration.
     *
     * @return QueryConfig instance
     */
    QueryConfig getQueryConfig();

    /**
     * Validate that all required configuration is present and valid.
     *
     * @throws ConfigurationException if configuration is invalid
     */
    void validateConfiguration();
}
