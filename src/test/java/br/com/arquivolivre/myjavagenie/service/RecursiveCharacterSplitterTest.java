package br.com.arquivolivre.myjavagenie.service;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.arquivolivre.myjavagenie.config.IngestionConfig;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link RecursiveCharacterSplitter}, focused on chunk-size enforcement. */
class RecursiveCharacterSplitterTest {

  private static final int CHUNK_SIZE = 1000;
  private static final int OVERLAP = 200;
  // A chunk may carry up to `overlap` extra characters from the previous chunk.
  private static final int MAX_CHUNK = CHUNK_SIZE + OVERLAP;

  private RecursiveCharacterSplitter splitter() {
    IngestionConfig config = new IngestionConfig();
    config.setChunkSize(CHUNK_SIZE);
    config.setChunkOverlap(OVERLAP);
    return new RecursiveCharacterSplitter(config);
  }

  @Test
  void hugeSingleLineIsCappedAtChunkSizePlusOverlap() {
    // A single line far larger than chunkSize must be split down through the finer separators
    // (character-level fallback at the end) so no chunk ever exceeds chunkSize + overlap.
    String hugeLine = "x".repeat(20_000);
    String text = "first line\n" + hugeLine + "\nlast line";

    List<String> chunks = splitter().splitText(text, CHUNK_SIZE, OVERLAP);

    assertThat(chunks).isNotEmpty();
    int maxLen = chunks.stream().mapToInt(String::length).max().orElse(0);
    assertThat(maxLen).isLessThanOrEqualTo(MAX_CHUNK);
  }

  @Test
  void wordlessTextIsCappedViaHardSplit() {
    String text = "a".repeat(20_000);
    List<String> chunks = splitter().splitText(text, CHUNK_SIZE, OVERLAP);
    assertThat(chunks).hasSizeGreaterThan(1);
    assertThat(chunks.stream().allMatch(c -> c.length() <= MAX_CHUNK)).isTrue();
  }

  @Test
  void normalProseSplitsIntoBoundedChunks() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 200; i++) {
      sb.append("This is sentence number ").append(i).append(" in the document. ");
    }
    List<String> chunks = splitter().splitText(sb.toString(), CHUNK_SIZE, OVERLAP);
    assertThat(chunks).hasSizeGreaterThan(1);
    assertThat(chunks.stream().allMatch(c -> c.length() <= MAX_CHUNK)).isTrue();
  }

  @Test
  void shortTextProducesSingleChunk() {
    assertThat(splitter().splitText("A short document.", CHUNK_SIZE, OVERLAP))
        .containsExactly("A short document.");
  }

  @Test
  void blankTextProducesNoChunks() {
    assertThat(splitter().splitText("  ", CHUNK_SIZE, OVERLAP)).isEmpty();
  }
}
