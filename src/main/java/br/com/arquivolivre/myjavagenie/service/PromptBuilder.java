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

  /**
   * Fixed reply the model is instructed to return whenever the retrieved context does not answer
   * the question (e.g. an off-topic question). Exposed so callers can detect the refusal and avoid
   * attaching irrelevant source citations to it.
   */
  public static final String OUT_OF_SCOPE_ANSWER =
      "I can only answer questions about the Java 25 documentation, and I couldn't find "
          + "anything relevant to your question in my sources.";

  private static final String SYSTEM_PROMPT =
      "You are an assistant that answers strictly from the provided Java 25 documentation. "
          + "Use ONLY the information in the Context section below to answer the Question. "
          + "Do not use any outside or prior knowledge, and never make anything up. "
          + "If the Context does not contain information that answers the Question, or the "
          + "Question is not about Java 25, reply with exactly the following sentence and nothing "
          + "else: \""
          + OUT_OF_SCOPE_ANSWER
          + "\" When the Context does answer the Question, be accurate and cite the source files "
          + "you used.";

  /**
   * Builds a complete prompt for the language model. The strict grounding instructions are
   * prepended so the model only answers from the retrieved context (the provider sends this string
   * verbatim, so the instructions must live inside it).
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
    return SYSTEM_PROMPT + "\n\nContext:\n" + context + "\n\nQuestion: " + question + "\n\nAnswer:";
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

    return "[Source: " + sourceFile + section + "]\n" + chunk.getContent();
  }
}
