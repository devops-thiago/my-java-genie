#!/usr/bin/env bash
# Mirrors the official Oracle Java 25 documentation (guides + specifications)
# into docs/java25/, skipping the huge API javadoc (low value for Q&A in natural
# language) and binary assets. Files are saved as .html, which ingestion supports.
#
# Usage: ./scripts/fetch-java25-docs.sh [dest-dir]   (default: docs/java25)
set -euo pipefail
cd "$(dirname "$0")/.."

DEST="${1:-docs/java25}"
mkdir -p "$DEST"

if ! command -v wget >/dev/null 2>&1; then
  echo "ERROR: wget is required. Install with: sudo apt-get install -y wget" >&2
  exit 1
fi

echo "Mirroring Oracle Java 25 documentation into $DEST ..."
echo "(skipping the API javadoc and binary assets to keep the corpus focused)"

wget --mirror --convert-links --adjust-extension \
  --no-parent --no-host-directories --cut-dirs=4 \
  --reject css,js,png,gif,jpg,jpeg,svg,ico,webp,pdf \
  --reject-regex='(/docs/api/)' \
  --user-agent="Mozilla/5.0 (X11; Linux x86_64) Gecko/20100101 Firefox/120.0" \
  -e robots=off \
  --wait=1 --limit-rate=500k \
  --tries=2 --timeout=30 \
  -P "$DEST" \
  "https://docs.oracle.com/en/java/javase/25/" 2>&1 | tail -30 || true

echo
echo "Done. HTML documents saved under $DEST"
echo "Next: ./scripts/start-workshop.sh && ./scripts/ingest.sh"
