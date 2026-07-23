package br.com.arquivolivre.myjavagenie.service;

import br.com.arquivolivre.myjavagenie.config.QueryConfig;
import br.com.arquivolivre.myjavagenie.exception.EmbeddingGenerationException;
import br.com.arquivolivre.myjavagenie.exception.VectorDbQueryException;
import br.com.arquivolivre.myjavagenie.model.ScoredDocument;
import br.com.arquivolivre.myjavagenie.repository.VectorRepository;
import br.com.arquivolivre.myjavagenie.util.LogSanitizer;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

/**
 * Service responsible for retrieving relevant document chunks based on user queries. Uses embedding
 * generation and vector similarity search to find the most relevant documents.
 */
@Service
public class RetrievalEngine {
  private static final Logger logger = LoggerFactory.getLogger(RetrievalEngine.class);

  private final VectorRepository vectorRepository;
  private final EmbeddingModelProvider embeddingModel;
  private final QueryConfig queryConfig;
  private final Tracer tracer;

  public RetrievalEngine(
      VectorRepository vectorRepository,
      EmbeddingModelProvider embeddingModel,
      QueryConfig queryConfig,
      @Nullable Tracer tracer) {
    this.vectorRepository = vectorRepository;
    this.embeddingModel = embeddingModel;
    this.queryConfig = queryConfig;
    this.tracer = tracer;
  }

  /**
   * Retrieves relevant document chunks for a given query.
   *
   * @param query the user's question or search query
   * @return list of relevant document chunks sorted by relevance (highest first)
   * @throws EmbeddingGenerationException if query embedding generation fails
   * @throws VectorDbQueryException if vector database search fails
   */
  public List<ScoredDocument> retrieveRelevantChunks(String query) {
    logger.debug("Retrieving relevant chunks for query: {}", LogSanitizer.sanitize(query));

    // Generate embedding for the query with tracing
    Span embedSpan = tracer != null ? tracer.spanBuilder("embed-query").startSpan() : null;
    float[] queryEmbedding;
    try (Scope embedScope = embedSpan != null ? embedSpan.makeCurrent() : null) {
      if (embedSpan != null) {
        embedSpan.setAttribute("embedding.query_length", query.length());
      }

      queryEmbedding = embeddingModel.embed(query);
      logger.debug(
          "Generated query embedding with {} dimensions",
          LogSanitizer.sanitize(queryEmbedding.length));

      if (embedSpan != null) {
        embedSpan.setAttribute("embedding.dimensions", queryEmbedding.length);
        embedSpan.setStatus(StatusCode.OK);
      }
    } catch (Exception e) {
      logger.error("Failed to generate embedding for query: {}", LogSanitizer.sanitize(query), e);
      if (embedSpan != null) {
        embedSpan.setStatus(StatusCode.ERROR, "Embedding generation failed");
        embedSpan.recordException(e);
      }
      throw new EmbeddingGenerationException("Failed to generate query embedding", e);
    } finally {
      if (embedSpan != null) {
        embedSpan.end();
      }
    }

    // Perform similarity search with configured parameters
    int topK = queryConfig.maxRetrievedChunks();
    double threshold = queryConfig.similarityThreshold();

    logger.debug(
        "Performing similarity search with topK={}, threshold={}",
        LogSanitizer.sanitize(topK),
        LogSanitizer.sanitize(threshold));

    Span searchSpan = tracer != null ? tracer.spanBuilder("vector-search").startSpan() : null;
    List<ScoredDocument> scoredDocuments;
    try (Scope searchScope = searchSpan != null ? searchSpan.makeCurrent() : null) {
      if (searchSpan != null) {
        searchSpan.setAttribute("vector.top_k", topK);
        searchSpan.setAttribute("vector.similarity_threshold", threshold);
      }

      scoredDocuments = vectorRepository.similaritySearch(queryEmbedding, topK, threshold);

      if (searchSpan != null) {
        searchSpan.setAttribute("vector.results_count", scoredDocuments.size());
        searchSpan.setStatus(StatusCode.OK);
      }
    } catch (Exception e) {
      logger.error("Vector database similarity search failed", e);
      if (searchSpan != null) {
        searchSpan.setStatus(StatusCode.ERROR, "Vector search failed");
        searchSpan.recordException(e);
      }
      throw new VectorDbQueryException("Failed to perform similarity search", e);
    } finally {
      if (searchSpan != null) {
        searchSpan.end();
      }
    }

    // Filter results below similarity threshold (additional safety check)
    List<ScoredDocument> filteredDocuments =
        scoredDocuments.stream()
            .filter(doc -> doc.getSimilarityScore() >= threshold)
            .collect(Collectors.toList());

    logger.debug(
        "Found {} documents above threshold {} (before: {})",
        LogSanitizer.sanitize(filteredDocuments.size()),
        LogSanitizer.sanitize(threshold),
        LogSanitizer.sanitize(scoredDocuments.size()));

    // Limit results to maxRetrievedChunks, keeping each chunk's similarity score
    List<ScoredDocument> relevantChunks =
        filteredDocuments.stream()
            .limit(queryConfig.maxRetrievedChunks())
            .collect(Collectors.toList());

    logger.info(
        "Retrieved {} relevant chunks for query", LogSanitizer.sanitize(relevantChunks.size()));

    return relevantChunks;
  }
}
