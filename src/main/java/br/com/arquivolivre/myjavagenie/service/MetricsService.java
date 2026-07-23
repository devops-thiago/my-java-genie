package br.com.arquivolivre.myjavagenie.service;

import br.com.arquivolivre.myjavagenie.util.LogSanitizer;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

/**
 * Service for recording custom OpenTelemetry metrics for the RAG system. Tracks query performance,
 * token usage, and error rates.
 */
@Service
@ConditionalOnBean(Meter.class)
public class MetricsService {
  private static final Logger logger = LoggerFactory.getLogger(MetricsService.class);

  // Attribute keys
  private static final AttributeKey<String> PROVIDER_KEY = AttributeKey.stringKey("provider");
  private static final AttributeKey<String> MODEL_KEY = AttributeKey.stringKey("model");
  private static final AttributeKey<String> STATUS_KEY = AttributeKey.stringKey("status");
  private static final AttributeKey<String> ERROR_TYPE_KEY = AttributeKey.stringKey("error_type");

  // Metrics
  private final DoubleHistogram queryDuration;
  private final LongCounter queryTotal;
  private final LongCounter queryErrors;
  private final DoubleHistogram tokensPrompt;
  private final DoubleHistogram tokensCompletion;
  private final DoubleHistogram tokensCost;

  public MetricsService(@Nullable Meter meter) {
    if (meter == null) {
      logger.warn("Meter not available, metrics will not be recorded");
      this.queryDuration = null;
      this.queryTotal = null;
      this.queryErrors = null;
      this.tokensPrompt = null;
      this.tokensCompletion = null;
      this.tokensCost = null;
      return;
    }

    logger.info("Initializing MetricsService with OpenTelemetry Meter");

    // Query duration histogram (in milliseconds)
    this.queryDuration =
        meter
            .histogramBuilder("rag.query.duration")
            .setDescription("Duration of query processing in milliseconds")
            .setUnit("ms")
            .build();

    // Query total counter
    this.queryTotal =
        meter
            .counterBuilder("rag.query.total")
            .setDescription("Total number of queries processed")
            .build();

    // Query errors counter
    this.queryErrors =
        meter
            .counterBuilder("rag.query.errors")
            .setDescription("Total number of query errors")
            .build();

    // Token usage histograms
    this.tokensPrompt =
        meter
            .histogramBuilder("rag.tokens.prompt")
            .setDescription("Number of prompt tokens used per query")
            .setUnit("tokens")
            .build();

    this.tokensCompletion =
        meter
            .histogramBuilder("rag.tokens.completion")
            .setDescription("Number of completion tokens generated per query")
            .setUnit("tokens")
            .build();

    // Token cost counter (in USD)
    this.tokensCost =
        meter
            .histogramBuilder("rag.tokens.cost")
            .setDescription("Estimated cost of tokens in USD")
            .setUnit("USD")
            .build();

    logger.info("MetricsService initialized successfully");
  }

  /**
   * Records a successful query with duration and token metrics.
   *
   * @param provider the LLM provider name
   * @param model the model name
   * @param durationMs query duration in milliseconds
   * @param promptTokens number of prompt tokens
   * @param completionTokens number of completion tokens
   */
  public void recordQuerySuccess(
      String provider, String model, long durationMs, int promptTokens, int completionTokens) {
    if (!isEnabled()) {
      return;
    }

    Attributes attributes =
        Attributes.of(
            PROVIDER_KEY, provider,
            MODEL_KEY, model,
            STATUS_KEY, "success");

    queryDuration.record(durationMs, attributes);
    queryTotal.add(1, attributes);
    tokensPrompt.record(promptTokens, attributes);
    tokensCompletion.record(completionTokens, attributes);

    // Estimate cost (simplified - actual costs vary by provider)
    double estimatedCost = estimateCost(provider, promptTokens, completionTokens);
    tokensCost.record(estimatedCost, attributes);

    logger.debug(
        "Recorded successful query metrics: provider={}, model={}, duration={}ms, "
            + "promptTokens={}, completionTokens={}, cost=${}",
        LogSanitizer.sanitize(provider),
        LogSanitizer.sanitize(model),
        LogSanitizer.sanitize(durationMs),
        LogSanitizer.sanitize(promptTokens),
        LogSanitizer.sanitize(completionTokens),
        LogSanitizer.sanitize(estimatedCost));
  }

  /**
   * Records a query error.
   *
   * @param provider the LLM provider name
   * @param model the model name
   * @param errorType the type of error
   * @param durationMs query duration in milliseconds before error
   */
  public void recordQueryError(String provider, String model, String errorType, long durationMs) {
    if (!isEnabled()) {
      return;
    }

    Attributes attributes =
        Attributes.of(
            PROVIDER_KEY, provider,
            MODEL_KEY, model,
            STATUS_KEY, "error",
            ERROR_TYPE_KEY, errorType);

    queryDuration.record(durationMs, attributes);
    queryTotal.add(1, attributes);
    queryErrors.add(1, attributes);

    logger.debug(
        "Recorded query error metrics: provider={}, model={}, errorType={}, duration={}ms",
        LogSanitizer.sanitize(provider),
        LogSanitizer.sanitize(model),
        LogSanitizer.sanitize(errorType),
        LogSanitizer.sanitize(durationMs));
  }

  /**
   * Records a query with no results found.
   *
   * @param durationMs query duration in milliseconds
   */
  public void recordQueryNoResults(long durationMs) {
    if (!isEnabled()) {
      return;
    }

    Attributes attributes =
        Attributes.of(
            PROVIDER_KEY, "none",
            MODEL_KEY, "none",
            STATUS_KEY, "no_results");

    queryDuration.record(durationMs, attributes);
    queryTotal.add(1, attributes);

    logger.debug(
        "Recorded no results query metrics: duration={}ms", LogSanitizer.sanitize(durationMs));
  }

  /**
   * Estimates the cost of token usage based on provider pricing. This is a simplified estimation -
   * actual costs may vary.
   *
   * @param provider the LLM provider
   * @param promptTokens number of prompt tokens
   * @param completionTokens number of completion tokens
   * @return estimated cost in USD
   */
  private double estimateCost(String provider, int promptTokens, int completionTokens) {
    // Simplified cost estimation (per 1000 tokens)
    // These are approximate rates and should be updated based on actual pricing
    double promptCostPer1k;
    double completionCostPer1k;

    switch (provider.toLowerCase(Locale.ROOT)) {
      case "openai":
        // GPT-4 pricing (approximate)
        promptCostPer1k = 0.03;
        completionCostPer1k = 0.06;
        break;
      case "anthropic":
        // Claude pricing (approximate)
        promptCostPer1k = 0.008;
        completionCostPer1k = 0.024;
        break;
      case "gemini":
        // Gemini pricing (approximate)
        promptCostPer1k = 0.00025;
        completionCostPer1k = 0.0005;
        break;
      case "self-hosted":
      default:
        // Self-hosted models have no API cost
        return 0.0;
    }

    double promptCost = (promptTokens / 1000.0) * promptCostPer1k;
    double completionCost = (completionTokens / 1000.0) * completionCostPer1k;

    return promptCost + completionCost;
  }

  /** Checks if metrics recording is enabled. */
  private boolean isEnabled() {
    return queryDuration != null;
  }
}
