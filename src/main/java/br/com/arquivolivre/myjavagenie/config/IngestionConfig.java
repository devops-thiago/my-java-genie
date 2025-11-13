package br.com.arquivolivre.myjavagenie.config;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Configuration properties for document ingestion settings.
 */
@ConfigurationProperties(prefix = "ingestion")
@Validated
public class IngestionConfig {

    @NotNull(message = "Chunk size must be specified")
    @Positive(message = "Chunk size must be positive")
    private Integer chunkSize;

    @NotNull(message = "Chunk overlap must be specified")
    @PositiveOrZero(message = "Chunk overlap must be zero or positive")
    private Integer chunkOverlap;

    @NotNull(message = "Batch size must be specified")
    @Positive(message = "Batch size must be positive")
    private Integer batchSize;

    private List<String> supportedFormats;

    // Getters and Setters

    public Integer getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(Integer chunkSize) {
        this.chunkSize = chunkSize;
    }

    public Integer getChunkOverlap() {
        return chunkOverlap;
    }

    public void setChunkOverlap(Integer chunkOverlap) {
        this.chunkOverlap = chunkOverlap;
    }

    public Integer getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(Integer batchSize) {
        this.batchSize = batchSize;
    }

    public List<String> getSupportedFormats() {
        return supportedFormats;
    }

    public void setSupportedFormats(List<String> supportedFormats) {
        this.supportedFormats = supportedFormats;
    }
}
