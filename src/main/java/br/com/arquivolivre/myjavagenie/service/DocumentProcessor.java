package br.com.arquivolivre.myjavagenie.service;

import br.com.arquivolivre.myjavagenie.model.Document;
import br.com.arquivolivre.myjavagenie.model.DocumentChunk;
import br.com.arquivolivre.myjavagenie.model.DocumentMetadata;

import java.util.List;

/**
 * Interface for processing documents and splitting them into chunks.
 * Implementations should handle text segmentation while preserving metadata.
 */
public interface DocumentProcessor {

    /**
     * Process a document and split it into chunks.
     *
     * @param document the document to process
     * @return list of document chunks with metadata
     */
    List<DocumentChunk> processDocument(Document document);

    /**
     * Split text into chunks with the specified metadata.
     *
     * @param text     the text to chunk
     * @param metadata the metadata to associate with each chunk
     * @return list of document chunks
     */
    List<DocumentChunk> chunkText(String text, DocumentMetadata metadata);
}
