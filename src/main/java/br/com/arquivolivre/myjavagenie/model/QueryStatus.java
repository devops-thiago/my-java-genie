package br.com.arquivolivre.myjavagenie.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class QueryStatus {
  private String sessionId;
  private ProcessingStage stage;
  private String message;
  private boolean completed;
  private ChatResponse response;

  public QueryStatus() {}

  public QueryStatus(String sessionId, ProcessingStage stage, String message) {
    this.sessionId = sessionId;
    this.stage = stage;
    this.message = message;
    this.completed = stage == ProcessingStage.COMPLETE || stage == ProcessingStage.ERROR;
  }

  public QueryStatus(String sessionId, ChatResponse response) {
    this.sessionId = sessionId;
    this.stage = ProcessingStage.COMPLETE;
    this.message = "Query processing completed";
    this.completed = true;
    this.response = response;
  }

  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  public ProcessingStage getStage() {
    return stage;
  }

  public void setStage(ProcessingStage stage) {
    this.stage = stage;
  }

  /** Field name expected by the chat UI. */
  @JsonProperty("status")
  public String getStatus() {
    return stage == null ? null : stage.name();
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public boolean isCompleted() {
    return completed;
  }

  public void setCompleted(boolean completed) {
    this.completed = completed;
  }

  public ChatResponse getResponse() {
    return response;
  }

  public void setResponse(ChatResponse response) {
    this.response = response;
  }

  public enum ProcessingStage {
    EMBEDDING,
    SEARCHING,
    GENERATING,
    COMPLETE,
    ERROR
  }
}
