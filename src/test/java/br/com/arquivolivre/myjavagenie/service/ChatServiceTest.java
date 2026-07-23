package br.com.arquivolivre.myjavagenie.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.arquivolivre.myjavagenie.exception.LlmException;
import br.com.arquivolivre.myjavagenie.model.ChatMessage;
import br.com.arquivolivre.myjavagenie.model.ChatResponse;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

  @Mock private ChatModel chatModel;

  private SessionManager sessionManager;
  private ChatService chatService;

  @BeforeEach
  void setUp() {
    sessionManager = new SessionManager();
    ReflectionTestUtils.setField(sessionManager, "sessionTimeoutSeconds", 1800L);
    chatService = new ChatService(chatModel, sessionManager);
  }

  @Test
  void processMessageShouldReturnLlmAnswerAndStoreHistory() {
    when(chatModel.chat(anyList())).thenReturn(llmResponse("hello back"));

    ChatResponse response = chatService.processMessage("session-1", "hello");

    assertThat(response.getSessionId()).isEqualTo("session-1");
    assertThat(response.getAnswer()).isEqualTo("hello back");
    assertThat(response.getResponseTimeMs()).isGreaterThanOrEqualTo(0);
    assertThat(chatService.getHistory("session-1")).hasSize(2);
    assertThat(chatService.getHistory("session-1").get(0).role())
        .isEqualTo(ChatMessage.MessageRole.USER);
    assertThat(chatService.getHistory("session-1").get(1).role())
        .isEqualTo(ChatMessage.MessageRole.ASSISTANT);
    verify(chatModel).chat(anyList());
  }

  @Test
  void processMessageShouldWrapLlmFailures() {
    when(chatModel.chat(anyList())).thenThrow(new RuntimeException("boom"));

    assertThatThrownBy(() -> chatService.processMessage("session-1", "hello"))
        .isInstanceOf(LlmException.class)
        .hasMessageContaining("Failed to get response from LLM");
  }

  @Test
  void getHistoryShouldReturnEmptyWhenSessionMissing() {
    assertThat(chatService.getHistory("missing")).isEmpty();
  }

  @Test
  void clearHistoryShouldReturnFalseWhenSessionMissing() {
    assertThat(chatService.clearHistory("missing")).isFalse();
  }

  @Test
  void clearHistoryShouldClearExistingSession() {
    when(chatModel.chat(anyList())).thenReturn(llmResponse("ok"));
    chatService.processMessage("session-1", "hi");

    assertThat(chatService.clearHistory("session-1")).isTrue();
    assertThat(chatService.getHistory("session-1")).isEmpty();
  }

  @Test
  void sessionExistsShouldReflectSessionManager() {
    when(chatModel.chat(anyList())).thenReturn(llmResponse("ok"));
    chatService.processMessage("session-1", "hi");

    assertThat(chatService.sessionExists("session-1")).isTrue();
    assertThat(chatService.sessionExists("missing")).isFalse();
  }

  @Test
  void getHistoryShouldReturnMessages() {
    when(chatModel.chat(anyList())).thenReturn(llmResponse("ok"));
    chatService.processMessage("session-1", "hi");

    List<ChatMessage> history = chatService.getHistory("session-1");
    assertThat(history).hasSize(2);
    assertThat(history.get(0).content()).isEqualTo("hi");
  }

  private static dev.langchain4j.model.chat.response.ChatResponse llmResponse(String text) {
    return dev.langchain4j.model.chat.response.ChatResponse.builder()
        .aiMessage(AiMessage.from(text))
        .build();
  }
}
