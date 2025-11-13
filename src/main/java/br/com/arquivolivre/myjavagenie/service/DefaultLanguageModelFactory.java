package br.com.arquivolivre.myjavagenie.service;

import br.com.arquivolivre.myjavagenie.config.ModelConfig;
import br.com.arquivolivre.myjavagenie.exception.InvalidConfigurationException;
import br.com.arquivolivre.myjavagenie.exception.ModelInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Default implementation of the language model factory.
 * Creates the appropriate provider based on configuration.
 */
@Component
public class DefaultLanguageModelFactory implements LanguageModelFactory {

    private static final Logger logger = LoggerFactory.getLogger(DefaultLanguageModelFactory.class);

    @Override
    public LanguageModelProvider createProvider(ModelConfig config) {
        if (config == null) {
            throw new InvalidConfigurationException("Model configuration is required");
        }

        String provider = config.getProvider();
        if (provider == null || provider.isEmpty()) {
            throw new InvalidConfigurationException("Model provider type must be specified");
        }

        logger.info("Creating language model provider: {}", provider);

        try {
            switch (provider.toLowerCase()) {
                case "self-hosted":
                    validateSelfHostedConfig(config);
                    return new SelfHostedModelProvider(config);

                case "openai":
                    validateOpenAIConfig(config);
                    return new OpenAIModelProvider(config);

                case "anthropic":
                    throw new ModelInitializationException(
                            "Anthropic provider is not yet implemented");

                case "gemini":
                    validateGeminiConfig(config);
                    return new GeminiModelProvider(config);

                default:
                    throw new InvalidConfigurationException(
                            "Unknown model provider: " + provider +
                                    ". Supported providers: self-hosted, openai, gemini");
            }
        } catch (IllegalArgumentException e) {
            throw new InvalidConfigurationException(
                    "Invalid configuration for provider " + provider + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ModelInitializationException(
                    "Failed to initialize model provider " + provider + ": " + e.getMessage(), e);
        }
    }

    /**
     * Validates self-hosted model configuration.
     *
     * @param config the model configuration
     * @throws InvalidConfigurationException if configuration is invalid
     */
    private void validateSelfHostedConfig(ModelConfig config) {
        ModelConfig.SelfHostedSettings settings = config.getSelfHosted();
        if (settings == null) {
            throw new InvalidConfigurationException(
                    "Self-hosted settings are required for self-hosted provider");
        }

        if (settings.getBaseUrl() == null || settings.getBaseUrl().isEmpty()) {
            throw new InvalidConfigurationException(
                    "Base URL is required for self-hosted provider");
        }

        if (settings.getModelName() == null || settings.getModelName().isEmpty()) {
            throw new InvalidConfigurationException(
                    "Model name is required for self-hosted provider");
        }
    }

    /**
     * Validates OpenAI model configuration.
     *
     * @param config the model configuration
     * @throws InvalidConfigurationException if configuration is invalid
     */
    private void validateOpenAIConfig(ModelConfig config) {
        ModelConfig.OpenAISettings settings = config.getOpenai();
        if (settings == null) {
            throw new InvalidConfigurationException(
                    "OpenAI settings are required for openai provider");
        }

        if (settings.getApiKey() == null || settings.getApiKey().isEmpty()) {
            throw new InvalidConfigurationException(
                    "API key is required for OpenAI provider");
        }

        if (settings.getModelName() == null || settings.getModelName().isEmpty()) {
            throw new InvalidConfigurationException(
                    "Model name is required for OpenAI provider");
        }
    }

    /**
     * Validates Gemini model configuration.
     *
     * @param config the model configuration
     * @throws InvalidConfigurationException if configuration is invalid
     */
    private void validateGeminiConfig(ModelConfig config) {
        ModelConfig.GeminiSettings settings = config.getGemini();
        if (settings == null) {
            throw new InvalidConfigurationException(
                    "Gemini settings are required for gemini provider");
        }

        if (settings.getLocation() == null || settings.getLocation().isEmpty()) {
            throw new InvalidConfigurationException(
                    "Location is required for Gemini provider");
        }

        if (settings.getModelName() == null || settings.getModelName().isEmpty()) {
            throw new InvalidConfigurationException(
                    "Model name is required for Gemini provider");
        }

        // Project ID can come from config or environment variable
        if ((settings.getProjectId() == null || settings.getProjectId().isEmpty()) &&
                (System.getenv("GOOGLE_CLOUD_PROJECT") == null ||
                        System.getenv("GOOGLE_CLOUD_PROJECT").isEmpty())) {
            throw new InvalidConfigurationException(
                    "Project ID is required for Gemini provider. " +
                            "Set via configuration or GOOGLE_CLOUD_PROJECT environment variable");
        }
    }
}
