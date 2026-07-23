package br.com.arquivolivre.myjavagenie.model;

import java.util.ArrayList;
import java.util.List;

public class IngestionResult {
  private int documentsLoaded;
  private int chunksCreated;
  private List<Integer> chunkSizes = new ArrayList<>();

  public IngestionResult() {}

  public IngestionResult(int documentsLoaded, int chunksCreated, List<Integer> chunkSizes) {
    this.documentsLoaded = documentsLoaded;
    this.chunksCreated = chunksCreated;
    this.chunkSizes = chunkSizes != null ? chunkSizes : new ArrayList<>();
  }

  public int getDocumentsLoaded() {
    return documentsLoaded;
  }

  public void setDocumentsLoaded(int documentsLoaded) {
    this.documentsLoaded = documentsLoaded;
  }

  public int getChunksCreated() {
    return chunksCreated;
  }

  public void setChunksCreated(int chunksCreated) {
    this.chunksCreated = chunksCreated;
  }

  public List<Integer> getChunkSizes() {
    return chunkSizes;
  }

  public void setChunkSizes(List<Integer> chunkSizes) {
    this.chunkSizes = chunkSizes;
  }
}
