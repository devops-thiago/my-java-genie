package br.com.arquivolivre.myjavagenie.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.arquivolivre.myjavagenie.model.ChatMessage;
import br.com.arquivolivre.myjavagenie.model.ChatResponse;
import br.com.arquivolivre.myjavagenie.service.ChatService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ChatControllerTest {

  private ChatService chatService;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    chatService = mock(ChatService.class);
    mockMvc =
        MockMvcBuilders.standaloneSetup(new ChatController(chatService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  void queryShouldReturnAnswer() throws Exception {
    when(chatService.processMessage(eq("s1"), eq("hello"), isNull()))
        .thenReturn(new ChatResponse("s1", "hi there", 12L));

    mockMvc
        .perform(
            post("/api/chat/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sessionId\":\"s1\",\"message\":\"hello\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sessionId").value("s1"))
        .andExpect(jsonPath("$.answer").value("hi there"))
        .andExpect(jsonPath("$.responseTimeMs").value(12));
  }

  @Test
  void queryShouldRejectBlankMessage() throws Exception {
    mockMvc
        .perform(
            post("/api/chat/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sessionId\":\"s1\",\"message\":\"\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void getHistoryShouldReturnNotFoundWhenMissing() throws Exception {
    when(chatService.getHistory("missing")).thenReturn(List.of());
    when(chatService.sessionExists("missing")).thenReturn(false);

    mockMvc
        .perform(get("/api/chat/history").param("sessionId", "missing"))
        .andExpect(status().isNotFound());
  }

  @Test
  void getHistoryShouldReturnMessages() throws Exception {
    when(chatService.getHistory("s1"))
        .thenReturn(List.of(new ChatMessage(ChatMessage.MessageRole.USER, "hello")));
    when(chatService.sessionExists("s1")).thenReturn(true);

    mockMvc
        .perform(get("/api/chat/history").param("sessionId", "s1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].content").value("hello"));
  }

  @Test
  void clearHistoryShouldReturnNoContent() throws Exception {
    when(chatService.clearHistory("s1")).thenReturn(true);

    mockMvc
        .perform(delete("/api/chat/history").param("sessionId", "s1"))
        .andExpect(status().isNoContent());
  }
}
