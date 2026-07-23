package br.com.arquivolivre.myjavagenie.model;

public class DocumentChunk {
  private final String filename;
  private final int chunkIndex;
  private final String content;

  public DocumentChunk(String filename, int chunkIndex, String content) {
    this.filename = filename;
    this.chunkIndex = chunkIndex;
    this.content = content;
  }

  public String getFilename() {
    return filename;
  }

  public int getChunkIndex() {
    return chunkIndex;
  }

  public String getContent() {
    return content;
  }
}
