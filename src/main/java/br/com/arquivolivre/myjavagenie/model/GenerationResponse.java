package br.com.arquivolivre.myjavagenie.model;

import java.util.Objects;

/**
 * Response model for language model generation.
 * Contains the generated text and token usage information.
 */
public class GenerationResponse {
    private String text;
    private int promptTokens;
    private int completionTokens;
    private int totalTokens;

    public GenerationResponse() {
    }

    public GenerationResponse(String text, int promptTokens, int completionTokens, int totalTokens) {
        this.text = text;
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.totalTokens = totalTokens;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getPromptTokens() {
        return promptTokens;
    }

    public void setPromptTokens(int promptTokens) {
        this.promptTokens = promptTokens;
    }

    public int getCompletionTokens() {
        return completionTokens;
    }

    public void setCompletionTokens(int completionTokens) {
        this.completionTokens = completionTokens;
    }

    public int getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(int totalTokens) {
        this.totalTokens = totalTokens;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GenerationResponse that = (GenerationResponse) o;
        return promptTokens == that.promptTokens &&
                completionTokens == that.completionTokens &&
                totalTokens == that.totalTokens &&
                Objects.equals(text, that.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, promptTokens, completionTokens, totalTokens);
    }

    @Override
    public String toString() {
        return "GenerationResponse{" +
                "text='" + (text != null && text.length() > 100 ?
                text.substring(0, 100) + "..." : text) + '\'' +
                ", promptTokens=" + promptTokens +
                ", completionTokens=" + completionTokens +
                ", totalTokens=" + totalTokens +
                '}';
    }
}
