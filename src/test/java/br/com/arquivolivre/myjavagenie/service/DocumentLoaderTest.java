package br.com.arquivolivre.myjavagenie.service;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.arquivolivre.myjavagenie.model.LoadedDocument;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DocumentLoaderTest {

  private final DocumentLoader loader = new DocumentLoader();

  @Test
  void stripHtmlRemovesTags() {
    String text = DocumentLoader.stripHtml("<html><body><h1>Hi</h1><p>World</p></body></html>");
    assertThat(text).contains("Hi").contains("World").doesNotContain("<h1>");
  }

  @Test
  void loadHtmlFile(@TempDir Path tempDir) throws Exception {
    Path file = tempDir.resolve("sample.html");
    Files.writeString(file, "<html><body><p>Patterns in Java</p></body></html>");

    List<LoadedDocument> docs = loader.load(file);
    assertThat(docs).hasSize(1);
    assertThat(docs.get(0).getContent()).contains("Patterns in Java");
  }
}
