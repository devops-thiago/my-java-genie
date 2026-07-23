package br.com.arquivolivre.myjavagenie.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Immutable configuration properties for query processing settings.
 *
 * <p>Populated through Spring Boot constructor binding; construct directly with the canonical
 * constructor in tests.
 */
@ConfigurationProperties(prefix = "query")
@Validated
public record QueryConfig(
    @NotNull(message = "Max retrieved chunks must be specified")
        @Positive(message = "Max retrieved chunks must be positive")
        Integer maxRetrievedChunks,
    @NotNull(message = "Similarity threshold must be specified")
        @DecimalMin(value = "0.0", message = "Similarity threshold must be at least 0.0")
        @DecimalMax(value = "1.0", message = "Similarity threshold must be at most 1.0")
        Double similarityThreshold,
    @NotNull(message = "Timeout seconds must be specified")
        @Positive(message = "Timeout seconds must be positive")
        Integer timeoutSeconds,
    Boolean enableCache,
    Integer cacheTtlMinutes) {}
