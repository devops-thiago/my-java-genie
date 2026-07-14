#!/usr/bin/env bash
# Prompts for the OpenAI API key and saves it to .env (used by docker-compose.workshop.yml).
set -euo pipefail
cd "$(dirname "$0")/.."

if [ ! -f .env ]; then
  cp .env.example .env
  echo "Created .env from .env.example"
fi

read -rsp "Paste your OpenAI API key (sk-...): " KEY
echo

if [ -z "$KEY" ]; then
  echo "ERROR: empty key. Aborting." >&2
  exit 1
fi

if grep -q '^OPENAI_API_KEY=' .env; then
  sed -i "s|^OPENAI_API_KEY=.*|OPENAI_API_KEY=${KEY}|" .env
else
  echo "OPENAI_API_KEY=${KEY}" >> .env
fi

echo "OK: OPENAI_API_KEY saved to .env"
echo "Next: ./scripts/start-workshop.sh"
