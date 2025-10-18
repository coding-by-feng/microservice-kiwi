#!/bin/bash

# Kiwi IP Change Daemon
# - Monitors infrastructure/service IPs via netdiscover (by MAC)
# - If changed vs saved config, forces re-run of setup Step 24 (IP update)
# - Runs as a single-instance background daemon with simple PID lock

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
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

# MAC addresses (must match set_up.sh detect_network_ips)
INFRA_MAC="2c:cf:67:53:f1:c4"
SERVICE_MAC="14:d4:24:7f:12:27"

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

install_netdiscover_if_needed() {
  if ! command -v netdiscover >/dev/null 2>&1; then
    log "netdiscover not found; installing..."
    apt-get update -y >>"$LOG_FILE" 2>&1 || true
    apt-get install -y netdiscover >>"$LOG_FILE" 2>&1 || true
  fi
}

quick_scan() {
  local range="192.168.1.0/16"
  local timeout_secs=20
  local iface=""
  if command -v ip >/dev/null 2>&1; then
    iface=$(ip -o -4 route show to default 2>/dev/null | awk '{for(i=1;i<=NF;i++) if($i=="dev"){print $(i+1); exit}}')
  fi
  local out=""
  if command -v timeout >/dev/null 2>&1; then
    out=$(timeout ${timeout_secs}s netdiscover -r "$range" ${iface:+-i "$iface"} -P -N 2>/dev/null || true)
    [ -z "$out" ] && out=$(timeout ${timeout_secs}s netdiscover -r "$range" ${iface:+-i "$iface"} -P 2>/dev/null || true)
    [ -z "$out" ] && out=$(timeout ${timeout_secs}s netdiscover -r "$range" ${iface:+-i "$iface"} 2>/dev/null || true)
  else
    out=$(netdiscover -r "$range" ${iface:+-i "$iface"} -P -N 2>/dev/null || true)
    [ -z "$out" ] && out=$(netdiscover -r "$range" ${iface:+-i "$iface"} 2>/dev/null || true)
  fi
  if [ -z "$out" ]; then
    # Fallback to arp cache
    if command -v ip >/dev/null 2>&1; then
      out=$(ip neigh show 2>/dev/null | awk '{if($1~/^[0-9]/ && $5~/([0-9a-f]{2}:){5}[0-9a-f]{2}/) printf("%s    %s\n", $1, $5)}')
    fi
  fi
  echo "$out" | tr '[:upper:]' '[:lower:]'
}

parse_ip_by_mac() {
  local scan="$1" mac="$2"
  echo "$scan" | awk -v mac="$mac" 'index($0, mac){print $1; exit}'
}

get_cfg() {
  local key="$1"
  [ -r "$CONFIG_FILE" ] || { echo ""; return 0; }
  grep -E "^${key}=" "$CONFIG_FILE" 2>/dev/null | sed -E "s/^${key}=//" | tail -n1
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
install_netdiscover_if_needed

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

  SCAN=$(quick_scan)
  if [ -z "$SCAN" ]; then
    log "Scan returned no data; will retry."
    sleep "$INTERVAL"
    continue
  fi

  NEW_INFRA=$(parse_ip_by_mac "$SCAN" "$INFRA_MAC")
  NEW_SERVICE=$(parse_ip_by_mac "$SCAN" "$SERVICE_MAC")

  if [ -z "$NEW_INFRA" ] && [ -z "$NEW_SERVICE" ]; then
    log "No target devices detected in scan; sleeping."
    sleep "$INTERVAL"
    continue
  fi

  CHANGED=0
  if [ -n "$NEW_INFRA" ] && [ "$NEW_INFRA" != "${OLD_INFRA:-}" ]; then
    log "INFRASTRUCTURE_IP changed: ${OLD_INFRA:-<unset>} -> $NEW_INFRA"
    CHANGED=1
  fi
  if [ -n "$NEW_SERVICE" ] && [ "$NEW_SERVICE" != "${OLD_SERVICE:-}" ]; then
    log "SERVICE_IP changed: ${OLD_SERVICE:-<unset>} -> $NEW_SERVICE"
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
    log "Reminder for $SCRIPT_USER: after IP change and Step 24, run 'source ~/.bashrc' (or 'source ~/.zshrc' if using zsh) in your shell to apply env changes."
    log "Change handled; sleeping extra to debounce."
    sleep $(( INTERVAL * 2 ))
  else
    # No change
    sleep "$INTERVAL"
  fi

done

