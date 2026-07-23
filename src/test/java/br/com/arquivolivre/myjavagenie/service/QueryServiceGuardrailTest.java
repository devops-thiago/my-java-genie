package br.com.arquivolivre.myjavagenie.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.arquivolivre.myjavagenie.config.ModelConfig;
import br.com.arquivolivre.myjavagenie.config.QueryConfig;
import br.com.arquivolivre.myjavagenie.model.DocumentChunk;
import br.com.arquivolivre.myjavagenie.model.DocumentMetadata;
import br.com.arquivolivre.myjavagenie.model.GenerationResponse;
import br.com.arquivolivre.myjavagenie.model.QueryResponse;
import br.com.arquivolivre.myjavagenie.model.ScoredDocument;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the grounding guardrail in {@link QueryService}: a grounded answer keeps its
 * source references, while the fixed out-of-scope reply suppresses them.
 */
class QueryServiceGuardrailTest {

  private RetrievalEngine retrievalEngine;
  private LanguageModelProvider languageModel;
  private QueryService queryService;

  @BeforeEach
  void setUp() {
    retrievalEngine = mock(RetrievalEngine.class);
    languageModel = mock(LanguageModelProvider.class);
    TokenUsageTracker tokenTracker = mock(TokenUsageTracker.class);

    QueryConfig queryConfig = new QueryConfig(5, 0.5, 30, false, 0);
    ModelConfig modelConfig = new ModelConfig("openai", null, null, null, null, 0.7, 512);

    lenient().when(languageModel.getProviderName()).thenReturn("test-provider");

    queryService =
        new QueryService(
            retrievalEngine,
            languageModel,
            new PromptBuilder(),
            tokenTracker,
            queryConfig,
            modelConfig,
            null,
            null);
  }

  private void mockRetrieval() {
    DocumentChunk chunk =
        new DocumentChunk("Records are immutable data carriers.", new DocumentMetadata(), 10);
    when(retrievalEngine.retrieveRelevantChunks(anyString()))
        .thenReturn(List.of(new ScoredDocument(chunk, 0.8)));
  }

  @Test
  void groundedAnswerKeepsSources() {
    mockRetrieval();
    when(languageModel.generate(any()))
        .thenReturn(new GenerationResponse("Records are described in the JLS.", 100, 50, 150));

    QueryResponse response = queryService.processQuery("What are records?");

    assertThat(response.getAnswer()).contains("Records");
    assertThat(response.getSources()).hasSize(1);
  }

  @Test
  void outOfScopeReplySuppressesSources() {
    mockRetrieval();
    when(languageModel.generate(any()))
        .thenReturn(new GenerationResponse(PromptBuilder.OUT_OF_SCOPE_ANSWER, 100, 30, 130));

    QueryResponse response = queryService.processQuery("Best chocolate chip cookie recipe?");

    assertThat(response.getAnswer()).isEqualTo(PromptBuilder.OUT_OF_SCOPE_ANSWER);
    assertThat(response.getSources()).isEmpty();
  }

  @Test
  void promptCarriesGroundingInstructionsAndContext() {
    PromptBuilder builder = new PromptBuilder();
    DocumentChunk chunk =
        new DocumentChunk("Pattern matching for switch.", new DocumentMetadata(), 8);

    String prompt = builder.buildPrompt("What changed in switch?", List.of(chunk));

    assertThat(prompt)
        .contains("Use ONLY the information in the Context section")
        .contains(PromptBuilder.OUT_OF_SCOPE_ANSWER)
        .contains("Pattern matching for switch.")
        .contains("Question: What changed in switch?");
  }
}
