package br.com.arquivolivre.myjavagenie.model;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Result of a document ingestion operation. Contains statistics about the ingestion process
 * including successes and failures.
 */
public class IngestionResult {
  private int documentsProcessed;
  private int chunksCreated;
  private int failures;
  private List<String> failedDocuments;
  private Duration duration;
  private String status;

  public IngestionResult() {
    this.failedDocuments = new ArrayList<>();
    this.status = "SUCCESS";
  }

  public IngestionResult(int documentsProcessed, int chunksCreated, int failures) {
    this();
    this.documentsProcessed = documentsProcessed;
    this.chunksCreated = chunksCreated;
    this.failures = failures;
    if (failures > 0) {
      this.status = "PARTIAL_SUCCESS";
    }
  }

  public int getDocumentsProcessed() {
    return documentsProcessed;
  }

  public void setDocumentsProcessed(int documentsProcessed) {
    this.documentsProcessed = documentsProcessed;
  }

  public int getChunksCreated() {
    return chunksCreated;
  }

  public void setChunksCreated(int chunksCreated) {
    this.chunksCreated = chunksCreated;
  }

  public int getFailures() {
    return failures;
  }

  public void setFailures(int failures) {
    this.failures = failures;
  }

  public List<String> getFailedDocuments() {
    return failedDocuments == null ? null : new ArrayList<>(failedDocuments);
  }

  public void setFailedDocuments(List<String> failedDocuments) {
    this.failedDocuments =
        failedDocuments != null ? new ArrayList<>(failedDocuments) : new ArrayList<>();
  }

  public void addFailedDocument(String documentName) {
    this.failedDocuments.add(documentName);
    this.failures++;
    if (this.documentsProcessed > 0) {
      this.status = "PARTIAL_SUCCESS";
    } else {
      this.status = "FAILURE";
    }
  }

  public Duration getDuration() {
    return duration;
  }

  public void setDuration(Duration duration) {
    this.duration = duration;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public void incrementDocumentsProcessed() {
    this.documentsProcessed++;
  }

  public void addChunks(int count) {
    this.chunksCreated += count;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    IngestionResult that = (IngestionResult) o;
    return documentsProcessed == that.documentsProcessed
        && chunksCreated == that.chunksCreated
        && failures == that.failures
        && Objects.equals(failedDocuments, that.failedDocuments)
        && Objects.equals(duration, that.duration)
        && Objects.equals(status, that.status);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        documentsProcessed, chunksCreated, failures, failedDocuments, duration, status);
  }

  @Override
  public String toString() {
    return "IngestionResult{"
        + "documentsProcessed="
        + documentsProcessed
        + ", chunksCreated="
        + chunksCreated
        + ", failures="
        + failures
        + ", failedDocuments="
        + failedDocuments
        + ", duration="
        + duration
        + ", status='"
        + status
        + '\''
        + '}';
  }
}
