package br.com.arquivolivre.myjavagenie.service;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

@Service
public class RagTelemetry {

  private static final AttributeKey<String> MODEL_KEY = AttributeKey.stringKey("model");
  private static final AttributeKey<Long> TOP_K_KEY = AttributeKey.longKey("top_k");

  private final Tracer tracer;
  private final DoubleHistogram queryLatency;
  private final DoubleHistogram retrieveLatency;
  private final DoubleHistogram generateLatency;
  private final LongCounter chunksRetrieved;
  private final LongCounter tokensPrompt;
  private final LongCounter tokensCompletion;
  private final LongCounter ingestChunks;

  public RagTelemetry(Tracer tracer, Meter meter) {
    this.tracer = tracer;
    this.queryLatency =
        meter
            .histogramBuilder("rag.query.latency")
            .setDescription("End-to-end RAG query latency")
            .setUnit("ms")
            .build();
    this.retrieveLatency =
        meter
            .histogramBuilder("rag.retrieve.latency")
            .setDescription("Retrieve (embed + search) latency")
            .setUnit("ms")
            .build();
    this.generateLatency =
        meter
            .histogramBuilder("rag.generate.latency")
            .setDescription("LLM generation latency")
            .setUnit("ms")
            .build();
    this.chunksRetrieved =
        meter
            .counterBuilder("rag.chunks.retrieved")
            .setDescription("Chunks returned by similarity search")
            .build();
    this.tokensPrompt =
        meter
            .counterBuilder("rag.tokens.prompt")
            .setDescription("Prompt tokens used for generation")
            .build();
    this.tokensCompletion =
        meter
            .counterBuilder("rag.tokens.completion")
            .setDescription("Completion tokens from the LLM")
            .build();
    this.ingestChunks =
        meter
            .counterBuilder("rag.ingest.chunks")
            .setDescription("Chunks embedded and stored during ingest")
            .build();
  }

  public <T> T inSpan(String name, Supplier<T> action) {
    Span span = tracer.spanBuilder(name).startSpan();
    try (Scope ignored = span.makeCurrent()) {
      return action.get();
    } catch (RuntimeException e) {
      span.recordException(e);
      throw e;
    } finally {
      span.end();
    }
  }

  public void inSpan(String name, Runnable action) {
    inSpan(
        name,
        () -> {
          action.run();
          return null;
        });
  }

  public void recordQuery(long latencyMs, String model, int topK) {
    Attributes attrs = Attributes.of(MODEL_KEY, model, TOP_K_KEY, (long) topK);
    queryLatency.record(latencyMs, attrs);
  }

  public void recordRetrieve(long latencyMs, int chunkCount, int topK) {
    Attributes attrs = Attributes.of(TOP_K_KEY, (long) topK);
    retrieveLatency.record(latencyMs, attrs);
    chunksRetrieved.add(chunkCount, attrs);
  }

  public void recordGenerate(long latencyMs, String model, int promptTokens, int completionTokens) {
    Attributes attrs = Attributes.of(MODEL_KEY, model);
    generateLatency.record(latencyMs, attrs);
    if (promptTokens > 0) {
      tokensPrompt.add(promptTokens, attrs);
    }
    if (completionTokens > 0) {
      tokensCompletion.add(completionTokens, attrs);
    }
  }

  public void recordIngestChunks(int chunkCount) {
    ingestChunks.add(chunkCount);
  }
}
