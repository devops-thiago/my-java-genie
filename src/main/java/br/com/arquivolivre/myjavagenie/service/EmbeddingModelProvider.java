package br.com.arquivolivre.myjavagenie.service;

import br.com.arquivolivre.myjavagenie.exception.EmbeddingGenerationException;
import java.util.List;

/**
 * Interface for embedding model providers that convert text into vector embeddings. Implementations
 * should handle the conversion of text strings into numerical vector representations suitable for
 * similarity search in vector databases.
 */
public interface EmbeddingModelProvider {

  /**
   * Generates an embedding vector for a single text input.
   *
   * @param text the text to embed
   * @return the embedding vector as a float array
   * @throws EmbeddingGenerationException if embedding generation fails
   */
  float[] embed(String text);

  /**
   * Generates embedding vectors for multiple text inputs in a batch. This method is optimized for
   * efficiency during bulk ingestion operations.
   *
   * @param texts the list of texts to embed
   * @return a list of embedding vectors, one for each input text
   * @throws EmbeddingGenerationException if embedding generation fails
   */
  List<float[]> embedBatch(List<String> texts);

  /**
   * Returns the dimensionality of the embedding vectors produced by this provider.
   *
   * @return the number of dimensions in the embedding vectors
   */
  int getDimensions();
}
