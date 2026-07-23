package br.com.arquivolivre.myjavagenie.service;

import br.com.arquivolivre.myjavagenie.exception.LlmException;
import br.com.arquivolivre.myjavagenie.model.ChatMessage;
import br.com.arquivolivre.myjavagenie.model.ChatResponse;
import br.com.arquivolivre.myjavagenie.model.ChatSession;
import br.com.arquivolivre.myjavagenie.model.QueryResponse;
import br.com.arquivolivre.myjavagenie.model.QueryStatus;
import br.com.arquivolivre.myjavagenie.websocket.ChatWebSocketHandler;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ChatService {
  private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

  private final QueryService queryService;
  private final SessionManager sessionManager;

  @Autowired(required = false)
  private ChatWebSocketHandler webSocketHandler;

  public ChatService(QueryService queryService, SessionManager sessionManager) {
    this.queryService = queryService;
    this.sessionManager = sessionManager;
  }

  public ChatResponse processMessage(String sessionId, String message) {
    return processMessage(sessionId, message, null);
  }

  public ChatResponse processMessage(String sessionId, String message, String webSocketSessionId) {
    long started = System.currentTimeMillis();
    ChatSession session = sessionManager.getOrCreateSession(sessionId);
    session.addMessage(new ChatMessage(ChatMessage.MessageRole.USER, message));

    try {
      sendStatusUpdate(
          webSocketSessionId,
          session.getSessionId(),
          QueryStatus.ProcessingStage.EMBEDDING,
          "Preparing search query");
      sendStatusUpdate(
          webSocketSessionId,
          session.getSessionId(),
          QueryStatus.ProcessingStage.SEARCHING,
          "Searching documents");
      sendStatusUpdate(
          webSocketSessionId,
          session.getSessionId(),
          QueryStatus.ProcessingStage.GENERATING,
          "Generating response");

      QueryResponse queryResponse = queryService.query(message);
      session.addMessage(
          new ChatMessage(
              ChatMessage.MessageRole.ASSISTANT,
              queryResponse.getAnswer(),
              queryResponse.getSources()));

      long elapsed = System.currentTimeMillis() - started;
      ChatResponse response =
          new ChatResponse(
              session.getSessionId(),
              queryResponse.getAnswer(),
              queryResponse.getSources(),
              null,
              elapsed);

      if (webSocketHandler != null) {
        sendStatus(
            webSocketSessionId,
            session.getSessionId(),
            new QueryStatus(session.getSessionId(), response));
      }

      logger.info("Chat RAG reply ready for session {} in {}ms", session.getSessionId(), elapsed);
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
      throw new LlmException("Failed to process chat query: " + e.getMessage(), e);
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
    if (webSocketHandler == null) {
      return;
    }
    if (webSocketSessionId != null && !webSocketSessionId.isBlank()) {
      webSocketHandler.sendStatusUpdate(webSocketSessionId, status);
    }
    webSocketHandler.sendStatusUpdateForChatSession(chatSessionId, status);
  }
}
