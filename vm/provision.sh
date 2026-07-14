#!/usr/bin/env bash
# =============================================================================
# Workshop VM provisioning for My Java Genie (TDC)
# =============================================================================
# Installs everything the workshop needs on a fresh Ubuntu/Xubuntu 24.04:
#   - base tools, Docker Engine + compose plugin
#   - Eclipse Temurin JDK 21, Maven, Node 20 LTS
#   - VS Code + Java/Spring extensions
#   - the repo (workshop-vm branch) at ~/my-java-genie
#   - official Java 25 docs (mirrored into docs/java25/)
#   - pre-pulled ChromaDB image + pre-built app image
#   - WORKSHOP_VM=true in /etc/environment (auto-activates the Maven workshop profile)
#
# Run as root (idempotent). Two ways to run:
#   sudo WORKSHOP_VM=true bash vm/provision.sh
#   curl -fsSL https://raw.githubusercontent.com/devops-thiago/my-java-genie/workshop-vm/vm/provision.sh | sudo WORKSHOP_VM=true bash
# =============================================================================
set -uo pipefail

LOG=/var/log/workshop-provision.log
mkdir -p "$(dirname "$LOG")"
exec > >(tee -a "$LOG") 2>&1

echo "=== Workshop provisioning started at $(date) ==="
if [ "$(id -u)" -ne 0 ]; then
  echo "ERROR: run as root (use sudo)." >&2
  exit 1
fi

WS_USER="${WORKSHOP_USER:-workshop}"
export WORKSHOP_VM=true
export DEBIAN_FRONTEND=noninteractive

# ---------------------------------------------------------------------------
# 1. Base packages
# ---------------------------------------------------------------------------
apt-get update
apt-get install -y --no-install-recommends \
  ca-certificates curl wget git build-essential unzip gnupg \
  software-properties-common lsb-release xz-utils

# ---------------------------------------------------------------------------
# 2. WORKSHOP_VM=true globally (Maven workshop profile auto-activates)
# ---------------------------------------------------------------------------
if ! grep -q '^WORKSHOP_VM=' /etc/environment; then
  echo 'WORKSHOP_VM=true' >> /etc/environment
fi

# ---------------------------------------------------------------------------
# 3. Docker Engine + compose plugin
# ---------------------------------------------------------------------------
if ! command -v docker >/dev/null 2>&1; then
  install -m 0755 -d /etc/apt/keyrings
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
  chmod a+r /etc/apt/keyrings/docker.asc
  CODENAME="$(. /etc/os-release && echo "$VERSION_CODENAME")"
  ARCH="$(dpkg --print-architecture)"
  echo "deb [arch=${ARCH} signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu ${CODENAME} stable" \
    > /etc/apt/sources.list.d/docker.list
  apt-get update
  apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
fi
usermod -aG docker "$WS_USER" 2>/dev/null || true
systemctl enable --now docker 2>/dev/null || true

# ---------------------------------------------------------------------------
# 4. Eclipse Temurin JDK 21
# ---------------------------------------------------------------------------
if ! command -v java >/dev/null 2>&1 || ! java -version 2>&1 | grep -q '"21'; then
  install -m 0755 -d /etc/apt/keyrings
  curl -fsSL https://packages.adoptium.net/artifactory/api/gpg/key/public \
    | gpg --dearmor -o /etc/apt/keyrings/adoptium.gpg
  CODENAME="$(. /etc/os-release && echo "$VERSION_CODENAME")"
  ARCH="$(dpkg --print-architecture)"
  echo "deb [arch=${ARCH} signed-by=/etc/apt/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb ${CODENAME} main" \
    > /etc/apt/sources.list.d/adoptium.list
  apt-get update
  apt-get install -y temurin-21-jdk
fi
# Make Temurin the default java/javac if multiple JDKs are present
if command -v update-alternatives >/dev/null 2>&1; then
  JAVA_HOME_CAND="$(find /usr/lib/jvm -maxdepth 1 -name 'temurin-21-jdk*' | head -n1)"
  if [ -n "$JAVA_HOME_CAND" ]; then
    export JAVA_HOME="$JAVA_HOME_CAND"
    update-alternatives --set java "$JAVA_HOME/bin/java" 2>/dev/null || true
    update-alternatives --set javac "$JAVA_HOME/bin/javac" 2>/dev/null || true
    if ! grep -q '^JAVA_HOME=' /etc/environment; then
      echo "JAVA_HOME=$JAVA_HOME" >> /etc/environment
    fi
  fi
fi

# ---------------------------------------------------------------------------
# 5. Maven
# ---------------------------------------------------------------------------
if ! command -v mvn >/dev/null 2>&1; then
  apt-get install -y maven
fi

# ---------------------------------------------------------------------------
# 6. Node 20 LTS (NodeSource)
# ---------------------------------------------------------------------------
if ! command -v node >/dev/null 2>&1 || ! node -v 2>/dev/null | grep -q '^v20'; then
  curl -fsSL https://deb.nodesource.com/setup_20.x | bash -
  apt-get install -y nodejs
fi

# ---------------------------------------------------------------------------
# 7. VS Code + extensions
# ---------------------------------------------------------------------------
if ! command -v code >/dev/null 2>&1; then
  install -m 0755 -d /etc/apt/keyrings
  curl -fsSL https://packages.microsoft.com/keys/microsoft.asc \
    | gpg --dearmor -o /etc/apt/keyrings/packages.microsoft.gpg
  echo "deb [arch=amd64 signed-by=/etc/apt/keyrings/packages.microsoft.gpg] https://packages.microsoft.com/repos/code stable main" \
    > /etc/apt/sources.list.d/vscode.list
  apt-get update
  apt-get install -y code
fi
# Extensions for the workshop user (best-effort)
runuser -u "$WS_USER" -- code --install-extension vscjava.vscode-java-pack --force 2>/dev/null || true
runuser -u "$WS_USER" -- code --install-extension vmware.vscode-spring-boot --force 2>/dev/null || true
runuser -u "$WS_USER" -- code --install-extension esbenp.prettier-vscode --force 2>/dev/null || true

# ---------------------------------------------------------------------------
# 8. Clone the repo (workshop-vm branch)
# ---------------------------------------------------------------------------
REPO_DIR="/home/$WS_USER/my-java-genie"
REPO_URL="https://github.com/devops-thiago/my-java-genie"
REPO_BRANCH="workshop-vm"
if [ ! -d "$REPO_DIR/.git" ]; then
  runuser -u "$WS_USER" -- git clone -b "$REPO_BRANCH" "$REPO_URL" "$REPO_DIR"
else
  runuser -u "$WS_USER" -- git -C "$REPO_DIR" fetch origin "$REPO_BRANCH" || true
  runuser -u "$WS_USER" -- git -C "$REPO_DIR" checkout "$REPO_BRANCH" || true
  runuser -u "$WS_USER" -- git -C "$REPO_DIR" reset --hard "origin/$REPO_BRANCH" || true
fi
chmod +x "$REPO_DIR"/scripts/*.sh 2>/dev/null || true
chown -R "$WS_USER":"$WS_USER" "$REPO_DIR"

# ---------------------------------------------------------------------------
# 9. Fetch official Java 25 docs (non-fatal)
# ---------------------------------------------------------------------------
echo "--- Fetching Java 25 docs (this can take several minutes) ---"
runuser -u "$WS_USER" -- bash -lc "cd '$REPO_DIR' && ./scripts/fetch-java25-docs.sh" \
  || echo "WARN: Java 25 docs fetch had issues (see above). You can retry with ./scripts/fetch-java25-docs.sh"

# ---------------------------------------------------------------------------
# 10. Pre-pull ChromaDB and build the app image (non-fatal)
# ---------------------------------------------------------------------------
echo "--- Pre-pulling ChromaDB image ---"
docker pull chromadb/chroma:0.4.24 || echo "WARN: chromadb pull failed"
echo "--- Building the app image (first build takes a while) ---"
( cd "$REPO_DIR" && OPENAI_API_KEY=build-placeholder docker compose -f docker-compose.workshop.yml build ) \
  || echo "WARN: app image build had issues (see above). It will be built on first ./scripts/start-workshop.sh"

# ---------------------------------------------------------------------------
# 11. Desktop shortcut + README
# ---------------------------------------------------------------------------
DESKTOP="/home/$WS_USER/Desktop"
mkdir -p "$DESKTOP"
cat > "$DESKTOP/README-WORKSHOP.txt" <<EOF
Workshop - My Java Genie
========================
1) Open a terminal in ~/my-java-genie
2) ./scripts/set-api-key.sh        (paste your OpenAI API key)
3) ./scripts/start-workshop.sh     (starts ChromaDB + app)
4) Open http://localhost:8080      (chat UI)
5) ./scripts/ingest.sh             (ingest Java 25 docs, if not already done)
See WORKSHOP.md (in the repo) for the full guide.
EOF
chown -R "$WS_USER":"$WS_USER" "$DESKTOP"

# ---------------------------------------------------------------------------
# 12. Cleanup to reduce OVA size (keep Docker images we need)
# ---------------------------------------------------------------------------
apt-get clean
rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*
docker builder prune -f 2>/dev/null || true   # build cache only, keeps images
# Zero out free space so the dynamic disk compresses better in the OVA
( sync; dd if=/dev/zero of=/zero bs=1M 2>/dev/null || true; rm -f /zero ) || true

echo "=== Workshop provisioning finished at $(date) ==="
touch /var/lib/workshop-provision.done
