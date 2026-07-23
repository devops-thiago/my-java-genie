package br.com.arquivolivre.myjavagenie.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for language model settings. Supports self-hosted, OpenAI, and Anthropic
 * model providers.
 */
@ConfigurationProperties(prefix = "model")
@Validated
public class ModelConfig {

  @NotBlank(message = "Model provider type must be specified")
  private String provider;

  @Valid private SelfHostedSettings selfHosted;

  @Valid private OpenAISettings openai;

  @Valid private AnthropicSettings anthropic;

  @Valid private GeminiSettings gemini;

  @NotNull(message = "Temperature must be specified")
  private Double temperature;

  @NotNull(message = "Max tokens must be specified")
  @Positive(message = "Max tokens must be positive")
  private Integer maxTokens;

  // Getters and Setters

  public String getProvider() {
    return provider;
  }

  public void setProvider(String provider) {
    this.provider = provider;
  }

  public SelfHostedSettings getSelfHosted() {
    return selfHosted;
  }

  public void setSelfHosted(SelfHostedSettings selfHosted) {
    this.selfHosted = selfHosted;
  }

  public OpenAISettings getOpenai() {
    return openai;
  }

  public void setOpenai(OpenAISettings openai) {
    this.openai = openai;
  }

  public AnthropicSettings getAnthropic() {
    return anthropic;
  }

  public void setAnthropic(AnthropicSettings anthropic) {
    this.anthropic = anthropic;
  }

  public GeminiSettings getGemini() {
    return gemini;
  }

  public void setGemini(GeminiSettings gemini) {
    this.gemini = gemini;
  }

  public Double getTemperature() {
    return temperature;
  }

  public void setTemperature(Double temperature) {
    this.temperature = temperature;
  }

  public Integer getMaxTokens() {
    return maxTokens;
  }

  public void setMaxTokens(Integer maxTokens) {
    this.maxTokens = maxTokens;
  }

  /** Configuration for self-hosted models (e.g., Ollama). */
  public static class SelfHostedSettings {
    @NotBlank(message = "Self-hosted base URL must be specified")
    private String baseUrl;

    @NotBlank(message = "Self-hosted model name must be specified")
    private String modelName;

    @Positive(message = "Timeout seconds must be positive")
    private Integer timeoutSeconds;

    public String getBaseUrl() {
      return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
      this.baseUrl = baseUrl;
    }

    public String getModelName() {
      return modelName;
    }

    public void setModelName(String modelName) {
      this.modelName = modelName;
    }

    public Integer getTimeoutSeconds() {
      return timeoutSeconds;
    }

    public void setTimeoutSeconds(Integer timeoutSeconds) {
      this.timeoutSeconds = timeoutSeconds;
    }
  }

  /** Configuration for OpenAI API. */
  public static class OpenAISettings {
    private String apiKey;

    @NotBlank(message = "OpenAI model name must be specified")
    private String modelName;

    private String baseUrl;

    @Positive(message = "Timeout seconds must be positive")
    private Integer timeoutSeconds;

    public String getApiKey() {
      return apiKey;
    }

    public void setApiKey(String apiKey) {
      this.apiKey = apiKey;
    }

    public String getModelName() {
      return modelName;
    }

    public void setModelName(String modelName) {
      this.modelName = modelName;
    }

    public String getBaseUrl() {
      return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
      this.baseUrl = baseUrl;
    }

    public Integer getTimeoutSeconds() {
      return timeoutSeconds;
    }

    public void setTimeoutSeconds(Integer timeoutSeconds) {
      this.timeoutSeconds = timeoutSeconds;
    }
  }

  /** Configuration for Anthropic API. */
  public static class AnthropicSettings {
    private String apiKey;

    @NotBlank(message = "Anthropic model name must be specified")
    private String modelName;

    @Positive(message = "Timeout seconds must be positive")
    private Integer timeoutSeconds;

    public String getApiKey() {
      return apiKey;
    }

    public void setApiKey(String apiKey) {
      this.apiKey = apiKey;
    }

    public String getModelName() {
      return modelName;
    }

    public void setModelName(String modelName) {
      this.modelName = modelName;
    }

    public Integer getTimeoutSeconds() {
      return timeoutSeconds;
    }

    public void setTimeoutSeconds(Integer timeoutSeconds) {
      this.timeoutSeconds = timeoutSeconds;
    }
  }

  /** Configuration for Google Gemini API via Vertex AI. */
  public static class GeminiSettings {
    private String projectId;

    @NotBlank(message = "Gemini location must be specified")
    private String location;

    @NotBlank(message = "Gemini model name must be specified")
    private String modelName;

    private String apiKey;

    @Positive(message = "Timeout seconds must be positive")
    private Integer timeoutSeconds;

    public String getProjectId() {
      return projectId;
    }

    public void setProjectId(String projectId) {
      this.projectId = projectId;
    }

    public String getLocation() {
      return location;
    }

    public void setLocation(String location) {
      this.location = location;
    }

    public String getModelName() {
      return modelName;
    }

    public void setModelName(String modelName) {
      this.modelName = modelName;
    }

    public String getApiKey() {
      return apiKey;
    }

    public void setApiKey(String apiKey) {
      this.apiKey = apiKey;
    }

    public Integer getTimeoutSeconds() {
      return timeoutSeconds;
    }

    public void setTimeoutSeconds(Integer timeoutSeconds) {
      this.timeoutSeconds = timeoutSeconds;
    }
  }
}
