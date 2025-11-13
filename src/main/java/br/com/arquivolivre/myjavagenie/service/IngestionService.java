package br.com.arquivolivre.myjavagenie.service;

import br.com.arquivolivre.myjavagenie.config.IngestionConfig;
import br.com.arquivolivre.myjavagenie.exception.IngestionException;
import br.com.arquivolivre.myjavagenie.model.Document;
import br.com.arquivolivre.myjavagenie.model.DocumentChunk;
import br.com.arquivolivre.myjavagenie.model.IngestionResult;
import br.com.arquivolivre.myjavagenie.repository.VectorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for ingesting documents into the RAG system.
 * Orchestrates the document loading, processing, embedding generation, and storage workflow.
 */
@Service
public class IngestionService {

    private static final Logger logger = LoggerFactory.getLogger(IngestionService.class);

    private final DocumentLoader documentLoader;
    private final DocumentProcessor documentProcessor;
    private final EmbeddingModelProvider embeddingModel;
    private final VectorRepository vectorRepository;
    private final IngestionConfig config;

    public IngestionService(
            DocumentLoader documentLoader,
            DocumentProcessor documentProcessor,
            EmbeddingModelProvider embeddingModel,
            VectorRepository vectorRepository,
            IngestionConfig config) {
        this.documentLoader = documentLoader;
        this.documentProcessor = documentProcessor;
        this.embeddingModel = embeddingModel;
        this.vectorRepository = vectorRepository;
        this.config = config;
    }

    /**
     * Ingests documents from the specified path.
     * Loads documents, processes them into chunks, generates embeddings, and stores them in the vector database.
     *
     * @param documentPath the path to the directory containing documents to ingest
     * @return IngestionResult containing statistics about the ingestion process
     * @throws IngestionException if ingestion fails completely
     */
    public IngestionResult ingestDocuments(Path documentPath) {
        logger.info("Starting document ingestion from path: {}", documentPath);
        Instant startTime = Instant.now();

        IngestionResult result = new IngestionResult();

        try {
            // Load documents from the specified path
            logger.info("Loading documents from: {}", documentPath);
            List<Document> documents = documentLoader.loadDocuments(documentPath);
            logger.info("Loaded {} documents", documents.size());

            if (documents.isEmpty()) {
                logger.warn("No documents found at path: {}", documentPath);
                result.setDuration(Duration.between(startTime, Instant.now()));
                return result;
            }

            // Process each document
            for (Document document : documents) {
                try {
                    processDocument(document, result);
                } catch (Exception e) {
                    logger.error("Failed to process document: {}",
                            document.getMetadata().getSourceFile(), e);
                    result.addFailedDocument(document.getMetadata().getSourceFile());
                }
            }

            Instant endTime = Instant.now();
            result.setDuration(Duration.between(startTime, endTime));

            logger.info("Ingestion completed: {}", result);
            return result;

        } catch (Exception e) {
            logger.error("Document ingestion failed", e);
            throw new IngestionException("Failed to ingest documents from path: " + documentPath, e);
        }
    }

    /**
     * Process a single document: chunk it, generate embeddings, and store in vector database.
     */
    private void processDocument(Document document, IngestionResult result) {
        String sourceFile = document.getMetadata().getSourceFile();
        logger.debug("Processing document: {}", sourceFile);

        // Check if document already exists (resumption capability)
        // For now, we'll process all documents; future enhancement could check for existing chunks

        // Process and chunk the document
        List<DocumentChunk> chunks = documentProcessor.processDocument(document);
        logger.debug("Created {} chunks from document: {}", chunks.size(), sourceFile);

        if (chunks.isEmpty()) {
            logger.warn("No chunks created from document: {}", sourceFile);
            result.incrementDocumentsProcessed();
            return;
        }

        // Process chunks in batches
        int totalChunks = chunks.size();
        int batchSize = config.getBatchSize();

        for (int i = 0; i < totalChunks; i += batchSize) {
            int endIndex = Math.min(i + batchSize, totalChunks);
            List<DocumentChunk> batch = chunks.subList(i, endIndex);

            try {
                processBatch(batch, sourceFile, i, totalChunks);
                result.addChunks(batch.size());
            } catch (Exception e) {
                logger.error("Failed to process batch {}-{} for document: {}",
                        i, endIndex, sourceFile, e);
                throw e;
            }
        }

        result.incrementDocumentsProcessed();
        logger.info("Successfully processed document: {} ({} chunks)", sourceFile, totalChunks);
    }

    /**
     * Process a batch of chunks: generate embeddings and store in vector database.
     */
    private void processBatch(List<DocumentChunk> batch, String sourceFile, int startIndex, int totalChunks) {
        logger.debug("Processing batch {}-{}/{} for document: {}",
                startIndex, startIndex + batch.size(), totalChunks, sourceFile);

        // Extract text content from chunks
        List<String> texts = new ArrayList<>();
        for (DocumentChunk chunk : batch) {
            texts.add(chunk.getContent());
        }

        // Generate embeddings in batch
        logger.debug("Generating embeddings for {} chunks", batch.size());
        List<float[]> embeddings = embeddingModel.embedBatch(texts);

        if (embeddings.size() != batch.size()) {
            throw new IngestionException(
                    String.format("Embedding count mismatch: expected %d, got %d",
                            batch.size(), embeddings.size()));
        }

        // Store chunks and embeddings in vector database
        logger.debug("Storing {} chunks in vector database", batch.size());
        vectorRepository.storeBatch(batch, embeddings);

        // Log progress
        int processedChunks = startIndex + batch.size();
        double progress = (processedChunks * 100.0) / totalChunks;
        logger.info("Progress for {}: {}/{} chunks ({:.1f}%)",
                sourceFile, processedChunks, totalChunks, progress);
    }

    /**
     * Ingest a single document file.
     * Useful for incremental ingestion or testing.
     *
     * @param documentPath the path to the document file
     * @return IngestionResult containing statistics about the ingestion process
     * @throws IngestionException if ingestion fails
     */
    public IngestionResult ingestDocument(Path documentPath) {
        logger.info("Starting single document ingestion: {}", documentPath);
        Instant startTime = Instant.now();

        IngestionResult result = new IngestionResult();

        try {
            // Load single document
            Document document = documentLoader.loadDocument(documentPath);
            logger.info("Loaded document: {}", documentPath.getFileName());

            // Process the document
            processDocument(document, result);

            Instant endTime = Instant.now();
            result.setDuration(Duration.between(startTime, endTime));

            logger.info("Single document ingestion completed: {}", result);
            return result;

        } catch (Exception e) {
            logger.error("Single document ingestion failed", e);
            throw new IngestionException("Failed to ingest document: " + documentPath, e);
        }
    }
}
