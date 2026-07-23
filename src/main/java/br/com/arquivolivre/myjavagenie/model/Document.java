package br.com.arquivolivre.myjavagenie.model;

import java.util.Objects;

/**
 * Represents a source document to be processed and ingested. Contains the raw content and metadata
 * about the document.
 */
public class Document {
  private String content;
  private DocumentMetadata metadata;

  public Document() {}

  public Document(String content, DocumentMetadata metadata) {
    this.content = content;
    this.metadata = DocumentMetadata.copyOf(metadata);
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Document document = (Document) o;
    return Objects.equals(content, document.content) && Objects.equals(metadata, document.metadata);
  }

  @Override
  public int hashCode() {
    return Objects.hash(content, metadata);
  }

  @Override
  public String toString() {
    return "Document{"
        + "content='"
        + (content != null && content.length() > 100 ? content.substring(0, 100) + "..." : content)
        + '\''
        + ", metadata="
        + metadata
        + '}';
  }
}
