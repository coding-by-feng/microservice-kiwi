#!/usr/bin/env bash
# Auto-initialize ENV on Raspberry Pi reboot
# - Starts a tmux session and launches ngrok
# - Runs easy-check with automated inputs: 23 (infra), y, Enter, 0 (exit)
# - Runs easy-deploy with -mode=ouej
#
# Environment overrides:
#   SESSION_NAME   (default: sweepo)
#   NGROK_DOMAIN   (default: www.sweepo.co.nz)
#   NGROK_PORT     (default: 1968)
#   LOG_FILE       (default: $HOME/sweepo-init.log)
#   ATTACH_TMUX    (default: 0) If set to 1, do the following interactive flow:
#                   - tmux new-session -d -s $SESSION_NAME (if not exists)
#                   - pre-type: "ngrok http --domain=$NGROK_DOMAIN $NGROK_PORT" in window 'ngrok' (no Enter)
#                   - attach to the session so user can review and press Enter

set -euo pipefail

SESSION_NAME=${SESSION_NAME:-sweepo}
NGROK_DOMAIN=${NGROK_DOMAIN:-www.sweepo.co.nz}
NGROK_PORT=${NGROK_PORT:-1968}
LOG_FILE=${LOG_FILE:-"$HOME/sweepo-init.log"}
ATTACH_TMUX=${ATTACH_TMUX:-0}

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

timestamp() { date '+%Y-%m-%d %H:%M:%S'; }
log() { echo "[$(timestamp)] $*" | tee -a "$LOG_FILE"; }
cmd_exists() { command -v "$1" >/dev/null 2>&1; }

sudo_nopass_ready() {
  if cmd_exists sudo; then
    sudo -n true >/dev/null 2>&1 || return 1
    return 0
  fi
  return 1
}

ensure_tmux_session() {
  if ! cmd_exists tmux; then
    log "ERROR: tmux is not installed. Install with: sudo apt-get install -y tmux"
    return 1
  fi
  if tmux has-session -t "$SESSION_NAME" 2>/dev/null; then
    log "tmux session '$SESSION_NAME' already exists"
  else
    tmux new-session -d -s "$SESSION_NAME"
    log "Created tmux session '$SESSION_NAME'"
  fi
}

ensure_tmux_window() {
  local win="$1"
  ensure_tmux_session || return 1
  if tmux list-windows -t "$SESSION_NAME" -F '#{window_name}' | grep -qx "$win"; then
    return 0
  fi
  tmux new-window -t "$SESSION_NAME" -n "$win"
}

# When ATTACH_TMUX=1, prepare the session, pre-type the ngrok command, and attach for the user
attach_and_prime_ngrok() {
  ensure_tmux_session || return 1
  # Use/ensure a dedicated 'ngrok' window to keep things tidy
  ensure_tmux_window ngrok || return 1
  tmux select-window -t "${SESSION_NAME}:ngrok"
  # Pre-type the command without pressing Enter, even if ngrok isn't installed yet
  tmux send-keys -t "${SESSION_NAME}:ngrok" "ngrok http --domain=${NGROK_DOMAIN} ${NGROK_PORT}"
  log "Attaching to tmux session '${SESSION_NAME}'. The command has been typed; press Enter to run it:"
  log "  ngrok http --domain=${NGROK_DOMAIN} ${NGROK_PORT}"
  exec tmux attach-session -t "$SESSION_NAME"
}

start_ngrok_in_tmux() {
  if ! cmd_exists ngrok; then
    log "WARN: ngrok not found in PATH; skipping ngrok startup. Install from https://ngrok.com/download"
    return 0
  fi
  ensure_tmux_window ngrok || return 1
  if pgrep -f "ngrok .*http .*--domain=${NGROK_DOMAIN}.* ${NGROK_PORT}" >/dev/null 2>&1; then
    log "ngrok already running for ${NGROK_DOMAIN}:${NGROK_PORT}, skipping"
    return 0
  fi
  # Run ngrok in tmux and append all output to the log file
  tmux send-keys -t "${SESSION_NAME}:ngrok" "ngrok http --domain=${NGROK_DOMAIN} ${NGROK_PORT} 2>&1 | tee -a '$LOG_FILE'" C-m
  log "Started ngrok in tmux [${SESSION_NAME}:ngrok] => http(s)://${NGROK_DOMAIN} -> localhost:${NGROK_PORT}"
}

resolve_path() {
  # $1 is a friendly name; $2 is primary candidate; $3 is fallback
  local name="$1"; shift
  local candidates=("$@")
  for c in "${candidates[@]}"; do
    if [ -x "$c" ]; then
      echo "$c"
      return 0
    fi
  done
  return 1
}

run_easy_check() {
  local easy_check
  if ! easy_check=$(resolve_path "easy-check" "$HOME/easy-check" "$SCRIPT_DIR/docker/checkContainers.sh"); then
    log "ERROR: easy-check not found. Expected at: $HOME/easy-check or $SCRIPT_DIR/docker/checkContainers.sh"
    return 1
  fi

  log "Running easy-check to start infrastructure containers (option 23)..."

  if sudo_nopass_ready; then
    # Automated, non-interactive run
    {
      printf "23\ny\n\n0\n" | sudo -n "$easy_check"
    } 2>&1 | tee -a "$LOG_FILE"
    log "easy-check automation complete"
  else
    # Fallback: open in tmux and instruct manual password entry, then auto-send steps
    log "WARN: sudo without password is not configured. Starting easy-check in tmux window 'kason'."
    ensure_tmux_window kason || return 1
    tmux send-keys -t "${SESSION_NAME}:kason" "sudo '$easy_check'" C-m
    # Give sudo a moment for potential password prompt
    sleep 2
    # Try to send the sequence anyway; if a password is required, keys may not match prompts.
    tmux send-keys -t "${SESSION_NAME}:kason" "23" C-m "y" C-m C-m "0" C-m
    log "If password was required by sudo, attach to tmux and complete manually: tmux attach -t ${SESSION_NAME} (window: kason)"
  fi
}

run_easy_deploy() {
  local easy_deploy
  if ! easy_deploy=$(resolve_path "easy-deploy" "$HOME/easy-deploy" "$SCRIPT_DIR/docker/deployKason.sh"); then
    log "ERROR: easy-deploy not found. Expected at: $HOME/easy-deploy or $SCRIPT_DIR/docker/deployKason.sh"
    return 1
  fi

  log "Running easy-deploy with -mode=ouej (use existing jars)..."
  if sudo_nopass_ready; then
    {
      sudo -n -E "$easy_deploy" -mode=ouej
    } 2>&1 | tee -a "$LOG_FILE"
    log "easy-deploy completed"
  else
    ensure_tmux_window kason || return 1
    tmux send-keys -t "${SESSION_NAME}:kason" "sudo -E '$easy_deploy' -mode=ouej 2>&1 | tee -a '$LOG_FILE'" C-m
    log "Started easy-deploy in tmux window 'kason'. If prompted for password, attach: tmux attach -t ${SESSION_NAME}"
  fi
}

main() {
  mkdir -p "$(dirname "$LOG_FILE")"
  touch "$LOG_FILE"
  log "==== SweePo Pi auto init starting ===="

  ensure_tmux_session || true

  # Interactive attach mode: create/ensure session, pre-type ngrok command, then attach.
  if [ "$ATTACH_TMUX" = "1" ]; then
    attach_and_prime_ngrok
    # exec above should replace the process; if it returns, stop further automation.
    log "Attach mode finished/returned unexpectedly; skipping further steps."
    return 0
  fi

  start_ngrok_in_tmux || true

  run_easy_check || log "easy-check step encountered a problem (see log)."
  run_easy_deploy || log "easy-deploy step encountered a problem (see log)."

  log "==== SweePo Pi auto init finished ===="
  log "Tip: Attach to tmux: tmux attach -t ${SESSION_NAME}; detach with Ctrl+b then d"
}

main "$@"
