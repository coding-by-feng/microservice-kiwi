#!/bin/bash

# Kiwi IP Change Daemon
# - Monitors infrastructure/service IPs via netdiscover (by MAC)
# - If changed vs saved config, forces re-run of setup Step 24 (IP update)
# - Runs as a single-instance background daemon with simple PID lock

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SETUP_SCRIPT="$(cd "$SCRIPT_DIR/.." && pwd)/set_up.sh"
SETUP_BASE_DIR="/root/microservice-kiwi/kiwi-deploy"
CONFIG_FILE="$SETUP_BASE_DIR/.kiwi_setup_config"
PROGRESS_FILE="$SETUP_BASE_DIR/.kiwi_setup_progress"
LOG_FILE="$SETUP_BASE_DIR/ip_change_daemon.log"
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

set_cfg() {
  local key="$1" val="$2"
  mkdir -p "$(dirname "$CONFIG_FILE")" 2>/dev/null || true
  touch "$CONFIG_FILE" 2>/dev/null || true
  sed -i "/^${key}=.*/d" "$CONFIG_FILE" 2>/dev/null || true
  echo "${key}=${val}" >> "$CONFIG_FILE"
}

force_reinit_step24() {
  if [ -f "$PROGRESS_FILE" ]; then
    sed -i '/^ip_update$/d' "$PROGRESS_FILE" 2>/dev/null || true
  fi
}

run_step24() {
  log "Running Step 24 (IP update) via set_up.sh"
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
log "Daemon main loop started (interval=${INTERVAL}s)"

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
    # Update config pre-emptively so repeated scans don't re-trigger before Step 24 completes
    [ -n "$NEW_INFRA" ] && set_cfg INFRASTRUCTURE_IP "$NEW_INFRA" && set_cfg FASTDFS_NON_LOCAL_IP "$NEW_INFRA"
    [ -n "$NEW_SERVICE" ] && set_cfg SERVICE_IP "$NEW_SERVICE"
    force_reinit_step24
    run_step24
    log "Change handled; sleeping extra to debounce."
    sleep $(( INTERVAL * 2 ))
  else
    # No change
    sleep "$INTERVAL"
  fi

done

