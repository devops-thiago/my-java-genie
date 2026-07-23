package br.com.arquivolivre.myjavagenie.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents a chunk of a document with its content, metadata, and token count. Used for storing
 * and retrieving document segments in the vector database.
 */
public class DocumentChunk {
  private String id;
  private String content;
  private DocumentMetadata metadata;
  private int tokenCount;

  public DocumentChunk() {
    this.id = UUID.randomUUID().toString();
  }

  public DocumentChunk(String content, DocumentMetadata metadata, int tokenCount) {
    this.id = UUID.randomUUID().toString();
    this.content = content;
    this.metadata = DocumentMetadata.copyOf(metadata);
    this.tokenCount = tokenCount;
  }

  public DocumentChunk(String id, String content, DocumentMetadata metadata, int tokenCount) {
    this.id = id;
    this.content = content;
    this.metadata = DocumentMetadata.copyOf(metadata);
    this.tokenCount = tokenCount;
  }

  /** Copy constructor used for defensive copies. */
  public DocumentChunk(DocumentChunk other) {
    this.id = other.id;
    this.content = other.content;
    this.metadata = DocumentMetadata.copyOf(other.metadata);
    this.tokenCount = other.tokenCount;
  }

  /** Returns a defensive copy of the given chunk, or {@code null} if {@code chunk} is null. */
  public static DocumentChunk copyOf(DocumentChunk chunk) {
    return chunk == null ? null : new DocumentChunk(chunk);
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public DocumentMetadata getMetadata() {
    return DocumentMetadata.copyOf(metadata);
  }

  public void setMetadata(DocumentMetadata metadata) {
    this.metadata = DocumentMetadata.copyOf(metadata);
  }

  public int getTokenCount() {
    return tokenCount;
  }

  public void setTokenCount(int tokenCount) {
    this.tokenCount = tokenCount;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DocumentChunk that = (DocumentChunk) o;
    return tokenCount == that.tokenCount
        && Objects.equals(id, that.id)
        && Objects.equals(content, that.content)
        && Objects.equals(metadata, that.metadata);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, content, metadata, tokenCount);
  }

  @Override
  public String toString() {
    return "DocumentChunk{"
        + "id='"
        + id
        + '\''
        + ", content='"
        + (content != null && content.length() > 50 ? content.substring(0, 50) + "..." : content)
        + '\''
        + ", metadata="
        + metadata
        + ", tokenCount="
        + tokenCount
        + '}';
  }
}
