package br.com.arquivolivre.myjavagenie.model;

import java.util.Objects;

/**
 * Wraps a DocumentChunk with its similarity score from a vector database search.
 * Used to represent search results with relevance scores.
 */
public class ScoredDocument {
    private DocumentChunk chunk;
    private double similarityScore;

    public ScoredDocument() {
    }

    public ScoredDocument(DocumentChunk chunk, double similarityScore) {
        this.chunk = chunk;
        this.similarityScore = similarityScore;
    }

    public DocumentChunk getChunk() {
        return chunk;
    }

    public void setChunk(DocumentChunk chunk) {
        this.chunk = chunk;
    }

    public double getSimilarityScore() {
        return similarityScore;
    }

    public void setSimilarityScore(double similarityScore) {
        this.similarityScore = similarityScore;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScoredDocument that = (ScoredDocument) o;
        return Double.compare(that.similarityScore, similarityScore) == 0 &&
                Objects.equals(chunk, that.chunk);
    }

    @Override
    public int hashCode() {
        return Objects.hash(chunk, similarityScore);
    }

    @Override
    public String toString() {
        return "ScoredDocument{" +
                "chunk=" + chunk +
                ", similarityScore=" + similarityScore +
                '}';
    }
}
