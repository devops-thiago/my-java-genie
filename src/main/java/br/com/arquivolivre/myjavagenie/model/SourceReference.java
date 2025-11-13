package br.com.arquivolivre.myjavagenie.model;

import java.util.Objects;

/**
 * Reference to a source document used in generating an answer.
 * Contains information about the filename, section, and chunk index.
 */
public class SourceReference {
    private String filename;
    private String section;
    private int chunkIndex;

    public SourceReference() {
    }

    public SourceReference(String filename, String section, int chunkIndex) {
        this.filename = filename;
        this.section = section;
        this.chunkIndex = chunkIndex;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(int chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SourceReference that = (SourceReference) o;
        return chunkIndex == that.chunkIndex &&
                Objects.equals(filename, that.filename) &&
                Objects.equals(section, that.section);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filename, section, chunkIndex);
    }

    @Override
    public String toString() {
        return "SourceReference{" +
                "filename='" + filename + '\'' +
                ", section='" + section + '\'' +
                ", chunkIndex=" + chunkIndex +
                '}';
    }
}
