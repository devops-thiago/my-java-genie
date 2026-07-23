package br.com.arquivolivre.myjavagenie.service;

import br.com.arquivolivre.myjavagenie.config.ModelConfig;
import br.com.arquivolivre.myjavagenie.config.QueryConfig;
import br.com.arquivolivre.myjavagenie.exception.ModelInvocationException;
import br.com.arquivolivre.myjavagenie.exception.ModelTimeoutException;
import br.com.arquivolivre.myjavagenie.exception.RagSystemException;
import br.com.arquivolivre.myjavagenie.model.*;
import br.com.arquivolivre.myjavagenie.util.LogSanitizer;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

/**
 * Service that orchestrates the query processing flow. Coordinates retrieval, prompt building,
 * generation, and token tracking.
 */
@Service
public class QueryService {
  private static final Logger logger = LoggerFactory.getLogger(QueryService.class);

  private final RetrievalEngine retrievalEngine;
  private final LanguageModelProvider languageModel;
  private final PromptBuilder promptBuilder;
  private final TokenUsageTracker tokenTracker;
  private final QueryConfig queryConfig;
  private final ModelConfig modelConfig;
  private final Tracer tracer;
  private final MetricsService metricsService;

  public QueryService(
      RetrievalEngine retrievalEngine,
      LanguageModelProvider languageModel,
      PromptBuilder promptBuilder,
      TokenUsageTracker tokenTracker,
      QueryConfig queryConfig,
      ModelConfig modelConfig,
      @Nullable Tracer tracer,
      @Nullable MetricsService metricsService) {
    this.retrievalEngine = retrievalEngine;
    this.languageModel = languageModel;
    this.promptBuilder = promptBuilder;
    this.tokenTracker = tokenTracker;
    this.queryConfig = queryConfig;
    this.modelConfig = modelConfig;
    this.tracer = tracer;
    this.metricsService = metricsService;
  }

  /**
   * Processes a user query and generates an answer with sources.
   *
   * @param question the user's question
   * @return QueryResponse containing answer, sources, token usage, and response time
   * @throws RagSystemException if query processing fails
   */
  public QueryResponse processQuery(String question) {
    if (question == null || question.trim().isEmpty()) {
      throw new IllegalArgumentException("Question cannot be null or empty");
    }

    // Create root span for query processing
    Span span = tracer != null ? tracer.spanBuilder("process-query").startSpan() : null;

    try (Scope scope = span != null ? span.makeCurrent() : null) {
      if (span != null) {
        span.setAttribute("query.text", truncateForLog(question));
        span.setAttribute("query.length", question.length());
      }

      logger.info("Processing query: {}", LogSanitizer.sanitize(truncateForLog(question)));
      long startTime = System.currentTimeMillis();

      try {
        // Step 1: Retrieve relevant chunks (with similarity scores)
        logger.debug("Step 1: Retrieving relevant document chunks");
        List<ScoredDocument> scoredChunks = retrievalEngine.retrieveRelevantChunks(question);

        // Handle case when no relevant documents are found
        if (scoredChunks.isEmpty()) {
          logger.warn(
              "No relevant documents found for query: {}",
              LogSanitizer.sanitize(truncateForLog(question)));
          if (span != null) {
            span.setAttribute("query.chunks_retrieved", 0);
            span.setAttribute("query.no_results", true);
          }
          return createNoResultsResponse(question, startTime);
        }

        logger.info("Retrieved {} relevant chunks", LogSanitizer.sanitize(scoredChunks.size()));
        if (span != null) {
          span.setAttribute("query.chunks_retrieved", scoredChunks.size());
        }

        List<DocumentChunk> relevantChunks =
            scoredChunks.stream().map(ScoredDocument::getChunk).collect(Collectors.toList());

        // Step 2: Build prompt with retrieved context
        logger.debug("Step 2: Building prompt with context");
        String prompt = buildPromptWithSpan(question, relevantChunks);
        logger.debug("Prompt built with {} characters", LogSanitizer.sanitize(prompt.length()));

        // Step 3: Generate answer using language model with timeout
        logger.debug("Step 3: Generating answer using language model");
        GenerationResponse generationResponse = generateWithTimeout(prompt);

        String answer = generationResponse.getText();
        logger.info(
            "Generated answer with {} tokens",
            LogSanitizer.sanitize(generationResponse.getTotalTokens()));

        if (span != null) {
          span.setAttribute("llm.tokens.prompt", generationResponse.getPromptTokens());
          span.setAttribute("llm.tokens.completion", generationResponse.getCompletionTokens());
          span.setAttribute("llm.tokens.total", generationResponse.getTotalTokens());
          span.setAttribute("llm.provider", languageModel.getProviderName());
        }

        // Step 4: Extract source references. When the model returned the fixed out-of-scope reply
        // (the retrieved context did not answer the question), do not attach irrelevant sources.
        logger.debug("Step 4: Extracting source references");
        boolean outOfScope =
            answer != null
                && answer.contains("I can only answer questions about the Java 25 documentation");
        List<SourceReference> sources =
            outOfScope ? List.of() : extractSourceReferences(scoredChunks);

        // Step 5: Track token usage
        logger.debug("Step 5: Recording token usage");
        TokenUsageMetrics tokenMetrics =
            new TokenUsageMetrics(
                generationResponse.getPromptTokens(),
                generationResponse.getCompletionTokens(),
                generationResponse.getTotalTokens());
        tokenTracker.recordTokenUsage(question, tokenMetrics);

        // Step 6: Construct and return response
        long responseTime = System.currentTimeMillis() - startTime;
        QueryResponse response = new QueryResponse(answer, sources, tokenMetrics, responseTime);

        logger.info("Query processed successfully in {}ms", LogSanitizer.sanitize(responseTime));
        if (span != null) {
          span.setAttribute("query.response_time_ms", responseTime);
          span.setStatus(StatusCode.OK);
        }

        // Record metrics
        if (metricsService != null) {
          metricsService.recordQuerySuccess(
              languageModel.getProviderName(),
              modelConfig.provider(),
              responseTime,
              generationResponse.getPromptTokens(),
              generationResponse.getCompletionTokens());
        }

        return response;

      } catch (ModelTimeoutException e) {
        long responseTime = System.currentTimeMillis() - startTime;
        logger.error(
            "Query timed out after {}ms: {}",
            LogSanitizer.sanitize(responseTime),
            LogSanitizer.sanitize(e.getMessage()));
        if (span != null) {
          span.setStatus(StatusCode.ERROR, "Query timeout");
          span.recordException(e);
        }
        if (metricsService != null) {
          metricsService.recordQueryError(
              languageModel.getProviderName(), modelConfig.provider(), "timeout", responseTime);
        }
        throw e;
      } catch (ModelInvocationException e) {
        long responseTime = System.currentTimeMillis() - startTime;
        logger.error(
            "Model invocation failed after {}ms: {}",
            LogSanitizer.sanitize(responseTime),
            LogSanitizer.sanitize(e.getMessage()),
            e);
        if (span != null) {
          span.setStatus(StatusCode.ERROR, "Model invocation failed");
          span.recordException(e);
        }
        if (metricsService != null) {
          metricsService.recordQueryError(
              languageModel.getProviderName(),
              modelConfig.provider(),
              "model_invocation",
              responseTime);
        }
        throw e;
      } catch (Exception e) {
        long responseTime = System.currentTimeMillis() - startTime;
        logger.error(
            "Unexpected error processing query after {}ms", LogSanitizer.sanitize(responseTime), e);
        if (span != null) {
          span.setStatus(StatusCode.ERROR, "Unexpected error");
          span.recordException(e);
        }
        if (metricsService != null) {
          metricsService.recordQueryError(
              languageModel.getProviderName(), modelConfig.provider(), "unexpected", responseTime);
        }
        throw new RagSystemException("Failed to process query: " + e.getMessage(), e);
      }
    } finally {
      if (span != null) {
        span.end();
      }
    }
  }

  /** Builds prompt with tracing instrumentation. */
  private String buildPromptWithSpan(String question, List<DocumentChunk> relevantChunks) {
    Span span = tracer != null ? tracer.spanBuilder("build-prompt").startSpan() : null;
    try (Scope scope = span != null ? span.makeCurrent() : null) {
      if (span != null) {
        span.setAttribute("prompt.chunks_count", relevantChunks.size());
      }
      String prompt = promptBuilder.buildPrompt(question, relevantChunks);
      if (span != null) {
        span.setAttribute("prompt.length", prompt.length());
        span.setStatus(StatusCode.OK);
      }
      return prompt;
    } catch (Exception e) {
      if (span != null) {
        span.setStatus(StatusCode.ERROR, "Failed to build prompt");
        span.recordException(e);
      }
      throw e;
    } finally {
      if (span != null) {
        span.end();
      }
    }
  }

  /**
   * Generates a response using the language model with timeout handling.
   *
   * @param prompt the prompt to send to the language model
   * @return GenerationResponse from the language model
   * @throws ModelTimeoutException if generation exceeds timeout
   * @throws ModelInvocationException if generation fails
   */
  private GenerationResponse generateWithTimeout(String prompt) {
    Span span = tracer != null ? tracer.spanBuilder("llm-generate").startSpan() : null;

    try (Scope scope = span != null ? span.makeCurrent() : null) {
      if (span != null) {
        span.setAttribute("llm.provider", languageModel.getProviderName());
        span.setAttribute("llm.temperature", modelConfig.temperature());
        span.setAttribute("llm.max_tokens", modelConfig.maxTokens());
        span.setAttribute("llm.prompt_length", prompt.length());
      }

      GenerationRequest request =
          new GenerationRequest(prompt, modelConfig.temperature(), modelConfig.maxTokens());

      int timeoutSeconds = queryConfig.timeoutSeconds();

      // Execute generation asynchronously with timeout
      CompletableFuture<GenerationResponse> future =
          CompletableFuture.supplyAsync(
              () -> {
                try {
                  return languageModel.generate(request);
                } catch (Exception e) {
                  logger.error("Language model generation failed", e);
                  throw new ModelInvocationException(
                      "Language model generation failed: " + e.getMessage(), e);
                }
              });

      try {
        GenerationResponse response = future.get(timeoutSeconds, TimeUnit.SECONDS);
        if (span != null) {
          span.setStatus(StatusCode.OK);
        }
        return response;
      } catch (TimeoutException e) {
        future.cancel(true);
        logger.error(
            "Language model generation timed out after {} seconds",
            LogSanitizer.sanitize(timeoutSeconds));
        if (span != null) {
          span.setStatus(StatusCode.ERROR, "Timeout");
          span.recordException(e);
        }
        throw new ModelTimeoutException(
            String.format("Language model generation timed out after %d seconds", timeoutSeconds),
            e);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        logger.error("Language model generation interrupted", e);
        if (span != null) {
          span.setStatus(StatusCode.ERROR, "Interrupted");
          span.recordException(e);
        }
        throw new ModelInvocationException("Language model generation interrupted", e);
      } catch (ExecutionException e) {
        Throwable cause = e.getCause();
        if (span != null) {
          span.setStatus(StatusCode.ERROR, "Execution failed");
          span.recordException(cause);
        }
        if (cause instanceof ModelInvocationException) {
          throw (ModelInvocationException) cause;
        }
        logger.error("Language model generation execution failed", e);
        throw new ModelInvocationException(
            "Language model generation failed: " + cause.getMessage(), cause);
      }
    } finally {
      if (span != null) {
        span.end();
      }
    }
  }

  /**
   * Creates a response when no relevant documents are found.
   *
   * @param question the user's question
   * @param startTime the query start time
   * @return QueryResponse indicating no results found
   */
  private QueryResponse createNoResultsResponse(String question, long startTime) {
    String answer =
        "I couldn't find any relevant information in the Java 25 documentation "
            + "to answer your question. Please try rephrasing your question or asking about "
            + "a different topic.";

    long responseTime = System.currentTimeMillis() - startTime;
    TokenUsageMetrics emptyMetrics = new TokenUsageMetrics(0, 0, 0);

    // Still track the query even though no results were found
    tokenTracker.recordTokenUsage(question, emptyMetrics);

    // Record metrics for no results
    if (metricsService != null) {
      metricsService.recordQueryNoResults(responseTime);
    }

    return new QueryResponse(answer, List.of(), emptyMetrics, responseTime);
  }

  /**
   * Extracts source references from document chunks.
   *
   * @param scoredChunks the retrieved chunks with similarity scores
   * @return list of source references
   */
  private List<SourceReference> extractSourceReferences(List<ScoredDocument> scoredChunks) {
    return scoredChunks.stream()
        .map(
            scored -> {
              DocumentChunk chunk = scored.getChunk();
              DocumentMetadata metadata = chunk != null ? chunk.getMetadata() : null;

              String filename =
                  metadata != null && metadata.getSourceFile() != null
                      ? metadata.getSourceFile()
                      : "Unknown";

              String section =
                  metadata != null && metadata.getSection() != null ? metadata.getSection() : null;

              int chunkIndex = metadata != null ? metadata.getChunkIndex() : 0;

              return new SourceReference(
                  filename, section, chunkIndex, scored.getSimilarityScore());
            })
        .collect(Collectors.toList());
  }

  /**
   * Truncates a string for logging purposes.
   *
   * @param text the text to truncate
   * @return truncated text
   */
  private String truncateForLog(String text) {
    if (text == null) {
      return "";
    }
    return text.length() > 100 ? text.substring(0, 100) + "..." : text;
  }
}
