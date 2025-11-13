package br.com.arquivolivre.myjavagenie.service;

import br.com.arquivolivre.myjavagenie.config.ModelConfig;
import br.com.arquivolivre.myjavagenie.exception.ModelInitializationException;
import br.com.arquivolivre.myjavagenie.exception.ModelInvocationException;
import br.com.arquivolivre.myjavagenie.exception.ModelTimeoutException;
import br.com.arquivolivre.myjavagenie.model.GenerationRequest;
import br.com.arquivolivre.myjavagenie.model.GenerationResponse;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.ResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Language model provider for Google Gemini API via Vertex AI.
 * Implements retry logic with exponential backoff and handles Gemini-specific errors.
 */
public class GeminiModelProvider implements LanguageModelProvider {

    private static final Logger logger = LoggerFactory.getLogger(GeminiModelProvider.class);
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_RETRY_DELAY_MS = 1000;

    private final VertexAI vertexAI;
    private final GenerativeModel model;
    private final String modelName;
    private final int timeoutSeconds;
    private final double temperature;
    private final int maxTokens;

    /**
     * Creates a Gemini model provider with the given configuration.
     *
     * @param config the model configuration
     */
    public GeminiModelProvider(ModelConfig config) {
        ModelConfig.GeminiSettings settings = config.getGemini();
        if (settings == null) {
            throw new ModelInitializationException("Gemini settings are required");
        }

        if (settings.getLocation() == null || settings.getLocation().isEmpty()) {
            throw new ModelInitializationException("Gemini location is required");
        }

        if (settings.getModelName() == null || settings.getModelName().isEmpty()) {
            throw new ModelInitializationException("Gemini model name is required");
        }

        this.modelName = settings.getModelName();
        this.timeoutSeconds = settings.getTimeoutSeconds() != null ?
                settings.getTimeoutSeconds() : 30;
        this.temperature = config.getTemperature();
        this.maxTokens = config.getMaxTokens();

        logger.info("Initializing Gemini model provider: {} in location: {}",
                modelName, settings.getLocation());

        try {
            // Initialize Vertex AI client
            String projectId = settings.getProjectId();
            String location = settings.getLocation();

            if (projectId == null || projectId.isEmpty()) {
                // Try to get from environment
                projectId = System.getenv("GOOGLE_CLOUD_PROJECT");
                if (projectId == null || projectId.isEmpty()) {
                    throw new ModelInitializationException(
                            "Google Cloud project ID is required. Set via configuration or GOOGLE_CLOUD_PROJECT environment variable");
                }
            }

            // Initialize VertexAI with credentials
            if (settings.getApiKey() != null && !settings.getApiKey().isEmpty()) {
                // Use API key authentication (for testing/development)
                logger.info("Using API key authentication for Gemini");
                GoogleCredentials credentials = GoogleCredentials.fromStream(
                        new ByteArrayInputStream(
                                String.format("{\"type\":\"authorized_user\",\"client_id\":\"\",\"client_secret\":\"\",\"refresh_token\":\"%s\"}",
                                        settings.getApiKey()).getBytes(StandardCharsets.UTF_8)
                        )
                );
                this.vertexAI = new VertexAI.Builder()
                        .setProjectId(projectId)
                        .setLocation(location)
                        .setCredentials(credentials)
                        .build();
            } else {
                // Use Application Default Credentials
                logger.info("Using Application Default Credentials for Gemini");
                this.vertexAI = new VertexAI(projectId, location);
            }

            // Create generative model
            this.model = new GenerativeModel(modelName, vertexAI);

            logger.info("Gemini model provider initialized successfully");

        } catch (IOException e) {
            logger.error("Failed to initialize Gemini model provider", e);
            throw new ModelInitializationException("Failed to initialize Gemini model provider", e);
        }
    }

    @Override
    public GenerationResponse generate(GenerationRequest request) {
        logger.debug("Generating response for prompt with {} characters",
                request.getPrompt() != null ? request.getPrompt().length() : 0);

        int attempt = 0;
        Exception lastException = null;

        while (attempt < MAX_RETRIES) {
            try {
                long startTime = System.currentTimeMillis();

                // Generate content using Gemini
                GenerateContentResponse response = model.generateContent(request.getPrompt());

                long duration = System.currentTimeMillis() - startTime;

                // Check if generation timed out
                if (duration > timeoutSeconds * 1000L) {
                    throw new ModelTimeoutException(
                            "Model generation exceeded timeout of " + timeoutSeconds + " seconds");
                }

                // Extract text from response
                String responseText = ResponseHandler.getText(response);

                // Extract token usage from response metadata
                int promptTokens = 0;
                int completionTokens = 0;

                if (response.getUsageMetadata() != null) {
                    promptTokens = response.getUsageMetadata().getPromptTokenCount();
                    completionTokens = response.getUsageMetadata().getCandidatesTokenCount();
                }

                int totalTokens = promptTokens + completionTokens;

                logger.info("Gemini token usage - prompt: {}, completion: {}, total: {}",
                        promptTokens, completionTokens, totalTokens);
                logger.debug("Generation completed in {}ms", duration);

                return new GenerationResponse(
                        responseText,
                        promptTokens,
                        completionTokens,
                        totalTokens
                );

            } catch (Exception e) {
                attempt++;
                lastException = e;

                // Handle specific Gemini errors
                if (isTimeoutException(e)) {
                    logger.error("Model invocation timed out after {} seconds", timeoutSeconds);
                    throw new ModelTimeoutException(
                            "Model generation timed out after " + timeoutSeconds + " seconds", e);
                }

                if (isRateLimitException(e)) {
                    logger.warn("Rate limit exceeded (attempt {}/{})", attempt, MAX_RETRIES);
                }

                if (isSafetyFilterException(e)) {
                    logger.error("Safety filter triggered: {}", e.getMessage());
                    throw new ModelInvocationException(
                            "Content was blocked by Gemini safety filters", e);
                }

                if (isQuotaExceededException(e)) {
                    logger.error("Quota exceeded: {}", e.getMessage());
                    throw new ModelInvocationException(
                            "Gemini API quota exceeded", e);
                }

                if (attempt < MAX_RETRIES) {
                    // Exponential backoff
                    long delay = INITIAL_RETRY_DELAY_MS * (long) Math.pow(2, attempt - 1);
                    logger.warn("Model invocation failed (attempt {}/{}), retrying in {}ms: {}",
                            attempt, MAX_RETRIES, delay, e.getMessage());

                    try {
                        TimeUnit.MILLISECONDS.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new ModelInvocationException(
                                "Model invocation interrupted during retry", ie);
                    }
                } else {
                    logger.error("Model invocation failed after {} attempts", MAX_RETRIES);
                }
            }
        }

        throw new ModelInvocationException(
                "Failed to generate response after " + MAX_RETRIES + " attempts", lastException);
    }

    @Override
    public boolean isAvailable() {
        try {
            // Try a simple generation to check availability
            GenerateContentResponse response = model.generateContent("test");
            String text = ResponseHandler.getText(response);
            return text != null;
        } catch (Exception e) {
            logger.warn("Model availability check failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getProviderName() {
        return "gemini";
    }

    /**
     * Checks if an exception is a timeout exception.
     *
     * @param e the exception to check
     * @return true if it's a timeout exception
     */
    private boolean isTimeoutException(Exception e) {
        return e instanceof java.util.concurrent.TimeoutException ||
                e.getCause() instanceof java.util.concurrent.TimeoutException ||
                (e.getMessage() != null && e.getMessage().toLowerCase().contains("timeout")) ||
                (e.getMessage() != null && e.getMessage().toLowerCase().contains("deadline exceeded"));
    }

    /**
     * Checks if an exception is a rate limit exception.
     *
     * @param e the exception to check
     * @return true if it's a rate limit exception
     */
    private boolean isRateLimitException(Exception e) {
        return e.getMessage() != null &&
                (e.getMessage().toLowerCase().contains("rate limit") ||
                        e.getMessage().toLowerCase().contains("429") ||
                        e.getMessage().toLowerCase().contains("resource exhausted"));
    }

    /**
     * Checks if an exception is a safety filter exception.
     *
     * @param e the exception to check
     * @return true if it's a safety filter exception
     */
    private boolean isSafetyFilterException(Exception e) {
        return e.getMessage() != null &&
                (e.getMessage().toLowerCase().contains("safety") ||
                        e.getMessage().toLowerCase().contains("blocked") ||
                        e.getMessage().toLowerCase().contains("content filter"));
    }

    /**
     * Checks if an exception is a quota exceeded exception.
     *
     * @param e the exception to check
     * @return true if it's a quota exceeded exception
     */
    private boolean isQuotaExceededException(Exception e) {
        return e.getMessage() != null &&
                (e.getMessage().toLowerCase().contains("quota") ||
                        e.getMessage().toLowerCase().contains("limit exceeded"));
    }

    /**
     * Closes the Vertex AI client and releases resources.
     */
    public void close() {
        try {
            if (vertexAI != null) {
                vertexAI.close();
                logger.info("Gemini model provider closed");
            }
        } catch (Exception e) {
            logger.warn("Error closing Gemini model provider: {}", e.getMessage());
        }
    }
}
