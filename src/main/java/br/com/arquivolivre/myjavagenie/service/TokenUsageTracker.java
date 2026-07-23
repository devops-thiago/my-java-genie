package br.com.arquivolivre.myjavagenie.service;

import br.com.arquivolivre.myjavagenie.model.TokenUsageMetrics;
import br.com.arquivolivre.myjavagenie.util.LogSanitizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for tracking and monitoring token usage across queries. Provides methods to record token
 * consumption, calculate cumulative usage, and retrieve usage statistics for cost analysis.
 */
@Service
public class TokenUsageTracker {
  private static final Logger logger = LoggerFactory.getLogger(TokenUsageTracker.class);

  private final AtomicLong totalPromptTokens = new AtomicLong(0);
  private final AtomicLong totalCompletionTokens = new AtomicLong(0);
  private final AtomicLong totalTokens = new AtomicLong(0);
  private final AtomicInteger queryCount = new AtomicInteger(0);
  private final ConcurrentHashMap<String, List<QueryTokenRecord>> queryHistory =
      new ConcurrentHashMap<>();

  /**
   * Records token usage for a specific query.
   *
   * @param query the user query
   * @param metrics the token usage metrics
   */
  public void recordTokenUsage(String query, TokenUsageMetrics metrics) {
    if (metrics == null) {
      logger.warn(
          "Attempted to record null token metrics for query: {}", LogSanitizer.sanitize(query));
      return;
    }

    // Update cumulative totals
    totalPromptTokens.addAndGet(metrics.getPromptTokens());
    totalCompletionTokens.addAndGet(metrics.getCompletionTokens());
    totalTokens.addAndGet(metrics.getTotalTokens());
    queryCount.incrementAndGet();

    // Store query record
    QueryTokenRecord record = new QueryTokenRecord(query, metrics, LocalDateTime.now());

    String queryKey = generateQueryKey(query);
    queryHistory.computeIfAbsent(queryKey, k -> new ArrayList<>()).add(record);

    // Log token usage with structured format for metrics analysis
    logger.info(
        "TOKEN_METRICS | query=\"{}\" | promptTokens={} | completionTokens={} | totalTokens={} | timestamp={}",
        LogSanitizer.sanitize(truncateQuery(query)),
        LogSanitizer.sanitize(metrics.getPromptTokens()),
        LogSanitizer.sanitize(metrics.getCompletionTokens()),
        LogSanitizer.sanitize(metrics.getTotalTokens()),
        LogSanitizer.sanitize(record.timestamp()));
  }

  /**
   * Retrieves cumulative token usage statistics.
   *
   * @return cumulative token usage metrics
   */
  public TokenUsageMetrics getCumulativeUsage() {
    return new TokenUsageMetrics(
        (int) totalPromptTokens.get(), (int) totalCompletionTokens.get(), (int) totalTokens.get());
  }

  /**
   * Retrieves the total number of queries processed.
   *
   * @return total query count
   */
  public int getQueryCount() {
    return queryCount.get();
  }

  /**
   * Calculates the average tokens per query.
   *
   * @return average total tokens per query, or 0 if no queries processed
   */
  public double getAverageTokensPerQuery() {
    int count = queryCount.get();
    if (count == 0) {
      return 0.0;
    }
    return (double) totalTokens.get() / count;
  }

  /**
   * Retrieves usage statistics as a formatted summary.
   *
   * @return usage statistics summary
   */
  public UsageStatistics getUsageStatistics() {
    return new UsageStatistics(
        queryCount.get(),
        totalPromptTokens.get(),
        totalCompletionTokens.get(),
        totalTokens.get(),
        getAverageTokensPerQuery());
  }

  /**
   * Retrieves query history for a specific query pattern.
   *
   * @param query the query to look up
   * @return list of token records for the query
   */
  public List<QueryTokenRecord> getQueryHistory(String query) {
    String queryKey = generateQueryKey(query);
    return new ArrayList<>(queryHistory.getOrDefault(queryKey, new ArrayList<>()));
  }

  /** Resets all tracking statistics. Useful for testing or periodic resets. */
  public void reset() {
    totalPromptTokens.set(0);
    totalCompletionTokens.set(0);
    totalTokens.set(0);
    queryCount.set(0);
    queryHistory.clear();
    logger.info("Token usage tracker reset");
  }

  /** Logs a summary of current usage statistics. */
  public void logUsageSummary() {
    UsageStatistics stats = getUsageStatistics();
    logger.info(
        "TOKEN_SUMMARY | queries={} | totalTokens={} | avgTokensPerQuery={} | "
            + "promptTokens={} | completionTokens={}",
        LogSanitizer.sanitize(stats.queryCount()),
        LogSanitizer.sanitize(stats.totalTokens()),
        LogSanitizer.sanitize(String.format("%.2f", stats.averageTokensPerQuery())),
        LogSanitizer.sanitize(stats.totalPromptTokens()),
        LogSanitizer.sanitize(stats.totalCompletionTokens()));
  }

  private String generateQueryKey(String query) {
    // Normalize query for grouping similar queries
    return query.toLowerCase(Locale.ROOT).trim();
  }

  private String truncateQuery(String query) {
    if (query == null) {
      return "";
    }
    return query.length() > 50 ? query.substring(0, 50) + "..." : query;
  }

  /** Record of token usage for a specific query execution. */
  public record QueryTokenRecord(String query, TokenUsageMetrics metrics, LocalDateTime timestamp) {

    /** Defensively copies the mutable metrics so the record cannot be mutated through it. */
    public QueryTokenRecord {
      metrics = TokenUsageMetrics.copyOf(metrics);
    }

    @Override
    public TokenUsageMetrics metrics() {
      return TokenUsageMetrics.copyOf(metrics);
    }

    @Override
    public String toString() {
      return "QueryTokenRecord{"
          + "query='"
          + query
          + '\''
          + ", metrics="
          + metrics
          + ", timestamp="
          + timestamp
          + '}';
    }
  }

  /** Aggregated usage statistics. */
  public record UsageStatistics(
      int queryCount,
      long totalPromptTokens,
      long totalCompletionTokens,
      long totalTokens,
      double averageTokensPerQuery) {

    @Override
    public String toString() {
      return "UsageStatistics{"
          + "queryCount="
          + queryCount
          + ", totalPromptTokens="
          + totalPromptTokens
          + ", totalCompletionTokens="
          + totalCompletionTokens
          + ", totalTokens="
          + totalTokens
          + ", averageTokensPerQuery="
          + averageTokensPerQuery
          + '}';
    }
  }
}
