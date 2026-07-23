package br.com.arquivolivre.myjavagenie.service;

import br.com.arquivolivre.myjavagenie.model.ScoredChunk;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PromptBuilder {

  public String buildSystemPrompt() {
    return """
        You are a helpful assistant for Java documentation.
        Answer in Portuguese.
        Use ONLY the provided context. If the context is insufficient, say you do not know based on the documents.
        """;
  }

  public String buildUserPrompt(String originalQuestion, List<ScoredChunk> chunks) {
    StringBuilder context = new StringBuilder();
    for (int i = 0; i < chunks.size(); i++) {
      ScoredChunk chunk = chunks.get(i);
      context
          .append("[")
          .append(i + 1)
          .append("] ")
          .append(chunk.getFilename())
          .append(" #")
          .append(chunk.getChunkIndex())
          .append(" (score=")
          .append(String.format("%.3f", chunk.getScore()))
          .append(")\n")
          .append(chunk.getContent())
          .append("\n\n");
    }
    return "Context:\n"
        + context
        + "Question:\n"
        + originalQuestion
        + "\n\nAnswer in Portuguese using only the context.";
  }
}
