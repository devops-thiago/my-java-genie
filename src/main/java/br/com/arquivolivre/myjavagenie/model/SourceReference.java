package br.com.arquivolivre.myjavagenie.model;

import java.util.Objects;

/**
 * Reference to a source document used in generating an answer. Contains the filename, section,
 * chunk index, and the similarity score with which the chunk was retrieved.
 */
public class SourceReference {
  private String filename;
  private String section;
  private int chunkIndex;
  private double score;

  public SourceReference() {}

  public SourceReference(String filename, String section, int chunkIndex) {
    this(filename, section, chunkIndex, 0.0);
  }

  public SourceReference(String filename, String section, int chunkIndex, double score) {
    this.filename = filename;
    this.section = section;
    this.chunkIndex = chunkIndex;
    this.score = score;
  }

  public String getFilename() {
    return filename;
  }

  public void setFilename(String filename) {
    this.filename = filename;
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

  /** The similarity score (0.0–1.0) with which this chunk was retrieved. */
  public double getScore() {
    return score;
  }

  public void setScore(double score) {
    this.score = score;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SourceReference that = (SourceReference) o;
    return chunkIndex == that.chunkIndex
        && Double.compare(that.score, score) == 0
        && Objects.equals(filename, that.filename)
        && Objects.equals(section, that.section);
  }

  @Override
  public int hashCode() {
    return Objects.hash(filename, section, chunkIndex, score);
  }

  @Override
  public String toString() {
    return "SourceReference{"
        + "filename='"
        + filename
        + '\''
        + ", section='"
        + section
        + '\''
        + ", chunkIndex="
        + chunkIndex
        + ", score="
        + score
        + '}';
  }
}
