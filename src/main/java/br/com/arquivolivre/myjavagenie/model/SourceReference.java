package br.com.arquivolivre.myjavagenie.model;

public class SourceReference {
  private String filename;
  private String section;
  private int chunkIndex;

  public SourceReference() {}

  public SourceReference(String filename, String section, int chunkIndex) {
    this.filename = filename;
    this.section = section;
    this.chunkIndex = chunkIndex;
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
}
