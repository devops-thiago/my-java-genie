/**
 * Service layer containing business logic. Contains query service, ingestion service, language
 * model providers, embedding providers, and other core services.
 *
 * <p>Key components:
 *
 * <ul>
 *   <li>{@link br.com.arquivolivre.myjavagenie.service.LanguageModelProvider} - Interface for LLM
 *       providers
 *   <li>{@link br.com.arquivolivre.myjavagenie.service.LanguageModelFactory} - Factory for creating
 *       LLM providers
 *   <li>{@link br.com.arquivolivre.myjavagenie.service.SelfHostedModelProvider} - Self-hosted model
 *       implementation
 *   <li>{@link br.com.arquivolivre.myjavagenie.service.OpenAIModelProvider} - OpenAI model
 *       implementation
 *   <li>{@link br.com.arquivolivre.myjavagenie.service.DefaultLanguageModelFactory} - Default
 *       factory implementation
 *   <li>{@link br.com.arquivolivre.myjavagenie.service.EmbeddingModelProvider} - Interface for
 *       embedding model providers
 *   <li>{@link br.com.arquivolivre.myjavagenie.service.DefaultEmbeddingModelProvider} - Default
 *       embedding model implementation
 * </ul>
 */
package br.com.arquivolivre.myjavagenie.service;
