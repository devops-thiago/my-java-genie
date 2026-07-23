package br.com.arquivolivre.myjavagenie.model;

import jakarta.validation.constraints.NotBlank;
import java.util.Objects;

/** Request model for user queries. Contains the question to be answered by the RAG system. */
public class QueryRequest {
  @NotBlank(message = "Question cannot be blank")
  private String question;

  public QueryRequest() {}

  public QueryRequest(String question) {
    this.question = question;
  }

  public String getQuestion() {
    return question;
  }

  public void setQuestion(String question) {
    this.question = question;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    QueryRequest that = (QueryRequest) o;
    return Objects.equals(question, that.question);
  }

  @Override
  public int hashCode() {
    return Objects.hash(question);
  }

  @Override
  public String toString() {
    return "QueryRequest{" + "question='" + question + '\'' + '}';
  }
}
