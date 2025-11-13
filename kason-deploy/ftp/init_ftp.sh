#!/usr/bin/env sh
set -eu

# Init script to build and run kason-ftp with interactive credential prompts.
# - Prompts for FTP_USER and FTP_PASS (re-prompts until non-empty)
# - On macOS (Darwin), uses port mappings and asks for PASV_ADDRESS (tries to auto-detect)
# - On Linux, uses --network host
# - Ensures FTP_BASE_DIR exists and is mounted

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
IMAGE_TAG=${IMAGE_TAG:-kason-ftp:1.0}
CONTAINER_NAME=${CONTAINER_NAME:-kason-ftp}
DEFAULT_BASE_DIR=${FTP_BASE_DIR:-/rangi_windows}
OS=$(uname -s || echo Unknown)
DOCKER_BIN=${DOCKER_BIN:-docker}

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "ERROR: Required command '$1' not found in PATH." >&2
    exit 1
  fi
}

prompt_nonempty() {
  var_name=$1
  prompt=$2
  initial=${3-}
  value=${initial-}
  while [ -z "${value}" ]; do
    printf "%s" "$prompt" >&2
    IFS= read -r value
  done
  eval "$var_name=\"$value\""
}

prompt_secret() {
  var_name=$1
  prompt=$2
  initial=${3-}
  value=${initial-}
  while [ -z "${value}" ]; do
    printf "%s" "$prompt" >&2
    stty -echo 2>/dev/null || true
    IFS= read -r value
    stty echo 2>/dev/null || true
    printf "\n" >&2
  done
  eval "$var_name=\"$value\""
}

prompt_with_default() {
  var_name=$1
  prompt=$2
  default_val=$3
  printf "%s" "$prompt" >&2
  IFS= read -r value || true
  if [ -z "${value}" ]; then
    value=$default_val
  fi
  eval "$var_name=\"$value\""
}

detect_macos_ip() {
  for ifx in en0 en1 en2; do
    ip=$(ipconfig getifaddr "$ifx" 2>/dev/null || true)
    if [ -n "$ip" ]; then
      echo "$ip"
      return 0
    fi
  done
  # Fallback: none
  return 1
}

# 0) Pre-flight
require_cmd "$DOCKER_BIN"

# 1) Collect inputs
prompt_nonempty FTP_USER "Enter FTP username: " "${FTP_USER-}"
prompt_secret  FTP_PASS "Enter FTP password: " "${FTP_PASS-}"
prompt_with_default FTP_BASE_DIR "Enter FTP base dir inside container [${DEFAULT_BASE_DIR}]: " "${DEFAULT_BASE_DIR}"

# Ensure FTP_BASE_DIR is absolute
case "$FTP_BASE_DIR" in
  /*) ;; # absolute OK
  *) FTP_BASE_DIR="$(pwd)/$FTP_BASE_DIR" ;;
esac

HOST_DIR="$FTP_BASE_DIR"

# Create base dir if missing (try without and with sudo)
if [ ! -d "$HOST_DIR" ]; then
  if ! mkdir -p "$HOST_DIR" 2>/dev/null; then
    echo "Creating $HOST_DIR with sudo..." >&2
    sudo mkdir -p "$HOST_DIR"
  fi
fi

# 2) Build image
echo "Building image $IMAGE_TAG from $SCRIPT_DIR ..."
"$DOCKER_BIN" build -t "$IMAGE_TAG" "$SCRIPT_DIR"

# 3) Remove existing container if present
if "$DOCKER_BIN" ps -a --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
  echo "Removing existing container: $CONTAINER_NAME"
  "$DOCKER_BIN" rm -f "$CONTAINER_NAME" >/dev/null 2>&1 || true
fi

# 4) Run container (macOS vs Linux)
RUN_ARGS_COMMON="-d --name $CONTAINER_NAME --restart unless-stopped -e FTP_USER=$FTP_USER -e FTP_PASS=$FTP_PASS -e FTP_BASE_DIR=$FTP_BASE_DIR -v $HOST_DIR:$FTP_BASE_DIR"

if [ "$OS" = "Darwin" ]; then
  echo "Detected macOS: exposing ports 21 and 21100-21110."
  DEFAULT_IP=""
  if ip_auto=$(detect_macos_ip 2>/dev/null); then
    DEFAULT_IP="$ip_auto"
  fi
  prompt_with_default PASV_ADDRESS "Enter PASV_ADDRESS (external IP/hostname) [${DEFAULT_IP:-skip}]: " "${PASV_ADDRESS:-$DEFAULT_IP}"
  if [ -n "${PASV_ADDRESS}" ]; then
    RUN_ARGS_COMMON="$RUN_ARGS_COMMON -e PASV_ADDRESS=$PASV_ADDRESS"
  fi
  echo "Running container $CONTAINER_NAME (macOS/NAT mode) ..."
  "$DOCKER_BIN" run $RUN_ARGS_COMMON -p 21:21 -p 21100-21110:21100-21110 "$IMAGE_TAG"
else
  echo "Detected $OS: using host network."
  echo "Running container $CONTAINER_NAME ..."
  "$DOCKER_BIN" run $RUN_ARGS_COMMON --network host "$IMAGE_TAG"
fi

# 5) Done
CID=$("$DOCKER_BIN" ps -aqf name="^${CONTAINER_NAME}$" | head -n1)
if [ -n "$CID" ]; then
  echo "Container started: $CONTAINER_NAME ($CID)"
else
  echo "Container start attempted; check '$DOCKER_BIN ps -a' for status." >&2
fi
