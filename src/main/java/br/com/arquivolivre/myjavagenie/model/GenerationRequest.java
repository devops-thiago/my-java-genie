package br.com.arquivolivre.myjavagenie.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Request model for language model generation.
 * Contains the prompt and generation parameters.
 */
public class GenerationRequest {
    private String prompt;
    private double temperature;
    private int maxTokens;
    private List<String> stopSequences;

    public GenerationRequest() {
        this.stopSequences = new ArrayList<>();
    }

    public GenerationRequest(String prompt, double temperature, int maxTokens) {
        this.prompt = prompt;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
        this.stopSequences = new ArrayList<>();
    }

    public GenerationRequest(String prompt, double temperature, int maxTokens,
                             List<String> stopSequences) {
        this.prompt = prompt;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
        this.stopSequences = stopSequences != null ? stopSequences : new ArrayList<>();
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public List<String> getStopSequences() {
        return stopSequences;
    }

    public void setStopSequences(List<String> stopSequences) {
        this.stopSequences = stopSequences;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GenerationRequest that = (GenerationRequest) o;
        return Double.compare(that.temperature, temperature) == 0 &&
                maxTokens == that.maxTokens &&
                Objects.equals(prompt, that.prompt) &&
                Objects.equals(stopSequences, that.stopSequences);
    }

    @Override
    public int hashCode() {
        return Objects.hash(prompt, temperature, maxTokens, stopSequences);
    }

    @Override
    public String toString() {
        return "GenerationRequest{" +
                "prompt='" + (prompt != null && prompt.length() > 100 ?
                prompt.substring(0, 100) + "..." : prompt) + '\'' +
                ", temperature=" + temperature +
                ", maxTokens=" + maxTokens +
                ", stopSequences=" + stopSequences +
                '}';
    }
}
