package br.com.arquivolivre.myjavagenie.service;

import br.com.arquivolivre.myjavagenie.config.QueryConfig;
import br.com.arquivolivre.myjavagenie.exception.LlmException;
import br.com.arquivolivre.myjavagenie.model.QueryResponse;
import br.com.arquivolivre.myjavagenie.model.ScoredChunk;
import br.com.arquivolivre.myjavagenie.model.SourceReference;
import br.com.arquivolivre.myjavagenie.repository.VectorStoreRepository;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class QueryService {
  private static final Logger logger = LoggerFactory.getLogger(QueryService.class);

  private final ChatLanguageModel chatModel;
  private final VectorStoreRepository vectorStoreRepository;
  private final PromptBuilder promptBuilder;
  private final QueryConfig queryConfig;

  public QueryService(
      ChatLanguageModel chatModel,
      VectorStoreRepository vectorStoreRepository,
      PromptBuilder promptBuilder,
      QueryConfig queryConfig) {
    this.chatModel = chatModel;
    this.vectorStoreRepository = vectorStoreRepository;
    this.promptBuilder = promptBuilder;
    this.queryConfig = queryConfig;
  }

  public QueryResponse query(String question) {
    long started = System.currentTimeMillis();
    String searchQuery = translateForSearch(question);
    logger.info("query_pt='{}' query_en='{}'", question, searchQuery);

    List<ScoredChunk> chunks =
        vectorStoreRepository.search(
            searchQuery, queryConfig.getMaxRetrievedChunks(), queryConfig.getSimilarityThreshold());

    for (ScoredChunk chunk : chunks) {
      logger.info(
          "retrieved file={} chunk={} score={}",
          chunk.getFilename(),
          chunk.getChunkIndex(),
          String.format("%.3f", chunk.getScore()));
    }

    if (chunks.isEmpty()) {
      return new QueryResponse(
          "Não encontrei informação suficiente nos documentos para responder.",
          List.of(),
          searchQuery,
          System.currentTimeMillis() - started);
    }

    String answer = generate(question, chunks);
    List<SourceReference> sources = toSources(chunks);
    return new QueryResponse(answer, sources, searchQuery, System.currentTimeMillis() - started);
  }

  private String translateForSearch(String question) {
    try {
      String translated =
          chatModel.generate(
              "Translate the following user question to English for searching Java documentation. "
                  + "Reply with only the translation, no quotes.\n\n"
                  + question);
      if (translated == null || translated.isBlank()) {
        return question;
      }
      return translated.trim();
    } catch (Exception e) {
      logger.warn("Query translation failed, using original question: {}", e.getMessage());
      return question;
    }
  }

  private String generate(String question, List<ScoredChunk> chunks) {
    List<dev.langchain4j.data.message.ChatMessage> messages = new ArrayList<>();
    messages.add(SystemMessage.from(promptBuilder.buildSystemPrompt()));
    messages.add(UserMessage.from(promptBuilder.buildUserPrompt(question, chunks)));
    Response<AiMessage> response = chatModel.generate(messages);
    if (response == null || response.content() == null || response.content().text() == null) {
      throw new LlmException("LLM returned an empty response");
    }
    return response.content().text();
  }

  private List<SourceReference> toSources(List<ScoredChunk> chunks) {
    List<SourceReference> sources = new ArrayList<>();
    for (ScoredChunk chunk : chunks) {
      sources.add(new SourceReference(chunk.getFilename(), null, chunk.getChunkIndex()));
    }
    return sources;
  }
}
