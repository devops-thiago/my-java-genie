package br.com.arquivolivre.myjavagenie.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmbeddingConfiguration {
  private static final Logger logger = LoggerFactory.getLogger(EmbeddingConfiguration.class);

  @Bean
  public EmbeddingModel embeddingModel() {
    logger.info("Initializing local embedding model all-MiniLM-L6-v2 (384 dims)");
    return new AllMiniLmL6V2EmbeddingModel();
  }

  @Bean
  public ChromaEmbeddingStore chromaEmbeddingStore(VectorDbConfig config) {
    logger.info(
        "Connecting Chroma at {} collection={}",
        config.getConnectionUrl(),
        config.getCollectionName());
    return ChromaEmbeddingStore.builder()
        .baseUrl(config.getConnectionUrl())
        .collectionName(config.getCollectionName())
        .build();
  }
}
