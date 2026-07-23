package br.com.arquivolivre.myjavagenie.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record ChatMessage(
    String id, MessageRole role, String content, Instant timestamp, List<SourceReference> sources) {

  public ChatMessage(MessageRole role, String content) {
    this(UUID.randomUUID().toString(), role, content, Instant.now(), new ArrayList<>());
  }

  public ChatMessage(MessageRole role, String content, List<SourceReference> sources) {
    this(
        UUID.randomUUID().toString(),
        role,
        content,
        Instant.now(),
        sources != null ? new ArrayList<>(sources) : new ArrayList<>());
  }

  @JsonCreator
  public static ChatMessage create(
      @JsonProperty("id") String id,
      @JsonProperty("role") MessageRole role,
      @JsonProperty("content") String content,
      @JsonProperty("timestamp") Instant timestamp,
      @JsonProperty("sources") List<SourceReference> sources) {
    return new ChatMessage(
        id != null ? id : UUID.randomUUID().toString(),
        role,
        content,
        timestamp != null ? timestamp : Instant.now(),
        sources != null ? new ArrayList<>(sources) : new ArrayList<>());
  }

  @Override
  public List<SourceReference> sources() {
    return new ArrayList<>(sources);
  }

  public enum MessageRole {
    USER,
    ASSISTANT
  }
}
