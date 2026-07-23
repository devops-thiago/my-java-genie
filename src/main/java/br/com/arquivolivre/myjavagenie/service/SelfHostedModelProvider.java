package br.com.arquivolivre.myjavagenie.service;

import br.com.arquivolivre.myjavagenie.config.ModelConfig;
import br.com.arquivolivre.myjavagenie.exception.ModelInvocationException;
import br.com.arquivolivre.myjavagenie.exception.ModelTimeoutException;
import br.com.arquivolivre.myjavagenie.model.GenerationRequest;
import br.com.arquivolivre.myjavagenie.model.GenerationResponse;
import br.com.arquivolivre.myjavagenie.util.LogSanitizer;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import java.time.Duration;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Language model provider for self-hosted models using Ollama. Implements retry logic and error
 * handling for connection failures.
 */
public final class SelfHostedModelProvider implements LanguageModelProvider {

  private static final Logger logger = LoggerFactory.getLogger(SelfHostedModelProvider.class);
  private static final int MAX_RETRIES = 3;
  private static final long INITIAL_RETRY_DELAY_MS = 1000;

  private final ChatModel chatModel;
  private final String modelName;
  private final int timeoutSeconds;

  /**
   * Creates a self-hosted model provider with the given configuration.
   *
   * @param config the model configuration
   */
  public SelfHostedModelProvider(ModelConfig config) {
    ModelConfig.SelfHostedSettings settings = config.selfHosted();
    if (settings == null) {
      throw new IllegalArgumentException("Self-hosted settings are required");
    }

    this.modelName = settings.modelName();
    this.timeoutSeconds = settings.timeoutSeconds() != null ? settings.timeoutSeconds() : 60;

    logger.info(
        "Initializing self-hosted model provider: {} at {}",
        LogSanitizer.sanitize(modelName),
        LogSanitizer.sanitize(settings.baseUrl()));

    this.chatModel =
        OllamaChatModel.builder()
            .baseUrl(settings.baseUrl())
            .modelName(modelName)
            .temperature(config.temperature())
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .build();
  }

  @Override
  public GenerationResponse generate(GenerationRequest request) {
    logger.debug(
        "Generating response for prompt with {} characters",
        LogSanitizer.sanitize(request.getPrompt() != null ? request.getPrompt().length() : 0));

    int attempt = 0;
    Exception lastException = null;

    while (attempt < MAX_RETRIES) {
      try {
        long startTime = System.currentTimeMillis();

        String response = chatModel.chat(request.getPrompt());

        long duration = System.currentTimeMillis() - startTime;
        logger.debug("Generation completed in {}ms", LogSanitizer.sanitize(duration));

        // Ollama doesn't provide token usage in the basic response
        // Estimate tokens (rough approximation: 1 token ≈ 4 characters)
        int promptTokens = estimateTokens(request.getPrompt());
        int completionTokens = estimateTokens(response);

        return new GenerationResponse(
            response, promptTokens, completionTokens, promptTokens + completionTokens);

      } catch (Exception e) {
        attempt++;
        lastException = e;

        if (isTimeoutException(e)) {
          logger.error(
              "Model invocation timed out after {} seconds", LogSanitizer.sanitize(timeoutSeconds));
          throw new ModelTimeoutException(
              "Model generation timed out after " + timeoutSeconds + " seconds", e);
        }

        if (attempt < MAX_RETRIES) {
          long delay = INITIAL_RETRY_DELAY_MS * attempt;
          logger.warn(
              "Model invocation failed (attempt {}/{}), retrying in {}ms: {}",
              LogSanitizer.sanitize(attempt),
              MAX_RETRIES,
              LogSanitizer.sanitize(delay),
              LogSanitizer.sanitize(e.getMessage()));

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
      String testResponse = chatModel.chat("test");
      return testResponse != null;
    } catch (Exception e) {
      logger.warn("Model availability check failed: {}", LogSanitizer.sanitize(e.getMessage()));
      return false;
    }
  }

  @Override
  public String getProviderName() {
    return "self-hosted";
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

  /**
   * Checks if an exception is a timeout exception.
   *
   * @param e the exception to check
   * @return true if it's a timeout exception
   */
  private boolean isTimeoutException(Exception e) {
    return e instanceof java.util.concurrent.TimeoutException
        || e.getCause() instanceof java.util.concurrent.TimeoutException
        || e.getMessage() != null && e.getMessage().toLowerCase(Locale.ROOT).contains("timeout");
  }
}
