package br.com.arquivolivre.myjavagenie.service;

import br.com.arquivolivre.myjavagenie.exception.ModelInvocationException;
import br.com.arquivolivre.myjavagenie.exception.ModelTimeoutException;
import br.com.arquivolivre.myjavagenie.model.GenerationRequest;
import br.com.arquivolivre.myjavagenie.model.GenerationResponse;

/**
 * Interface for language model providers. Abstracts the interaction with different LLM
 * implementations (self-hosted, OpenAI, etc.).
 */
public interface LanguageModelProvider {

  /**
   * Generates a response based on the provided request.
   *
   * @param request the generation request containing prompt and parameters
   * @return the generation response with text and token usage
   * @throws ModelInvocationException if generation fails
   * @throws ModelTimeoutException if generation times out
   */
  GenerationResponse generate(GenerationRequest request);

  /**
   * Checks if the language model is available and ready to accept requests.
   *
   * @return true if the model is available, false otherwise
   */
  boolean isAvailable();

  /**
   * Returns the name of the provider implementation.
   *
   * @return the provider name (e.g., "self-hosted", "openai", "anthropic")
   */
  String getProviderName();
}
