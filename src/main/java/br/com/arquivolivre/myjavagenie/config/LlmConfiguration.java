package br.com.arquivolivre.myjavagenie.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class LlmConfiguration {

  private static final Logger logger = LoggerFactory.getLogger(LlmConfiguration.class);

  @Bean
  public ChatLanguageModel chatLanguageModel(LlmConfig config) {
    logger.info(
        "Initializing OpenAI-compatible chat model '{}' at {}",
        config.getModelName(),
        config.getBaseUrl());

    return OpenAiChatModel.builder()
        .apiKey(config.getApiKey())
        .baseUrl(config.getBaseUrl())
        .modelName(config.getModelName())
        .temperature(config.getTemperature())
        .maxTokens(config.getMaxTokens())
        .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
        .logRequests(false)
        .logResponses(false)
        .build();
  }

  @Bean
  public WebMvcConfigurer corsConfigurer() {
    return new WebMvcConfigurer() {
      @Override
      public void addCorsMappings(CorsRegistry registry) {
        registry
            .addMapping("/api/**")
            .allowedOrigins("http://localhost:3000")
            .allowedMethods("GET", "POST", "DELETE", "OPTIONS")
            .allowedHeaders("*");
      }
    };
  }
}
