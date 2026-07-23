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

## Tests

```bash
mvn clean test
```
