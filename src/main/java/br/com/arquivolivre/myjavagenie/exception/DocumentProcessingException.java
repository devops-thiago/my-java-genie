package br.com.arquivolivre.myjavagenie.exception;

/**
 * Exception thrown when document processing fails during ingestion. This includes errors in
 * reading, parsing, or chunking documents.
 */
public class DocumentProcessingException extends IngestionException {

  public DocumentProcessingException(String message) {
    super(message);
  }

  public DocumentProcessingException(String message, Throwable cause) {
    super(message, cause);
  }

  public static DocumentProcessingException forDocument(String documentPath, Throwable cause) {
    return new DocumentProcessingException(
        String.format("Failed to process document: %s", documentPath), cause);
  }
}
