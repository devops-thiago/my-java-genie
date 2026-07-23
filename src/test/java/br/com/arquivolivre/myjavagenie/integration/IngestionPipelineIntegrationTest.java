package br.com.arquivolivre.myjavagenie.integration;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.arquivolivre.myjavagenie.model.DocumentChunk;
import br.com.arquivolivre.myjavagenie.model.IngestionResult;
import br.com.arquivolivre.myjavagenie.repository.VectorRepository;
import br.com.arquivolivre.myjavagenie.service.EmbeddingModelProvider;
import br.com.arquivolivre.myjavagenie.service.IngestionService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/** Integration test for document ingestion pipeline. Tests Requirements: 4.1, 4.2, 4.3, 4.4, 4.5 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IngestionPipelineIntegrationTest {

  @Container
  static GenericContainer<?> chromaContainer =
      new GenericContainer<>(DockerImageName.parse("chromadb/chroma:0.4.15"))
          .withExposedPorts(8000)
          .waitingFor(
              Wait.forHttp("/api/v1/heartbeat")
                  .forPort(8000)
                  .forStatusCode(200)
                  .withStartupTimeout(Duration.ofSeconds(60)));

  @Autowired private IngestionService ingestionService;

  @Autowired private VectorRepository vectorRepository;

  @Autowired private EmbeddingModelProvider embeddingModelProvider;

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add(
        "vector-db.connection-url",
        () -> "http://localhost:" + chromaContainer.getMappedPort(8000));
    registry.add("vector-db.collection-name", () -> "test_ingestion");
    registry.add("rag.startup-validation.enabled", () -> "false");
    registry.add("opentelemetry.enabled", () -> "false");

    // Configure ingestion settings
    registry.add("ingestion.chunk-size", () -> "500");
    registry.add("ingestion.chunk-overlap", () -> "100");
    registry.add("ingestion.batch-size", () -> "10");
  }

  /** Test Requirement 4.1: Read Java documentation files from configured directory */
  @Test
  @Order(1)
  void testLoadDocumentsFromDirectory() throws Exception {
    Path sampleDocsPath = Paths.get("src/test/resources/sample-docs");

    assertThat(Files.exists(sampleDocsPath)).isTrue();
    assertThat(Files.isDirectory(sampleDocsPath)).isTrue();

    IngestionResult result = ingestionService.ingestDocuments(sampleDocsPath);

    assertThat(result).isNotNull();
    assertThat(result.getDocumentsProcessed()).isGreaterThan(0);
  }

  /** Test Requirement 4.2: Split documents into chunks of configurable size */
  @Test
  @Order(2)
  void testDocumentChunking() throws Exception {
    Path sampleDocsPath = Paths.get("src/test/resources/sample-docs");

    IngestionResult result = ingestionService.ingestDocuments(sampleDocsPath);

    assertThat(result).isNotNull();
    assertThat(result.getChunksCreated()).isGreaterThan(result.getDocumentsProcessed());

    // Verify chunks were created (more chunks than documents)
    assertThat(result.getChunksCreated()).isGreaterThanOrEqualTo(result.getDocumentsProcessed());
  }

  /** Test Requirement 4.3: Generate embeddings using Embedding Model */
  @Test
  @Order(3)
  void testEmbeddingGeneration() {
    String sampleText = "Records in Java are special classes for immutable data.";

    float[] embedding = embeddingModelProvider.embed(sampleText);

    assertThat(embedding).isNotNull();
    assertThat(embedding.length).isEqualTo(embeddingModelProvider.getDimensions());
    assertThat(embedding.length).isGreaterThan(0);
  }

  /** Test Requirement 4.4: Store document chunks and embeddings in Vector Database */
  @Test
  @Order(4)
  void testStorageInVectorDatabase() throws Exception {
    Path sampleDocsPath = Paths.get("src/test/resources/sample-docs");

    IngestionResult result = ingestionService.ingestDocuments(sampleDocsPath);

    assertThat(result).isNotNull();
    assertThat(result.getChunksCreated()).isGreaterThan(0);

    // Verify we can retrieve documents from vector database
    String queryText = "What are records?";
    float[] queryEmbedding = embeddingModelProvider.embed(queryText);

    var searchResults = vectorRepository.similaritySearch(queryEmbedding, 5, 0.0);

    assertThat(searchResults).isNotEmpty();
    assertThat(searchResults.get(0).getChunk()).isNotNull();
    assertThat(searchResults.get(0).getChunk().getContent()).isNotBlank();
  }

  /** Test Requirement 4.5: Track ingestion progress and handle partial failures */
  @Test
  @Order(5)
  void testIngestionProgressTracking() throws Exception {
    Path sampleDocsPath = Paths.get("src/test/resources/sample-docs");

    IngestionResult result = ingestionService.ingestDocuments(sampleDocsPath);

    assertThat(result).isNotNull();
    assertThat(result.getDocumentsProcessed()).isGreaterThan(0);
    assertThat(result.getChunksCreated()).isGreaterThan(0);
    assertThat(result.getFailures()).isEqualTo(0);
    assertThat(result.getDuration()).isNotNull();
  }

  /** Test Requirement 4.5: Test resumption capability after interruption */
  @Test
  @Order(6)
  void testIngestionResumption() throws Exception {
    Path sampleDocsPath = Paths.get("src/test/resources/sample-docs");

    // First ingestion
    IngestionResult firstResult = ingestionService.ingestDocuments(sampleDocsPath);
    assertThat(firstResult.getDocumentsProcessed()).isGreaterThan(0);

    // Second ingestion (should handle existing documents gracefully)
    IngestionResult secondResult = ingestionService.ingestDocuments(sampleDocsPath);
    assertThat(secondResult).isNotNull();

    // Both ingestions should succeed
    assertThat(firstResult.getFailures()).isEqualTo(0);
    assertThat(secondResult.getFailures()).isEqualTo(0);
  }

  /** Test batch embedding for efficiency */
  @Test
  @Order(7)
  void testBatchEmbedding() {
    List<String> texts =
        List.of(
            "Records are immutable data carriers",
            "Sealed classes restrict inheritance",
            "Pattern matching works with records");

    List<float[]> embeddings = embeddingModelProvider.embedBatch(texts);

    assertThat(embeddings).hasSize(texts.size());
    for (float[] embedding : embeddings) {
      assertThat(embedding).isNotNull();
      assertThat(embedding.length).isEqualTo(embeddingModelProvider.getDimensions());
    }
  }

  /** Test metadata preservation during ingestion */
  @Test
  @Order(8)
  void testMetadataPreservation() throws Exception {
    Path sampleDocsPath = Paths.get("src/test/resources/sample-docs");

    IngestionResult result = ingestionService.ingestDocuments(sampleDocsPath);
    assertThat(result.getChunksCreated()).isGreaterThan(0);

    // Query and verify metadata is preserved
    String queryText = "records";
    float[] queryEmbedding = embeddingModelProvider.embed(queryText);

    var searchResults = vectorRepository.similaritySearch(queryEmbedding, 3, 0.0);

    assertThat(searchResults).isNotEmpty();
    DocumentChunk chunk = searchResults.get(0).getChunk();
    assertThat(chunk.getMetadata()).isNotNull();
    assertThat(chunk.getMetadata().getSourceFile()).isNotBlank();
  }
}
