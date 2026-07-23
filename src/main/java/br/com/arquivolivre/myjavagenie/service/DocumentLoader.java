package br.com.arquivolivre.myjavagenie.service;

import br.com.arquivolivre.myjavagenie.exception.DocumentProcessingException;
import br.com.arquivolivre.myjavagenie.model.LoadedDocument;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DocumentLoader {
  private static final Logger logger = LoggerFactory.getLogger(DocumentLoader.class);
  private static final List<String> SUPPORTED =
      List.of(".md", ".markdown", ".html", ".htm", ".txt");

  public List<LoadedDocument> load(Path path) {
    if (!Files.exists(path)) {
      throw new DocumentProcessingException("Path does not exist: " + path);
    }
    try {
      if (Files.isRegularFile(path)) {
        return List.of(loadFile(path));
      }
      if (!Files.isDirectory(path)) {
        throw new DocumentProcessingException("Path is neither file nor directory: " + path);
      }
      List<LoadedDocument> documents = new ArrayList<>();
      try (Stream<Path> walk = Files.walk(path)) {
        walk.filter(Files::isRegularFile)
            .filter(this::supported)
            .forEach(
                file -> {
                  try {
                    documents.add(loadFile(file));
                  } catch (Exception e) {
                    logger.warn("Skipping {}: {}", file, e.getMessage());
                  }
                });
      }
      logger.info("Loaded {} documents from {}", documents.size(), path);
      return documents;
    } catch (IOException e) {
      throw new DocumentProcessingException("Failed to load documents from " + path, e);
    }
  }

  private LoadedDocument loadFile(Path file) throws IOException {
    String raw = Files.readString(file, StandardCharsets.UTF_8);
    String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
    String content = name.endsWith(".html") || name.endsWith(".htm") ? stripHtml(raw) : raw;
    logger.info("Loaded {} ({} chars)", file.getFileName(), content.length());
    return new LoadedDocument(file.getFileName().toString(), content);
  }

  private boolean supported(Path file) {
    String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
    return SUPPORTED.stream().anyMatch(name::endsWith);
  }

  static String stripHtml(String html) {
    String withoutScripts =
        html.replaceAll("(?is)<script[^>]*>.*?</script>", " ")
            .replaceAll("(?is)<style[^>]*>.*?</style>", " ");
    String text = withoutScripts.replaceAll("(?is)<[^>]+>", " ");
    return text.replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replaceAll("\\s+", " ")
        .trim();
  }
}
