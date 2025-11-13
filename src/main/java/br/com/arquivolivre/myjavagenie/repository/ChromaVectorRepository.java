package br.com.arquivolivre.myjavagenie.repository;

import br.com.arquivolivre.myjavagenie.config.VectorDbConfig;
import br.com.arquivolivre.myjavagenie.exception.VectorDbConnectionException;
import br.com.arquivolivre.myjavagenie.exception.VectorDbException;
import br.com.arquivolivre.myjavagenie.exception.VectorDbQueryException;
import br.com.arquivolivre.myjavagenie.model.DocumentChunk;
import br.com.arquivolivre.myjavagenie.model.DocumentMetadata;
import br.com.arquivolivre.myjavagenie.model.ScoredDocument;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ChromaDB implementation of the VectorRepository interface.
 * Provides vector storage and similarity search using ChromaDB.
 */
public class ChromaVectorRepository implements VectorRepository {

    private static final Logger logger = LoggerFactory.getLogger(ChromaVectorRepository.class);
    private static final int MAX_RETRIES = 2;
    private static final long RETRY_DELAY_MS = 1000;

    private final ChromaEmbeddingStore embeddingStore;
    private final VectorDbConfig config;
    private final String collectionName;

    public ChromaVectorRepository(VectorDbConfig config) {
        this.config = config;
        this.collectionName = config.getCollectionName();

        try {
            this.embeddingStore = createEmbeddingStore();
            logger.info("ChromaDB vector repository initialized successfully for collection: {}", collectionName);
        } catch (Exception e) {
            throw VectorDbConnectionException.forDatabase("ChromaDB", config.getConnectionUrl(), e);
        }
    }

    private ChromaEmbeddingStore createEmbeddingStore() {
        ChromaEmbeddingStore.Builder builder = ChromaEmbeddingStore.builder()
                .baseUrl(config.getConnectionUrl())
                .collectionName(collectionName);

        // Add optional ChromaDB-specific settings if configured
        if (config.getChroma() != null) {
            VectorDbConfig.ChromaSettings chromaSettings = config.getChroma();
            // ChromaDB tenant and database settings can be added here if supported by the client
        }

        return builder.build();
    }

    @Override
    public void store(DocumentChunk chunk, float[] embedding) {
        if (chunk == null || embedding == null) {
            throw new IllegalArgumentException("Chunk and embedding cannot be null");
        }

        executeWithRetry(() -> {
            TextSegment segment = createTextSegment(chunk);
            Embedding embeddingObj = new Embedding(embedding);

            embeddingStore.add(embeddingObj, segment);
            logger.debug("Stored chunk with ID: {}", chunk.getId());
            return null;
        }, "store");
    }

    @Override
    public void storeBatch(List<DocumentChunk> chunks, List<float[]> embeddings) {
        if (chunks == null || embeddings == null) {
            throw new IllegalArgumentException("Chunks and embeddings cannot be null");
        }
        if (chunks.size() != embeddings.size()) {
            throw new IllegalArgumentException(
                    String.format("Chunks size (%d) must match embeddings size (%d)",
                            chunks.size(), embeddings.size())
            );
        }

        if (chunks.isEmpty()) {
            logger.debug("No chunks to store in batch");
            return;
        }

        executeWithRetry(() -> {
            List<TextSegment> segments = chunks.stream()
                    .map(this::createTextSegment)
                    .collect(Collectors.toList());

            List<Embedding> embeddingObjs = embeddings.stream()
                    .map(Embedding::new)
                    .collect(Collectors.toList());

            embeddingStore.addAll(embeddingObjs, segments);
            logger.info("Stored batch of {} chunks", chunks.size());
            return null;
        }, "storeBatch");
    }

    @Override
    public List<ScoredDocument> similaritySearch(float[] queryEmbedding, int topK, double threshold) {
        if (queryEmbedding == null) {
            throw new IllegalArgumentException("Query embedding cannot be null");
        }
        if (topK <= 0) {
            throw new IllegalArgumentException("topK must be positive");
        }
        if (threshold < 0.0 || threshold > 1.0) {
            throw new IllegalArgumentException("Threshold must be between 0.0 and 1.0");
        }

        return executeWithRetry(() -> {
            Embedding queryEmbeddingObj = new Embedding(queryEmbedding);

            EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbeddingObj)
                    .maxResults(topK)
                    .minScore(threshold)
                    .build();

            EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);

            List<ScoredDocument> results = searchResult.matches().stream()
                    .filter(match -> match.score() >= threshold)
                    .map(this::convertToScoredDocument)
                    .collect(Collectors.toList());

            logger.debug("Similarity search returned {} results (threshold: {})", results.size(), threshold);
            return results;
        }, "similaritySearch");
    }

    @Override
    public void createCollection(String name, int dimensions) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Collection name cannot be null or empty");
        }
        if (dimensions <= 0) {
            throw new IllegalArgumentException("Dimensions must be positive");
        }

        // ChromaDB creates collections automatically when first accessed
        // This is a no-op for ChromaDB but kept for interface compatibility
        logger.info("Collection '{}' will be created automatically on first use (dimensions: {})", name, dimensions);
    }

    @Override
    public boolean collectionExists(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Collection name cannot be null or empty");
        }

        // ChromaDB doesn't provide a direct way to check collection existence
        // We'll assume the collection exists if we can initialize the store
        // In a production system, you might want to implement a more robust check
        return true;
    }

    /**
     * Converts a DocumentChunk to a LangChain4j TextSegment with metadata.
     */
    private TextSegment createTextSegment(DocumentChunk chunk) {
        Map<String, Object> metadataMap = new HashMap<>();
        metadataMap.put("id", chunk.getId());
        metadataMap.put("tokenCount", chunk.getTokenCount());

        if (chunk.getMetadata() != null) {
            DocumentMetadata docMetadata = chunk.getMetadata();
            if (docMetadata.getSourceFile() != null) {
                metadataMap.put("sourceFile", docMetadata.getSourceFile());
            }
            if (docMetadata.getSection() != null) {
                metadataMap.put("section", docMetadata.getSection());
            }
            metadataMap.put("chunkIndex", docMetadata.getChunkIndex());

            if (docMetadata.getAdditionalProperties() != null) {
                metadataMap.putAll(docMetadata.getAdditionalProperties());
            }
        }

        Metadata metadata = Metadata.from(metadataMap);
        return TextSegment.from(chunk.getContent(), metadata);
    }

    /**
     * Converts a LangChain4j EmbeddingMatch to a ScoredDocument.
     */
    private ScoredDocument convertToScoredDocument(EmbeddingMatch<TextSegment> match) {
        TextSegment segment = match.embedded();
        Metadata metadata = segment.metadata();

        String id = metadata.getString("id");
        String content = segment.text();
        int tokenCount = metadata.getInteger("tokenCount") != null ?
                metadata.getInteger("tokenCount") : 0;

        DocumentMetadata docMetadata = new DocumentMetadata();
        docMetadata.setSourceFile(metadata.getString("sourceFile"));
        docMetadata.setSection(metadata.getString("section"));
        Integer chunkIndex = metadata.getInteger("chunkIndex");
        docMetadata.setChunkIndex(chunkIndex != null ? chunkIndex : 0);

        // Extract additional properties
        Map<String, String> additionalProps = new HashMap<>();
        for (String key : metadata.toMap().keySet()) {
            if (!key.equals("id") && !key.equals("tokenCount") &&
                    !key.equals("sourceFile") && !key.equals("section") && !key.equals("chunkIndex")) {
                Object value = metadata.toMap().get(key);
                if (value != null) {
                    additionalProps.put(key, value.toString());
                }
            }
        }
        if (!additionalProps.isEmpty()) {
            docMetadata.setAdditionalProperties(additionalProps);
        }

        DocumentChunk chunk = new DocumentChunk(id, content, docMetadata, tokenCount);
        return new ScoredDocument(chunk, match.score());
    }

    /**
     * Executes an operation with retry logic for transient failures.
     */
    private <T> T executeWithRetry(RetryableOperation<T> operation, String operationName) {
        int attempt = 0;
        Exception lastException = null;

        while (attempt <= MAX_RETRIES) {
            try {
                return operation.execute();
            } catch (Exception e) {
                lastException = e;
                attempt++;

                if (attempt <= MAX_RETRIES) {
                    logger.warn("Operation '{}' failed (attempt {}/{}), retrying after {}ms: {}",
                            operationName, attempt, MAX_RETRIES + 1, RETRY_DELAY_MS, e.getMessage());
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new VectorDbException("Operation interrupted during retry", ie);
                    }
                } else {
                    logger.error("Operation '{}' failed after {} attempts", operationName, MAX_RETRIES + 1);
                }
            }
        }

        throw VectorDbQueryException.forOperation(operationName, lastException);
    }

    @FunctionalInterface
    private interface RetryableOperation<T> {
        T execute() throws Exception;
    }
}
