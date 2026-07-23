package br.com.arquivolivre.myjavagenie.service;

import br.com.arquivolivre.myjavagenie.model.DocumentChunk;
import br.com.arquivolivre.myjavagenie.model.IngestionResult;
import br.com.arquivolivre.myjavagenie.model.LoadedDocument;
import br.com.arquivolivre.myjavagenie.repository.VectorStoreRepository;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class IngestionService {
  private static final Logger logger = LoggerFactory.getLogger(IngestionService.class);

  private final DocumentLoader documentLoader;
  private final RecursiveCharacterSplitter splitter;
  private final VectorStoreRepository vectorStoreRepository;
  private final RagTelemetry telemetry;

  public IngestionService(
      DocumentLoader documentLoader,
      RecursiveCharacterSplitter splitter,
      VectorStoreRepository vectorStoreRepository,
      RagTelemetry telemetry) {
    this.documentLoader = documentLoader;
    this.splitter = splitter;
    this.vectorStoreRepository = vectorStoreRepository;
    this.telemetry = telemetry;
  }

  public IngestionResult ingest(String path) {
    return telemetry.inSpan(
        "ingest",
        () -> {
          List<LoadedDocument> documents = documentLoader.load(Path.of(path));
          List<DocumentChunk> chunks = new ArrayList<>();
          List<Integer> sizes = new ArrayList<>();

          for (LoadedDocument document : documents) {
            List<DocumentChunk> documentChunks = splitter.split(document);
            chunks.addAll(documentChunks);
            for (DocumentChunk chunk : documentChunks) {
              sizes.add(chunk.getContent().length());
              logger.info(
                  "Chunk {} from {} ({} chars)",
                  chunk.getChunkIndex(),
                  chunk.getFilename(),
                  chunk.getContent().length());
            }
          }

          telemetry.inSpan("embed", () -> vectorStoreRepository.storeAll(chunks));
          telemetry.recordIngestChunks(chunks.size());
          logger.info(
              "Ingested {} documents into {} embedded chunks", documents.size(), chunks.size());
          return new IngestionResult(documents.size(), chunks.size(), sizes);
        });
  }
}
