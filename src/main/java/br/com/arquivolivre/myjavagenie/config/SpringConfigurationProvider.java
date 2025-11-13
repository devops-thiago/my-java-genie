package br.com.arquivolivre.myjavagenie.config;

import br.com.arquivolivre.myjavagenie.exception.ConfigurationException;
import org.springframework.stereotype.Component;

/**
 * Spring-based implementation of ConfigurationProvider.
 * Wraps Spring's @ConfigurationProperties beans and provides validation.
 */
@Component
public class SpringConfigurationProvider implements ConfigurationProvider {

    private final ModelConfig modelConfig;
    private final VectorDbConfig vectorDbConfig;
    private final IngestionConfig ingestionConfig;
    private final QueryConfig queryConfig;

    public SpringConfigurationProvider(
            ModelConfig modelConfig,
            VectorDbConfig vectorDbConfig,
            IngestionConfig ingestionConfig,
            QueryConfig queryConfig) {
        this.modelConfig = modelConfig;
        this.vectorDbConfig = vectorDbConfig;
        this.ingestionConfig = ingestionConfig;
        this.queryConfig = queryConfig;
    }

    @Override
    public ModelConfig getModelConfig() {
        return modelConfig;
    }

    @Override
    public VectorDbConfig getVectorDbConfig() {
        return vectorDbConfig;
    }

    @Override
    public IngestionConfig getIngestionConfig() {
        return ingestionConfig;
    }

    @Override
    public QueryConfig getQueryConfig() {
        return queryConfig;
    }

    @Override
    public void validateConfiguration() {
        validateModelConfig();
        validateVectorDbConfig();
        validateIngestionConfig();
        validateQueryConfig();
    }

    private void validateModelConfig() {
        if (modelConfig == null) {
            throw new ConfigurationException("Model configuration is missing");
        }

        String provider = modelConfig.getProvider();
        if (provider == null || provider.isBlank()) {
            throw new ConfigurationException("Model provider must be specified");
        }

        // Validate provider-specific settings
        switch (provider.toLowerCase()) {
            case "self-hosted":
                validateSelfHostedConfig();
                break;
            case "openai":
                validateOpenAIConfig();
                break;
            case "anthropic":
                validateAnthropicConfig();
                break;
            default:
                throw new ConfigurationException(
                        "Unsupported model provider: " + provider + ". Supported providers: self-hosted, openai, anthropic");
        }

        // Validate common settings
        if (modelConfig.getTemperature() == null) {
            throw new ConfigurationException("Model temperature must be specified");
        }
        if (modelConfig.getTemperature() < 0.0 || modelConfig.getTemperature() > 2.0) {
            throw new ConfigurationException("Model temperature must be between 0.0 and 2.0");
        }

        if (modelConfig.getMaxTokens() == null || modelConfig.getMaxTokens() <= 0) {
            throw new ConfigurationException("Model max tokens must be a positive number");
        }
    }

    private void validateSelfHostedConfig() {
        ModelConfig.SelfHostedSettings settings = modelConfig.getSelfHosted();
        if (settings == null) {
            throw new ConfigurationException("Self-hosted model settings are missing");
        }
        if (settings.getBaseUrl() == null || settings.getBaseUrl().isBlank()) {
            throw new ConfigurationException("Self-hosted model base URL must be specified");
        }
        if (settings.getModelName() == null || settings.getModelName().isBlank()) {
            throw new ConfigurationException("Self-hosted model name must be specified");
        }
    }

    private void validateOpenAIConfig() {
        ModelConfig.OpenAISettings settings = modelConfig.getOpenai();
        if (settings == null) {
            throw new ConfigurationException("OpenAI settings are missing");
        }
        if (settings.getApiKey() == null || settings.getApiKey().isBlank()) {
            throw new ConfigurationException("OpenAI API key must be specified");
        }
        if (settings.getModelName() == null || settings.getModelName().isBlank()) {
            throw new ConfigurationException("OpenAI model name must be specified");
        }
    }

    private void validateAnthropicConfig() {
        ModelConfig.AnthropicSettings settings = modelConfig.getAnthropic();
        if (settings == null) {
            throw new ConfigurationException("Anthropic settings are missing");
        }
        if (settings.getApiKey() == null || settings.getApiKey().isBlank()) {
            throw new ConfigurationException("Anthropic API key must be specified");
        }
        if (settings.getModelName() == null || settings.getModelName().isBlank()) {
            throw new ConfigurationException("Anthropic model name must be specified");
        }
    }

    private void validateVectorDbConfig() {
        if (vectorDbConfig == null) {
            throw new ConfigurationException("Vector database configuration is missing");
        }

        if (vectorDbConfig.getType() == null || vectorDbConfig.getType().isBlank()) {
            throw new ConfigurationException("Vector database type must be specified");
        }

        if (vectorDbConfig.getConnectionUrl() == null || vectorDbConfig.getConnectionUrl().isBlank()) {
            throw new ConfigurationException("Vector database connection URL must be specified");
        }

        if (vectorDbConfig.getCollectionName() == null || vectorDbConfig.getCollectionName().isBlank()) {
            throw new ConfigurationException("Vector database collection name must be specified");
        }

        // Validate type-specific settings
        String type = vectorDbConfig.getType().toLowerCase();
        switch (type) {
            case "chroma":
                // ChromaDB settings are optional
                break;
            case "pgvector":
                validatePgVectorConfig();
                break;
            case "qdrant":
                // Qdrant settings are optional (API key and TLS)
                break;
            default:
                throw new ConfigurationException(
                        "Unsupported vector database type: " + type + ". Supported types: chroma, pgvector, qdrant");
        }
    }

    private void validatePgVectorConfig() {
        VectorDbConfig.PgVectorSettings settings = vectorDbConfig.getPgvector();
        if (settings == null) {
            throw new ConfigurationException("pgvector settings are missing");
        }
        if (settings.getHost() == null || settings.getHost().isBlank()) {
            throw new ConfigurationException("pgvector host must be specified");
        }
        if (settings.getPort() == null || settings.getPort() <= 0) {
            throw new ConfigurationException("pgvector port must be a positive number");
        }
        if (settings.getDatabase() == null || settings.getDatabase().isBlank()) {
            throw new ConfigurationException("pgvector database must be specified");
        }
        if (settings.getUsername() == null || settings.getUsername().isBlank()) {
            throw new ConfigurationException("pgvector username must be specified");
        }
    }

    private void validateIngestionConfig() {
        if (ingestionConfig == null) {
            throw new ConfigurationException("Ingestion configuration is missing");
        }

        if (ingestionConfig.getChunkSize() == null || ingestionConfig.getChunkSize() <= 0) {
            throw new ConfigurationException("Ingestion chunk size must be a positive number");
        }

        if (ingestionConfig.getChunkOverlap() == null || ingestionConfig.getChunkOverlap() < 0) {
            throw new ConfigurationException("Ingestion chunk overlap must be zero or positive");
        }

        if (ingestionConfig.getChunkOverlap() >= ingestionConfig.getChunkSize()) {
            throw new ConfigurationException("Ingestion chunk overlap must be less than chunk size");
        }

        if (ingestionConfig.getBatchSize() == null || ingestionConfig.getBatchSize() <= 0) {
            throw new ConfigurationException("Ingestion batch size must be a positive number");
        }
    }

    private void validateQueryConfig() {
        if (queryConfig == null) {
            throw new ConfigurationException("Query configuration is missing");
        }

        if (queryConfig.getMaxRetrievedChunks() == null || queryConfig.getMaxRetrievedChunks() <= 0) {
            throw new ConfigurationException("Query max retrieved chunks must be a positive number");
        }

        if (queryConfig.getSimilarityThreshold() == null) {
            throw new ConfigurationException("Query similarity threshold must be specified");
        }

        if (queryConfig.getSimilarityThreshold() < 0.0 || queryConfig.getSimilarityThreshold() > 1.0) {
            throw new ConfigurationException("Query similarity threshold must be between 0.0 and 1.0");
        }

        if (queryConfig.getTimeoutSeconds() == null || queryConfig.getTimeoutSeconds() <= 0) {
            throw new ConfigurationException("Query timeout seconds must be a positive number");
        }
    }
}
