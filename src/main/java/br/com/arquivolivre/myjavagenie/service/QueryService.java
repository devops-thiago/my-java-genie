package br.com.arquivolivre.myjavagenie.service;

import br.com.arquivolivre.myjavagenie.config.LlmConfig;
import br.com.arquivolivre.myjavagenie.config.QueryConfig;
import br.com.arquivolivre.myjavagenie.exception.LlmException;
import br.com.arquivolivre.myjavagenie.model.QueryResponse;
import br.com.arquivolivre.myjavagenie.model.ScoredChunk;
import br.com.arquivolivre.myjavagenie.model.SourceReference;
import br.com.arquivolivre.myjavagenie.repository.VectorStoreRepository;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class QueryService {
  private static final Logger logger = LoggerFactory.getLogger(QueryService.class);

  private final ChatModel chatModel;
  private final VectorStoreRepository vectorStoreRepository;
  private final PromptBuilder promptBuilder;
  private final QueryConfig queryConfig;
  private final LlmConfig llmConfig;
  private final RagTelemetry telemetry;

  public QueryService(
      ChatModel chatModel,
      VectorStoreRepository vectorStoreRepository,
      PromptBuilder promptBuilder,
      QueryConfig queryConfig,
      LlmConfig llmConfig,
      RagTelemetry telemetry) {
    this.chatModel = chatModel;
    this.vectorStoreRepository = vectorStoreRepository;
    this.promptBuilder = promptBuilder;
    this.queryConfig = queryConfig;
    this.llmConfig = llmConfig;
    this.telemetry = telemetry;
  }

  public QueryResponse query(String question) {
    return telemetry.inSpan(
        "query",
        () -> {
          long started = System.currentTimeMillis();
          String searchQuery = telemetry.inSpan("translate", () -> translateForSearch(question));
          logger.info("query_pt='{}' query_en='{}'", question, searchQuery);

          int topK = queryConfig.getMaxRetrievedChunks();
          long retrieveStarted = System.currentTimeMillis();
          List<ScoredChunk> chunks =
              telemetry.inSpan(
                  "retrieve",
                  () ->
                      vectorStoreRepository.search(
                          searchQuery, topK, queryConfig.getSimilarityThreshold()));
          telemetry.recordRetrieve(
              System.currentTimeMillis() - retrieveStarted, chunks.size(), topK);

          for (ScoredChunk chunk : chunks) {
            logger.info(
                "retrieved file={} chunk={} score={}",
                chunk.getFilename(),
                chunk.getChunkIndex(),
                String.format("%.3f", chunk.getScore()));
          }

          if (chunks.isEmpty()) {
            long latency = System.currentTimeMillis() - started;
            telemetry.recordQuery(latency, llmConfig.getModelName(), topK);
            return new QueryResponse(
                "Não encontrei informação suficiente nos documentos para responder.",
                List.of(),
                searchQuery,
                latency);
          }

          String answer = telemetry.inSpan("generate", () -> generate(question, chunks));

          // When the model returned the fixed out-of-scope reply (context did not answer the
          // question), do not attach the nearest-but-irrelevant chunks as sources.
          boolean outOfScope = answer != null && answer.contains(PromptBuilder.OUT_OF_SCOPE_MARKER);
          List<SourceReference> sources = outOfScope ? List.of() : toSources(chunks);
          long latency = System.currentTimeMillis() - started;
          telemetry.recordQuery(latency, llmConfig.getModelName(), topK);
          logger.info(
              "query done in {}ms topK={} sources={} maxTokens={}",
              latency,
              topK,
              sources.size(),
              llmConfig.getMaxTokens());
          return new QueryResponse(answer, sources, searchQuery, latency);
        });
  }

  private String translateForSearch(String question) {
    try {
      String translated =
          chatModel.chat(
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
    String userPrompt = promptBuilder.buildUserPrompt(question, chunks);
    messages.add(UserMessage.from(userPrompt));

    long started = System.currentTimeMillis();
    ChatResponse response = chatModel.chat(messages);
    long generateMs = System.currentTimeMillis() - started;

    if (response == null || response.aiMessage() == null || response.aiMessage().text() == null) {
      throw new LlmException("LLM returned an empty response");
    }

    int promptTokens = promptTokens(response, userPrompt);
    int completionTokens = completionTokens(response, response.aiMessage().text());
    telemetry.recordGenerate(generateMs, llmConfig.getModelName(), promptTokens, completionTokens);
    logger.info(
        "generate latency={}ms promptTokens≈{} completionTokens≈{}",
        generateMs,
        promptTokens,
        completionTokens);
    return response.aiMessage().text();
  }

  private int promptTokens(ChatResponse response, String userPrompt) {
    TokenUsage usage = response.tokenUsage();
    if (usage != null && usage.inputTokenCount() != null) {
      return usage.inputTokenCount();
    }
    return Math.max(1, userPrompt.length() / 4);
  }

  private int completionTokens(ChatResponse response, String answer) {
    TokenUsage usage = response.tokenUsage();
    if (usage != null && usage.outputTokenCount() != null) {
      return usage.outputTokenCount();
    }
    return Math.max(1, answer.length() / 4);
  }

  private List<SourceReference> toSources(List<ScoredChunk> chunks) {
    List<SourceReference> sources = new ArrayList<>();
    for (ScoredChunk chunk : chunks) {
      sources.add(new SourceReference(chunk.getFilename(), null, chunk.getChunkIndex()));
    }
    return sources;
  }
}
