package br.com.arquivolivre.myjavagenie.repository;

import br.com.arquivolivre.myjavagenie.exception.VectorDbException;
import br.com.arquivolivre.myjavagenie.exception.VectorDbQueryException;
import br.com.arquivolivre.myjavagenie.model.DocumentChunk;
import br.com.arquivolivre.myjavagenie.model.ScoredDocument;
import java.util.List;

/**
 * Interface for vector database operations. Provides methods for storing document chunks with
 * embeddings and performing similarity searches. Implementations should handle connection
 * management, error handling, and retry logic.
 */
public interface VectorRepository {

  /**
   * Stores a single document chunk with its embedding in the vector database.
   *
   * @param chunk the document chunk to store
   * @param embedding the vector embedding for the chunk
   * @throws VectorDbException if storage fails
   */
  void store(DocumentChunk chunk, float[] embedding);

  /**
   * Stores multiple document chunks with their embeddings in a batch operation. This method is more
   * efficient than calling store() multiple times.
   *
   * @param chunks the list of document chunks to store
   * @param embeddings the list of vector embeddings corresponding to each chunk
   * @throws VectorDbException if storage fails
   * @throws IllegalArgumentException if chunks and embeddings lists have different sizes
   */
  void storeBatch(List<DocumentChunk> chunks, List<float[]> embeddings);

  /**
   * Performs a similarity search to find the most relevant document chunks. Results are ranked by
   * cosine similarity and filtered by the threshold.
   *
   * @param queryEmbedding the vector embedding of the query
   * @param topK the maximum number of results to return
   * @param threshold the minimum similarity score (0.0 to 1.0) for results to include
   * @return list of scored documents sorted by similarity score in descending order
   * @throws VectorDbQueryException if the search fails
   */
  List<ScoredDocument> similaritySearch(float[] queryEmbedding, int topK, double threshold);

  /**
   * Creates a new collection in the vector database.
   *
   * @param name the name of the collection to create
   * @param dimensions the dimensionality of the vectors to be stored
   * @throws VectorDbException if collection creation fails
   */
  void createCollection(String name, int dimensions);

  /**
   * Checks if a collection exists in the vector database.
   *
   * @param name the name of the collection to check
   * @return true if the collection exists, false otherwise
   * @throws VectorDbException if the check fails
   */
  boolean collectionExists(String name);
}
