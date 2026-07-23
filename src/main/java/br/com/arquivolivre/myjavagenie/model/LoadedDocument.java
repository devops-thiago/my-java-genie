package br.com.arquivolivre.myjavagenie.model;

public class LoadedDocument {
  private final String filename;
  private final String content;

  public LoadedDocument(String filename, String content) {
    this.filename = filename;
    this.content = content;
  }

  public String getFilename() {
    return filename;
  }

  public String getContent() {
    return content;
  }
}
