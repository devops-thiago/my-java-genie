package br.com.arquivolivre.myjavagenie.integration;

import br.com.arquivolivre.myjavagenie.model.QueryRequest;
import br.com.arquivolivre.myjavagenie.model.QueryResponse;
import br.com.arquivolivre.myjavagenie.service.IngestionService;
import br.com.arquivolivre.myjavagenie.service.TokenUsageTracker;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for end-to-end query flow.
 * Tests Requirements: 1.1, 1.2, 1.3, 5.5
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class QueryFlowIntegrationTest {

    @Container
    static GenericContainer<?> chromaContainer = new GenericContainer<>(
            DockerImageName.parse("chromadb/chroma:0.4.15"))
            .withExposedPorts(8000)
            .waitingFor(Wait.forHttp("/api/v1/heartbeat")
                    .forPort(8000)
                    .forStatusCode(200)
                    .withStartupTimeout(Duration.ofSeconds(60)));

    private static WireMockServer wireMockServer;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private IngestionService ingestionService;

    @Autowired
    private TokenUsageTracker tokenUsageTracker;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Configure ChromaDB connection
        registry.add("vector-db.connection-url",
                () -> "http://localhost:" + chromaContainer.getMappedPort(8000));
        registry.add("vector-db.collection-name", () -> "test_java25_docs");
        registry.add("rag.startup-validation.enabled", () -> "false");

        // Configure to use OpenAI provider (will be mocked)
        registry.add("model.provider", () -> "openai");
        registry.add("model.openai.api-key", () -> "test-api-key");
        registry.add("model.openai.model-name", () -> "gpt-4");
        registry.add("model.openai.base-url", () -> "http://localhost:8080");
        registry.add("model.temperature", () -> "0.7");
        registry.add("model.max-tokens", () -> "500");

        // Configure query settings
        registry.add("query.max-retrieved-chunks", () -> "5");
        registry.add("query.similarity-threshold", () -> "0.3");
        registry.add("query.timeout-seconds", () -> "30");
    }

    @BeforeAll
    static void setupWireMock() {
        wireMockServer = new WireMockServer(8080);
        wireMockServer.start();
        WireMock.configureFor("localhost", 8080);
    }

    @AfterAll
    static void tearDownWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @BeforeEach
    void setupMocks() {
        wireMockServer.resetAll();

        // Mock OpenAI chat completion endpoint
        stubFor(post(urlPathEqualTo("/v1/chat/completions"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "id": "chatcmpl-test",
                                    "object": "chat.completion",
                                    "created": 1234567890,
                                    "model": "gpt-4",
                                    "choices": [{
                                        "index": 0,
                                        "message": {
                                            "role": "assistant",
                                            "content": "Records in Java are special classes that act as transparent carriers for immutable data. They provide a compact syntax with automatic implementations of constructors, accessors, equals, hashCode, and toString methods."
                                        },
                                        "finish_reason": "stop"
                                    }],
                                    "usage": {
                                        "prompt_tokens": 150,
                                        "completion_tokens": 45,
                                        "total_tokens": 195
                                    }
                                }
                                """)));
    }

    /**
     * Test Requirement 4.1, 4.2, 4.3, 4.4: Ingest sample documentation
     */
    @Test
    @Order(1)
    void testIngestSampleDocumentation() throws Exception {
        // Get path to sample docs
        Path sampleDocsPath = Paths.get("src/test/resources/sample-docs");

        // Ingest documents
        var result = ingestionService.ingestDocuments(sampleDocsPath);

        // Verify ingestion was successful
        assertThat(result).isNotNull();
        assertThat(result.getDocumentsProcessed()).isGreaterThan(0);
        assertThat(result.getChunksCreated()).isGreaterThan(0);
        assertThat(result.getFailedDocuments()).isEqualTo(0);
    }

    /**
     * Test Requirement 1.1: Retrieve relevant documentation chunks
     */
    @Test
    @Order(2)
    void testQueryRetrievesRelevantDocuments() {
        QueryRequest request = new QueryRequest("What are records in Java?");

        ResponseEntity<QueryResponse> response = restTemplate.postForEntity(
                "/api/query",
                request,
                QueryResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getAnswer()).isNotBlank();
    }

    /**
     * Test Requirement 1.2: Generate contextual answer using Language Model
     */
    @Test
    @Order(3)
    void testQueryGeneratesContextualAnswer() {
        QueryRequest request = new QueryRequest("Explain Java records");

        ResponseEntity<QueryResponse> response = restTemplate.postForEntity(
                "/api/query",
                request,
                QueryResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        QueryResponse queryResponse = response.getBody();
        assertThat(queryResponse).isNotNull();
        assertThat(queryResponse.getAnswer()).contains("Records");
        assertThat(queryResponse.getAnswer()).isNotBlank();
    }

    /**
     * Test Requirement 1.3: Return answer within acceptable time
     */
    @Test
    @Order(4)
    void testQueryResponseTime() {
        QueryRequest request = new QueryRequest("What are sealed classes?");

        long startTime = System.currentTimeMillis();
        ResponseEntity<QueryResponse> response = restTemplate.postForEntity(
                "/api/query",
                request,
                QueryResponse.class
        );
        long endTime = System.currentTimeMillis();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        long responseTime = endTime - startTime;
        // Should respond within 10 seconds (requirement states 95% under 10s)
        assertThat(responseTime).isLessThan(10000);

        // Verify response time is also tracked in the response
        assertThat(response.getBody().getResponseTimeMs()).isGreaterThan(0);
    }

    /**
     * Test Requirement 1.5: Include source references
     */
    @Test
    @Order(5)
    void testQueryIncludesSourceReferences() {
        QueryRequest request = new QueryRequest("Tell me about records");

        ResponseEntity<QueryResponse> response = restTemplate.postForEntity(
                "/api/query",
                request,
                QueryResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        QueryResponse queryResponse = response.getBody();
        assertThat(queryResponse).isNotNull();
        assertThat(queryResponse.getSources()).isNotEmpty();
        assertThat(queryResponse.getSources().get(0).getFilename()).isNotBlank();
    }

    /**
     * Test Requirement 5.5: Track token usage
     */
    @Test
    @Order(6)
    void testTokenUsageTracking() {
        QueryRequest request = new QueryRequest("What are the benefits of sealed classes?");

        ResponseEntity<QueryResponse> response = restTemplate.postForEntity(
                "/api/query",
                request,
                QueryResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        QueryResponse queryResponse = response.getBody();
        assertThat(queryResponse).isNotNull();

        // Verify token usage is tracked in response
        assertThat(queryResponse.getTokenUsage()).isNotNull();
        assertThat(queryResponse.getTokenUsage().getPromptTokens()).isGreaterThan(0);
        assertThat(queryResponse.getTokenUsage().getCompletionTokens()).isGreaterThan(0);
        assertThat(queryResponse.getTokenUsage().getTotalTokens()).isGreaterThan(0);

        // Verify cumulative tracking
        var stats = tokenUsageTracker.getUsageStatistics();
        assertThat(stats.queryCount()).isGreaterThan(0);
        assertThat(stats.totalTokens()).isGreaterThan(0);
    }

    /**
     * Test Requirement 1.4: Handle case when no relevant documents found
     */
    @Test
    @Order(7)
    void testQueryWithNoRelevantDocuments() {
        QueryRequest request = new QueryRequest("What is quantum computing in Java?");

        ResponseEntity<QueryResponse> response = restTemplate.postForEntity(
                "/api/query",
                request,
                QueryResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        QueryResponse queryResponse = response.getBody();
        assertThat(queryResponse).isNotNull();
        // Should still return an answer, even if no highly relevant docs found
        assertThat(queryResponse.getAnswer()).isNotBlank();
    }
}
