/**
 * Root package for the Java RAG System application.
 *
 * <p>This package contains the main application entry point and core configuration for a
 * Retrieval-Augmented Generation (RAG) system designed to answer questions about Java 25
 * documentation using natural language processing.
 *
 * <p>The system integrates:
 *
 * <ul>
 *   <li>LangChain4j for language model interactions
 *   <li>Vector databases for efficient document retrieval
 *   <li>Spring Boot for dependency injection and REST API
 *   <li>Embedding models for semantic search
 * </ul>
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Support for both self-hosted and paid language models
 *   <li>Configurable document chunking and retrieval
 *   <li>Token usage optimization and tracking
 *   <li>Clean architecture following SOLID principles
 * </ul>
 *
 * @see br.com.arquivolivre.myjavagenie.Application
 * @see br.com.arquivolivre.myjavagenie.config.RagSystemConfiguration
 */
package br.com.arquivolivre.myjavagenie;
