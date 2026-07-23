package br.com.arquivolivre.myjavagenie.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.arquivolivre.myjavagenie.model.ChatMessage;
import br.com.arquivolivre.myjavagenie.model.ChatRequest;
import br.com.arquivolivre.myjavagenie.model.ChatResponse;
import br.com.arquivolivre.myjavagenie.service.IngestionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
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
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * End-to-end integration test for Chat UI with backend. Tests full conversation flow through UI,
 * session management, and WebSocket real-time updates. Tests Requirements: 8.1, 8.2, 8.3, 8.4, 8.6
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ChatUIEndToEndTest {

  @Container
  static GenericContainer<?> chromaContainer =
      new GenericContainer<>(DockerImageName.parse("chromadb/chroma:1.5.9"))
          .withExposedPorts(8000)
          .waitingFor(
              Wait.forHttp("/api/v2/heartbeat")
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
    registry.add(
        "vector-db.connection-url",
        () -> "http://localhost:" + chromaContainer.getMappedPort(8000));
    registry.add("vector-db.collection-name", () -> "test_e2e_chat");
    registry.add("rag.startup-validation.enabled", () -> "false");
    registry.add("opentelemetry.enabled", () -> "false");

    registry.add("model.provider", () -> "openai");
    registry.add("model.openai.api-key", () -> "test-api-key");
    registry.add("model.openai.model-name", () -> "gpt-4");
    registry.add("model.openai.base-url", () -> "http://localhost:8085/v1");
    registry.add("model.temperature", () -> "0.7");
    registry.add("model.max-tokens", () -> "500");

    registry.add("query.max-retrieved-chunks", () -> "5");
    registry.add("query.similarity-threshold", () -> "0.3");
    registry.add("query.timeout-seconds", () -> "30");

    registry.add("chat.session.timeout-seconds", () -> "1800");
  }

  @BeforeAll
  static void setupWireMock() {
    wireMockServer = new WireMockServer(8085);
    wireMockServer.start();
    WireMock.configureFor("localhost", 8085);
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
                                            "content": "Records in Java 25 are immutable data carriers."
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

  @Test
  @Order(1)
  void setupIngestDocumentation() throws Exception {
    Path sampleDocsPath = Paths.get("src/test/resources/sample-docs");
    var result = ingestionService.ingestDocuments(sampleDocsPath);

    assertThat(result).isNotNull();
    assertThat(result.getDocumentsProcessed()).isGreaterThan(0);
  }

  /**
   * Test Requirement 8.1, 8.2, 8.3, 8.4, 8.6: Full conversation flow through UI Simulates a
   * complete user interaction with the chat UI
   */
  @Test
  @Order(2)
  void testFullConversationFlowThroughUI() throws Exception {
    // Step 1: Establish WebSocket connection (simulating UI connection)
    StandardWebSocketClient client = new StandardWebSocketClient();

    String wsUrl = "ws://localhost:" + port + "/ws/chat";
    WebSocketSession wsSession =
        client.execute(new TextWebSocketHandler(), wsUrl).get(5, TimeUnit.SECONDS);
    assertThat(wsSession.isOpen()).isTrue();

    String webSocketSessionId = wsSession.getId();

    // Step 2: User sends first question (creates new chat session)
    ChatRequest request1 = new ChatRequest(null, "What are records in Java?", webSocketSessionId);
    ResponseEntity<ChatResponse> response1 =
        restTemplate.postForEntity("/api/chat/query", request1, ChatResponse.class);

    // Verify first response
    assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.OK);
    ChatResponse chatResponse1 = response1.getBody();
    assertThat(chatResponse1).isNotNull();
    assertThat(chatResponse1.getSessionId()).isNotNull();
    assertThat(chatResponse1.getAnswer()).isNotBlank();
    assertThat(chatResponse1.getSources()).isNotEmpty();

    String sessionId = chatResponse1.getSessionId();

    // Step 3: User sends follow-up question (maintains session)
    ChatRequest request2 =
        new ChatRequest(sessionId, "Can you give me an example?", webSocketSessionId);
    ResponseEntity<ChatResponse> response2 =
        restTemplate.postForEntity("/api/chat/query", request2, ChatResponse.class);

    // Verify second response maintains session
    assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.OK);
    ChatResponse chatResponse2 = response2.getBody();
    assertThat(chatResponse2).isNotNull();
    assertThat(chatResponse2.getSessionId()).isEqualTo(sessionId);
    assertThat(chatResponse2.getAnswer()).isNotBlank();

    // Step 4: User retrieves conversation history (UI displays history)
    ResponseEntity<ChatMessage[]> historyResponse =
        restTemplate.getForEntity("/api/chat/history?sessionId=" + sessionId, ChatMessage[].class);

    assertThat(historyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    ChatMessage[] messages = historyResponse.getBody();
    assertThat(messages).isNotNull();
    assertThat(messages.length).isEqualTo(4); // 2 user + 2 assistant messages

    // Verify conversation flow
    assertThat(messages[0].role()).isEqualTo(ChatMessage.MessageRole.USER);
    assertThat(messages[0].content()).contains("records");
    assertThat(messages[1].role()).isEqualTo(ChatMessage.MessageRole.ASSISTANT);
    assertThat(messages[2].role()).isEqualTo(ChatMessage.MessageRole.USER);
    assertThat(messages[2].content()).contains("example");
    assertThat(messages[3].role()).isEqualTo(ChatMessage.MessageRole.ASSISTANT);

    // Step 5: User clears history (UI reset)
    restTemplate.delete("/api/chat/history?sessionId=" + sessionId);

    ResponseEntity<ChatMessage[]> clearedHistory =
        restTemplate.getForEntity("/api/chat/history?sessionId=" + sessionId, ChatMessage[].class);
    assertThat(clearedHistory.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(clearedHistory.getBody()).isEmpty();

    wsSession.close();
  }

  /** Test Requirement 8.4: Session management across multiple concurrent users */
  @Test
  @Order(3)
  void testMultipleUserSessionManagement() {
    // User 1 creates a session
    ChatRequest user1Request1 = new ChatRequest(null, "What are records?");
    ResponseEntity<ChatResponse> user1Response1 =
        restTemplate.postForEntity("/api/chat/query", user1Request1, ChatResponse.class);
    String user1SessionId = user1Response1.getBody().getSessionId();

    // User 2 creates a different session
    ChatRequest user2Request1 = new ChatRequest(null, "What are sealed classes?");
    ResponseEntity<ChatResponse> user2Response1 =
        restTemplate.postForEntity("/api/chat/query", user2Request1, ChatResponse.class);
    String user2SessionId = user2Response1.getBody().getSessionId();

    // Verify sessions are different
    assertThat(user1SessionId).isNotEqualTo(user2SessionId);

    // User 1 continues conversation
    ChatRequest user1Request2 = new ChatRequest(user1SessionId, "Tell me more");
    restTemplate.postForEntity("/api/chat/query", user1Request2, ChatResponse.class);

    // User 2 continues conversation
    ChatRequest user2Request2 = new ChatRequest(user2SessionId, "Give examples");
    restTemplate.postForEntity("/api/chat/query", user2Request2, ChatResponse.class);

    // Verify User 1 history
    ResponseEntity<ChatMessage[]> user1History =
        restTemplate.getForEntity(
            "/api/chat/history?sessionId=" + user1SessionId, ChatMessage[].class);
    assertThat(user1History.getBody()).hasSize(4);
    assertThat(user1History.getBody()[0].content()).contains("records");

    // Verify User 2 history
    ResponseEntity<ChatMessage[]> user2History =
        restTemplate.getForEntity(
            "/api/chat/history?sessionId=" + user2SessionId, ChatMessage[].class);
    assertThat(user2History.getBody()).hasSize(4);
    assertThat(user2History.getBody()[0].content()).contains("sealed classes");
  }

  /**
   * Test Requirement 8.6: WebSocket connection establishment Note: WebSocket status updates require
   * QueryService integration which is tested separately
   */
  @Test
  @Order(4)
  void testWebSocketConnectionEstablishment() throws Exception {
    StandardWebSocketClient client = new StandardWebSocketClient();

    String wsUrl = "ws://localhost:" + port + "/ws/chat";
    WebSocketSession wsSession =
        client.execute(new TextWebSocketHandler(), wsUrl).get(5, TimeUnit.SECONDS);

    // Verify WebSocket connection is established
    assertThat(wsSession.isOpen()).isTrue();
    assertThat(wsSession.getId()).isNotNull();

    // Execute a query to verify the system works
    String webSocketSessionId = wsSession.getId();
    ChatRequest request = new ChatRequest(null, "Explain records", webSocketSessionId);
    ResponseEntity<ChatResponse> response =
        restTemplate.postForEntity("/api/chat/query", request, ChatResponse.class);

    // Verify query succeeds
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getAnswer()).isNotBlank();
    assertThat(response.getBody().getSources()).isNotEmpty();

    wsSession.close();
  }

  /** Test error handling in full conversation flow */
  @Test
  @Order(5)
  void testErrorHandlingInConversationFlow() {
    // Test with invalid session ID - system should handle gracefully
    ChatRequest invalidRequest = new ChatRequest("invalid-session-id", "What are records?");
    ResponseEntity<ChatResponse> response =
        restTemplate.postForEntity("/api/chat/query", invalidRequest, ChatResponse.class);

    // Should handle gracefully and return a valid response
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getSessionId()).isNotNull();
    assertThat(response.getBody().getAnswer()).isNotBlank();
  }
}
