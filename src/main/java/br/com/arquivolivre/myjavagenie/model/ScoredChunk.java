package br.com.arquivolivre.myjavagenie.model;

public class ScoredChunk {
  private final String filename;
  private final int chunkIndex;
  private final String content;
  private final double score;

  public ScoredChunk(String filename, int chunkIndex, String content, double score) {
    this.filename = filename;
    this.chunkIndex = chunkIndex;
    this.content = content;
    this.score = score;
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

  public double getScore() {
    return score;
  }
}
