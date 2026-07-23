package br.com.arquivolivre.myjavagenie.service;

import br.com.arquivolivre.myjavagenie.exception.EmbeddingGenerationException;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.output.Response;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of EmbeddingModelProvider using LangChain4j's all-MiniLM-L6-v2 model. This
 * is a local embedding model that runs without requiring external API calls.
 */
public class DefaultEmbeddingModelProvider implements EmbeddingModelProvider {

  private static final Logger logger = LoggerFactory.getLogger(DefaultEmbeddingModelProvider.class);

  private final EmbeddingModel embeddingModel;
  private final int dimensions;
  private Tracer tracer;

  /** Creates a new DefaultEmbeddingModelProvider with the default all-MiniLM-L6-v2 model. */
  public DefaultEmbeddingModelProvider() {
    logger.info("Initializing DefaultEmbeddingModelProvider with all-MiniLM-L6-v2 model");
    try {
      this.embeddingModel = new AllMiniLmL6V2EmbeddingModel();
      // all-MiniLM-L6-v2 produces 384-dimensional embeddings
      this.dimensions = 384;
      logger.info("Successfully initialized embedding model with {} dimensions", dimensions);
    } catch (Exception e) {
      logger.error("Failed to initialize embedding model", e);
      throw new EmbeddingGenerationException("Failed to initialize embedding model", e);
    }
  }

  /**
   * Creates a new DefaultEmbeddingModelProvider with a custom embedding model. This constructor is
   * useful for testing or using alternative embedding models.
   *
   * @param embeddingModel the embedding model to use
   * @param dimensions the dimensionality of the embeddings
   */
  public DefaultEmbeddingModelProvider(EmbeddingModel embeddingModel, int dimensions) {
    this.embeddingModel = embeddingModel;
    this.dimensions = dimensions;
    logger.info(
        "Initialized DefaultEmbeddingModelProvider with custom model ({} dimensions)", dimensions);
  }

  /** Sets the tracer for instrumentation (optional). */
  public void setTracer(Tracer tracer) {
    this.tracer = tracer;
  }

  @Override
  public float[] embed(String text) {
    if (text == null || text.trim().isEmpty()) {
      throw new EmbeddingGenerationException("Cannot generate embedding for null or empty text");
    }

    Span span = tracer != null ? tracer.spanBuilder("embedding-generate").startSpan() : null;
    try (Scope scope = span != null ? span.makeCurrent() : null) {
      if (span != null) {
        span.setAttribute("embedding.text_length", text.length());
        span.setAttribute("embedding.model", "all-MiniLM-L6-v2");
      }

      logger.debug("Generating embedding for text of length: {}", text.length());
      Response<Embedding> response = embeddingModel.embed(text);

      if (response == null || response.content() == null) {
        throw new EmbeddingGenerationException("Embedding model returned null response");
      }

      float[] embedding = response.content().vector();
      logger.debug("Successfully generated embedding with {} dimensions", embedding.length);

      if (span != null) {
        span.setAttribute("embedding.dimensions", embedding.length);
        span.setStatus(StatusCode.OK);
      }
      return embedding;

    } catch (EmbeddingGenerationException e) {
      if (span != null) {
        span.setStatus(StatusCode.ERROR, "Embedding generation failed");
        span.recordException(e);
      }
      throw e;
    } catch (Exception e) {
      logger.error("Failed to generate embedding for text", e);
      if (span != null) {
        span.setStatus(StatusCode.ERROR, "Embedding generation failed");
        span.recordException(e);
      }
      throw new EmbeddingGenerationException("Failed to generate embedding", e);
    } finally {
      if (span != null) {
        span.end();
      }
    }
  }

  @Override
  public List<float[]> embedBatch(List<String> texts) {
    if (texts == null || texts.isEmpty()) {
      throw new EmbeddingGenerationException(
          "Cannot generate embeddings for null or empty text list");
    }

    logger.info("Generating embeddings for batch of {} texts", texts.size());

    try {
      // Convert strings to TextSegments for batch processing
      List<TextSegment> segments =
          texts.stream().map(TextSegment::from).collect(Collectors.toList());

      Response<List<Embedding>> response = embeddingModel.embedAll(segments);

      if (response == null || response.content() == null) {
        throw new EmbeddingGenerationException("Embedding model returned null response for batch");
      }

      List<float[]> embeddings = new ArrayList<>();
      for (Embedding embedding : response.content()) {
        if (embedding == null) {
          throw new EmbeddingGenerationException(
              "Embedding model returned null embedding in batch");
        }
        embeddings.add(embedding.vector());
      }

      logger.info("Successfully generated {} embeddings", embeddings.size());
      return embeddings;

    } catch (EmbeddingGenerationException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Failed to generate embeddings for batch", e);
      throw new EmbeddingGenerationException("Failed to generate embeddings for batch", e);
    }
  }

  @Override
  public int getDimensions() {
    return dimensions;
  }
}
