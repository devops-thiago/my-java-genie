package br.com.arquivolivre.myjavagenie.service;

import br.com.arquivolivre.myjavagenie.exception.DocumentProcessingException;
import br.com.arquivolivre.myjavagenie.model.Document;
import br.com.arquivolivre.myjavagenie.model.DocumentMetadata;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for loading documents from the filesystem. Supports common documentation formats:
 * Markdown, HTML, and plain text.
 */
@Service
public class DocumentLoader {

  private static final Logger logger = LoggerFactory.getLogger(DocumentLoader.class);

  // Supported file extensions
  private static final List<String> SUPPORTED_EXTENSIONS =
      List.of(
          ".md",
          ".markdown", // Markdown
          ".html",
          ".htm", // HTML
          ".txt" // Plain text
          );

  // Pattern to extract markdown headers
  private static final Pattern MARKDOWN_HEADER_PATTERN =
      Pattern.compile("^#{1,6}\\s+(.+)$", Pattern.MULTILINE);

  // Pattern to extract HTML title
  private static final Pattern HTML_TITLE_PATTERN =
      Pattern.compile("<title>(.+?)</title>", Pattern.CASE_INSENSITIVE);

  // Pattern to extract HTML h1-h6 headers
  private static final Pattern HTML_HEADER_PATTERN =
      Pattern.compile("<h[1-6][^>]*>(.+?)</h[1-6]>", Pattern.CASE_INSENSITIVE);

  /**
   * Load all supported documents from a directory.
   *
   * @param directoryPath the directory to load documents from
   * @return list of loaded documents
   * @throws DocumentProcessingException if loading fails
   */
  public List<Document> loadDocuments(Path directoryPath) {
    if (!Files.exists(directoryPath)) {
      throw new DocumentProcessingException("Directory does not exist: " + directoryPath);
    }

    if (!Files.isDirectory(directoryPath)) {
      throw new DocumentProcessingException("Path is not a directory: " + directoryPath);
    }

    List<Document> documents = new ArrayList<>();

    try (Stream<Path> paths = Files.walk(directoryPath)) {
      paths
          .filter(Files::isRegularFile)
          .filter(this::isSupportedFile)
          .forEach(
              path -> {
                try {
                  Document doc = loadDocument(path);
                  documents.add(doc);
                  logger.info("Loaded document: {}", path.getFileName());
                } catch (Exception e) {
                  logger.error("Failed to load document: {}", path, e);
                }
              });
    } catch (IOException e) {
      throw new DocumentProcessingException("Failed to walk directory: " + directoryPath, e);
    }

    logger.info("Loaded {} documents from {}", documents.size(), directoryPath);
    return documents;
  }

  /**
   * Load a single document from a file.
   *
   * @param filePath the file to load
   * @return the loaded document
   * @throws DocumentProcessingException if loading fails
   */
  public Document loadDocument(Path filePath) {
    if (!Files.exists(filePath)) {
      throw new DocumentProcessingException("File does not exist: " + filePath);
    }

    if (!Files.isRegularFile(filePath)) {
      throw new DocumentProcessingException("Path is not a file: " + filePath);
    }

    if (!isSupportedFile(filePath)) {
      throw new DocumentProcessingException("Unsupported file format: " + filePath);
    }

    try {
      String content = Files.readString(filePath);
      DocumentMetadata metadata = extractMetadata(filePath, content);

      return new Document(content, metadata);
    } catch (IOException e) {
      throw new DocumentProcessingException("Failed to read file: " + filePath, e);
    }
  }

  /** Check if a file is supported based on its extension. */
  private boolean isSupportedFile(Path filePath) {
    String fileName = filePath.getFileName().toString().toLowerCase();
    return SUPPORTED_EXTENSIONS.stream().anyMatch(fileName::endsWith);
  }

  /** Extract metadata from file path and content. */
  private DocumentMetadata extractMetadata(Path filePath, String content) {
    String fileName = filePath.getFileName().toString();
    String section = extractSection(filePath, content);

    DocumentMetadata metadata = new DocumentMetadata(fileName, section, 0);

    // Add file type
    String extension = getFileExtension(fileName);
    metadata.addProperty("fileType", extension);

    // Add file path
    metadata.addProperty("filePath", filePath.toString());

    // Add parent directory as category
    Path parent = filePath.getParent();
    if (parent != null) {
      metadata.addProperty("category", parent.getFileName().toString());
    }

    return metadata;
  }

  /** Extract section/title from document content based on file type. */
  private String extractSection(Path filePath, String content) {
    String fileName = filePath.getFileName().toString().toLowerCase();

    if (fileName.endsWith(".md") || fileName.endsWith(".markdown")) {
      return extractMarkdownTitle(content);
    } else if (fileName.endsWith(".html") || fileName.endsWith(".htm")) {
      return extractHtmlTitle(content);
    }

    // For plain text, use first non-empty line or filename
    String[] lines = content.split("\n", 2);
    if (lines.length > 0 && !lines[0].trim().isEmpty()) {
      return lines[0].trim().substring(0, Math.min(lines[0].trim().length(), 100));
    }

    return removeExtension(filePath.getFileName().toString());
  }

  /** Extract title from Markdown content (first header). */
  private String extractMarkdownTitle(String content) {
    Matcher matcher = MARKDOWN_HEADER_PATTERN.matcher(content);
    if (matcher.find()) {
      return matcher.group(1).trim();
    }
    return "Untitled";
  }

  /** Extract title from HTML content. */
  private String extractHtmlTitle(String content) {
    // Try <title> tag first
    Matcher titleMatcher = HTML_TITLE_PATTERN.matcher(content);
    if (titleMatcher.find()) {
      return stripHtmlTags(titleMatcher.group(1).trim());
    }

    // Try first header tag
    Matcher headerMatcher = HTML_HEADER_PATTERN.matcher(content);
    if (headerMatcher.find()) {
      return stripHtmlTags(headerMatcher.group(1).trim());
    }

    return "Untitled";
  }

  /** Strip HTML tags from text. */
  private String stripHtmlTags(String html) {
    return html.replaceAll("<[^>]+>", "").trim();
  }

  /** Get file extension from filename. */
  private String getFileExtension(String fileName) {
    int lastDot = fileName.lastIndexOf('.');
    if (lastDot > 0 && lastDot < fileName.length() - 1) {
      return fileName.substring(lastDot + 1).toLowerCase();
    }
    return "";
  }

  /** Remove file extension from filename. */
  private String removeExtension(String fileName) {
    int lastDot = fileName.lastIndexOf('.');
    if (lastDot > 0) {
      return fileName.substring(0, lastDot);
    }
    return fileName;
  }
}
