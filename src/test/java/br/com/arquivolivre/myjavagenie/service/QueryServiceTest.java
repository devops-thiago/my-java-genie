package br.com.arquivolivre.myjavagenie.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import br.com.arquivolivre.myjavagenie.config.LlmConfig;
import br.com.arquivolivre.myjavagenie.config.QueryConfig;
import br.com.arquivolivre.myjavagenie.model.QueryResponse;
import br.com.arquivolivre.myjavagenie.model.ScoredChunk;
import br.com.arquivolivre.myjavagenie.repository.VectorStoreRepository;
import dev.langchain4j.data.message.AiMessage;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for {@link QueryService}, focused on the grounding guardrail. */
@ExtendWith(MockitoExtension.class)
class QueryServiceTest {

  @Mock private dev.langchain4j.model.chat.ChatModel chatModel;
  @Mock private VectorStoreRepository vectorStoreRepository;

  private QueryService queryService;

  @BeforeEach
  void setUp() {
    QueryConfig config = new QueryConfig();
    config.setMaxRetrievedChunks(5);
    config.setSimilarityThreshold(0.5);
    LlmConfig llmConfig = new LlmConfig();
    llmConfig.setModelName("test-model");
    llmConfig.setMaxTokens(512);
    RagTelemetry telemetry =
        new RagTelemetry(
            io.opentelemetry.api.OpenTelemetry.noop().getTracer("test"),
            io.opentelemetry.api.OpenTelemetry.noop().getMeter("test"));
    queryService =
        new QueryService(
            chatModel, vectorStoreRepository, new PromptBuilder(), config, llmConfig, telemetry);
  }

  private static dev.langchain4j.model.chat.response.ChatResponse llmResponse(String text) {
    return dev.langchain4j.model.chat.response.ChatResponse.builder()
        .aiMessage(AiMessage.from(text))
        .build();
  }

  private void mockPipeline(String generatedAnswer) {
    // translateForSearch uses chat(String); generation uses chat(List<ChatMessage>).
    when(chatModel.chat(anyString())).thenReturn("translated question");
    when(chatModel.chat(anyList())).thenReturn(llmResponse(generatedAnswer));
    when(vectorStoreRepository.search(anyString(), anyInt(), anyDouble()))
        .thenReturn(List.of(new ScoredChunk("spec.html", 0, "some context", 0.8)));
  }

  @Test
  void groundedAnswerKeepsSources() {
    mockPipeline("Records são classes imutáveis descritas na especificação.");

    QueryResponse response = queryService.query("O que são records?");

    assertThat(response.getAnswer()).contains("Records");
    assertThat(response.getSources()).hasSize(1);
    assertThat(response.getSources().get(0).getFilename()).isEqualTo("spec.html");
  }

  @Test
  void outOfScopeReplySuppressesSources() {
    mockPipeline(PromptBuilder.OUT_OF_SCOPE_ANSWER);

    QueryResponse response = queryService.query("Qual a melhor receita de cookies?");

    assertThat(response.getAnswer()).isEqualTo(PromptBuilder.OUT_OF_SCOPE_ANSWER);
    assertThat(response.getSources()).isEmpty();
  }

  @Test
  void emptyRetrievalShortCircuitsWithoutLlmAnswer() {
    when(chatModel.chat(anyString())).thenReturn("translated question");
    when(vectorStoreRepository.search(anyString(), anyInt(), anyDouble())).thenReturn(List.of());

    QueryResponse response = queryService.query("pergunta sem match");

    assertThat(response.getAnswer()).contains("Não encontrei informação suficiente");
    assertThat(response.getSources()).isEmpty();
  }
}
