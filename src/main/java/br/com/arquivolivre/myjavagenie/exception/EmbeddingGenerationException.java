package br.com.arquivolivre.myjavagenie.exception;

/** Exception thrown when embedding generation fails during ingestion. */
public class EmbeddingGenerationException extends IngestionException {

  public EmbeddingGenerationException(String message) {
    super(message);
  }

  public EmbeddingGenerationException(String message, Throwable cause) {
    super(message, cause);
  }

  public static EmbeddingGenerationException forChunk(int chunkIndex, Throwable cause) {
    return new EmbeddingGenerationException(
        String.format("Failed to generate embedding for chunk at index: %d", chunkIndex), cause);
  }
}
