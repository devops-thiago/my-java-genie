#!/usr/bin/env bash
# Ingests the Java 25 docs into the RAG system.
# Usage: ./scripts/ingest.sh [docs-dir]   (default: docs/java25)
set -euo pipefail
cd "$(dirname "$0")/.."

DOCS_DIR="${1:-docs/java25}"
APP_URL="${APP_URL:-http://localhost:8080}"

if [ ! -d "$DOCS_DIR" ]; then
  echo "ERROR: docs directory '$DOCS_DIR' not found." >&2
  echo "Run ./scripts/fetch-java25-docs.sh first to download the Java 25 docs." >&2
  exit 1
fi

# The app reads files from the given path. Use the in-container path when the app
# runs in Docker (./docs is mounted at /app/docs), otherwise the host path.
if docker ps --format '{{.Names}}' | grep -q 'java-rag-workshop-app'; then
  INGEST_PATH="/app/docs/java25"
else
  INGEST_PATH="$(cd "$DOCS_DIR" && pwd)"
fi

echo "Ingesting documents from: $INGEST_PATH"
curl -X POST "${APP_URL}/api/ingest?path=${INGEST_PATH}" \
  -H "Content-Type: application/json" \
  --max-time 600
echo
