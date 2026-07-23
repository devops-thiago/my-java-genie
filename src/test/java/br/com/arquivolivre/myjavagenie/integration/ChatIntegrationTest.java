package br.com.arquivolivre.myjavagenie.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.arquivolivre.myjavagenie.model.ChatMessage;
import br.com.arquivolivre.myjavagenie.model.ChatRequest;
import br.com.arquivolivre.myjavagenie.model.ChatResponse;
import br.com.arquivolivre.myjavagenie.model.QueryStatus;
import br.com.arquivolivre.myjavagenie.service.IngestionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/** Integration test for chat functionality. Tests Requirements: 8.1, 8.2, 8.3, 8.4, 8.7 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ChatIntegrationTest {

  @Container
  static GenericContainer<?> chromaContainer =
      new GenericContainer<>(DockerImageName.parse("chromadb/chroma:0.4.15"))
          .withExposedPorts(8000)
          .waitingFor(
              Wait.forHttp("/api/v1/heartbeat")
                  .forPort(8000)
                  .forStatusCode(200)
                  .withStartupTimeout(Duration.ofSeconds(60)));

  private static WireMockServer wireMockServer;
  private final ObjectMapper objectMapper = new ObjectMapper();
  @LocalServerPort private int port;
  @Autowired private TestRestTemplate restTemplate;
  @Autowired private IngestionService ingestionService;

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    // Configure ChromaDB connection
    registry.add(
        "vector-db.connection-url",
        () -> "http://localhost:" + chromaContainer.getMappedPort(8000));
    registry.add("vector-db.collection-name", () -> "test_chat_docs");
    registry.add("rag.startup-validation.enabled", () -> "false");
    registry.add("opentelemetry.enabled", () -> "false");

    // Configure to use OpenAI provider (will be mocked)
    registry.add("model.provider", () -> "openai");
    registry.add("model.openai.api-key", () -> "test-api-key");
    registry.add("model.openai.model-name", () -> "gpt-4");
    registry.add("model.openai.base-url", () -> "http://localhost:8081/v1");
    registry.add("model.temperature", () -> "0.7");
    registry.add("model.max-tokens", () -> "500");

    // Configure query settings
    registry.add("query.max-retrieved-chunks", () -> "5");
    registry.add("query.similarity-threshold", () -> "0.3");
    registry.add("query.timeout-seconds", () -> "30");

    // Configure chat session timeout
    registry.add("chat.session.timeout-seconds", () -> "1800");
  }

  @BeforeAll
  static void setupWireMock() {
    wireMockServer = new WireMockServer(8081);
    wireMockServer.start();
    WireMock.configureFor("localhost", 8081);
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
    stubFor(
        post(urlPathEqualTo("/v1/chat/completions"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """
                                {
                                    "id": "chatcmpl-test",
                                    "object": "chat.completion",
                                    "created": 1234567890,
                                    "model": "gpt-4",
                                    "choices": [{
                                        "index": 0,
                                        "message": {
                                            "role": "assistant",
                                            "content": "Records in Java are immutable data carriers with automatic implementations of constructors, accessors, equals, hashCode, and toString methods."
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

  /** Setup: Ingest sample documentation */
  @Test
  @Order(1)
  void setupIngestSampleDocumentation() throws Exception {
    Path sampleDocsPath = Paths.get("src/test/resources/sample-docs");
    var result = ingestionService.ingestDocuments(sampleDocsPath);

    assertThat(result).isNotNull();
    assertThat(result.getDocumentsProcessed()).isGreaterThan(0);
  }

  /** Test Requirement 8.1, 8.2: Chat session creation and message processing */
  @Test
  @Order(2)
  void testChatSessionCreationAndMessageProcessing() {
    // Create a chat request without session ID (should create new session)
    ChatRequest request = new ChatRequest(null, "What are records in Java?");

    ResponseEntity<ChatResponse> response =
        restTemplate.postForEntity("/api/chat/query", request, ChatResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    ChatResponse chatResponse = response.getBody();
    assertThat(chatResponse).isNotNull();
    assertThat(chatResponse.getSessionId()).isNotNull();
    assertThat(chatResponse.getAnswer()).isNotBlank();
    assertThat(chatResponse.getAnswer()).contains("Records");
  }

  /** Test Requirement 8.4: Maintain conversation context across multiple questions */
  @Test
  @Order(3)
  void testConversationContextMaintenance() {
    // First message - create session
    ChatRequest request1 = new ChatRequest(null, "What are records in Java?");
    ResponseEntity<ChatResponse> response1 =
        restTemplate.postForEntity("/api/chat/query", request1, ChatResponse.class);

    assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.OK);
    String sessionId = response1.getBody().getSessionId();
    assertThat(sessionId).isNotNull();

    // Second message - use same session
    ChatRequest request2 = new ChatRequest(sessionId, "What are sealed classes?");
    ResponseEntity<ChatResponse> response2 =
        restTemplate.postForEntity("/api/chat/query", request2, ChatResponse.class);

    assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response2.getBody().getSessionId()).isEqualTo(sessionId);

    // Verify history contains both messages
    ResponseEntity<ChatMessage[]> historyResponse =
        restTemplate.getForEntity("/api/chat/history?sessionId=" + sessionId, ChatMessage[].class);

    assertThat(historyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    ChatMessage[] messages = historyResponse.getBody();
    assertThat(messages).isNotNull();
    assertThat(messages.length).isEqualTo(4); // 2 user messages + 2 assistant responses

    // Verify message order and roles
    assertThat(messages[0].role()).isEqualTo(ChatMessage.MessageRole.USER);
    assertThat(messages[0].content()).contains("records");
    assertThat(messages[1].role()).isEqualTo(ChatMessage.MessageRole.ASSISTANT);
    assertThat(messages[2].role()).isEqualTo(ChatMessage.MessageRole.USER);
    assertThat(messages[2].content()).contains("sealed classes");
    assertThat(messages[3].role()).isEqualTo(ChatMessage.MessageRole.ASSISTANT);
  }

  /** Test Requirement 8.3: Retrieve message history */
  @Test
  @Order(4)
  void testMessageHistoryRetrieval() {
    // Create a session with messages
    ChatRequest request = new ChatRequest(null, "Explain Java records");
    ResponseEntity<ChatResponse> response =
        restTemplate.postForEntity("/api/chat/query", request, ChatResponse.class);

    String sessionId = response.getBody().getSessionId();

    // Retrieve history
    ResponseEntity<ChatMessage[]> historyResponse =
        restTemplate.getForEntity("/api/chat/history?sessionId=" + sessionId, ChatMessage[].class);

    assertThat(historyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    ChatMessage[] messages = historyResponse.getBody();
    assertThat(messages).isNotNull();
    assertThat(messages.length).isEqualTo(2); // 1 user message + 1 assistant response

    // Verify message content
    assertThat(messages[0].content()).isEqualTo("Explain Java records");
    assertThat(messages[1].content()).isNotBlank();
  }

  /** Test Requirement 8.3: History retrieval for non-existent session */
  @Test
  @Order(5)
  void testHistoryRetrievalForNonExistentSession() {
    String nonExistentSessionId = "non-existent-session-id";

    ResponseEntity<ChatMessage[]> historyResponse =
        restTemplate.getForEntity(
            "/api/chat/history?sessionId=" + nonExistentSessionId, ChatMessage[].class);

    assertThat(historyResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  /** Test Requirement 8.7: Clear conversation history */
  @Test
  @Order(6)
  void testClearConversationHistory() {
    // Create a session with messages
    ChatRequest request1 = new ChatRequest(null, "What are records?");
    ResponseEntity<ChatResponse> response1 =
        restTemplate.postForEntity("/api/chat/query", request1, ChatResponse.class);

    String sessionId = response1.getBody().getSessionId();

    // Add another message
    ChatRequest request2 = new ChatRequest(sessionId, "What are sealed classes?");
    restTemplate.postForEntity("/api/chat/query", request2, ChatResponse.class);

    // Verify history has messages
    ResponseEntity<ChatMessage[]> historyBefore =
        restTemplate.getForEntity("/api/chat/history?sessionId=" + sessionId, ChatMessage[].class);
    assertThat(historyBefore.getBody()).isNotNull();
    assertThat(historyBefore.getBody().length).isGreaterThan(0);

    // Clear history
    restTemplate.delete("/api/chat/history?sessionId=" + sessionId);

    // Verify history is empty
    ResponseEntity<ChatMessage[]> historyAfter =
        restTemplate.getForEntity("/api/chat/history?sessionId=" + sessionId, ChatMessage[].class);
    assertThat(historyAfter.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(historyAfter.getBody()).isEmpty();
  }

  /** Test Requirement 8.7: Clear history for non-existent session */
  @Test
  @Order(7)
  void testClearHistoryForNonExistentSession() {
    String nonExistentSessionId = "non-existent-session-id";

    ResponseEntity<Void> response =
        restTemplate.exchange(
            "/api/chat/history?sessionId=" + nonExistentSessionId,
            org.springframework.http.HttpMethod.DELETE,
            null,
            Void.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  /** Test Requirement 8.5: Display source references in chat responses */
  @Test
  @Order(8)
  void testSourceReferencesInChatResponse() {
    ChatRequest request = new ChatRequest(null, "Tell me about records");

    ResponseEntity<ChatResponse> response =
        restTemplate.postForEntity("/api/chat/query", request, ChatResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    ChatResponse chatResponse = response.getBody();
    assertThat(chatResponse).isNotNull();
    assertThat(chatResponse.getSources()).isNotEmpty();
    assertThat(chatResponse.getSources().get(0).getFilename()).isNotBlank();
  }

  /** Test Requirement 8.6: WebSocket connection and status messages */
  @Test
  @Order(9)
  void testWebSocketConnectionAndMessages() throws Exception {
    StandardWebSocketClient client = new StandardWebSocketClient();
    List<QueryStatus> receivedStatuses = new ArrayList<>();
    CompletableFuture<Void> completionFuture = new CompletableFuture<>();

    TextWebSocketHandler handler =
        new TextWebSocketHandler() {
          @Override
          protected void handleTextMessage(WebSocketSession session, TextMessage message)
              throws Exception {
            QueryStatus status = objectMapper.readValue(message.getPayload(), QueryStatus.class);
            receivedStatuses.add(status);

            if (status.isCompleted()) {
              completionFuture.complete(null);
            }
          }
        };

    String wsUrl = "ws://localhost:" + port + "/ws/chat";
    WebSocketSession wsSession = client.execute(handler, wsUrl).get(5, TimeUnit.SECONDS);

    assertThat(wsSession).isNotNull();
    assertThat(wsSession.isOpen()).isTrue();

    String webSocketSessionId = wsSession.getId();

    // Send a chat query with WebSocket session ID
    ChatRequest request = new ChatRequest(null, "What are records?", webSocketSessionId);
    restTemplate.postForEntity("/api/chat/query", request, ChatResponse.class);

    // Wait for completion message
    completionFuture.get(10, TimeUnit.SECONDS);

    // Verify we received status updates
    assertThat(receivedStatuses).isNotEmpty();
    assertThat(receivedStatuses)
        .anyMatch(s -> s.getStage() == QueryStatus.ProcessingStage.EMBEDDING);
    assertThat(receivedStatuses)
        .anyMatch(s -> s.getStage() == QueryStatus.ProcessingStage.SEARCHING);
    assertThat(receivedStatuses)
        .anyMatch(s -> s.getStage() == QueryStatus.ProcessingStage.GENERATING);
    assertThat(receivedStatuses)
        .anyMatch(s -> s.getStage() == QueryStatus.ProcessingStage.COMPLETED);

    // Verify completion status has response
    QueryStatus completionStatus =
        receivedStatuses.stream().filter(QueryStatus::isCompleted).findFirst().orElse(null);
    assertThat(completionStatus).isNotNull();
    assertThat(completionStatus.getResponse()).isNotNull();
    assertThat(completionStatus.getResponse().getAnswer()).isNotBlank();

    wsSession.close();
  }

  /** Test chat request validation */
  @Test
  @Order(10)
  void testChatRequestValidation() {
    // Test with blank message
    ChatRequest request = new ChatRequest(null, "");

    ResponseEntity<ChatResponse> response =
        restTemplate.postForEntity("/api/chat/query", request, ChatResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }
}
