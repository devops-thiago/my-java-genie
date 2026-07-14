# Workshop - My Java Genie (VM TDC)

Bem-vindo(a)! Esta VM já vem com tudo pronto para o workshop do **My Java Genie** (sistema RAG sobre a documentação do Java 25): JDK 21, Maven, Node 20, Docker, VS Code e o repositório clonado em `~/my-java-genie`.

O modelo de LLM usado é a **API OpenAI** (`gpt-4o-mini`). **Cada participante usa a sua própria chave** da OpenAI — ela **não** vem na VM.

## 1. Configurar sua chave OpenAI

```bash
cd ~/my-java-genie
./scripts/set-api-key.sh
```

Cole sua chave (`sk-...`) quando solicitado. Ela é salva em `.env` (não é compartilhada nem exportada com a VM).

> Pegue sua chave em https://platform.openai.com/api-keys

## 2. Subir o sistema (ChromaDB + app)

```bash
./scripts/start-workshop.sh
```

Na primeira execução a imagem da app é construída (pode levar alguns minutos). Quando terminar:

- **UI (chat):** http://localhost:8080
- **Health:** http://localhost:8080/actuator/health
- **Logs:** `docker compose -f docker-compose.workshop.yml logs -f java-rag-app`
- **Parar:** `docker compose -f docker-compose.workshop.yml down`

## 3. Ingerir a documentação do Java 25

Antes de perguntar, é preciso ingerir os documentos (já baixados na VM em `docs/java25/`):

```bash
./scripts/ingest.sh
```

Se a pasta estiver vazia, baixe os docs primeiro:

```bash
./scripts/fetch-java25-docs.sh
```

## 4. Perguntar (RAG)

- Pela **UI**: abra http://localhost:8080 e digite sua pergunta sobre Java 25.
- Por **curl**:

```bash
curl -X POST http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{"question": "What are sealed classes in Java 25?"}'
```

## 5. Desenvolvimento ao vivo (Maven + Node)

Rodar a app localmente (fora do Docker), com ChromaDB no Docker:

```bash
./scripts/run-dev.sh
```

Para desenvolver a UI (React) em http://localhost:3000:

```bash
cd chat-ui
npm install
npm start
```

## Portas

- **8080**: app (API + UI)
- **8000**: ChromaDB
- **3000**: dev server da UI (opcional, só durante `npm start`)

## Resolução de problemas

- **"API key is required for OpenAI provider"**: rode `./scripts/set-api-key.sh`.
- **"No relevant documents found"**: rode `./scripts/ingest.sh`.
- **App não sobe**: `docker compose -f docker-compose.workshop.yml logs java-rag-app`.
- **Sem internet**: a API OpenAI exige internet (modo NAT da VM). Teste: `curl -s https://api.openai.com/v1/models -H "Authorization: Bearer $OPENAI_API_KEY" | head`.

## Notas

- A chave OpenAI fica apenas em `.env` (runtime). A VM exportada (OVA) **não** contém chaves.
- A stack de observabilidade (Grafana/Tempo/Mimir/Loki) foi desativada para caber em 6GB de RAM.
- Embeddings são locais (`all-minilm-l6-v2`, em processo) — não consomem a API.
