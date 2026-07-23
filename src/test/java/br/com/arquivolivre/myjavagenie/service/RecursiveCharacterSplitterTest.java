package br.com.arquivolivre.myjavagenie.service;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.arquivolivre.myjavagenie.config.IngestionConfig;
import br.com.arquivolivre.myjavagenie.model.DocumentChunk;
import br.com.arquivolivre.myjavagenie.model.DocumentMetadata;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link RecursiveCharacterSplitter}, focused on chunk-size enforcement. */
class RecursiveCharacterSplitterTest {

  private static final int CHUNK_SIZE = 1000;
  private static final int OVERLAP = 200;
  // A chunk may carry up to `overlap` extra characters from the previous chunk.
  private static final int MAX_CHUNK = CHUNK_SIZE + OVERLAP;

  private RecursiveCharacterSplitter splitter() {
    return new RecursiveCharacterSplitter(
        new IngestionConfig(CHUNK_SIZE, OVERLAP, 100, List.of(".txt")));
  }

  private DocumentMetadata metadata() {
    return new DocumentMetadata("test.txt", "Section", 0);
  }

  @Test
  void hugeSingleSplitIsRecursivelyCappedAtChunkSize() {
    // A single line (a "\n"-delimited split) far larger than chunkSize used to be emitted as one
    // giant chunk; it must now be split down so no chunk exceeds the configured size.
    String hugeLine = "x".repeat(20_000);
    String text = "first line\n" + hugeLine + "\nlast line";

    List<DocumentChunk> chunks = splitter().chunkText(text, metadata());

    assertThat(chunks).isNotEmpty();
    int maxLen = chunks.stream().mapToInt(c -> c.getContent().length()).max().orElse(0);
    assertThat(maxLen).isLessThanOrEqualTo(MAX_CHUNK);
  }

  @Test
  void longWordlessTextIsCappedAtChunkSize() {
    // No separators at all (worst case) still respects the cap via character-level fallback.
    String text = "a".repeat(20_000);
    List<DocumentChunk> chunks = splitter().chunkText(text, metadata());
    assertThat(chunks).hasSizeGreaterThan(1);
    assertThat(chunks.stream().allMatch(c -> c.getContent().length() <= MAX_CHUNK)).isTrue();
  }

  @Test
  void normalProseSplitsIntoBoundedChunks() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 200; i++) {
      sb.append("This is sentence number ").append(i).append(" in the document. ");
    }
    List<DocumentChunk> chunks = splitter().chunkText(sb.toString(), metadata());
    assertThat(chunks).hasSizeGreaterThan(1);
    assertThat(chunks.stream().allMatch(c -> c.getContent().length() <= MAX_CHUNK)).isTrue();
  }

  @Test
  void shortTextProducesSingleChunk() {
    List<DocumentChunk> chunks = splitter().chunkText("A short document.", metadata());
    assertThat(chunks).hasSize(1);
    assertThat(chunks.get(0).getContent()).isEqualTo("A short document.");
  }

  @Test
  void emptyTextProducesNoChunks() {
    assertThat(splitter().chunkText("", metadata())).isEmpty();
  }
}
