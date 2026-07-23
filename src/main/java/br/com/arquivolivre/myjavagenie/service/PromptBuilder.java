package br.com.arquivolivre.myjavagenie.service;

import br.com.arquivolivre.myjavagenie.model.DocumentChunk;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Builds prompts for the language model by combining system instructions, user questions, and
 * retrieved document context. Optimizes token usage by keeping prompts concise.
 */
@Component
public class PromptBuilder {

  private static final String SYSTEM_PROMPT =
      "You are an expert on Java 25 documentation. "
          + "Answer questions accurately based on the provided context. "
          + "If the context doesn't contain relevant information, say so. "
          + "Keep answers concise and cite sources when possible.";

  private static final String USER_PROMPT_TEMPLATE = "Context:\n%s\n\nQuestion: %s\n\nAnswer:";

  private static final String CONTEXT_CHUNK_TEMPLATE = "[Source: %s%s]\n%s";

  /**
   * Builds a complete prompt for the language model.
   *
   * @param question the user's question
   * @param retrievedChunks the relevant document chunks retrieved from the vector database
   * @return the formatted prompt string
   */
  public String buildPrompt(String question, List<DocumentChunk> retrievedChunks) {
    if (question == null || question.trim().isEmpty()) {
      throw new IllegalArgumentException("Question cannot be null or empty");
    }

    String context = formatContext(retrievedChunks);
    return String.format(USER_PROMPT_TEMPLATE, context, question);
  }

  /**
   * Gets the system prompt that defines the assistant's role and behavior.
   *
   * @return the system prompt string
   */
  public String getSystemPrompt() {
    return SYSTEM_PROMPT;
  }

  /**
   * Formats document chunks into a context string with source references.
   *
   * @param chunks the document chunks to format
   * @return formatted context string
   */
  private String formatContext(List<DocumentChunk> chunks) {
    if (chunks == null || chunks.isEmpty()) {
      return "No relevant documentation found.";
    }

    return chunks.stream().map(this::formatChunk).collect(Collectors.joining("\n\n"));
  }

  /**
   * Formats a single document chunk with source reference.
   *
   * @param chunk the document chunk to format
   * @return formatted chunk string
   */
  private String formatChunk(DocumentChunk chunk) {
    String sourceFile =
        chunk.getMetadata() != null && chunk.getMetadata().getSourceFile() != null
            ? chunk.getMetadata().getSourceFile()
            : "Unknown";

    String section =
        chunk.getMetadata() != null && chunk.getMetadata().getSection() != null
            ? ", Section: " + chunk.getMetadata().getSection()
            : "";

    return String.format(CONTEXT_CHUNK_TEMPLATE, sourceFile, section, chunk.getContent());
  }
}
