# My Java Genie

> A production-ready Retrieval-Augmented Generation (RAG) system for querying Java 25 documentation using natural language.

**Ask questions about Java 25 in plain English and get accurate answers from official documentation.**

## ✨ Features

- 💬 **Interactive Web Chat UI** - React-based interface with real-time updates
- 🤖 **Flexible Model Support** - Use self-hosted (Ollama) or paid APIs (OpenAI, Anthropic, Gemini)
- 📊 **Multiple Vector Databases** - ChromaDB, pgvector, or Qdrant
- 🔍 **Full Observability** - OpenTelemetry + Grafana stack for traces, metrics, and logs
- 🚀 **Production Ready** - Docker deployment, monitoring, and health checks
- 💰 **Cost Optimized** - Built-in token usage optimization strategies

## 🚀 Quick Start

### Option 1: Docker (Recommended)

**Get up and running in 2 commands:**

```bash
# 1. Start all services (app + ChromaDB + Ollama + observability stack)
docker-compose up -d

# 2. Open http://localhost:8080 in your browser
```

That's it! Docker Compose deploys everything: the a### Option 2: Local Development

For local development (running the app outside Docker):

**Prerequisites:** Java 17+, Maven 3.6+

```bash
# 1. Start infrastructure services only
docker-compose up -d chromadb ollama

# 2. Run the application locally
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 3. Open http://localhost:8080
```

## 📖 Table of Contents

- [Getting Started](#getting-started)
  - [Using the Chat UI](#using-the-chat-ui)
  - [Ingesting Documents](#ingesting-documents)
- [Configuration](#configuration)
- [Docker Deployment](#docker-deployment)
- [Observability](#observability)
- [API Reference](#api-reference)
- [Advanced Topics](#advanced-topics)
- [Troubleshooting](#troubleshooting)

## 🎯 Getting Started

### Using the Chat UI

1. **Start the application** (see Quick Start above)
2. **Open your browser** to `http://localhost:8080`
3. **Type your question** about Java 25 documentation
4. **View the answer** with source references and syntax-highlighted code

**Chat UI Features:**
- ⚡ Real-time processing status updates via WebSocket
- 📝 Markdown rendering with syntax-highlighted code blocks
- 📚 Source references showing which docs were used
- 💾 Conversation history saved in browser
- 📱 Responsive design (desktop, tablet, mobile)

### Ingesting Documents

Before querying, ingest Java 25 documentation:

```bash
curl -X POST "http://localhost:8080/api/ingest?path=/path/to/java25/docs"
```

**Response:**
```json
{
  "documentsProcessed": 45,
  "chunksCreated": 523,
  "processingTimeMs": 12450,
  "status": "SUCCESS"
}
```

**Supported formats:** Markdown (`.md`), HTML (`.html`), Plain text (`.txt`)

### Example Queries

```bash
# Feature explanation
curl -X POST http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{"question": "What are sealed classes in Java 25?"}'

# Code examples
curl -X POST http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{"question": "Show me an example of pattern matching"}'

# Comparisons
curl -X POST http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{"question": "Difference between records and regular classes?"}'
```

## ⚙️ Configuration

### Model Providers

Choose between self-hosted (free) or paid API models:

#### Self-Hosted (Ollama)

```yaml
model:
  provider: self-hosted
  self-hosted:
    base-url: http://localhost:11434
    model-name: llama2  # or mistral, codellama, llama3
```

**Available models:** `llama2`, `mistral`, `codellama`, `llama3`

#### OpenAI

```yaml
model:
  provider: openai
  openai:
    api-key: ${OPENAI_API_KEY}
    model-name: gpt-4-turbo-preview  # or gpt-3.5-turbo
```

```bash
export OPENAI_API_KEY="sk-..."
```

#### Anthropic Claude

```yaml
model:
  provider: anthropic
  anthropic:
    api-key: ${ANTHROPIC_API_KEY}
    model-name: claude-3-sonnet-20240229
```

#### Google Gemini

```yaml
model:
  provider: gemini
  gemini:
    api-key: ${GOOGLE_API_KEY}
    model-name: gemini-pro  # or gemini-1.5-pro, gemini-1.5-flash
```

```bash
export GOOGLE_API_KEY="your-api-key"
```

### Vector Databases

**ChromaDB** (Recommended for getting started):
```bash
docker run -d -p 8000:8000 chromadb/chroma:latest
```

**PostgreSQL with pgvector:**
```bash
docker run -d -p 5432:5432 \
  -e POSTGRES_PASSWORD=yourpassword \
  ankane/pgvector:latest
```

**Qdrant:**
```bash
docker run -d -p 6333:6333 qdrant/qdrant:latest
```

### Configuration Profiles

- **`dev`** - Self-hosted models + local ChromaDB + verbose logging
- **`prod`** - Paid APIs + optimized settings + production logging
- **`docker`** - Docker-specific URLs and settings

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## 🐳 Docker Deployment

### Quick Start with Docker

```bash
# Start all services (app + infrastructure + observability)
docker-compose up -d

# Check status
docker-compose ps

# View application logs
docker-compose logs -f my-java-genie-app

# View all logs
docker-compose logs -f

# Stop services
docker-compose down
```

### Services Overview

**Core Services:**
- **chromadb** (port 8000) - Vector database
- **ollama** (port 11434) - Self-hosted LLM runtime
- **my-java-genie-app** (port 8080) - Main application

**Observability Stack:**
- **grafana** (port 3000) - Dashboards (admin/admin)
- **tempo** (port 3200) - Distributed tracing
- **mimir** (port 9009) - Metrics storage
- **loki** (port 3100) - Log aggregation
- **alloy** (ports 4317, 4318) - OpenTelemetry collector

### Service Configuration

#### ChromaDB
- **Port**: 8000
- **Volume**: `chroma-data` for persistent storage
- **Health Check**: Automatic readiness verification

#### Ollama
- **Port**: 11434
- **Volume**: `ollama-data` for model storage
- **GPU Support**: Enabled by default (remove if no GPU available)
- **Models**: Pull models using `docker exec my-java-genie-ollama ollama pull llama2`

#### My Java Genie Application
- **Port**: 8080
- **Profile**: Uses `application-docker.yml` configuration
- **Automatic startup**: Deployed with docker-compose
- **Volumes**: `./docs` mounted as `/app/docs` (read-only)

#### Grafana Alloy (OpenTelemetry Collector)
- **Ports**: 4317 (OTLP gRPC), 4318 (OTLP HTTP), 12345 (UI)
- **Configuration**: `alloy-config.alloy`
- **Purpose**: Receives telemetry data and routes to Tempo, Mimir, and Loki

#### Grafana Tempo (Distributed Tracing)
- **Port**: 3200
- **Configuration**: `tempo-config.yaml`
- **Volume**: `tempo-data` for trace storage
- **Retention**: 48 hours (configurable)

#### Grafana Mimir (Metrics Storage)
- **Port**: 9009
- **Configuration**: `mimir-config.yaml`
- **Volume**: `mimir-data` for metrics storage
- **Retention**: 7 days (configurable)

#### Grafana Loki (Log Aggregation)
- **Port**: 3100
- **Configuration**: `loki-config.yaml`
- **Volume**: `loki-data` for log storage
- **Retention**: 7 days (configurable)

#### Grafana (Observability Dashboard)
- **Port**: 3000
- **Credentials**: admin/admin (change on first login)
- **Volume**: `grafana-data` for dashboards and settings
- **Pre-configured**: Datasources and RAG System dashboard included

### Docker Commands

```bash
# Start all services (including observability stack)
docker-compose up -d

# Start only core services (without observability)
docker-compose up -d chromadb ollama my-java-genie-app

# Start only observability stack
docker-compose up -d alloy tempo mimir loki grafana

# View logs
docker-compose logs -f
docker-compose logs -f my-java-genie-app
docker-compose logs -f grafana

# Check service status
docker-compose ps

# Stop services
docker-compose down

# Stop and remove volumes (clean slate)
docker-compose down -v

# Rebuild application image
docker-compose build my-java-genie-app
docker-compose up -d my-java-genie-app

# Restart specific service
docker-compose restart grafana
```

### Managing Ollama Models

```bash
# List available models
docker exec my-java-genie-ollama ollama list

# Pull additional models
docker exec my-java-genie-ollama ollama pull mistral
docker exec my-java-genie-ollama ollama pull codellama
docker exec my-java-genie-ollama ollama pull llama3

# Remove a model
docker exec my-java-genie-ollama ollama rm llama2

# Check model info
docker exec my-java-genie-ollama ollama show llama2
```

### Docker Environment Variables

Override default configuration using environment variables:

```bash
# In docker-compose.yml or .env file
SPRING_PROFILES_ACTIVE=docker
VECTOR_DB_URL=http://chromadb:8000
MODEL_BASE_URL=http://ollama:11434
MODEL_NAME=llama2
COLLECTION_NAME=java25_docs
```

### Resource Requirements

**Minimum (Core Services Only)**:
- CPU: 4 cores
- RAM: 8GB
- Disk: 20GB (for models and data)

**Recommended (With Observability Stack)**:
- CPU: 8 cores
- RAM: 16GB
- GPU: NVIDIA GPU with 8GB+ VRAM (for faster inference)
- Disk: 50GB

**Resource Breakdown**:
- ChromaDB: ~200MB RAM
- Ollama: ~4-8GB RAM (depends on model)
- Application: ~512MB RAM
- Grafana Alloy: ~256MB RAM
- Tempo: ~512MB RAM
- Mimir: ~512MB RAM
- Loki: ~256MB RAM
- Grafana: ~256MB RAM
- Total: ~7-13GB RAM with full stack

### GPU Support

If you have an NVIDIA GPU and want to use it with Ollama:

1. Install [NVIDIA Container Toolkit](https://docs.nvidia.com/datacenter/cloud-native/container-toolkit/install-guide.html)

2. The `docker-compose.yml` already includes GPU configuration:
```yaml
deploy:
  resources:
    reservations:
      devices:
        - driver: nvidia
          count: all
          capabilities: [gpu]
```

3. If you don't have a GPU, remove or comment out the `deploy` section in `docker-compose.yml`

### Troubleshooting Docker Setup

**Issue**: Ollama container fails to start
```bash
# Check logs
docker logs my-java-genie-ollama

# If GPU error, remove GPU configuration from docker-compose.yml
```

**Issue**: ChromaDB connection refused
```bash
# Verify ChromaDB is running
docker ps | grep chromadb

# Check ChromaDB logs
docker logs my-java-genie-chromadb

# Test connection
curl http://localhost:8000/api/v1/heartbeat
```

**Issue**: Application can't connect to services
```bash
# Ensure all services are on the same network
docker network inspect my-java-genie_rag-network

# Check service names resolve correctly
docker exec my-java-genie-application ping chromadb
docker exec my-java-genie-application ping ollama
```

**Issue**: Out of disk space
```bash
# Check Docker disk usage
docker system df

# Clean up unused resources
docker system prune -a --volumes

# Remove specific volumes
docker volume rm my-java-genie_ollama-data
```

## Configuration

### Model Providers

The system supports multiple language model providers. Configure via `application.yml`:

#### Self-Hosted Models (Ollama)

```yaml
model:
  provider: self-hosted
  self-hosted:
    base-url: http://localhost:11434
    model-name: llama2  # or mistral, codellama, etc.
    timeout-seconds: 60
  temperature: 0.7
  max-tokens: 500
```

**Supported Ollama Models**:
- `llama2` - General purpose, good balance
- `mistral` - Fast and efficient
- `codellama` - Optimized for code understanding
- `llama3` - Latest version with improved capabilities

**Setup**:
```bash
# Install Ollama
curl -fsSL https://ollama.ai/install.sh | sh

# Pull a model
ollama pull llama2

# Start Ollama server
ollama serve
```

#### OpenAI

```yaml
model:
  provider: openai
  openai:
    api-key: ${OPENAI_API_KEY}
    model-name: gpt-4-turbo-preview  # or gpt-3.5-turbo
    timeout-seconds: 30
  temperature: 0.7
  max-tokens: 500
```

**Supported Models**:
- `gpt-4-turbo-preview` - Best quality, higher cost
- `gpt-4` - Excellent quality
- `gpt-3.5-turbo` - Fast and cost-effective

**Setup**:
```bash
export OPENAI_API_KEY="sk-..."
```

#### Anthropic Claude

```yaml
model:
  provider: anthropic
  anthropic:
    api-key: ${ANTHROPIC_API_KEY}
    model-name: claude-3-sonnet-20240229
    timeout-seconds: 30
  temperature: 0.7
  max-tokens: 500
```

**Supported Models**:
- `claude-3-opus-20240229` - Highest capability
- `claude-3-sonnet-20240229` - Balanced performance
- `claude-3-haiku-20240307` - Fast and efficient

#### Google Gemini

```yaml
model:
  provider: gemini
  gemini:
    project-id: ${GOOGLE_CLOUD_PROJECT}
    location: us-central1
    model-name: gemini-pro
    api-key: ${GOOGLE_API_KEY}  # Alternative to ADC
    timeout-seconds: 30
  temperature: 0.7
  max-tokens: 500
```

**Supported Models**:
- `gemini-pro` - Text-only model, optimized for text generation
- `gemini-1.5-pro` - Latest version with extended context window
- `gemini-1.5-flash` - Faster, cost-effective option

**Authentication Options**:

1. **API Key** (Simplest):
```bash
export GOOGLE_API_KEY="your-api-key-here"
```

2. **Application Default Credentials** (For GCP):
```bash
gcloud auth application-default login
export GOOGLE_CLOUD_PROJECT="your-project-id"
```

3. **Service Account** (Production):
```bash
export GOOGLE_APPLICATION_CREDENTIALS="/path/to/service-account-key.json"
export GOOGLE_CLOUD_PROJECT="your-project-id"
```

**Setup**:
```bash
# Option 1: Using API Key
export GOOGLE_API_KEY="your-api-key"

# Option 2: Using Application Default Credentials
gcloud auth application-default login
export GOOGLE_CLOUD_PROJECT="your-project-id"

# Run the application
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

**Features**:
- Automatic retry with exponential backoff (3 attempts)
- Token usage tracking and cost estimation
- Safety filter handling
- Rate limiting support

### Vector Database Setup

#### ChromaDB (Recommended for Getting Started)

**Docker Setup**:
```bash
docker run -d \
  --name chromadb \
  -p 8000:8000 \
  -v chroma-data:/chroma/chroma \
  chromadb/chroma:latest
```

**Configuration**:
```yaml
vector-db:
  type: chroma
  connection-url: http://localhost:8000
  collection-name: java25_docs
  chroma:
    tenant: default_tenant
    database: default_database
```

#### PostgreSQL with pgvector

**Docker Setup**:
```bash
docker run -d \
  --name postgres-pgvector \
  -e POSTGRES_PASSWORD=yourpassword \
  -e POSTGRES_DB=ragdb \
  -p 5432:5432 \
  ankane/pgvector:latest
```

**Configuration**:
```yaml
vector-db:
  type: pgvector
  pgvector:
    host: localhost
    port: 5432
    database: ragdb
    username: postgres
    password: ${POSTGRES_PASSWORD}
    schema: public
    table-name: document_embeddings
```

#### Qdrant

**Docker Setup**:
```bash
docker run -d \
  --name qdrant \
  -p 6333:6333 \
  -v qdrant-data:/qdrant/storage \
  qdrant/qdrant:latest
```

**Configuration**:
```yaml
vector-db:
  type: qdrant
  connection-url: http://localhost:6333
  collection-name: java25_docs
  qdrant:
    api-key: ${QDRANT_API_KEY:}
    use-tls: false
```

### Configuration Profiles

The system includes pre-configured profiles:

#### Development Profile (`application-dev.yml`)
- Self-hosted models (Ollama)
- Local ChromaDB
- Verbose logging
- Lower similarity threshold for experimentation
- Enabled caching

**Usage**:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

#### Production Profile (`application-prod.yml`)
- Paid API models (OpenAI/Anthropic)
- Production vector database
- Optimized logging
- Higher similarity threshold for quality
- Extended cache TTL

**Usage**:
```bash
export OPENAI_API_KEY="your-key"
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

#### Custom Configuration

Create your own profile:
```bash
# Create application-custom.yml
cp src/main/resources/application-dev.yml src/main/resources/application-custom.yml

# Edit as needed
vim src/main/resources/application-custom.yml

# Run with custom profile
mvn spring-boot:run -Dspring-boot.run.profiles=custom
```

## Usage

### Web Chat UI

The easiest way to interact with the RAG system is through the web-based chat interface.

#### Accessing the Chat UI

1. Start the application (see Quick Start)
2. Open your browser to `http://localhost:8080`
3. Type your question about Java 25 in the chat input
4. View the answer with source references

#### Features

- **Real-time Updates**: WebSocket connection provides live processing status (embedding, searching, generating)
- **Markdown Rendering**: Formatted responses with syntax-highlighted code blocks
- **Source References**: Click to view which documentation sections were used
- **Conversation History**: Full session history maintained in browser
- **Session Persistence**: Conversations saved in localStorage
- **Responsive Design**: Works on desktop, tablet, and mobile devices
- **Error Handling**: Clear error messages with retry options
- **Clear History**: Reset conversation with one click

#### Chat UI Architecture

The UI is built with React and TypeScript:
- **Frontend**: React 18+ with TypeScript
- **Communication**: REST API for queries, WebSocket for real-time updates
- **State Management**: React Context API
- **Markdown**: react-markdown with syntax highlighting
- **Deployment**: Bundled into Spring Boot JAR or served separately

#### Development

To develop the UI locally:

```bash
cd chat-ui
npm install
npm start
```

The development server runs on `http://localhost:3000` and proxies API requests to `http://localhost:8080`.

#### Building

Build the UI for production:

```bash
cd chat-ui
npm run build
```

This builds the UI into `../src/main/resources/static/`, which will be bundled into the Spring Boot JAR.

#### Customization

- **Styling**: Modify CSS files in `chat-ui/src/components/`
- **Themes**: Change color schemes in CSS variables
- **Code Highlighting**: Adjust syntax highlighter theme in `MessageList.tsx`
- **WebSocket**: Configure reconnection behavior in `websocket.ts`

For detailed information, see [docs/CHAT_UI.md](docs/CHAT_UI.md).

### Document Ingestion

Before querying, you need to ingest Java 25 documentation:

**Ingest from Directory**:
```bash
curl -X POST "http://localhost:8080/api/ingest?path=/path/to/java25/docs"
```

**Response**:
```json
{
  "documentsProcessed": 45,
  "chunksCreated": 523,
  "embeddingsGenerated": 523,
  "failures": 0,
  "processingTimeMs": 12450,
  "status": "SUCCESS"
}
```

**Supported Formats**:
- Markdown (`.md`)
- HTML (`.html`)
- Plain text (`.txt`)

**Ingestion Configuration**:
```yaml
ingestion:
  chunk-size: 1000        # Characters per chunk
  chunk-overlap: 200      # Overlap between chunks
  batch-size: 100         # Embeddings per batch
```

### Querying

**Basic Query**:
```bash
curl -X POST http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{
    "question": "What are sealed classes in Java 25?"
  }'
```

**Response**:
```json
{
  "answer": "Sealed classes in Java 25 are classes that restrict which other classes can extend them. They provide more control over inheritance hierarchies...",
  "sources": [
    {
      "filename": "sealed-classes.md",
      "section": "Introduction",
      "chunkIndex": 0
    },
    {
      "filename": "sealed-classes.md",
      "section": "Usage",
      "chunkIndex": 2
    }
  ],
  "tokenUsage": {
    "promptTokens": 1250,
    "completionTokens": 180,
    "totalTokens": 1430
  },
  "responseTimeMs": 2340
}
```

### Example Queries

Here are example queries with expected response types:

**1. Feature Explanation**
```bash
curl -X POST http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{"question": "Explain record classes in Java 25"}'
```
**Expected**: Detailed explanation of record classes, syntax, and use cases.

**2. Code Examples**
```bash
curl -X POST http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{"question": "Show me an example of a sealed class"}'
```
**Expected**: Code snippet demonstrating sealed class syntax with explanation.

**3. Comparison Questions**
```bash
curl -X POST http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{"question": "What is the difference between records and regular classes?"}'
```
**Expected**: Comparative analysis highlighting key differences.

**4. Best Practices**
```bash
curl -X POST http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{"question": "When should I use pattern matching in switch statements?"}'
```
**Expected**: Guidelines and recommendations for appropriate usage.

**5. Troubleshooting**
```bash
curl -X POST http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{"question": "Why am I getting a compilation error with sealed classes?"}'
```
**Expected**: Common issues and solutions related to sealed classes.

## Observability

The My Java Genie includes comprehensive observability through OpenTelemetry integration with the Grafana stack.

### OpenTelemetry Integration

The system automatically collects and exports:
- **Distributed Traces**: Track request flow through the entire query pipeline
- **Custom Metrics**: Monitor query performance, token usage, and costs
- **Correlated Logs**: All logs include trace and span IDs for easy correlation

#### Quick Start

1. Start the observability stack:
```bash
docker-compose up -d
```

This starts:
- **Grafana Alloy**: OpenTelemetry collector (ports 4317, 4318, 12345)
- **Grafana Tempo**: Distributed tracing backend (port 3200)
- **Grafana Mimir**: Prometheus-compatible metrics storage (port 9009)
- **Grafana Loki**: Log aggregation system (port 3100)
- **Grafana**: Unified visualization dashboard (port 3000)

2. Access Grafana at `http://localhost:3000`
   - Username: `admin`
   - Password: `admin`

3. View the pre-built **RAG System Overview** dashboard

#### Configuration

Enable OpenTelemetry in `application.yml`:

```yaml
opentelemetry:
  enabled: true
  service-name: my-java-genie
  
  traces:
    enabled: true
    endpoint: http://localhost:4317
    sampling-rate: 1.0  # 100% for development, reduce for production
    
  metrics:
    enabled: true
    endpoint: http://localhost:4317
    export-interval-millis: 60000
    
  logs:
    enabled: true
    endpoint: http://localhost:4317
```

### Grafana Stack

#### Why Grafana Stack?

This implementation uses Grafana's modern observability stack:

- **Grafana Alloy** (vs OpenTelemetry Collector): Native Grafana integration with better performance
- **Grafana Tempo** (vs Jaeger): More efficient storage and better scalability
- **Grafana Mimir** (vs Prometheus): Horizontally scalable with long-term retention
- **Grafana Loki** (vs ELK): Simpler architecture with lower resource requirements

#### Accessing Services

- **Grafana**: http://localhost:3000 - Unified interface for all observability data
- **Grafana Alloy**: http://localhost:12345 - Collector status and configuration
- **Tempo**: http://localhost:3200 - Trace storage (accessed via Grafana)
- **Mimir**: http://localhost:9009 - Metrics storage (accessed via Grafana)
- **Loki**: http://localhost:3100 - Log storage (accessed via Grafana)

### Viewing Traces, Metrics, and Logs

#### Distributed Traces

Traces show the complete flow of a query through the system:

```
process-query (root span)
├── embed-query (embedding generation)
├── vector-search (similarity search)
├── build-prompt (prompt construction)
└── llm-generate (LLM API call)
```

**View traces in Grafana**:
1. Go to **Explore** → Select **Tempo** datasource
2. Search by service name, operation, or duration
3. Click on a trace to see detailed span information
4. View span attributes (query text, tokens, model, etc.)
5. Jump to related logs using trace correlation

#### Custom Metrics

Available metrics:

- `rag.query.duration` - Query processing time (histogram)
- `rag.query.total` - Total queries processed (counter)
- `rag.query.errors` - Failed queries (counter)
- `rag.tokens.prompt` - Prompt tokens per query (histogram)
- `rag.tokens.completion` - Completion tokens per query (histogram)
- `rag.tokens.cost` - Estimated cost in USD (histogram)

**Query metrics in Grafana**:
1. Go to **Explore** → Select **Mimir** datasource
2. Use PromQL queries:

```promql
# 95th percentile query duration
histogram_quantile(0.95, sum(rate(rag_query_duration_bucket[5m])) by (le, provider))

# Query rate by status
sum(rate(rag_query_total[5m])) by (status)

# Token usage by provider
histogram_quantile(0.95, sum(rate(rag_tokens_prompt_bucket[5m])) by (le, provider))
```

#### Log Correlation

All logs include trace context for easy correlation:

**View logs in Grafana**:
1. Go to **Explore** → Select **Loki** datasource
2. Use LogQL queries:

```logql
# All logs from the RAG system
{service_name="my-java-genie"}

# Error logs only
{service_name="my-java-genie"} |= "ERROR"

# Logs for a specific trace
{service_name="my-java-genie"} | json | trace_id="abc123"
```

**Jump between traces and logs**:
- From a trace in Tempo, click "Logs for this span" to see related logs
- From logs in Loki, click on trace_id to view the full trace

#### Pre-built Dashboard

The **RAG System Overview** dashboard includes:

1. **Query Duration** (p50, p95, p99) - Track response times
2. **Query Rate by Status** - Monitor success/error rates
3. **Token Usage** (p95) - Track prompt and completion tokens
4. **Estimated Token Cost** - Monitor API costs over time
5. **Error Rate by Type** - Identify common errors
6. **Service Graph** - Visualize dependencies

Access: **Dashboards** → **RAG System** → **RAG System Overview**

### Production Considerations

#### Sampling

For production, reduce sampling to minimize overhead:

```yaml
opentelemetry:
  traces:
    sampling-rate: 0.1  # Sample 10% of traces
```

#### Data Retention

Default retention periods (configurable):
- **Tempo**: 48 hours
- **Mimir**: 7 days
- **Loki**: 7 days

Adjust in `tempo-config.yaml`, `mimir-config.yaml`, and `loki-config.yaml`.

#### Security

- Use TLS for production OTLP endpoints
- Secure Grafana with proper authentication
- Restrict access to observability UIs
- Use API keys for Grafana datasources

#### Performance

- Batch processing reduces export overhead
- Async export doesn't block application threads
- Memory limits prevent resource exhaustion
- Fallback to no-op if export fails

For detailed information, see [docs/OBSERVABILITY.md](docs/OBSERVABILITY.md) and [OBSERVABILITY_QUICKSTART.md](OBSERVABILITY_QUICKSTART.md).

## Token Optimization Strategies

The system implements several strategies to minimize token usage and reduce costs:

### 1. Chunk Limiting

**Configuration**:
```yaml
query:
  max-retrieved-chunks: 5  # Limit context size
```

**Impact**: Reduces prompt tokens by limiting retrieved context. Balance between cost and answer quality.

**Recommendation**:
- Development: 5-7 chunks
- Production: 3-5 chunks
- Complex queries: 7-10 chunks

### 2. Similarity Threshold

**Configuration**:
```yaml
query:
  similarity-threshold: 0.75  # Filter low-relevance chunks
```

**Impact**: Only includes highly relevant chunks, reducing noise and token count.

**Recommendation**:
- Strict (0.80+): High precision, may miss context
- Balanced (0.70-0.80): Good trade-off
- Lenient (0.60-0.70): More context, higher cost

### 3. Response Token Limits

**Configuration**:
```yaml
model:
  max-tokens: 500  # Limit response length
```

**Impact**: Caps completion tokens per response.

**Recommendation**:
- Brief answers: 300-500 tokens
- Detailed explanations: 500-800 tokens
- Comprehensive responses: 800-1500 tokens

### 4. Temperature Tuning

**Configuration**:
```yaml
model:
  temperature: 0.7  # Control randomness
```

**Impact**: Lower temperature (0.3-0.5) produces more focused, deterministic responses with potentially fewer tokens.

**Recommendation**:
- Factual queries: 0.3-0.5
- Creative explanations: 0.7-0.9

### 5. Query Caching

**Configuration**:
```yaml
query:
  enable-cache: true
  cache-ttl-minutes: 120
```

**Impact**: Caches responses for identical queries, eliminating redundant API calls.

**Recommendation**: Enable in production for frequently asked questions.

### 6. Batch Processing

For ingestion, the system processes embeddings in batches:

**Configuration**:
```yaml
ingestion:
  batch-size: 100  # Embeddings per batch
```

**Impact**: Reduces API calls during document ingestion.

### 7. Monitoring Token Usage

Track token consumption via metrics:

```bash
# Access metrics endpoint
curl http://localhost:8080/actuator/metrics/rag.tokens.total

# View token usage in Docker logs
docker-compose logs -f my-java-genie-app | grep "Token usage"
```

**Cost Estimation** (OpenAI GPT-4 Turbo):
- Input: $0.01 per 1K tokens
- Output: $0.03 per 1K tokens
- Average query: ~1500 input + 300 output = $0.024

**Monthly Cost Example**:
- 1000 queries/month: ~$24
- 10,000 queries/month: ~$240

## API Reference

### Health Check

**Endpoint**: `GET /api/health`

**Response**:
```json
{
  "status": "UP",
  "components": {
    "languageModel": {
      "status": "UP",
      "provider": "openai",
      "model": "gpt-4-turbo-preview"
    },
    "vectorDatabase": {
      "status": "UP",
      "type": "chroma",
      "collection": "java25_docs"
    }
  }
}
```

### Query Endpoint

**Endpoint**: `POST /api/query`

**Request**:
```json
{
  "question": "string (required)"
}
```

**Response**:
```json
{
  "answer": "string",
  "sources": [
    {
      "filename": "string",
      "section": "string",
      "chunkIndex": "integer"
    }
  ],
  "tokenUsage": {
    "promptTokens": "integer",
    "completionTokens": "integer",
    "totalTokens": "integer"
  },
  "responseTimeMs": "long"
}
```

**Status Codes**:
- `200 OK`: Successful query
- `400 Bad Request`: Invalid request format
- `503 Service Unavailable`: Model or vector DB unavailable
- `504 Gateway Timeout`: Query exceeded timeout

### Ingestion Endpoint

**Endpoint**: `POST /api/ingest`

**Parameters**:
- `path` (query parameter): Directory path containing documents

**Response**:
```json
{
  "documentsProcessed": "integer",
  "chunksCreated": "integer",
  "embeddingsGenerated": "integer",
  "failures": "integer",
  "processingTimeMs": "long",
  "status": "string"
}
```

## Monitoring and Health Checks

### Actuator Endpoints

The system exposes Spring Boot Actuator endpoints:

**Health**: `GET /actuator/health`
```bash
curl http://localhost:8080/actuator/health
```

**Metrics**: `GET /actuator/metrics`
```bash
# List all metrics
curl http://localhost:8080/actuator/metrics

# Specific metric
curl http://localhost:8080/actuator/metrics/rag.query.duration
```

**Prometheus**: `GET /actuator/prometheus`
```bash
curl http://localhost:8080/actuator/prometheus
```

### Logging

**Log Levels**:
```yaml
logging:
  level:
    br.com.arquivolivre.myjavagenie: DEBUG          # Application logs
    br.com.arquivolivre.myjavagenie.service: DEBUG  # Service layer
    dev.langchain4j: WARN       # LangChain4j library
```

**View Logs**:
```bash
# Docker logs (recommended)
docker-compose logs -f my-java-genie-app

# Filter for errors
docker-compose logs my-java-genie-app | grep ERROR

# Filter for specific patterns
docker-compose logs my-java-genie-app | grep "Token usage"

# Local development logs
mvn spring-boot:run | grep ERROR
```

## Troubleshooting

### Common Issues

**1. "Language Model unavailable"**

**Cause**: Model provider not running or misconfigured.

**Solution**:
```bash
# For Ollama
ollama serve
ollama list  # Verify model is pulled

# For OpenAI
echo $OPENAI_API_KEY  # Verify API key is set

# For Gemini
echo $GOOGLE_API_KEY  # Verify API key is set
# OR
echo $GOOGLE_CLOUD_PROJECT  # Verify project ID for ADC
gcloud auth application-default login
```

**2. "Vector Database connection failed"**

**Cause**: Vector DB not running or wrong connection URL.

**Solution**:
```bash
# Check ChromaDB
curl http://localhost:8000/api/v1/heartbeat

# Check Docker container
docker ps | grep chroma
docker logs chromadb
```

**3. "No relevant documents found"**

**Cause**: Documents not ingested or similarity threshold too high.

**Solution**:
```bash
# Verify ingestion
curl http://localhost:8080/api/health

# Lower threshold in config
query:
  similarity-threshold: 0.6  # Lower from 0.75
```

**4. "Query timeout"**

**Cause**: Model response too slow or timeout too short.

**Solution**:
```yaml
query:
  timeout-seconds: 20  # Increase from 10

model:
  self-hosted:
    timeout-seconds: 90  # Increase for self-hosted
  gemini:
    timeout-seconds: 30  # Adjust for Gemini
```

**5. High Token Usage**

**Cause**: Too many chunks or large responses.

**Solution**:
```yaml
query:
  max-retrieved-chunks: 3      # Reduce from 5
  similarity-threshold: 0.80   # Increase threshold

model:
  max-tokens: 300              # Reduce from 500
```

### Chat UI Issues

**6. Chat UI Not Loading**

**Cause**: Static files not built or not found.

**Solution**:
```bash
# Rebuild the UI
cd chat-ui
npm run build

# Verify static files exist
ls -la src/main/resources/static/

# Rebuild Docker image
docker-compose build my-java-genie-app
docker-compose up -d my-java-genie-app
```

**7. WebSocket Connection Failed**

**Cause**: WebSocket endpoint not accessible or CORS issues.

**Solution**:
```bash
# Check WebSocket endpoint
curl -i -N -H "Connection: Upgrade" -H "Upgrade: websocket" \
  http://localhost:8080/ws/chat?sessionId=test

# Check application logs for WebSocket errors
docker-compose logs -f my-java-genie-app | grep WebSocket

# Verify CORS configuration in application.yml
```

**8. Chat UI Shows Old Data**

**Cause**: Browser cache or stale session.

**Solution**:
- Clear browser cache (Ctrl+Shift+Delete)
- Clear localStorage: Open browser console and run `localStorage.clear()`
- Hard refresh: Ctrl+Shift+R (Windows/Linux) or Cmd+Shift+R (Mac)

### Observability Issues

**9. Traces Not Appearing in Grafana**

**Cause**: OpenTelemetry not configured or Alloy not running.

**Solution**:
```bash
# Check OpenTelemetry is enabled
grep "opentelemetry.enabled" src/main/resources/application.yml

# Verify Alloy is running
docker ps | grep alloy
curl http://localhost:12345/ready

# Check Alloy logs
docker logs my-java-genie-alloy

# Verify Tempo is receiving traces
docker logs my-java-genie-tempo
curl http://localhost:3200/api/search
```

**10. Metrics Not Updating**

**Cause**: Mimir not running or metrics not being exported.

**Solution**:
```bash
# Check Mimir status
docker logs my-java-genie-mimir
curl http://localhost:9009/ready

# Verify Alloy is forwarding metrics
curl http://localhost:12345/metrics

# Check metric export interval in application.yml
# Query Mimir directly
curl 'http://localhost:9009/prometheus/api/v1/query?query=up'
```

**11. Logs Not Appearing in Loki**

**Cause**: Loki not running or log format incorrect.

**Solution**:
```bash
# Check Loki status
docker logs my-java-genie-loki
curl http://localhost:3100/ready

# Verify log format includes trace_id
docker-compose logs my-java-genie-app | grep trace_id

# Query Loki directly
curl 'http://localhost:3100/loki/api/v1/labels'
```

**12. Grafana Can't Connect to Datasources**

**Cause**: Datasources not configured or services not reachable.

**Solution**:
```bash
# Check all observability services are running
docker-compose ps

# Verify network connectivity
docker exec my-java-genie-grafana ping tempo
docker exec my-java-genie-grafana ping mimir
docker exec my-java-genie-grafana ping loki

# Check Grafana datasource configuration
# Go to Grafana → Configuration → Data Sources
# Test each datasource connection
```

### Gemini Provider Issues

**13. Gemini API Authentication Failed**

**Cause**: Invalid API key or missing credentials.

**Solution**:
```bash
# Verify API key is set
echo $GOOGLE_API_KEY

# Or verify ADC is configured
gcloud auth application-default print-access-token

# Check project ID
echo $GOOGLE_CLOUD_PROJECT

# Verify credentials file exists (if using service account)
echo $GOOGLE_APPLICATION_CREDENTIALS
ls -la $GOOGLE_APPLICATION_CREDENTIALS
```

**14. Gemini Rate Limiting**

**Cause**: Exceeded API quota or rate limits.

**Solution**:
- Check Google Cloud Console for quota limits
- Implement request throttling in application
- Upgrade to higher quota tier if needed
- The system automatically retries with exponential backoff

**15. Gemini Safety Filters Triggered**

**Cause**: Query or response triggered content safety filters.

**Solution**:
- Review the query content
- Check application logs for safety filter details
- Adjust query phrasing if needed
- Consider using different model variant

### Debug Mode

Enable verbose logging:

```yaml
logging:
  level:
    br.com.arquivolivre.myjavagenie: DEBUG
    dev.langchain4j: DEBUG
    io.opentelemetry: DEBUG  # For OpenTelemetry debugging
```

Or via command line:
```bash
mvn spring-boot:run -Dlogging.level.br.com.arquivolivre.myjavagenie=DEBUG
```

### Getting Help

If issues persist:

1. Check the health endpoint: `curl http://localhost:8080/actuator/health`
2. Review application logs: `docker-compose logs -f my-java-genie-app`
3. View traces in Grafana for detailed request flow
4. Consult specific documentation:
   - [docs/TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md) - Comprehensive troubleshooting guide
   - [docs/CHAT_UI.md](docs/CHAT_UI.md) - Chat UI specific issues
   - [docs/OBSERVABILITY.md](docs/OBSERVABILITY.md) - Observability details
   - [OBSERVABILITY_QUICKSTART.md](OBSERVABILITY_QUICKSTART.md) - Quick observability setup

## Docker Files

The following files support Docker deployment:

- **docker-compose.yml**: Main Docker Compose configuration (deploys all services)
- **Dockerfile**: Multi-stage build for the Java application
- **.dockerignore**: Files to exclude from Docker build context
- **.env.example**: Example environment variables (copy to `.env`)
- **src/main/resources/application-docker.yml**: Docker-specific configuration
- **docs/DOCKER.md**: Comprehensive Docker deployment guide

## Project Structure

```
my-java-genie/
├── docker-compose.yml           # Docker orchestration
├── Dockerfile                   # Application container
├── pom.xml                      # Maven configuration
│
├── src/main/
│   ├── java/                    # Java source code
│   │   └── br/com/arquivolivre/myjavagenie/
│   │       ├── config/          # Configuration classes
│   │       ├── controller/      # REST API endpoints
│   │       ├── service/         # Business logic
│   │       ├── repository/      # Data access layer
│   │       ├── model/           # Domain models & DTOs
│   │       └── exception/       # Custom exceptions
│   │
│   └── resources/
│       ├── application.yml      # Default configuration
│       ├── application-dev.yml  # Development profile
│       ├── application-prod.yml # Production profile
│       └── application-docker.yml # Docker profile
│
├── chat-ui/                     # React TypeScript UI
│   ├── src/                     # React components & services
│   └── package.json
│
├── grafana/                     # Observability stack
│   ├── dashboards/              # Pre-built dashboards
│   └── provisioning/            # Datasource configs
│
├── docs/                        # Documentation
│   ├── CHAT_UI.md
│   ├── OBSERVABILITY.md
│   ├── TROUBLESHOOTING.md
│   └── DOCKER.md
│
└── *.yaml                       # Observability configs
    ├── alloy-config.alloy       # OpenTelemetry collector
    ├── tempo-config.yaml        # Distributed tracing
    ├── mimir-config.yaml        # Metrics storage
    └── loki-config.yaml         # Log aggregation
```

## Contributing

This project follows clean code principles and SOLID design. When contributing:

1. Follow existing code structure and naming conventions
2. Write unit tests for new functionality
3. Update documentation for configuration changes
4. Ensure all tests pass before submitting

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

Copyright (c) 2025 Thiago Gonzaga

## Support

For issues and questions:
- Check the [Troubleshooting](#troubleshooting) section
- Review application logs: `docker-compose logs -f my-java-genie-app`
- Check health endpoint: `http://localhost:8080/api/health`
