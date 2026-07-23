package br.com.arquivolivre.myjavagenie.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Metadata associated with a document chunk. Contains information about the source file, section,
 * and chunk position.
 */
public class DocumentMetadata {
  private String sourceFile;
  private String section;
  private int chunkIndex;
  private Map<String, String> additionalProperties;

  public DocumentMetadata() {
    this.additionalProperties = new HashMap<>();
  }

  public DocumentMetadata(String sourceFile, String section, int chunkIndex) {
    this.sourceFile = sourceFile;
    this.section = section;
    this.chunkIndex = chunkIndex;
    this.additionalProperties = new HashMap<>();
  }

  /** Copy constructor used for defensive copies. */
  public DocumentMetadata(DocumentMetadata other) {
    this.sourceFile = other.sourceFile;
    this.section = other.section;
    this.chunkIndex = other.chunkIndex;
    this.additionalProperties =
        other.additionalProperties == null
            ? new HashMap<>()
            : new HashMap<>(other.additionalProperties);
  }

  /**
   * Returns a defensive copy of the given metadata, or {@code null} if {@code metadata} is {@code
   * null}.
   */
  public static DocumentMetadata copyOf(DocumentMetadata metadata) {
    return metadata == null ? null : new DocumentMetadata(metadata);
  }

  public String getSourceFile() {
    return sourceFile;
  }

  public void setSourceFile(String sourceFile) {
    this.sourceFile = sourceFile;
  }

  public String getSection() {
    return section;
  }

  public void setSection(String section) {
    this.section = section;
  }

  public int getChunkIndex() {
    return chunkIndex;
  }

  public void setChunkIndex(int chunkIndex) {
    this.chunkIndex = chunkIndex;
  }

  public Map<String, String> getAdditionalProperties() {
    return additionalProperties == null ? null : new HashMap<>(additionalProperties);
  }

  public void setAdditionalProperties(Map<String, String> additionalProperties) {
    this.additionalProperties =
        additionalProperties == null ? new HashMap<>() : new HashMap<>(additionalProperties);
  }

  public void addProperty(String key, String value) {
    this.additionalProperties.put(key, value);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DocumentMetadata that = (DocumentMetadata) o;
    return chunkIndex == that.chunkIndex
        && Objects.equals(sourceFile, that.sourceFile)
        && Objects.equals(section, that.section)
        && Objects.equals(additionalProperties, that.additionalProperties);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sourceFile, section, chunkIndex, additionalProperties);
  }

  @Override
  public String toString() {
    return "DocumentMetadata{"
        + "sourceFile='"
        + sourceFile
        + '\''
        + ", section='"
        + section
        + '\''
        + ", chunkIndex="
        + chunkIndex
        + ", additionalProperties="
        + additionalProperties
        + '}';
  }
}
