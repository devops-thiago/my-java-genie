package br.com.arquivolivre.myjavagenie.service;

import br.com.arquivolivre.myjavagenie.exception.LlmException;
import br.com.arquivolivre.myjavagenie.model.ChatMessage;
import br.com.arquivolivre.myjavagenie.model.ChatResponse;
import br.com.arquivolivre.myjavagenie.model.ChatSession;
import br.com.arquivolivre.myjavagenie.model.QueryStatus;
import br.com.arquivolivre.myjavagenie.websocket.ChatWebSocketHandler;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ChatService {
  private static final Logger logger = LoggerFactory.getLogger(ChatService.class);
  private static final String SYSTEM_PROMPT =
      "You are a helpful assistant. Answer clearly and concisely.";

  private final ChatLanguageModel chatModel;
  private final SessionManager sessionManager;

  @Autowired(required = false)
  private ChatWebSocketHandler webSocketHandler;

  public ChatService(ChatLanguageModel chatModel, SessionManager sessionManager) {
    this.chatModel = chatModel;
    this.sessionManager = sessionManager;
  }

  public ChatResponse processMessage(String sessionId, String message) {
    return processMessage(sessionId, message, null);
  }

  public ChatResponse processMessage(String sessionId, String message, String webSocketSessionId) {
    long started = System.currentTimeMillis();
    ChatSession session = sessionManager.getOrCreateSession(sessionId);

    session.addMessage(new ChatMessage(ChatMessage.MessageRole.USER, message));

    sendStatusUpdate(
        webSocketSessionId,
        session.getSessionId(),
        QueryStatus.ProcessingStage.GENERATING,
        "Generating response");

    try {
      String answer = generateReply(session);
      session.addMessage(new ChatMessage(ChatMessage.MessageRole.ASSISTANT, answer));
      long elapsed = System.currentTimeMillis() - started;
      ChatResponse response = new ChatResponse(session.getSessionId(), answer, elapsed);

      if (webSocketHandler != null) {
        QueryStatus completion = new QueryStatus(session.getSessionId(), response);
        sendStatus(webSocketSessionId, session.getSessionId(), completion);
      }

      logger.info("Chat reply ready for session {} in {}ms", session.getSessionId(), elapsed);
      return response;
    } catch (Exception e) {
      sendStatusUpdate(
          webSocketSessionId,
          session.getSessionId(),
          QueryStatus.ProcessingStage.ERROR,
          e.getMessage());
      if (e instanceof LlmException llmException) {
        throw llmException;
      }
      throw new LlmException("Failed to get response from LLM: " + e.getMessage(), e);
    }
  }

  public List<ChatMessage> getHistory(String sessionId) {
    ChatSession session = sessionManager.getSession(sessionId);
    if (session == null) {
      return List.of();
    }
    return session.getMessages();
  }

  public boolean clearHistory(String sessionId) {
    ChatSession session = sessionManager.getSession(sessionId);
    if (session == null) {
      return false;
    }
    session.clearMessages();
    return true;
  }

  public boolean sessionExists(String sessionId) {
    return sessionManager.getSession(sessionId) != null;
  }

  private String generateReply(ChatSession session) {
    List<dev.langchain4j.data.message.ChatMessage> messages = new ArrayList<>();
    messages.add(SystemMessage.from(SYSTEM_PROMPT));

    for (ChatMessage message : session.getMessages()) {
      if (message.role() == ChatMessage.MessageRole.USER) {
        messages.add(UserMessage.from(message.content()));
      } else {
        messages.add(AiMessage.from(message.content()));
      }
    }

    Response<AiMessage> response = chatModel.generate(messages);
    if (response == null || response.content() == null || response.content().text() == null) {
      throw new LlmException("LLM returned an empty response");
    }
    return response.content().text();
  }

  private void sendStatusUpdate(
      String webSocketSessionId,
      String chatSessionId,
      QueryStatus.ProcessingStage stage,
      String message) {
    if (webSocketHandler == null) {
      return;
    }
    sendStatus(webSocketSessionId, chatSessionId, new QueryStatus(chatSessionId, stage, message));
  }

  private void sendStatus(String webSocketSessionId, String chatSessionId, QueryStatus status) {
    if (webSocketSessionId != null && !webSocketSessionId.isBlank()) {
      webSocketHandler.sendStatusUpdate(webSocketSessionId, status);
    }
    webSocketHandler.sendStatusUpdateForChatSession(chatSessionId, status);
  }
}
