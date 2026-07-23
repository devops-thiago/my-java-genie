package br.com.arquivolivre.myjavagenie.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.arquivolivre.myjavagenie.model.ChatMessage;
import br.com.arquivolivre.myjavagenie.model.ChatResponse;
import br.com.arquivolivre.myjavagenie.model.QueryResponse;
import br.com.arquivolivre.myjavagenie.model.SourceReference;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

  @Mock private QueryService queryService;

  private SessionManager sessionManager;
  private ChatService chatService;

  @BeforeEach
  void setUp() {
    sessionManager = new SessionManager();
    ReflectionTestUtils.setField(sessionManager, "sessionTimeoutSeconds", 1800L);
    chatService = new ChatService(queryService, sessionManager);
  }

  @Test
  void processMessageShouldUseRagAndStoreSources() {
    when(queryService.query(anyString()))
        .thenReturn(
            new QueryResponse(
                "Resposta", List.of(new SourceReference("doc.html", null, 0)), "translated", 10L));

    ChatResponse response = chatService.processMessage("session-1", "O que sao records?");

    assertThat(response.getAnswer()).isEqualTo("Resposta");
    assertThat(response.getSources()).hasSize(1);
    assertThat(chatService.getHistory("session-1")).hasSize(2);
    assertThat(chatService.getHistory("session-1").get(1).role())
        .isEqualTo(ChatMessage.MessageRole.ASSISTANT);
    verify(queryService).query("O que sao records?");
  }
}
