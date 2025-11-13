package br.com.arquivolivre.myjavagenie.service;

import br.com.arquivolivre.myjavagenie.config.IngestionConfig;
import br.com.arquivolivre.myjavagenie.model.Document;
import br.com.arquivolivre.myjavagenie.model.DocumentChunk;
import br.com.arquivolivre.myjavagenie.model.DocumentMetadata;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of DocumentProcessor that splits text recursively at natural boundaries.
 * Attempts to split at paragraphs first, then sentences, then words, and finally characters.
 */
@Service
public class RecursiveCharacterSplitter implements DocumentProcessor {

    // Separators in order of preference (paragraph, double newline, newline, sentence, space, character)
    private static final String[] SEPARATORS = {
            "\n\n\n",  // Multiple paragraph breaks
            "\n\n",    // Paragraph break
            "\n",      // Line break
            ". ",      // Sentence end
            "! ",      // Exclamation
            "? ",      // Question
            "; ",      // Semicolon
            ", ",      // Comma
            " ",       // Space
            ""         // Character level (fallback)
    };
    private final IngestionConfig config;

    public RecursiveCharacterSplitter(IngestionConfig config) {
        this.config = config;
    }

    @Override
    public List<DocumentChunk> processDocument(Document document) {
        if (document == null || document.getContent() == null) {
            return new ArrayList<>();
        }
        return chunkText(document.getContent(), document.getMetadata());
    }

    @Override
    public List<DocumentChunk> chunkText(String text, DocumentMetadata metadata) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }

        List<DocumentChunk> chunks = new ArrayList<>();
        List<String> textChunks = splitText(text, config.getChunkSize(), config.getChunkOverlap());

        for (int i = 0; i < textChunks.size(); i++) {
            String chunkContent = textChunks.get(i);

            // Create metadata for this chunk
            DocumentMetadata chunkMetadata = new DocumentMetadata(
                    metadata.getSourceFile(),
                    metadata.getSection(),
                    i
            );

            // Copy additional properties
            if (metadata.getAdditionalProperties() != null) {
                chunkMetadata.setAdditionalProperties(new java.util.HashMap<>(metadata.getAdditionalProperties()));
            }

            // Calculate token count (simple estimation: ~4 characters per token)
            int tokenCount = estimateTokenCount(chunkContent);

            DocumentChunk chunk = new DocumentChunk(chunkContent, chunkMetadata, tokenCount);
            chunks.add(chunk);
        }

        return chunks;
    }

    /**
     * Split text into chunks using recursive splitting at natural boundaries.
     */
    private List<String> splitText(String text, int chunkSize, int chunkOverlap) {
        List<String> chunks = new ArrayList<>();

        if (text.length() <= chunkSize) {
            chunks.add(text);
            return chunks;
        }

        // Try each separator in order
        for (String separator : SEPARATORS) {
            if (separator.isEmpty()) {
                // Fallback to character-level splitting
                chunks = splitByCharacters(text, chunkSize, chunkOverlap);
                break;
            }

            List<String> splits = splitBySeparator(text, separator);

            // Check if this separator produces reasonable splits
            if (splits.size() > 1) {
                chunks = mergeSplits(splits, chunkSize, chunkOverlap, separator);
                break;
            }
        }

        return chunks;
    }

    /**
     * Split text by a specific separator.
     */
    private List<String> splitBySeparator(String text, String separator) {
        List<String> splits = new ArrayList<>();

        if (separator.isEmpty()) {
            // Character-level split
            for (int i = 0; i < text.length(); i++) {
                splits.add(String.valueOf(text.charAt(i)));
            }
            return splits;
        }

        Pattern pattern = Pattern.compile(Pattern.quote(separator));
        Matcher matcher = pattern.matcher(text);

        int lastEnd = 0;
        while (matcher.find()) {
            String part = text.substring(lastEnd, matcher.end());
            if (!part.trim().isEmpty()) {
                splits.add(part);
            }
            lastEnd = matcher.end();
        }

        // Add remaining text
        if (lastEnd < text.length()) {
            String remaining = text.substring(lastEnd);
            if (!remaining.trim().isEmpty()) {
                splits.add(remaining);
            }
        }

        return splits;
    }

    /**
     * Merge splits into chunks of appropriate size with overlap.
     */
    private List<String> mergeSplits(List<String> splits, int chunkSize, int chunkOverlap, String separator) {
        List<String> chunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();

        for (String split : splits) {
            // If adding this split would exceed chunk size and we have content
            if (currentChunk.length() > 0 &&
                    currentChunk.length() + split.length() > chunkSize) {

                // Save current chunk
                chunks.add(currentChunk.toString().trim());

                // Start new chunk with overlap
                String overlapText = getOverlapText(currentChunk.toString(), chunkOverlap);
                currentChunk = new StringBuilder(overlapText);
            }

            currentChunk.append(split);
        }

        // Add final chunk if it has content
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }

        return chunks;
    }

    /**
     * Get overlap text from the end of a chunk.
     */
    private String getOverlapText(String text, int overlapSize) {
        if (text.length() <= overlapSize) {
            return text;
        }

        String overlap = text.substring(text.length() - overlapSize);

        // Try to start at a word boundary
        int spaceIndex = overlap.indexOf(' ');
        if (spaceIndex > 0 && spaceIndex < overlap.length() / 2) {
            overlap = overlap.substring(spaceIndex + 1);
        }

        return overlap;
    }

    /**
     * Fallback method to split by characters when no good separator is found.
     */
    private List<String> splitByCharacters(String text, int chunkSize, int chunkOverlap) {
        List<String> chunks = new ArrayList<>();
        int start = 0;

        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            chunks.add(text.substring(start, end));
            start = end - chunkOverlap;

            // Prevent infinite loop
            if (start >= end) {
                start = end;
            }
        }

        return chunks;
    }

    /**
     * Estimate token count using a simple heuristic.
     * Approximation: 1 token ≈ 4 characters for English text.
     */
    private int estimateTokenCount(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        // Simple estimation: divide character count by 4
        // This is a rough approximation; actual tokenization varies by model
        return (int) Math.ceil(text.length() / 4.0);
    }
}
