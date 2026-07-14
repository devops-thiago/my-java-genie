#!/usr/bin/env bash
# Starts ChromaDB + the app (OpenAI provider) using the slim workshop compose.
set -euo pipefail
cd "$(dirname "$0")/.."

if [ ! -f .env ] || ! grep -q '^OPENAI_API_KEY=sk' .env; then
  echo "ERROR: OPENAI_API_KEY not set in .env. Run: ./scripts/set-api-key.sh" >&2
  exit 1
fi

echo "Starting ChromaDB + app (building image on first run)..."
docker compose -f docker-compose.workshop.yml up -d --build

echo "Waiting for the app to become healthy..."
until curl -sf http://localhost:8080/actuator/health >/dev/null 2>&1; do
  printf '.'; sleep 3
done
echo
echo "App is up!"
echo "  UI:     http://localhost:8080"
echo "  Health: http://localhost:8080/actuator/health"
echo "  Logs:   docker compose -f docker-compose.workshop.yml logs -f java-rag-app"
echo "  Stop:   docker compose -f docker-compose.workshop.yml down"
