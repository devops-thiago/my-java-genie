package br.com.arquivolivre.myjavagenie.exception;

/** Exception thrown when a requested vector database collection does not exist. */
public class CollectionNotFoundException extends VectorDbException {

  public CollectionNotFoundException(String message) {
    super(message);
  }

  public CollectionNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }

  public static CollectionNotFoundException forCollection(String collectionName) {
    return new CollectionNotFoundException(
        String.format("Vector database collection not found: %s", collectionName));
  }

  public static CollectionNotFoundException forCollection(String collectionName, Throwable cause) {
    return new CollectionNotFoundException(
        String.format("Vector database collection not found: %s", collectionName), cause);
  }
}
