package br.com.arquivolivre.myjavagenie.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for vector database settings. Supports ChromaDB, pgvector, and Qdrant.
 */
@ConfigurationProperties(prefix = "vector-db")
@Validated
public class VectorDbConfig {

  @NotBlank(message = "Vector database type must be specified")
  private String type;

  @NotBlank(message = "Connection URL must be specified")
  private String connectionUrl;

  @NotBlank(message = "Collection name must be specified")
  private String collectionName;

  @Valid private ChromaSettings chroma;

  @Valid private PgVectorSettings pgvector;

  @Valid private QdrantSettings qdrant;

  // Getters and Setters

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getConnectionUrl() {
    return connectionUrl;
  }

  public void setConnectionUrl(String connectionUrl) {
    this.connectionUrl = connectionUrl;
  }

  public String getCollectionName() {
    return collectionName;
  }

  public void setCollectionName(String collectionName) {
    this.collectionName = collectionName;
  }

  public ChromaSettings getChroma() {
    return chroma;
  }

  public void setChroma(ChromaSettings chroma) {
    this.chroma = chroma;
  }

  public PgVectorSettings getPgvector() {
    return pgvector;
  }

  public void setPgvector(PgVectorSettings pgvector) {
    this.pgvector = pgvector;
  }

  public QdrantSettings getQdrant() {
    return qdrant;
  }

  public void setQdrant(QdrantSettings qdrant) {
    this.qdrant = qdrant;
  }

  /** Configuration for ChromaDB. */
  public static class ChromaSettings {
    private String tenant;
    private String database;

    public String getTenant() {
      return tenant;
    }

    public void setTenant(String tenant) {
      this.tenant = tenant;
    }

    public String getDatabase() {
      return database;
    }

    public void setDatabase(String database) {
      this.database = database;
    }
  }

  /** Configuration for PostgreSQL with pgvector extension. */
  public static class PgVectorSettings {
    @NotBlank(message = "PostgreSQL host must be specified")
    private String host;

    @Positive(message = "PostgreSQL port must be positive")
    private Integer port;

    @NotBlank(message = "PostgreSQL database must be specified")
    private String database;

    @NotBlank(message = "PostgreSQL username must be specified")
    private String username;

    private String password;

    private String schema;

    @NotBlank(message = "PostgreSQL table name must be specified")
    private String tableName;

    public String getHost() {
      return host;
    }

    public void setHost(String host) {
      this.host = host;
    }

    public Integer getPort() {
      return port;
    }

    public void setPort(Integer port) {
      this.port = port;
    }

    public String getDatabase() {
      return database;
    }

    public void setDatabase(String database) {
      this.database = database;
    }

    public String getUsername() {
      return username;
    }

    public void setUsername(String username) {
      this.username = username;
    }

    public String getPassword() {
      return password;
    }

    public void setPassword(String password) {
      this.password = password;
    }

    public String getSchema() {
      return schema;
    }

    public void setSchema(String schema) {
      this.schema = schema;
    }

    public String getTableName() {
      return tableName;
    }

    public void setTableName(String tableName) {
      this.tableName = tableName;
    }
  }

  /** Configuration for Qdrant vector database. */
  public static class QdrantSettings {
    private String apiKey;
    private Boolean useTls;

    public String getApiKey() {
      return apiKey;
    }

    public void setApiKey(String apiKey) {
      this.apiKey = apiKey;
    }

    public Boolean getUseTls() {
      return useTls;
    }

    public void setUseTls(Boolean useTls) {
      this.useTls = useTls;
    }
  }
}
