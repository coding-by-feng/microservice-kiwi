#!/bin/bash
set -euo pipefail

# Auto-update script for yt-dlp.
# 1. Verifies presence of binary
# 2. Attempts in-place update for standalone binary (yt-dlp -U)
# 3. Falls back to pip upgrade if standalone update not supported
# 4. If binary missing, downloads latest standalone build
# Logs are appended by cron to /var/log/yt-dlp-update.log

BIN="${YTDLP_BIN:-/usr/local/bin/yt-dlp}"
LATEST_URL="https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp_linux"

log() { echo "[$(date +'%Y-%m-%d %H:%M:%S')] $*"; }

update_standalone() {
  local output
  if ! command -v yt-dlp >/dev/null 2>&1; then
    log "yt-dlp not found, downloading standalone binary..."
    curl -L --fail -o "$BIN" "$LATEST_URL"
    chmod +x "$BIN"
    return 0
  fi

  output=$(yt-dlp -U 2>&1 || true)
  log "$output"
  if echo "$output" | grep -qi "ERROR:"; then
    log "Standalone update reported an error. Attempting direct download..."
    curl -L --fail -o "$BIN" "$LATEST_URL"
    chmod +x "$BIN"
    return 0
  fi

  if echo "$output" | grep -qi "Not a standalone executable"; then
    if command -v pip3 >/dev/null 2>&1; then
      log "Detected non-standalone install; using pip to upgrade yt-dlp..."
      pip3 install -U yt-dlp || log "pip upgrade failed"
    else
      log "pip3 unavailable; downloading standalone binary instead..."
      curl -L --fail -o "$BIN" "$LATEST_URL"
      chmod +x "$BIN"
    fi
  fi
}

verify_binary() {
  if [ -f "$BIN" ]; then
    if [ ! -x "$BIN" ]; then
      log "Binary exists but is not executable. Fixing permissions."
      chmod +x "$BIN" || true
    fi
    local size
    size=$(stat -c %s "$BIN" 2>/dev/null || stat -f %z "$BIN" 2>/dev/null || echo 0)
    if [ "$size" -lt 400000 ]; then
      log "Warning: yt-dlp binary size ($size bytes) seems unusually small. Will redownload."
      curl -L --fail -o "$BIN" "$LATEST_URL"
      chmod +x "$BIN"
    fi
  else
    log "yt-dlp binary missing; performing fresh download."
    curl -L --fail -o "$BIN" "$LATEST_URL"
    chmod +x "$BIN"
  fi
}

main() {
  log "Starting yt-dlp daily update check..."
  verify_binary
  update_standalone
  # Record version after update
  if command -v yt-dlp >/dev/null 2>&1; then
    local ver
    ver=$(yt-dlp --version 2>/dev/null || echo "unknown")
    log "Current yt-dlp version: $ver"
  fi
  log "Update cycle complete."
}

main "$@"

