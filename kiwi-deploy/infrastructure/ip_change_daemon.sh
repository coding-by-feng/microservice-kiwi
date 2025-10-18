#!/bin/bash

# Kiwi IP Change Daemon (Single-Pi)
# - Monitors infrastructure IP via hostname -I (prefers 192.168/10/172 ranges)
# - Service IP is assumed to be 127.0.0.1
# - If changed vs saved config, forces re-run of setup Step 24 (IP update)
# - Runs as a single-instance background daemon with simple PID lock

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}" )" && pwd)"
SETUP_SCRIPT="$(cd "$SCRIPT_DIR/.." && pwd)/set_up.sh"

# Resolve target user/home and setup base dir (prefer env passed by caller)
SCRIPT_USER="${SCRIPT_USER:-${SUDO_USER:-$(logname 2>/dev/null || echo ${USER:-root})}}"
SCRIPT_HOME="${SCRIPT_HOME:-$(eval echo ~$SCRIPT_USER)}"
# Store config/progress/log directly under the user's home directory
SETUP_BASE_DIR="${KIWI_SETUP_BASE_DIR:-"$SCRIPT_HOME"}"
CONFIG_FILE="$SETUP_BASE_DIR/.kiwi_setup_config"
PROGRESS_FILE="$SETUP_BASE_DIR/.kiwi_setup_progress"
LOG_FILE="$SCRIPT_HOME/ip_change_daemon.log"
PID_FILE="/var/run/kiwi-ip-change-daemon.pid"
LOCK_FILE="/var/run/kiwi-ip-change-daemon.lock"

# Interval (seconds) between checks; can override via env IP_CHECK_INTERVAL
INTERVAL="${IP_CHECK_INTERVAL:-60}"

log() {
  mkdir -p "$(dirname "$LOG_FILE")" 2>/dev/null || true
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*" | tee -a "$LOG_FILE"
}

need_root() {
  if [ "${EUID:-$(id -u)}" -ne 0 ]; then
    echo "This daemon must run as root (sudo)." >&2
    exit 1
  fi
}

get_cfg() {
  local key="$1"
  [ -r "$CONFIG_FILE" ] || { echo ""; return 0; }
  grep -E "^${key}=" "$CONFIG_FILE" 2>/dev/null | sed -E "s/^${key}=//" | tail -n1
}

# Determine infra IP via hostname -I
get_infra_ip_hostname() {
  local raw selected
  raw=$(hostname -I 2>/dev/null | tr -s ' ' ' ' || true)
  if [ -z "$raw" ]; then
    echo "127.0.0.1"; return 0
  fi
  selected=$(echo "$raw" | tr ' ' '\n' | awk 'match($0,/^192\.168\.[0-9]+\.[0-9]+$/){print; exit}')
  if [ -z "$selected" ]; then
    selected=$(echo "$raw" | tr ' ' '\n' | awk 'match($0,/^10\.[0-9]+\.[0-9]+\.[0-9]+$/){print; exit}')
  fi
  if [ -z "$selected" ]; then
    selected=$(echo "$raw" | tr ' ' '\n' | awk 'match($0,/^172\.(1[6-9]|2[0-9]|3[0-1])\.[0-9]+\.[0-9]+$/){print; exit}')
  fi
  if [ -z "$selected" ]; then
    selected=$(echo "$raw" | tr ' ' '\n' | awk 'match($0,/^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$/){print; exit}')
  fi
  [ -z "$selected" ] && selected="127.0.0.1"
  echo "$selected"
}

# Run Step 24 with optional override IPs (positional args)
run_step24() {
  local ov_infra="${1:-}"
  local ov_service="${2:-}"
  log "Running Step 24 (IP update) via set_up.sh with overrides: infra='${ov_infra:-<none>}', service='${ov_service:-<none>}'"
  INFRASTRUCTURE_IP_OVERRIDE="$ov_infra" \
  SERVICE_IP_OVERRIDE="$ov_service" \
  bash "$SETUP_SCRIPT" --run-step=24 >>"$LOG_FILE" 2>&1 || log "Step 24 execution returned non-zero (see log)"
}

is_running() {
  if [ -f "$PID_FILE" ]; then
    local pid
    pid=$(cat "$PID_FILE" 2>/dev/null || echo "")
    if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
      return 0
    fi
  fi
  return 1
}

start_nohup() {
  need_root
  if is_running; then
    log "Daemon already running (PID $(cat "$PID_FILE"))"
    exit 0
  fi
  log "Starting daemon in background..."
  nohup "$0" nohup-child >>"$LOG_FILE" 2>&1 &
  echo $! > "$PID_FILE"
  log "Daemon started (PID $!)"
  exit 0
}

stop_daemon() {
  if is_running; then
    local pid
    pid=$(cat "$PID_FILE")
    log "Stopping daemon (PID $pid)"
    kill "$pid" 2>/dev/null || true
    sleep 1
    if kill -0 "$pid" 2>/dev/null; then
      log "Force killing daemon (PID $pid)"
      kill -9 "$pid" 2>/dev/null || true
    fi
  else
    log "Daemon is not running"
  fi
  rm -f "$PID_FILE" "$LOCK_FILE" 2>/dev/null || true
  exit 0
}

status_daemon() {
  if is_running; then
    echo "RUNNING (PID $(cat "$PID_FILE"))"
  else
    echo "STOPPED"
  fi
  exit 0
}

# Command handling
case "${1:-}" in
  start) start_nohup ;;
  stop)  stop_daemon ;;
  status) status_daemon ;;
  nohup-child) ;; # continue below
  *) start_nohup ;;
esac

need_root

# Ensure single instance via lock
exec 9>"$LOCK_FILE" || true
if ! flock -n 9; then
  log "Another instance is already running; exiting."
  exit 0
fi

echo $$ > "$PID_FILE"
log "Daemon main loop started (interval=${INTERVAL}s, user=$SCRIPT_USER, home=$SCRIPT_HOME)"

while true; do
  # Read last known IPs from config
  OLD_INFRA=$(get_cfg INFRASTRUCTURE_IP)
  OLD_SERVICE=$(get_cfg SERVICE_IP)

  NEW_INFRA=$(get_infra_ip_hostname)
  NEW_SERVICE="127.0.0.1"

  CHANGED=0
  if [ -n "$NEW_INFRA" ] && [ "$NEW_INFRA" != "${OLD_INFRA:-}" ]; then
    log "INFRASTRUCTURE_IP change detected: ${OLD_INFRA:-<unset>} -> $NEW_INFRA"
    CHANGED=1
  fi
  # Service is fixed to localhost in single-Pi; still check for drift
  if [ -n "$NEW_SERVICE" ] && [ "$NEW_SERVICE" != "${OLD_SERVICE:-}" ]; then
    log "SERVICE_IP change detected: ${OLD_SERVICE:-<unset>} -> $NEW_SERVICE"
    CHANGED=1
  fi

  if [ "$CHANGED" -eq 1 ]; then
    # Clear step 24 completion so it runs fresh and updates config itself
    if [ -f "$PROGRESS_FILE" ]; then
      sed -i '/^ip_update$/d' "$PROGRESS_FILE" 2>/dev/null || true
    fi
    # Run step 24 with explicit override values we just discovered
    run_step24 "$NEW_INFRA" "$NEW_SERVICE"
    # Add a reminder for interactive shells to reload environment
    log "Reminder for $SCRIPT_USER: after IP change and Step 24, run 'source ~/.bashrc' (or 'source ~/.zshrc' if using zsh)."
    log "Change handled; sleeping extra to debounce."
    sleep $(( INTERVAL * 2 ))
  else
    sleep "$INTERVAL"
  fi

done

