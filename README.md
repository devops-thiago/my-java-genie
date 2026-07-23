# My Java Genie

Chat app in Java (Spring Boot + LangChain4j) with a React UI. Talks to any OpenAI-compatible LLM API.

## Requirements

- JDK 21+
- Maven 3.9+
- Node 20+ (UI)
- Docker (for ChromaDB in later steps)
- LLM API key (e.g. [Ollama Cloud](https://ollama.com))

## Configuration

Copy `.env.example` to `.env` and set your values:

```bash
cp .env.example .env
```

| Variable | Description |
|---|---|
| `LLM_API_KEY` | API key |
| `LLM_BASE_URL` | OpenAI-compatible base URL (default: `https://ollama.com/v1`) |
| `LLM_MODEL_NAME` | Model id |

Any OpenAI-compatible server works (Ollama Cloud, local Ollama, vLLM, LM Studio, etc.) by changing `LLM_BASE_URL`.

## Run

```bash
set -a && source .env && set +a
mvn spring-boot:run
```

UI (another terminal):

```bash
cd chat-ui && npm install && npm start
```

- App: http://localhost:8080  
- UI: http://localhost:3000  

```bash
curl -X POST http://localhost:8080/api/chat/query \
  -H "Content-Type: application/json" \
  -d '{"sessionId":"demo","message":"Olá!"}'
```

## Ingest and query

```bash
docker compose up -d chromadb

curl -X POST "http://localhost:8080/api/ingest?path=docs/specs/primitive-types-in-patterns-instanceof-switch-jls.html"

curl -X POST http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{"question":"O que mudou com patterns e primitive types no switch?"}'
```

Chat UI uses the same RAG path via `/api/chat/query`.

## Cost knobs

| Variable | Effect |
|---|---|
| `QUERY_TOP_K` | How many chunks go into the prompt (try `5` then `2`) |
| `INGEST_CHUNK_SIZE` / `INGEST_CHUNK_OVERLAP` | Chunk shape at ingest time |
| `LLM_MAX_TOKENS` | Max completion tokens from the LLM |

After a query, logs show latency and approximate token counts.

## OpenTelemetry

Enabled by default with the **logging** exporter (spans and metrics print to the console).

```bash
# default — watch the app log after ingest/query
OTEL_EXPORTER=logging

# optional — send to an OTLP collector (e.g. instructor Grafana stack)
OTEL_EXPORTER=otlp
OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4317
```

Instrumented spans: `ingest`, `embed`, `query`, `translate`, `retrieve`, `generate`.

Metrics: `rag.query.latency`, `rag.retrieve.latency`, `rag.generate.latency`,
`rag.chunks.retrieved`, `rag.tokens.prompt`, `rag.tokens.completion`, `rag.ingest.chunks`.

## Tests

```bash
mvn clean test
```
