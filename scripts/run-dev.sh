#!/usr/bin/env bash
# Local dev: starts only ChromaDB, then runs the app with Maven (workshop profile).
set -euo pipefail
cd "$(dirname "$0")/.."

if [ ! -f .env ] || ! grep -q '^OPENAI_API_KEY=sk' .env; then
  echo "ERROR: OPENAI_API_KEY not set in .env. Run: ./scripts/set-api-key.sh" >&2
  exit 1
fi

echo "Starting ChromaDB only..."
docker compose -f docker-compose.workshop.yml up -d chromadb

export OPENAI_API_KEY="$(grep '^OPENAI_API_KEY=' .env | cut -d= -f2-)"

echo "Running app locally with Maven (workshop profile)..."
mvn spring-boot:run -Dspring-boot.run.profiles=workshop \
  -Dspotless.check.skip=true -Dcheckstyle.skip=true -Dspotbugs.skip=true
