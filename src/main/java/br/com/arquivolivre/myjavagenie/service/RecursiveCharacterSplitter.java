package br.com.arquivolivre.myjavagenie.service;

import br.com.arquivolivre.myjavagenie.config.IngestionConfig;
import br.com.arquivolivre.myjavagenie.model.DocumentChunk;
import br.com.arquivolivre.myjavagenie.model.LoadedDocument;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RecursiveCharacterSplitter {
  private static final String[] SEPARATORS = {"\n\n", "\n", ". ", "! ", "? ", "; ", ", ", " ", ""};

  private final IngestionConfig config;

  public RecursiveCharacterSplitter(IngestionConfig config) {
    this.config = config;
  }

  public List<DocumentChunk> split(LoadedDocument document) {
    List<String> parts =
        splitText(document.getContent(), config.getChunkSize(), config.getChunkOverlap());
    List<DocumentChunk> chunks = new ArrayList<>();
    for (int i = 0; i < parts.size(); i++) {
      chunks.add(new DocumentChunk(document.getFilename(), i, parts.get(i)));
    }
    return chunks;
  }

  List<String> splitText(String text, int chunkSize, int overlap) {
    if (text == null || text.isBlank()) {
      return List.of();
    }
    if (text.length() <= chunkSize) {
      return List.of(text);
    }
    // Overlap is applied once, over the final chunk list. Applying it inside the recursion would
    // stack one prefix per separator level and let chunks grow past chunkSize + overlap.
    return applyOverlap(splitRecursive(text, chunkSize, overlap, 0), overlap);
  }

  private List<String> splitRecursive(String text, int chunkSize, int overlap, int separatorIndex) {
    if (text.length() <= chunkSize) {
      return List.of(text);
    }
    if (separatorIndex >= SEPARATORS.length) {
      return hardSplit(text, chunkSize, overlap);
    }

    String separator = SEPARATORS[separatorIndex];
    if (separator.isEmpty()) {
      return hardSplit(text, chunkSize, overlap);
    }

    String[] pieces = text.split(java.util.regex.Pattern.quote(separator), -1);
    if (pieces.length == 1) {
      return splitRecursive(text, chunkSize, overlap, separatorIndex + 1);
    }

    List<String> chunks = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    for (String piece : pieces) {
      String candidate = current.isEmpty() ? piece : current + separator + piece;
      if (candidate.length() <= chunkSize) {
        if (!current.isEmpty()) {
          current.append(separator);
        }
        current.append(piece);
      } else {
        if (!current.isEmpty()) {
          chunks.addAll(splitRecursive(current.toString(), chunkSize, overlap, separatorIndex + 1));
        }
        current = new StringBuilder(piece);
      }
    }
    if (!current.isEmpty()) {
      chunks.addAll(splitRecursive(current.toString(), chunkSize, overlap, separatorIndex + 1));
    }
    return chunks;
  }

  private List<String> hardSplit(String text, int chunkSize, int overlap) {
    List<String> chunks = new ArrayList<>();
    int step = Math.max(1, chunkSize - overlap);
    for (int start = 0; start < text.length(); start += step) {
      int end = Math.min(text.length(), start + chunkSize);
      chunks.add(text.substring(start, end));
      if (end == text.length()) {
        break;
      }
    }
    return chunks;
  }

  private List<String> applyOverlap(List<String> chunks, int overlap) {
    if (chunks.size() <= 1 || overlap <= 0) {
      return chunks;
    }
    List<String> withOverlap = new ArrayList<>();
    withOverlap.add(chunks.get(0));
    for (int i = 1; i < chunks.size(); i++) {
      String previous = chunks.get(i - 1);
      String prefix =
          previous.length() <= overlap ? previous : previous.substring(previous.length() - overlap);
      withOverlap.add(prefix + chunks.get(i));
    }
    return withOverlap;
  }
}
