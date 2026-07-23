package br.com.arquivolivre.myjavagenie.service;

import br.com.arquivolivre.myjavagenie.model.Document;
import java.nio.file.Path;
import java.util.List;

/**
 * Abstraction over loading documents from a source for ingestion. Consumers depend on this
 * interface rather than a concrete implementation, keeping the document source swappable
 * (filesystem, object store, …).
 */
public interface DocumentReader {

  /**
   * Loads all supported documents from a directory.
   *
   * @param directoryPath the directory to load documents from
   * @return the list of loaded documents
   */
  List<Document> loadDocuments(Path directoryPath);

  /**
   * Loads a single document from a file.
   *
   * @param filePath the file to load
   * @return the loaded document
   */
  Document loadDocument(Path filePath);
}
