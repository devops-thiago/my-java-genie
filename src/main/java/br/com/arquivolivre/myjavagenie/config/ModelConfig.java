package br.com.arquivolivre.myjavagenie.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Immutable configuration properties for language model settings. Supports self-hosted, OpenAI,
 * Anthropic, and Gemini model providers.
 *
 * <p>Populated through Spring Boot constructor binding; construct directly with the canonical
 * constructor in tests.
 */
@ConfigurationProperties(prefix = "model")
@Validated
public record ModelConfig(
    @NotBlank(message = "Model provider type must be specified") String provider,
    @Valid SelfHostedSettings selfHosted,
    @Valid OpenAISettings openai,
    @Valid AnthropicSettings anthropic,
    @Valid GeminiSettings gemini,
    @NotNull(message = "Temperature must be specified") Double temperature,
    @NotNull(message = "Max tokens must be specified")
        @Positive(message = "Max tokens must be positive")
        Integer maxTokens) {

  /** Configuration for self-hosted models (e.g., Ollama). */
  public record SelfHostedSettings(
      @NotBlank(message = "Self-hosted base URL must be specified") String baseUrl,
      @NotBlank(message = "Self-hosted model name must be specified") String modelName,
      @Positive(message = "Timeout seconds must be positive") Integer timeoutSeconds) {}

  /** Configuration for OpenAI API. */
  public record OpenAISettings(
      String apiKey,
      @NotBlank(message = "OpenAI model name must be specified") String modelName,
      String baseUrl,
      @Positive(message = "Timeout seconds must be positive") Integer timeoutSeconds) {}

  /** Configuration for Anthropic API. */
  public record AnthropicSettings(
      String apiKey,
      @NotBlank(message = "Anthropic model name must be specified") String modelName,
      @Positive(message = "Timeout seconds must be positive") Integer timeoutSeconds) {}

  /** Configuration for Google Gemini API via Vertex AI. */
  public record GeminiSettings(
      String projectId,
      @NotBlank(message = "Gemini location must be specified") String location,
      @NotBlank(message = "Gemini model name must be specified") String modelName,
      String apiKey,
      @Positive(message = "Timeout seconds must be positive") Integer timeoutSeconds) {}
}
