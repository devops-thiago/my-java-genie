package br.com.arquivolivre.myjavagenie.model;

/** Represents the status of a query processing operation. */
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
    this.completed = false;
  }

  public QueryStatus(String sessionId, ChatResponse response) {
    this.sessionId = sessionId;
    this.stage = ProcessingStage.COMPLETED;
    this.message = "Query processing completed";
    this.completed = true;
    this.response = ChatResponse.copyOf(response);
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
    return ChatResponse.copyOf(response);
  }

  public void setResponse(ChatResponse response) {
    this.response = ChatResponse.copyOf(response);
  }

  @Override
  public String toString() {
    return "QueryStatus{"
        + "sessionId='"
        + sessionId
        + '\''
        + ", stage="
        + stage
        + ", message='"
        + message
        + '\''
        + ", completed="
        + completed
        + '}';
  }

  /** Enum representing the stages of query processing. */
  public enum ProcessingStage {
    EMBEDDING("Generating query embedding"),
    SEARCHING("Searching for relevant documents"),
    GENERATING("Generating response"),
    COMPLETED("Processing completed");

    private final String description;

    ProcessingStage(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }
}
