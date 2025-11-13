/**
 * Exception hierarchy for the RAG system.
 * <p>
 * All exceptions extend from {@link br.com.arquivolivre.myjavagenie.exception.RagSystemException} which is a RuntimeException.
 * The hierarchy is organized as follows:
 * <ul>
 *   <li>{@link br.com.arquivolivre.myjavagenie.exception.ModelException} - Language model related errors
 *     <ul>
 *       <li>{@link br.com.arquivolivre.myjavagenie.exception.ModelInitializationException} - Model initialization failures</li>
 *       <li>{@link br.com.arquivolivre.myjavagenie.exception.ModelInvocationException} - Model invocation failures</li>
 *       <li>{@link br.com.arquivolivre.myjavagenie.exception.ModelTimeoutException} - Model operation timeouts</li>
 *     </ul>
 *   </li>
 *   <li>{@link br.com.arquivolivre.myjavagenie.exception.VectorDbException} - Vector database related errors
 *     <ul>
 *       <li>{@link br.com.arquivolivre.myjavagenie.exception.VectorDbConnectionException} - Database connection failures</li>
 *       <li>{@link br.com.arquivolivre.myjavagenie.exception.VectorDbQueryException} - Query operation failures</li>
 *       <li>{@link br.com.arquivolivre.myjavagenie.exception.CollectionNotFoundException} - Collection not found</li>
 *     </ul>
 *   </li>
 *   <li>{@link br.com.arquivolivre.myjavagenie.exception.IngestionException} - Document ingestion related errors
 *     <ul>
 *       <li>{@link br.com.arquivolivre.myjavagenie.exception.DocumentProcessingException} - Document processing failures</li>
 *       <li>{@link br.com.arquivolivre.myjavagenie.exception.EmbeddingGenerationException} - Embedding generation failures</li>
 *     </ul>
 *   </li>
 *   <li>{@link br.com.arquivolivre.myjavagenie.exception.ConfigurationException} - Configuration related errors
 *     <ul>
 *       <li>{@link br.com.arquivolivre.myjavagenie.exception.InvalidConfigurationException} - Invalid configuration</li>
 *       <li>{@link br.com.arquivolivre.myjavagenie.exception.MissingConfigurationException} - Missing required configuration</li>
 *     </ul>
 *   </li>
 * </ul>
 */
package br.com.arquivolivre.myjavagenie.exception;
