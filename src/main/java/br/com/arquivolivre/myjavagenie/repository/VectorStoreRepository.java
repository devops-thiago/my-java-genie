package br.com.arquivolivre.myjavagenie.repository;

import br.com.arquivolivre.myjavagenie.model.DocumentChunk;
import br.com.arquivolivre.myjavagenie.model.ScoredChunk;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

@Repository
public class VectorStoreRepository {
  private static final Logger logger = LoggerFactory.getLogger(VectorStoreRepository.class);

  private final EmbeddingModel embeddingModel;
  private final ChromaEmbeddingStore embeddingStore;

  public VectorStoreRepository(EmbeddingModel embeddingModel, ChromaEmbeddingStore embeddingStore) {
    this.embeddingModel = embeddingModel;
    this.embeddingStore = embeddingStore;
  }

  public void storeAll(List<DocumentChunk> chunks) {
    if (chunks.isEmpty()) {
      return;
    }
    List<TextSegment> segments = new ArrayList<>();
    List<Embedding> embeddings = new ArrayList<>();
    for (DocumentChunk chunk : chunks) {
      Map<String, String> meta = new HashMap<>();
      meta.put("filename", chunk.getFilename());
      meta.put("chunkIndex", String.valueOf(chunk.getChunkIndex()));
      TextSegment segment = TextSegment.from(chunk.getContent(), Metadata.from(meta));
      segments.add(segment);
      embeddings.add(embeddingModel.embed(segment).content());
    }
    embeddingStore.addAll(embeddings, segments);
    logger.info("Stored {} chunks in Chroma", chunks.size());
  }

  public List<ScoredChunk> search(String query, int topK, double minScore) {
    Embedding queryEmbedding = embeddingModel.embed(query).content();
    EmbeddingSearchRequest request =
        EmbeddingSearchRequest.builder()
            .queryEmbedding(queryEmbedding)
            .maxResults(topK)
            .minScore(minScore)
            .build();
    EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);
    List<ScoredChunk> scored = new ArrayList<>();
    for (EmbeddingMatch<TextSegment> match : result.matches()) {
      TextSegment segment = match.embedded();
      Metadata metadata = segment.metadata();
      scored.add(
          new ScoredChunk(
              metadata.getString("filename"),
              Integer.parseInt(metadata.getString("chunkIndex")),
              segment.text(),
              match.score()));
    }
    return scored;
  }
}
