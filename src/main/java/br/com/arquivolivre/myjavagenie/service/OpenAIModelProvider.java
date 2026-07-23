package br.com.arquivolivre.myjavagenie.service;

import br.com.arquivolivre.myjavagenie.config.ModelConfig;
import br.com.arquivolivre.myjavagenie.exception.ModelInvocationException;
import br.com.arquivolivre.myjavagenie.exception.ModelTimeoutException;
import br.com.arquivolivre.myjavagenie.model.GenerationRequest;
import br.com.arquivolivre.myjavagenie.model.GenerationResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Language model provider for OpenAI API. Implements token usage tracking from API responses. */
public class OpenAIModelProvider implements LanguageModelProvider {

  private static final Logger logger = LoggerFactory.getLogger(OpenAIModelProvider.class);
  private static final int MAX_RETRIES = 3;
  private static final long INITIAL_RETRY_DELAY_MS = 1000;

  private final OpenAiChatModel chatModel;
  private final String modelName;
  private final int timeoutSeconds;

  /**
   * Creates an OpenAI model provider with the given configuration.
   *
   * @param config the model configuration
   */
  public OpenAIModelProvider(ModelConfig config) {
    ModelConfig.OpenAISettings settings = config.getOpenai();
    if (settings == null) {
      throw new IllegalArgumentException("OpenAI settings are required");
    }

    if (settings.getApiKey() == null || settings.getApiKey().isEmpty()) {
      throw new IllegalArgumentException("OpenAI API key is required");
    }

    this.modelName = settings.getModelName();
    this.timeoutSeconds = settings.getTimeoutSeconds() != null ? settings.getTimeoutSeconds() : 60;

    logger.info("Initializing OpenAI model provider: {}", modelName);

    var builder =
        OpenAiChatModel.builder()
            .apiKey(settings.getApiKey())
            .modelName(modelName)
            .temperature(config.getTemperature())
            .maxTokens(config.getMaxTokens())
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .logRequests(false)
            .logResponses(false);

    // Allow custom base URL for testing
    if (settings.getBaseUrl() != null && !settings.getBaseUrl().isEmpty()) {
      builder.baseUrl(settings.getBaseUrl());
      logger.info("Using custom OpenAI base URL: {}", settings.getBaseUrl());
    }

    this.chatModel = builder.build();
  }

  @Override
  public GenerationResponse generate(GenerationRequest request) {
    logger.debug(
        "Generating response for prompt with {} characters",
        request.getPrompt() != null ? request.getPrompt().length() : 0);

    int attempt = 0;
    Exception lastException = null;

    while (attempt < MAX_RETRIES) {
      try {
        long startTime = System.currentTimeMillis();

        String responseText = chatModel.generate(request.getPrompt());

        long duration = System.currentTimeMillis() - startTime;
        logger.debug("Generation completed in {}ms", duration);

        // OpenAI basic chat model doesn't provide token usage in simple generate()
        // Estimate tokens (rough approximation: 1 token ≈ 4 characters)
        int promptTokens = estimateTokens(request.getPrompt());
        int completionTokens = estimateTokens(responseText);

        logger.info(
            "OpenAI estimated token usage - prompt: {}, completion: {}, total: {}",
            promptTokens,
            completionTokens,
            promptTokens + completionTokens);

        return new GenerationResponse(
            responseText, promptTokens, completionTokens, promptTokens + completionTokens);

      } catch (Exception e) {
        attempt++;
        lastException = e;

        if (isTimeoutException(e)) {
          logger.error("Model invocation timed out after {} seconds", timeoutSeconds);
          throw new ModelTimeoutException(
              "Model generation timed out after " + timeoutSeconds + " seconds", e);
        }

        if (isRateLimitException(e)) {
          logger.warn("Rate limit exceeded, retrying with exponential backoff");
        }

        if (attempt < MAX_RETRIES) {
          long delay = INITIAL_RETRY_DELAY_MS * (long) Math.pow(2, attempt - 1);
          logger.warn(
              "Model invocation failed (attempt {}/{}), retrying in {}ms: {}",
              attempt,
              MAX_RETRIES,
              delay,
              e.getMessage());

          try {
            Thread.sleep(delay);
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new ModelInvocationException("Model invocation interrupted during retry", ie);
          }
        } else {
          logger.error("Model invocation failed after {} attempts", MAX_RETRIES);
        }
      }
    }

    throw new ModelInvocationException(
        "Failed to generate response after " + MAX_RETRIES + " attempts", lastException);
  }

  @Override
  public boolean isAvailable() {
    try {
      // Try a simple generation to check availability
      String response = chatModel.generate("test");
      return response != null;
    } catch (Exception e) {
      logger.warn("Model availability check failed: {}", e.getMessage());
      return false;
    }
  }

  /**
   * Estimates the number of tokens in a text string. Uses a simple heuristic: 1 token ≈ 4
   * characters.
   *
   * @param text the text to estimate
   * @return estimated token count
   */
  private int estimateTokens(String text) {
    if (text == null || text.isEmpty()) {
      return 0;
    }
    return (int) Math.ceil(text.length() / 4.0);
  }

  @Override
  public String getProviderName() {
    return "openai";
  }

  /**
   * Checks if an exception is a timeout exception.
   *
   * @param e the exception to check
   * @return true if it's a timeout exception
   */
  private boolean isTimeoutException(Exception e) {
    return e instanceof java.util.concurrent.TimeoutException
        || e.getCause() instanceof java.util.concurrent.TimeoutException
        || (e.getMessage() != null && e.getMessage().toLowerCase().contains("timeout"));
  }

  /**
   * Checks if an exception is a rate limit exception.
   *
   * @param e the exception to check
   * @return true if it's a rate limit exception
   */
  private boolean isRateLimitException(Exception e) {
    return e.getMessage() != null
        && (e.getMessage().toLowerCase().contains("rate limit")
            || e.getMessage().toLowerCase().contains("429"));
  }
}
