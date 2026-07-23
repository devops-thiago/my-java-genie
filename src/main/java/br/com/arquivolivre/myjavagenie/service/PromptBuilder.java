package br.com.arquivolivre.myjavagenie.service;

import br.com.arquivolivre.myjavagenie.model.ScoredChunk;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PromptBuilder {

  /**
   * Fixed reply the model must return when the retrieved context does not answer the question (e.g.
   * an off-topic question). QueryService detects it via {@link #OUT_OF_SCOPE_MARKER} and suppresses
   * the (irrelevant) source citations.
   */
  public static final String OUT_OF_SCOPE_ANSWER =
      "Só posso responder perguntas sobre a documentação do Java, e não encontrei nada"
          + " relevante para a sua pergunta nas minhas fontes.";

  /** Stable substring used to detect the out-of-scope reply in the model's answer. */
  public static final String OUT_OF_SCOPE_MARKER =
      "Só posso responder perguntas sobre a documentação do Java";

  public String buildSystemPrompt() {
    return """
        You are an assistant that answers strictly from the provided Java documentation.
        Answer in Portuguese.
        Use ONLY the information in the provided context. Do not use any outside or prior \
        knowledge, and never make anything up.
        If the context does not contain information that answers the question, or the question \
        is not about the Java documentation, reply with exactly the following sentence and \
        nothing else: "%s"
        When the context does answer the question, be accurate and cite the source files you used.
        """
        .formatted(OUT_OF_SCOPE_ANSWER);
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
