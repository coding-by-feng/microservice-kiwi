#!/bin/bash

# --- Permission bootstrap (self-healing) ---
# Ensure all kiwi-deploy scripts are executable and common helper binaries are +x
{
  SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
  DEPLOY_ROOT="$(cd "$SCRIPT_DIR/.." && pwd 2>/dev/null || echo "$SCRIPT_DIR")"
  # Make every .sh under kiwi-deploy executable
  if [ -d "$DEPLOY_ROOT" ]; then
    find "$DEPLOY_ROOT" -type f -name "*.sh" -exec chmod 777 {} \; 2>/dev/null || true
  fi
  # Make easy-* symlinks/scripts under the original user's home executable
  ORIG_USER="${SUDO_USER:-$USER}"
  ORIG_HOME=$(eval echo "~$ORIG_USER")
  for f in "$ORIG_HOME"/easy-*; do
    [ -e "$f" ] && chmod +x "$f" || true
  done
  # Ensure yt-dlp_linux binaries are executable if present
  for f in "$ORIG_HOME"/docker/kiwi/ai/*/yt-dlp_linux "$ORIG_HOME"/docker/kiwi/ai/yt-dlp_linux; do
    [ -e "$f" ] && chmod +x "$f" || true
  done
} >/dev/null 2>&1 || true

# Helper: download latest yt-dlp into AI docker contexts (biz, batch)
download_latest_ytdlp() {
  echo "⬇️  Downloading latest yt-dlp (linux) for AI docker contexts..."
  local url="https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp_linux"
  local tmp_file="/tmp/yt-dlp_linux.$$"
  local dest_biz="$CURRENT_DIR/docker/kiwi/ai/biz"
  local dest_batch="$CURRENT_DIR/docker/kiwi/ai/batch"

  mkdir -p "$dest_biz" "$dest_batch"

  # Prefer curl; fallback to wget
  if command -v curl >/dev/null 2>&1; then
    if ! curl -L --fail -o "$tmp_file" "$url"; then
      echo "⚠️  curl failed to download yt-dlp; trying wget..."
      if ! command -v wget >/dev/null 2>&1 || ! wget -O "$tmp_file" "$url"; then
        echo "❌ Unable to download yt-dlp (curl/wget both failed)."
        return 1
      fi
    fi
  else
    if ! command -v wget >/dev/null 2>&1 || ! wget -O "$tmp_file" "$url"; then
      echo "❌ Neither curl nor wget available to download yt-dlp."
      return 1
    fi
  fi

  # Basic validation and install
  if [ ! -s "$tmp_file" ]; then
    echo "❌ Downloaded yt-dlp file is empty. Aborting copy."
    rm -f "$tmp_file"
    return 1
  fi

  chmod +x "$tmp_file"
  cp -f "$tmp_file" "$dest_biz/yt-dlp_linux"
  cp -f "$tmp_file" "$dest_batch/yt-dlp_linux"
  rm -f "$tmp_file"
  echo "✅ yt-dlp placed at:"
  echo "   - $dest_biz/yt-dlp_linux"
  echo "   - $dest_batch/yt-dlp_linux"
}

# Method 1: Check if running with sudo -E by examining environment variables
check_sudo_e() {
    echo "=============================================="
    echo "CHECKING SUDO -E STATUS:"
    echo "=============================================="

    # Check if running as root
    if [ "$EUID" -ne 0 ]; then
        echo "❌ ERROR: Not running as root/sudo"
        return 1
    fi

    # Check if SUDO_USER is set (indicates sudo was used)
    if [ -z "$SUDO_USER" ]; then
        echo "❌ WARNING: SUDO_USER not set - may not be running via sudo"
        return 1
    fi

    # Check if user environment variables are preserved
    # These are typically set in user's .bashrc and should be preserved with -E
    local env_preserved=true
    local missing_vars=()

    # Check for your specific environment variables
    if [ -z "$KIWI_ENC_PASSWORD" ]; then
        missing_vars+=("KIWI_ENC_PASSWORD")
        env_preserved=false
    fi

    if [ -z "$DB_IP" ]; then
        missing_vars+=("DB_IP")
        env_preserved=false
    fi

    if [ -z "$GROK_API_KEY" ]; then
        missing_vars+=("GROK_API_KEY")
        env_preserved=false
    fi

    # Display results
    echo "Current user: $(whoami)"
    echo "Original user: $SUDO_USER"
    echo "EUID: $EUID"
    echo "UID: $UID"

    if [ "$env_preserved" = true ]; then
        echo "✅ Environment variables preserved - likely running with sudo -E"
        echo "✅ All required environment variables are set"
    else
        echo "❌ WARNING: Some environment variables missing"
        echo "Missing variables: ${missing_vars[*]}"
        echo "💡 Recommendation: Run with 'sudo -E' to preserve environment variables"
        return 1
    fi

    echo "=============================================="
    return 0
}

# Method 2: Alternative check using HOME variable
check_sudo_e_alternative() {
    echo "ALTERNATIVE SUDO -E CHECK:"
    echo "=============================================="

    if [ "$EUID" -eq 0 ] && [ -n "$SUDO_USER" ]; then
        # Check if HOME points to original user's home
        expected_home="/home/$SUDO_USER"
        if [ "$HOME" = "$expected_home" ] || [ "$HOME" = "/root" ]; then
            echo "✅ Running with sudo"
            # Check if user's environment is preserved
            if [ -n "$KIWI_ENC_PASSWORD" ] || [ -n "$DB_IP" ]; then
                echo "✅ User environment preserved - likely sudo -E"
            else
                echo "❌ User environment not preserved - use sudo -E"
            fi
        fi
    fi
    echo "=============================================="
}

# Method 3: Enhanced error message for your existing check
enhanced_sudo_check() {
    if [ "$EUID" -ne 0 ]; then
        echo "=============================================="
        echo "❌ ERROR: This script must be run with sudo privileges"
        echo ""
        echo "To preserve your environment variables, please run:"
        echo "   sudo -E $0 $*"
        echo ""
        echo "The -E flag preserves your user environment variables"
        echo "which are required for this script to function properly."
        echo "=============================================="
        exit 1
    fi
}

# Method 4: Comprehensive check with recommendations
comprehensive_sudo_check() {
    echo "=============================================="
    echo "COMPREHENSIVE SUDO CHECK:"
    echo "=============================================="

    # Basic sudo check
    if [ "$EUID" -ne 0 ]; then
        echo "❌ Not running as root"
        echo "Please run: sudo -E $0 $*"
        exit 1
    fi

    # Check if it's actually sudo (not just root login)
    if [ -z "$SUDO_USER" ]; then
        echo "⚠️  WARNING: Running as root but SUDO_USER not set"
        echo "This might be a direct root login rather than sudo"
    else
        echo "✅ Running via sudo (original user: $SUDO_USER)"
    fi

    # Check environment preservation
    local critical_vars=("KIWI_ENC_PASSWORD" "DB_IP" "GROK_API_KEY" "MYSQL_ROOT_PASSWORD")
    local missing_count=0

    echo ""
    echo "Checking critical environment variables:"
    for var in "${critical_vars[@]}"; do
        if [ -z "${!var}" ]; then
            echo "❌ $var: NOT SET"
            ((missing_count++))
        else
            echo "✅ $var: SET"
        fi
    done

    if [ $missing_count -gt 0 ]; then
        echo ""
        echo "❌ $missing_count critical environment variables are missing"
        echo "💡 This suggests the script was not run with 'sudo -E'"
        echo ""
        echo "To fix this, run:"
        echo "   sudo -E $0 $*"
        echo ""
        echo "The -E flag tells sudo to preserve your environment variables."
        exit 1
    else
        echo ""
        echo "✅ All critical environment variables are present"
        echo "✅ Script appears to be running with proper sudo -E"
    fi

    echo "=============================================="
}

# Get current working directory
CURRENT_DIR="$(pwd)"

# Helper: set execute permissions for key scripts (extracted method)
ensure_execute_permissions() {
  echo "🔐 Setting execute permissions for scripts..."
  chmod 777 "$CURRENT_DIR/microservice-kiwi/kiwi-deploy/docker/"*.sh 2>/dev/null || true
  chmod 777 "$CURRENT_DIR/microservice-kiwi/kiwi-deploy/kiwi-ui/"*.sh 2>/dev/null || true
}

# NEW: Recursively ensure all kiwi-deploy shell scripts are executable (runs at EXIT)
ensure_deploy_permissions_recursive() {
  local script_dir
  local deploy_root
  script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
  deploy_root="$(cd "$script_dir/.." && pwd 2>/dev/null || echo "$script_dir")"
  if [ -d "$deploy_root" ]; then
    find "$deploy_root" -type f -name "*.sh" -exec chmod 777 {} \; 2>/dev/null || true
  fi
}

# Exit immediately if a command exits with a non-zero status
set -e
set -o pipefail
set -E  # inherit ERR in functions/subshells

# Echo all environment variables from .bashrc
echo "=============================================="
echo "ENVIRONMENT VARIABLES:"
echo "=============================================="
echo "KIWI_ENC_PASSWORD: $KIWI_ENC_PASSWORD"
echo "DB_IP: $DB_IP"
echo "GROK_API_KEY: $GROK_API_KEY"
echo "MYSQL_ROOT_PASSWORD: $MYSQL_ROOT_PASSWORD"
echo "REDIS_PASSWORD: $REDIS_PASSWORD"
echo "ES_ROOT_PASSWORD: $ES_ROOT_PASSWORD"
echo "ES_USER_NAME: $ES_USER_NAME"
echo "ES_USER_PASSWORD: $ES_USER_PASSWORD"
echo "=============================================="
echo ""

# Function to handle errors
error_handler() {
  local line_number=$1
  local command="$2"
  local exit_code=${3:-1}   # use value captured by trap
  echo "=============================================="
  echo "ERROR: Script failed at line $line_number"
  echo "Failed command: $command"
  echo "Exit code: $exit_code"
  # In build-only modes, don't show autoDeploy log to avoid confusion
  if [ "${ONLY_BUILD_MAVEN}" = true ]; then
    echo "Note: ONLY_BUILD_MAVEN=true (OBM/OBMAS). Docker steps are disabled in this mode."
  else
    if [ -f "$CURRENT_DIR/autoDeploy.log" ]; then
       echo "--- Last 40 lines of autoDeploy.log ---"
       tail -n 40 "$CURRENT_DIR/autoDeploy.log" || true
       echo "--------------------------------------"
    fi
  fi
  echo "TIP: Re-run with: bash -x $0 (plus your params) for more tracing."
  echo "=============================================="
  exit "$exit_code"
}

# Set error trap
trap 'rc=$?; error_handler ${LINENO} "$BASH_COMMAND" "$rc"' ERR
# Set exit trap to always restore script permissions (addresses git pull resetting +x)
trap 'ensure_deploy_permissions_recursive' EXIT

# Check if script is run with sudo privileges
if [ "$EUID" -ne 0 ]; then
  echo "Error: This script must be run with sudo privileges"
  echo "Please run: sudo -E $0 $*"
  exit 1
fi

cd "$CURRENT_DIR/microservice-kiwi/" || { echo "CRITICAL ERROR: Failed to change directory to $CURRENT_DIR/microservice-kiwi/"; exit 1; }

# Set execute permissions (run in every mode)
ensure_execute_permissions

# Define available microservices
declare -A MICROSERVICES=(
    ["eureka"]="kiwi-eureka"
    ["config"]="kiwi-config"
    ["upms"]="kiwi-upms-biz"
    ["auth"]="kiwi-auth"
    ["gate"]="kiwi-gateway"
    ["word"]="kiwi-word-biz"
    ["crawler"]="kiwi-word-crawler"
    ["ai"]="kiwi-ai-biz"
    ["tools"]="kiwi-tools-biz"   # NEW: tools
)

# Function to show help
show_help() {
  echo "Usage: $0 [MODE] [OPTIONS]"
  echo ""
  echo "Available modes:"
  echo "  -mode=sg      Skip git operations (stash and pull)"
  echo "  -mode=sm      Skip maven build operation"
  echo "  -mode=sbd     Skip Dockerfile building operation (copying Dockerfiles and JARs)"
  echo "  -mode=obm     Only build with Maven, then copy built JARs to ~/built_jar"
  echo "                Use with -s=svc1,svc2 to build only those services (e.g. -mode=obm -s=auth,gate)"
  echo "  -mode=ouej    Only use existing jars from ~/built_jar (skip git and maven)"
  echo "  -mode=osj     Only send existing jars to remote (skip git, maven, docker)"
  echo "  -mode=og      Only perform git stash+pull (no build/deploy)   # UPDATED"
  echo ""
  echo "Available options:"
  echo "  -c            Enable autoCheckService after deployment"
  echo "  -s=SERVICE    Build/deploy specific service(s) only"
  echo "                Available services: eureka, config, upms, auth, gate, word, crawler, ai, tools"
  echo "                Multiple services: -s=eureka,config,upms"
  echo "                All services: -s=all (default)"
  echo "  -help         Show this help message"
  echo ""
  echo "If no mode is specified, all operations will be executed."
  echo ""
  echo "Examples:"
  echo "  sudo -E $0                        # Full deployment (all services)"
  echo "  sudo -E $0 -mode=sg               # Skip only git operations"
  echo "  sudo -E $0 -mode=sm               # Skip only maven build"
  echo "  sudo -E $0 -mode=sbd              # Skip only Dockerfile building"
  echo "  sudo -E $0 -mode=obm              # Only Maven build and copy jars to ~/built_jar"
  echo "  sudo -E $0 -mode=osj              # Send jars from ~/built_jar to remote (no build)"
  echo "  sudo -E $0 -mode=ouej             # Use jars from ~/built_jar and deploy"
  echo "  sudo -E $0 -s=eureka,config       # Build only eureka and config services"
}

# Initialize variables
MODE=""
ENABLE_AUTO_CHECK=false
SKIP_GIT=false
SKIP_MAVEN=false
SKIP_DOCKER_BUILD=false
FAST_DEPLOY_MODE=false
SELECTED_SERVICES=""
BUILD_ALL_SERVICES=true
ONLY_BUILD_MAVEN=false
USE_EXISTING_JARS_ONLY=false
ONLY_SEND_JARS=false
ONLY_GIT_MODE=false

# Process all arguments
for arg in "$@"; do
  case "$arg" in
    -mode=sg)
      MODE="$arg"
      SKIP_GIT=true
      echo "⏭️  Skipping git stash and pull operations"
      ;;
    -mode=sm)
      MODE="$arg"
      SKIP_MAVEN=true
      echo "⏭️  Skipping maven build operation"
      ;;
    -mode=sbd)
      MODE="$arg"
      SKIP_DOCKER_BUILD=true
      echo "⏭️  Skipping Dockerfile building operation"
      ;;
    -mode=obm)
      MODE="$arg"
      ONLY_BUILD_MAVEN=true
      echo "🧱 OBM MODE: Only Maven build and copy jars to ~/built_jar (will stop all kiwi containers first)"
      ;;
    -mode=ouej)
      MODE="$arg"
      SKIP_GIT=true
      SKIP_MAVEN=true
      USE_EXISTING_JARS_ONLY=true
      echo "📦 OUEJ MODE: Using jars from ~/built_jar (skip git and maven)"
      ;;
    -mode=osj)
      MODE="$arg"
      SKIP_GIT=true
      SKIP_MAVEN=true
      SKIP_DOCKER_BUILD=true
      ONLY_SEND_JARS=true
      echo "🚚 OSJ MODE: Only send existing jars from ~/built_jar to remote"
      ;;
    -mode=og)
      MODE="$arg"
      ONLY_GIT_MODE=true
      SKIP_GIT=false
      SKIP_MAVEN=true
      SKIP_DOCKER_BUILD=true
      echo "🪄 OG MODE: Only git stash/pull (no Maven/Docker/deploy)"  # UPDATED
      ;;
    -s=*)
      SELECTED_SERVICES="${arg#-s=}"
      if [ "$SELECTED_SERVICES" != "all" ]; then
        BUILD_ALL_SERVICES=false
        echo "🎯 Selected services: $SELECTED_SERVICES"
      fi
      ;;
    -c)
      ENABLE_AUTO_CHECK=true
      echo "🔄 AutoCheckService will be enabled after deployment"
      ;;
    -help|--help|-h)
      show_help
      exit 0
      ;;
    *)
      if [ -n "$arg" ]; then
        echo "❌ Invalid parameter: $arg"
        echo "Use -help to see available options"
        exit 1
      fi
      ;;
  esac
done

# Export mode flags so child scripts (if any) can detect build-only mode
export KIWI_DEPLOY_MODE="$MODE"
export ONLY_BUILD_MAVEN
export ONLY_SEND_JARS
export ONLY_GIT_MODE

# Quick, centralized mode summary (helps trace the flow)
echo "=== deployKiwi.sh MODE SUMMARY ==="
echo "MODE: ${MODE:-<none>} | ONLY_BUILD_MAVEN=${ONLY_BUILD_MAVEN} | USE_EXISTING_JARS_ONLY=${USE_EXISTING_JARS_ONLY}"
echo "SKIP_GIT=${SKIP_GIT} | SKIP_MAVEN=${SKIP_MAVEN} | SKIP_DOCKER_BUILD=${SKIP_DOCKER_BUILD} | FAST_DEPLOY_MODE=${FAST_DEPLOY_MODE} | ONLY_SEND_JARS=${ONLY_SEND_JARS} | ONLY_GIT_MODE=${ONLY_GIT_MODE}"  # UPDATED
echo "Selected services: $([ "$BUILD_ALL_SERVICES" = true ] && echo ALL || echo "$SELECTED_SERVICES")"
echo "=================================="

# Ensure permissions right after mode parse (auto-run for any mode, e.g., -mode=obm)
ensure_execute_permissions

# Validate selected services
if [ "$BUILD_ALL_SERVICES" = false ]; then
  IFS=',' read -ra SERVICE_ARRAY <<< "$SELECTED_SERVICES"
  for service in "${SERVICE_ARRAY[@]}"; do
    if [[ ! " ${!MICROSERVICES[@]} " =~ " ${service} " ]]; then
      echo "❌ Invalid service: $service"
      echo "Available services: ${!MICROSERVICES[@]}"
      exit 1
    fi
  done
fi

# Function to check if a service should be built
should_build_service() {
  local service=$1
  if [ "$BUILD_ALL_SERVICES" = true ]; then
    return 0
  fi
  if [[ ",$SELECTED_SERVICES," =~ ",$service," ]]; then
    return 0
  fi
  return 1
}

# === New: send jars to remote host via FTP (first-run prompts saved) ===
send_jars_remote() {
  local original_home="$1"
  local src_dir="${original_home}/built_jar"
  local cfg_dir="${original_home}/.kiwi"
  local old_cfg_file="${cfg_dir}/obmas.conf"
  local cfg_file="${cfg_dir}/send.conf"

  mkdir -p "$cfg_dir"
  # Backward compatibility: migrate old obmas.conf → send.conf if present
  if [ -f "$old_cfg_file" ] && [ ! -f "$cfg_file" ]; then
    echo "ℹ️  Migrating FTP config from obmas.conf to send.conf"
    mv -f "$old_cfg_file" "$cfg_file" || cp -f "$old_cfg_file" "$cfg_file" || true
  fi

  if [ ! -f "$cfg_file" ]; then
    echo "📝 First-time FTP setup (send jars):"
    read -p "FTP host (hostname or IP): " REMOTE_HOST
    read -p "FTP user: " REMOTE_USER
    read -p "Remote directory (default: ~/built_jar): " REMOTE_DIR
    REMOTE_DIR=${REMOTE_DIR:-"~/built_jar"}
    read -s -p "FTP password: " REMOTE_PASS
    echo
    # Optional protocol/port with safe defaults
    local REMOTE_PROTO="ftp"
    local REMOTE_PORT="21"
    # Save config (restrict permissions)
    umask 077
    cat > "$cfg_file" <<EOF
REMOTE_HOST="$REMOTE_HOST"
REMOTE_USER="$REMOTE_USER"
REMOTE_DIR="$REMOTE_DIR"
REMOTE_PASS="$REMOTE_PASS"
REMOTE_PROTO="$REMOTE_PROTO"
REMOTE_PORT="$REMOTE_PORT"
EOF
    umask 022
    if [ -n "$SUDO_USER" ]; then
      chown "$SUDO_USER":"$SUDO_USER" "$cfg_file" || true
    fi
    echo "✅ Saved FTP configuration to $cfg_file"
  fi

  # shellcheck disable=SC1090
  source "$cfg_file"

  # Normalize default directory names
  if [ -z "${REMOTE_DIR:-}" ]; then
    REMOTE_DIR="~/built_jar"
  elif [ "$REMOTE_DIR" = "~/kiwi_jars" ]; then
    echo "ℹ️  Updating REMOTE_DIR from ~/kiwi_jars to ~/built_jar"
    REMOTE_DIR="~/built_jar"
  fi
  REMOTE_PROTO=${REMOTE_PROTO:-ftp}
  REMOTE_PORT=${REMOTE_PORT:-21}

  # Persist any migrated/defaulted values back to file
  umask 077
  cat > "$cfg_file" <<EOF
REMOTE_HOST="${REMOTE_HOST}"
REMOTE_USER="${REMOTE_USER}"
REMOTE_DIR="${REMOTE_DIR}"
REMOTE_PASS="${REMOTE_PASS}"
REMOTE_PROTO="${REMOTE_PROTO}"
REMOTE_PORT="${REMOTE_PORT}"
EOF
  umask 022
  if [ -n "$SUDO_USER" ]; then
    chown "$SUDO_USER":"$SUDO_USER" "$cfg_file" || true
  fi

  if [ ! -d "$src_dir" ] || [ -z "$(ls -1 "$src_dir"/*.jar 2>/dev/null)" ]; then
    echo "❌ No jars found to send in: $src_dir"
    return 1
  fi

  if ! command -v curl >/dev/null 2>&1; then
    echo "❌ 'curl' is required for FTP upload but not found in PATH"
    echo "   Please install curl and retry."
    return 1
  fi

  # Normalize remote path for FTP (strip leading ~/) – FTP servers typically start at user's home
  local REMOTE_PATH="$REMOTE_DIR"
  if [[ "$REMOTE_PATH" == ~/* ]]; then
    REMOTE_PATH="${REMOTE_PATH#~/}"
  elif [[ "$REMOTE_PATH" == "~" ]]; then
    REMOTE_PATH=""
  fi

  local base_url
  if [ -n "$REMOTE_PATH" ]; then
    base_url="${REMOTE_PROTO}://${REMOTE_HOST}:${REMOTE_PORT}/${REMOTE_PATH}/"
  else
    base_url="${REMOTE_PROTO}://${REMOTE_HOST}:${REMOTE_PORT}/"
  fi

  echo "🌐 Sending jars to ${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_PORT}/${REMOTE_PATH:-<home>} via ${REMOTE_PROTO^^} ..."
  local any_failed=false
  shopt -s nullglob
  local files=("$src_dir"/*.jar)
  local total=${#files[@]}
  local idx=0
  for f in "${files[@]}"; do
    [ -e "$f" ] || continue
    idx=$((idx+1))
    local fname
    fname="$(basename "$f")"
    local fsize
    if fsize=$(stat -c %s "$f" 2>/dev/null); then :; else fsize=$(stat -f %z "$f" 2>/dev/null || echo 0); fi
    echo "   → Uploading [$idx/$total] $fname (size: ${fsize} bytes)"
    if ! curl \
         --fail --ftp-create-dirs --progress-bar \
         --connect-timeout 10 --retry 3 --retry-delay 2 \
         -u "$REMOTE_USER:$REMOTE_PASS" \
         -T "$f" "${base_url}${fname}" \
         -w "\n     ↳ Done: %{size_upload} bytes in %{time_total}s (%{speed_upload} B/s)\n"; then
      echo "     ❌ Failed to upload $fname"
      any_failed=true
    else
      echo "     ✅ Uploaded $fname"
    fi
  done
  shopt -u nullglob

  if [ "$any_failed" = true ]; then
    echo "⚠️  Some uploads failed. Please check connectivity/credentials and retry."
    return 1
  fi

  echo "✅ All jars sent to ${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_PORT}/${REMOTE_PATH:-<home>}"
}

# === NEW: OSJ short-circuit workflow (send-only) ===
if [ "$ONLY_SEND_JARS" = true ]; then
  echo "== OSJ FLOW: ENTER =="
  # Determine original user's home
  if [ -n "$SUDO_USER" ]; then
    ORIGINAL_HOME=$(eval echo "~$SUDO_USER")
  else
    ORIGINAL_HOME="$HOME"
  fi
  SRC_DIR="${ORIGINAL_HOME}/built_jar"
  echo "Source jar dir: $SRC_DIR"
  echo "=============================================="

  # Minimal cleanup of invalid symlinks and validation
  echo "🔎 Pre-checking jars under: $SRC_DIR"
  if [ ! -d "$SRC_DIR" ]; then
    echo "❌ Directory not found: $SRC_DIR"
    echo "   Build jars locally with -mode=obm first."
    exit 1
  fi
  shopt -s nullglob
  for l in "$SRC_DIR"/*.jar; do
    if [ -L "$l" ]; then
      base="$(basename "$l")"
      target="$(readlink "$l" 2>/dev/null || true)"
      if [ "$target" = "$base" ] || [ ! -e "$l" ]; then
        echo "   🧹 Removing invalid symlink: $base -> ${target:-<broken>}"
        rm -f "$l"
      fi
    fi
  done
  shopt -u nullglob
  # Ensure at least one real jar exists
  if ! find "$SRC_DIR" -maxdepth 1 -type f -name '*.jar' | grep -q .; then
    echo "❌ No valid jar files found in $SRC_DIR after cleanup."
    echo "   Build locally with -mode=obm first."
    exit 1
  fi

  # Perform send (via FTP)
  send_jars_remote "$ORIGINAL_HOME" || { echo "❌ Failed sending jars"; exit 1; }

  echo "== OSJ FLOW: DONE. Exiting without any git/maven/docker steps. =="
  exit 0
fi

# === OG short-circuit workflow (Only Git) ===
if [ "$ONLY_GIT_MODE" = true ]; then
  echo "== OG FLOW: ENTER =="
  echo "📥 Performing git operations only (stash + pull)..."
  # Ensure we are at project root (already cd'ed earlier, but enforce)
  cd "$CURRENT_DIR/microservice-kiwi/" || { echo "❌ Cannot cd to project root"; exit 1; }
  git stash || true
  git pull --rebase || git pull

  # NEW: After git-only update, ensure kiwi-deploy shell scripts are executable
  echo "🔐 Updating permissions for kiwi-deploy shell scripts..."
  DEPLOY_DIR="$CURRENT_DIR/microservice-kiwi/kiwi-deploy"
  if [ -d "$DEPLOY_DIR" ]; then
    # Recursively set 777 for all .sh files under kiwi-deploy (portable for macOS/BSD find)
    find "$DEPLOY_DIR" -type f -name "*.sh" -exec chmod 777 {} \;
    echo "✅ Set 777 on all .sh files under: $DEPLOY_DIR"
  else
    echo "ℹ️  kiwi-deploy directory not found at: $DEPLOY_DIR"
  fi

  echo "✅ OG FLOW COMPLETE (git only + chmod). Exiting without any further steps."
  exit 0
fi

# Display final configuration
echo "=============================================="
echo "DEPLOYMENT CONFIGURATION:"
if [ "$FAST_DEPLOY_MODE" = true ]; then
  echo "🚀 MODE: FAST DEPLOY (Skip All Builds)"
  echo "   📋 Operations: Stop Containers → Remove Containers → Start Containers"
  echo "   ⚡ Estimated time: ~2-3 minutes"
elif [ "$SKIP_GIT" = true ] || [ "$SKIP_MAVEN" = true ] || [ "$SKIP_DOCKER_BUILD" = true ]; then
  echo "⚙️  MODE: PARTIAL BUILD"
  echo "   Git operations: $([ "$SKIP_GIT" = true ] && echo "SKIPPED" || echo "ENABLED")"
  echo "   Maven build: $([ "$SKIP_MAVEN" = true ] && echo "SKIPPED" || echo "ENABLED")"
  echo "   Docker building: $([ "$SKIP_DOCKER_BUILD" = true ] && echo "SKIPPED" || echo "ENABLED")"
else
  echo "🔨 MODE: FULL BUILD"
  echo "   📋 Operations: Git Pull → Maven Build → Docker Build → Deploy"
  echo "   ⏱️  Estimated time: ~10-15 minutes"
fi

if [ "$BUILD_ALL_SERVICES" = false ]; then
  echo "🎯 Services to build: $SELECTED_SERVICES"
else
  echo "🎯 Services to build: ALL"
fi

if [ "$ENABLE_AUTO_CHECK" = true ]; then
  echo "🔄 AutoCheckService: ENABLED"
else
  echo "🔄 AutoCheckService: DISABLED (use -c to enable)"
fi
echo "=============================================="
echo ""

# === OBM short-circuit workflow (Maven build only, copy jars) ===
if [ "$ONLY_BUILD_MAVEN" = true ]; then
  echo "== OBM FLOW: ENTER =="

  echo "🛑 Stopping all kiwi containers before OBM build..."
  "$CURRENT_DIR/microservice-kiwi/kiwi-deploy/docker/stopAll.sh" || true

  # Determine original user's home for Maven repo and built_jar
  if [ -n "$SUDO_USER" ]; then
    ORIGINAL_HOME=$(eval echo "~$SUDO_USER")
  else
    ORIGINAL_HOME="$HOME"
  fi
  BUILT_DIR="$ORIGINAL_HOME/built_jar"
  mkdir -p "$BUILT_DIR"

  # Optional git operations before build
  if [ "$SKIP_GIT" = false ]; then
    echo "📥 Git pulling (pre-build)..."
    ( git stash || true )
    if ! git pull --rebase; then git pull; fi
    # Immediately restore script permissions after git pull
    ensure_deploy_permissions_recursive
  else
    echo "⏭️  Git operations skipped"
  fi

  # Ensure TTS lib is installed into the correct local repo
  echo "📚 Installing VoiceRSS TTS library into local Maven repo..."
  echo "📂 Using Maven repository: $ORIGINAL_HOME/.m2"
  cd "$CURRENT_DIR/microservice-kiwi/kiwi-common/kiwi-common-tts/lib"
  mvn -q install:install-file \
      -Dfile=voicerss_tts.jar \
      -DgroupId=voicerss \
      -DartifactId=tts \
      -Dversion=2.0 \
      -Dpackaging=jar \
      -Dmaven.repo.local="$ORIGINAL_HOME/.m2/repository"
  cd "$CURRENT_DIR/microservice-kiwi/"

  # Maven build
  if [ "$BUILD_ALL_SERVICES" = true ]; then
    echo "🔨 Running maven build for all services..."
    mvn -q clean install -Dmaven.test.skip=true -B -Dmaven.repo.local="$ORIGINAL_HOME/.m2/repository"
  else
    echo "🔨 Running selective maven build..."
    echo "📦 Building common modules first..."
    mvn -q clean install -pl kiwi-common -am -Dmaven.test.skip=true -B -Dmaven.repo.local="$ORIGINAL_HOME/.m2/repository"
    IFS=',' read -ra SERVICE_ARRAY <<< "$SELECTED_SERVICES"
    for service in "${SERVICE_ARRAY[@]}"; do
      case "$service" in
        "upms")
          mvn -q clean install -pl kiwi-upms/kiwi-upms-biz -am -Dmaven.test.skip=true -B -Dmaven.repo.local="$ORIGINAL_HOME/.m2/repository"
          ;;
        "word")
          mvn -q clean install -pl kiwi-word/kiwi-word-biz -am -Dmaven.test.skip=true -B -Dmaven.repo.local="$ORIGINAL_HOME/.m2/repository"
          ;;
        "crawler")
          mvn -q clean install -pl kiwi-word/kiwi-word-crawler -am -Dmaven.test.skip=true -B -Dmaven.repo.local="$ORIGINAL_HOME/.m2/repository"
          ;;
        "ai")
          mvn -q clean install -pl kiwi-ai/kiwi-ai-biz -am -Dmaven.test.skip=true -B -Dmaven.repo.local="$ORIGINAL_HOME/.m2/repository"
          ;;
        *)
          module="${MICROSERVICES[$service]}"
          mvn -q clean install -pl "$module" -am -Dmaven.test.skip=true -B -Dmaven.repo.local="$ORIGINAL_HOME/.m2/repository"
          ;;
      esac
    done
  fi

  # Copy built jars into ~/built_jar
  echo "📦 Copying built jars into: $BUILT_DIR"
  copy_or_warn() {
    local src="$1"; local name="$2"
    if [ -f "$src" ]; then
      cp -f "$src" "$BUILT_DIR/"
      echo "   ✅ $name"
    else
      echo "   ⚠️  Missing jar for $name (expected at $src)"
    fi
  }

  # Build list and copy conditionally
  if should_build_service "eureka"; then
    copy_or_warn "$ORIGINAL_HOME/.m2/repository/me/fengorz/kiwi-eureka/2.0/kiwi-eureka-2.0.jar" eureka
  fi
  if should_build_service "config"; then
    copy_or_warn "$ORIGINAL_HOME/.m2/repository/me/fengorz/kiwi-config/2.0/kiwi-config-2.0.jar" config
  fi
  if should_build_service "upms"; then
    copy_or_warn "$ORIGINAL_HOME/.m2/repository/me/fengorz/kiwi-upms-biz/2.0/kiwi-upms-biz-2.0.jar" upms
  fi
  if should_build_service "auth"; then
    copy_or_warn "$ORIGINAL_HOME/.m2/repository/me/fengorz/kiwi-auth/2.0/kiwi-auth-2.0.jar" auth
  fi
  if should_build_service "gate"; then
    copy_or_warn "$ORIGINAL_HOME/.m2/repository/me/fengorz/kiwi-gateway/2.0/kiwi-gateway-2.0.jar" gateway
  fi
  if should_build_service "tools"; then
    copy_or_warn "$ORIGINAL_HOME/.m2/repository/me/fengorz/kiwi-tools-biz/2.0/kiwi-tools-biz-2.0.jar" tools
  fi
  if should_build_service "word"; then
    copy_or_warn "$ORIGINAL_HOME/.m2/repository/me/fengorz/kiwi-word-biz/2.0/kiwi-word-biz-2.0.jar" word-biz
  fi
  if should_build_service "crawler"; then
    copy_or_warn "$ORIGINAL_HOME/.m2/repository/me/fengorz/kiwi-word-crawler/2.0/kiwi-word-crawler-2.0.jar" crawler
  fi
  if should_build_service "ai"; then
    copy_or_warn "$ORIGINAL_HOME/.m2/repository/me/fengorz/kiwi-ai-biz/2.0/kiwi-ai-biz-2.0.jar" ai
  fi

  echo "== OBM FLOW: DONE. Exiting without any docker/cleanup steps. =="
  exit 0
fi

# Stop all services first (critical step)
# Add a visible guard before any docker-related action
# (OBM handled above)

echo "=============================================="
echo "🛑 INITIAL CLEANUP - STOPPING ALL SERVICES"
echo "=============================================="
echo "Running stopAll.sh to ensure clean deployment environment..."
"$CURRENT_DIR/microservice-kiwi/kiwi-deploy/docker/stopAll.sh" || {
    echo "❌ Failed to stop existing services"
    echo "This might cause deployment conflicts. Continue anyway? (y/N)"
    read -r response
    if [[ ! "$response" =~ ^[Yy]$ ]]; then
        echo "Deployment cancelled by user"
        exit 1
    fi
}
echo "✅ All existing services stopped and cleaned up"
echo ""

# Fast deploy mode optimization - skip unnecessary operations
if [ "$FAST_DEPLOY_MODE" = true ]; then
  echo "🚀 FAST DEPLOY MODE ACTIVATED"
  echo "=============================================="
  echo "⏭️  Skipping all build operations for maximum speed..."
  echo "📦 Using existing Docker images and configurations"
  echo "⚡ Proceeding directly to container deployment..."
  echo ""
else
  # Git operations
  if [ "$SKIP_GIT" = false ]; then
    echo "📥 Git pulling..."
    echo "📦 Stashing local changes..."
    git stash
    echo "⬇️  Pulling latest changes..."
    git pull
  else
    echo "⏭️  Git operations skipped"
  fi

  # Set execute permissions
  ensure_execute_permissions

  # Clean log directories efficiently (only if not in fast deploy mode)
  echo "🧹 Cleaning log directories..."
  if should_build_service "eureka"; then
    rm -rf "$CURRENT_DIR/docker/kiwi/eureka/logs/"* 2>/dev/null || true
  fi
  if should_build_service "config"; then
    rm -rf "$CURRENT_DIR/docker/kiwi/config/logs/"* 2>/dev/null || true
  fi
  if should_build_service "upms"; then
    rm -rf "$CURRENT_DIR/docker/kiwi/upms/logs/"* 2>/dev/null || true
  fi
  if should_build_service "auth"; then
    rm -rf "$CURRENT_DIR/docker/kiwi/auth/logs/"* 2>/dev/null || true
  fi
  if should_build_service "gate"; then
    rm -rf "$CURRENT_DIR/docker/kiwi/gate/logs/"* 2>/dev/null || true
  fi
  if should_build_service "word"; then
    rm -rf "$CURRENT_DIR/docker/kiwi/word/logs/"* 2>/dev/null || true
    rm -rf "$CURRENT_DIR/docker/kiwi/word/crawlerTmp/"* 2>/dev/null || true
    rm -rf "$CURRENT_DIR/docker/kiwi/word/bizTmp/"* 2>/dev/null || true
  fi
  if should_build_service "crawler"; then
    rm -rf "$CURRENT_DIR/docker/kiwi/crawler/logs/"* 2>/dev/null || true
  fi
  if should_build_service "ai"; then
    rm -rf "$CURRENT_DIR/docker/kiwi/ai/logs/"* 2>/dev/null || true
    rm -rf "$CURRENT_DIR/docker/kiwi/ai/tmp/"* 2>/dev/null || true
  fi
  if should_build_service "tools"; then
    rm -rf "$CURRENT_DIR/docker/kiwi/tools/logs/"* 2>/dev/null || true   # NEW
  fi

  # Maven build
  if [ "$SKIP_MAVEN" = false ]; then
    # Get the original user's home directory
    if [ -n "$SUDO_USER" ]; then
      ORIGINAL_HOME=$(eval echo "~$SUDO_USER")
    else
      ORIGINAL_HOME="$HOME"
    fi

    echo "📚 Installing VoiceRSS TTS library..."
    echo "📂 Using Maven repository: $ORIGINAL_HOME/.m2"
    cd "$CURRENT_DIR/microservice-kiwi/kiwi-common/kiwi-common-tts/lib"
    mvn -q install:install-file \
        -Dfile=voicerss_tts.jar \
        -DgroupId=voicerss \
        -DartifactId=tts \
        -Dversion=2.0 \
        -Dpackaging=jar \
        -Dmaven.repo.local="$ORIGINAL_HOME/.m2/repository"
    cd "$CURRENT_DIR/microservice-kiwi/"

    if [ "$BUILD_ALL_SERVICES" = true ]; then
      echo "🔨 Running maven build for all services..."
      mvn -q clean install -Dmaven.test.skip=true -B -Dmaven.repo.local="$ORIGINAL_HOME/.m2/repository"
    else
      echo "🔨 Running selective maven build..."
      # Build common modules first (always needed)
      echo "📦 Building common modules..."
      mvn -q clean install -pl kiwi-common -am -Dmaven.test.skip=true -B -Dmaven.repo.local="$ORIGINAL_HOME/.m2/repository"

      # Build selected services
      IFS=',' read -ra SERVICE_ARRAY <<< "$SELECTED_SERVICES"
      for service in "${SERVICE_ARRAY[@]}"; do
        module="${MICROSERVICES[$service]}"
        echo "📦 Building $service ($module)..."

        # Handle special cases for nested modules
        case "$service" in
          "upms")
            mvn -q clean install -pl kiwi-upms/kiwi-upms-biz -am -Dmaven.test.skip=true -B -Dmaven.repo.local="$ORIGINAL_HOME/.m2/repository"
            ;;
          "word")
            mvn -q clean install -pl kiwi-word/kiwi-word-biz -am -Dmaven.test.skip=true -B -Dmaven.repo.local="$ORIGINAL_HOME/.m2/repository"
            ;;
          "crawler")
            mvn -q clean install -pl kiwi-word/kiwi-word-crawler -am -Dmaven.test.skip=true -B -Dmaven.repo.local="$ORIGINAL_HOME/.m2/repository"
            ;;
          "ai")
            mvn -q clean install -pl kiwi-ai/kiwi-ai-biz -am -Dmaven.test.skip=true -B -Dmaven.repo.local="$ORIGINAL_HOME/.m2/repository"
            ;;
          *)
            mvn -q clean install -pl $module -am -Dmaven.test.skip=true -B -Dmaven.repo.local="$ORIGINAL_HOME/.m2/repository"
            ;;
        esac
      done
    fi
  else
    echo "⏭️  Maven build skipped"
  fi

  # Move Dockerfiles and JARs efficiently
  if [ "$SKIP_DOCKER_BUILD" = false ]; then
    # Download latest yt-dlp into AI docker contexts before build (only if AI is selected)
    if should_build_service "ai"; then
      download_latest_ytdlp || echo "⚠️  Proceeding without updating yt-dlp (download failed)."
    fi

    # Get the original user's home directory for JAR files
    if [ -n "$SUDO_USER" ]; then
      ORIGINAL_HOME=$(eval echo "~$SUDO_USER")
    else
      ORIGINAL_HOME="$HOME"
    fi

    echo "📋 Moving Dockerfiles, GCP credentials and JARs..."
    echo "📂 Maven repository: $ORIGINAL_HOME/.m2"
    [ "$USE_EXISTING_JARS_ONLY" = true ] && echo "📂 Using existing jars from: $ORIGINAL_HOME/built_jar"

    # NEW: Cleanup and validate ~/built_jar for OUEJ (remove broken/self symlinks and ensure real jars exist)
    cleanup_and_validate_built_jar_dir() {
      local dir="$1"
      echo "🔎 Pre-checking jars under: $dir"
      if [ ! -d "$dir" ]; then
        echo "❌ Directory not found: $dir"
        echo "   Build jars locally with -mode=obm first."
        exit 1
      fi
      shopt -s nullglob
      # Collect matching files (includes regular files and symlinks)
      local files=( "$dir/${prefix}-"*.jar )
      shopt -u nullglob

      # If none, return empty (success)
      if [ ${#files[@]} -eq 0 ]; then
        echo ""
        return 0
      fi

      # Pick newest by mtime; accept symlinks that resolve to existing files
      for f in "${files[@]}"; do
        [ -e "$f" ] || continue
        local mtime
        if mtime=$(stat -c %Y "$f" 2>/dev/null); then :; else mtime=$(stat -f %m "$f" 2>/dev/null || echo 0); fi
        if [ "$mtime" -gt "$latest_mtime" ]; then
          latest="$f"
          latest_mtime="$mtime"
        fi
      done

      echo "$latest"
      return 0
    }

    # Helper: find newest jar in a directory matching a given prefix
    find_built_by_prefix() {
      local prefix="$1"
      local dir="$2"
      local latest=""
      local latest_mtime=0
      shopt -s nullglob
      for f in "$dir/${prefix}-"*.jar; do
        [ -e "$f" ] || continue
        # Resolve symlink to ensure it exists
        if [ -L "$f" ] && [ ! -e "$f" ]; then
          continue
        fi
        local mtime
        if mtime=$(stat -c %Y "$f" 2>/dev/null); then :; else mtime=$(stat -f %m "$f" 2>/dev/null || echo 0); fi
        if [ "$mtime" -gt "$latest_mtime" ]; then
          latest="$f"
          latest_mtime="$mtime"
        fi
      done
      shopt -u nullglob
      if [ -n "$latest" ]; then
        echo "$latest"
        return 0
      fi
      return 1
    }

    resolve_jar_path() {
      local service="$1"     # key: eureka/config/...
      local m2_path="$2"     # fallback path under ~/.m2
      if [ "$USE_EXISTING_JARS_ONLY" = true ]; then
        case "$service" in
          eureka)  find_built_by_prefix "kiwi-eureka" "$ORIGINAL_HOME/built_jar" ;;
          config)  find_built_by_prefix "kiwi-config" "$ORIGINAL_HOME/built_jar" ;;
          upms)    find_built_by_prefix "kiwi-upms-biz" "$ORIGINAL_HOME/built_jar" ;;
          auth)    find_built_by_prefix "kiwi-auth" "$ORIGINAL_HOME/built_jar" ;;
          gate)    find_built_by_prefix "kiwi-gateway" "$ORIGINAL_HOME/built_jar" ;;
          word)    find_built_by_prefix "kiwi-word-biz" "$ORIGINAL_HOME/built_jar" ;;
          crawler) find_built_by_prefix "kiwi-word-crawler" "$ORIGINAL_HOME/built_jar" ;;
          ai)      find_built_by_prefix "kiwi-ai-biz" "$ORIGINAL_HOME/built_jar" ;;
          tools)   find_built_by_prefix "kiwi-tools-biz" "$ORIGINAL_HOME/built_jar" ;;  # NEW
          *) echo "" ;;
        esac
      else
        echo "$m2_path"
      fi
    }

    # eureka
    if should_build_service "eureka"; then
      echo "📄 Copying eureka files..."
      cp -f "$CURRENT_DIR/microservice-kiwi/kiwi-eureka/Dockerfile" "$CURRENT_DIR/docker/kiwi/eureka/"
      eureka_jar=$(resolve_jar_path "eureka" "$ORIGINAL_HOME/.m2/repository/me/fengorz/kiwi-eureka/2.0/kiwi-eureka-2.0.jar")
      [ -n "$eureka_jar" ] && [ -f "$eureka_jar" ] || { echo "❌ Missing jar for eureka in ${USE_EXISTING_JARS_ONLY:+$ORIGINAL_HOME/built_jar}. Use -mode=obm first."; exit 1; }
      cp -f "$eureka_jar" "$CURRENT_DIR/docker/kiwi/eureka/"
    fi

    # config
    if should_build_service "config"; then
      echo "📄 Copying config files..."
      cp -f "$CURRENT_DIR/microservice-kiwi/kiwi-config/Dockerfile" "$CURRENT_DIR/docker/kiwi/config/"
      config_jar=$(resolve_jar_path "config" "$ORIGINAL_HOME/.m2/repository/me/fengorz/kiwi-config/2.0/kiwi-config-2.0.jar")
      [ -n "$config_jar" ] && [ -f "$config_jar" ] || { echo "❌ Missing jar for config in ${USE_EXISTING_JARS_ONLY:+$ORIGINAL_HOME/built_jar}. Use -mode=obm first."; exit 1; }
      cp -f "$config_jar" "$CURRENT_DIR/docker/kiwi/config/"
    fi

    # upms
    if should_build_service "upms"; then
      echo "📄 Copying upms files..."
      cp -f "$CURRENT_DIR/microservice-kiwi/kiwi-upms/kiwi-upms-biz/Dockerfile" "$CURRENT_DIR/docker/kiwi/upms/"
      upms_jar=$(resolve_jar_path "upms" "$ORIGINAL_HOME/.m2/repository/me/fengorz/kiwi-upms-biz/2.0/kiwi-upms-biz-2.0.jar")
      [ -n "$upms_jar" ] && [ -f "$upms_jar" ] || { echo "❌ Missing jar for upms in ${USE_EXISTING_JARS_ONLY:+$ORIGINAL_HOME/built_jar}. Use -mode=obm first."; exit 1; }
      cp -f "$upms_jar" "$CURRENT_DIR/docker/kiwi/upms/"
    fi

    # auth
    if should_build_service "auth"; then
      echo "📄 Copying auth files..."
      cp -f "$CURRENT_DIR/microservice-kiwi/kiwi-auth/Dockerfile" "$CURRENT_DIR/docker/kiwi/auth/"
      auth_jar=$(resolve_jar_path "auth" "$ORIGINAL_HOME/.m2/repository/me/fengorz/kiwi-auth/2.0/kiwi-auth-2.0.jar")
      [ -n "$auth_jar" ] && [ -f "$auth_jar" ] || { echo "❌ Missing jar for auth in ${USE_EXISTING_JARS_ONLY:+$ORIGINAL_HOME/built_jar}. Use -mode=obm first."; exit 1; }
      cp -f "$auth_jar" "$CURRENT_DIR/docker/kiwi/auth/"
    fi

    # gate
    if should_build_service "gate"; then
      echo "📄 Copying gateway files..."
      cp -f "$CURRENT_DIR/microservice-kiwi/kiwi-gateway/Dockerfile" "$CURRENT_DIR/docker/kiwi/gate/"
      gate_jar=$(resolve_jar_path "gate" "$ORIGINAL_HOME/.m2/repository/me/fengorz/kiwi-gateway/2.0/kiwi-gateway-2.0.jar")
      [ -n "$gate_jar" ] && [ -f "$gate_jar" ] || { echo "❌ Missing jar for gate in ${USE_EXISTING_JARS_ONLY:+$ORIGINAL_HOME/built_jar}. Use -mode=obm first."; exit 1; }
      cp -f "$gate_jar" "$CURRENT_DIR/docker/kiwi/gate/"
    fi

    # tools (NEW)
    if should_build_service "tools"; then
      echo "📄 Copying tools files..."
      # Try common Dockerfile locations
      if [ -f "$CURRENT_DIR/microservice-kiwi/kiwi-tools/kiwi-tools-biz/Dockerfile" ]; then
        cp -f "$CURRENT_DIR/microservice-kiwi/kiwi-tools/kiwi-tools-biz/Dockerfile" "$CURRENT_DIR/docker/kiwi/tools/"
      elif [ -f "$CURRENT_DIR/microservice-kiwi/kiwi-tools/kiwi-tools-biz/docker/Dockerfile" ]; then
        cp -f "$CURRENT_DIR/microservice-kiwi/kiwi-tools/kiwi-tools-biz/docker/Dockerfile" "$CURRENT_DIR/docker/kiwi/tools/"
      else
        echo "⚠️  Tools Dockerfile not found under kiwi-tools/kiwi-tools-biz. Ensure it exists."
      fi
      tools_jar=$(resolve_jar_path "tools" "$ORIGINAL_HOME/.m2/repository/me/fengorz/kiwi-tools-biz/2.0/kiwi-tools-biz-2.0.jar")
      [ -n "$tools_jar" ] && [ -f "$tools_jar" ] || { echo "❌ Missing jar for tools in ${USE_EXISTING_JARS_ONLY:+$ORIGINAL_HOME/built_jar}. Use -mode=obm first."; exit 1; }
      cp -f "$tools_jar" "$CURRENT_DIR/docker/kiwi/tools/"
    fi

    # word
    if should_build_service "word"; then
      echo "📄 Copying word service files..."
      cp -f "$CURRENT_DIR/microservice-kiwi/kiwi-word/kiwi-word-biz/docker/biz/Dockerfile" "$CURRENT_DIR/docker/kiwi/word/biz"
      cp -f "$CURRENT_DIR/microservice-kiwi/kiwi-word/kiwi-word-biz/docker/crawler/Dockerfile" "$CURRENT_DIR/docker/kiwi/word/crawler"
      word_jar=$(resolve_jar_path "word" "$ORIGINAL_HOME/.m2/repository/me/fengorz/kiwi-word-biz/2.0/kiwi-word-biz-2.0.jar")
      [ -n "$word_jar" ] && [ -f "$word_jar" ] || { echo "❌ Missing jar for word-biz in ${USE_EXISTING_JARS_ONLY:+$ORIGINAL_HOME/built_jar}. Use -mode=obm first."; exit 1; }
      cp -f "$word_jar" "$CURRENT_DIR/docker/kiwi/word/"
      cp -f "$CURRENT_DIR/gcp-credentials.json" "$CURRENT_DIR/docker/kiwi/word/bizTmp"
    fi

    # crawler
    if should_build_service "crawler"; then
      echo "📄 Copying crawler files..."
      cp -f "$CURRENT_DIR/microservice-kiwi/kiwi-word/kiwi-word-crawler/Dockerfile" "$CURRENT_DIR/docker/kiwi/crawler/"
      crawler_jar=$(resolve_jar_path "crawler" "$ORIGINAL_HOME/.m2/repository/me/fengorz/kiwi-word-crawler/2.0/kiwi-word-crawler-2.0.jar")
      [ -n "$crawler_jar" ] && [ -f "$crawler_jar" ] || { echo "❌ Missing jar for crawler in ${USE_EXISTING_JARS_ONLY:+$ORIGINAL_HOME/built_jar}. Use -mode=obm first."; exit 1; }
      cp -f "$crawler_jar" "$CURRENT_DIR/docker/kiwi/crawler/"
    fi

    # ai
    if should_build_service "ai"; then
      echo "📄 Copying AI service files..."
      cp -f "$CURRENT_DIR/microservice-kiwi/kiwi-ai/kiwi-ai-biz/docker/biz/Dockerfile" "$CURRENT_DIR/docker/kiwi/ai/biz"
      cp -f "$CURRENT_DIR/microservice-kiwi/kiwi-ai/kiwi-ai-biz/docker/batch/Dockerfile" "$CURRENT_DIR/docker/kiwi/ai/batch"
      ai_jar=$(resolve_jar_path "ai" "$ORIGINAL_HOME/.m2/repository/me/fengorz/kiwi-ai-biz/2.0/kiwi-ai-biz-2.0.jar")
      [ -n "$ai_jar" ] && [ -f "$ai_jar" ] || { echo "❌ Missing jar for ai in ${USE_EXISTING_JARS_ONLY:+$ORIGINAL_HOME/built_jar}. Use -mode=obm first."; exit 1; }
      cp -f "$ai_jar" "$CURRENT_DIR/docker/kiwi/ai/biz"
      cp -f "$ai_jar" "$CURRENT_DIR/docker/kiwi/ai/batch"
    fi

  else
    echo "⏭️  Dockerfile building skipped"
  fi
fi

# Define container names mapping
declare -A CONTAINER_NAMES=(
    ["eureka"]="kiwi-eureka"
    ["config"]="kiwi-config"
    ["upms"]="kiwi-upms-biz"
    ["auth"]="kiwi-auth"
    ["gate"]="kiwi-gateway"
    ["word"]="kiwi-word-biz"
    ["crawler"]="kiwi-word-crawler"
    ["ai"]="kiwi-ai-biz"
    ["tools"]="kiwi-tools-biz"   # NEW
)

# Function to stop specific containers
stop_selected_containers() {
    local services="$1"
    IFS=',' read -ra SERVICE_ARRAY <<< "$services"

    for service in "${SERVICE_ARRAY[@]}"; do
        container_name="${CONTAINER_NAMES[$service]}"
        echo "🛑 Stopping $service container ($container_name)..."

        # Check if container exists and is running
        if docker ps -a --format '{{.Names}}' | grep -q "^${container_name}$"; then
            docker stop "$container_name" 2>/dev/null || true
            echo "   ✅ Stopped $container_name"
        else
            echo "   ℹ️  Container $container_name not found or not running"
        fi
    done
}

# Function to remove specific containers
remove_selected_containers() {
    local services="$1"
    IFS=',' read -ra SERVICE_ARRAY <<< "$services"

    for service in "${SERVICE_ARRAY[@]}"; do
        container_name="${CONTAINER_NAMES[$service]}"
        echo "🗑️  Removing $service container ($container_name)..."

        # Check if container exists
        if docker ps -a --format '{{.Names}}' | grep -q "^${container_name}$"; then
            docker rm -f "$container_name" 2>/dev/null || true
            echo "   ✅ Removed $container_name"
        else
            echo "   ℹ️  Container $container_name already removed"
        fi
    done
}

# Function to deploy specific service with Docker build and run
deploy_single_service() {
    local service=$1
    local DOCKER_DIR="$CURRENT_DIR/docker/kiwi"

    # Ensure Docker network exists
    docker network create kiwi-network 2>/dev/null || true

    case "$service" in
        "eureka")
            echo "📡 Building Eureka Service Discovery Server..."
            if [ ! -d "$DOCKER_DIR/eureka" ]; then
                echo "❌ Directory not found: $DOCKER_DIR/eureka"
                return 1
            fi
            cd "$DOCKER_DIR/eureka"
            docker build -t kiwi-eureka:latest .
            docker run -d --name kiwi-eureka \
                --network kiwi-network \
                -p 18000:18000 \
                -v "$DOCKER_DIR/eureka/logs:/logs" \
                -e EUREKA_SERVER=http://kiwi-eureka:18000/eureka/ \
                kiwi-eureka:latest
            echo "✅ Eureka deployed successfully"
            ;;

        "config")
            echo "⚙️ Building Config Server..."
            if [ ! -d "$DOCKER_DIR/config" ]; then
                echo "❌ Directory not found: $DOCKER_DIR/config"
                return 1
            fi
            cd "$DOCKER_DIR/config"
            docker build -t kiwi-config:latest .
            docker run -d --name kiwi-config \
                --network kiwi-network \
                -p 18001:18001 \
                -v "$DOCKER_DIR/config/logs:/logs" \
                -e EUREKA_SERVER=http://kiwi-eureka:18000/eureka/ \
                kiwi-config:latest
            echo "✅ Config Server deployed successfully"
            ;;

        "upms")
            echo "👤 Building UPMS Service..."
            if [ ! -d "$DOCKER_DIR/upms" ]; then
                echo "❌ Directory not found: $DOCKER_DIR/upms"
                return 1
            fi
            cd "$DOCKER_DIR/upms"
            docker build -t kiwi-upms:latest .
            docker run -d --name kiwi-upms-biz \
                --network kiwi-network \
                -p 18002:18002 \
                -v "$DOCKER_DIR/upms/logs:/logs" \
                -e EUREKA_SERVER=http://kiwi-eureka:18000/eureka/ \
                kiwi-upms:latest
            echo "✅ UPMS deployed successfully"
            ;;

        "auth")
            echo "🔐 Building Auth Service..."
            if [ ! -d "$DOCKER_DIR/auth" ]; then
                echo "❌ Directory not found: $DOCKER_DIR/auth"
                return 1
            fi
            cd "$DOCKER_DIR/auth"
            docker build -t kiwi-auth:latest .
            docker run -d --name kiwi-auth \
                --network kiwi-network \
                -p 18003:18003 \
                -v "$DOCKER_DIR/auth/logs:/logs" \
                -e EUREKA_SERVER=http://kiwi-eureka:18000/eureka/ \
                kiwi-auth:latest
            echo "✅ Auth Service deployed successfully"
            ;;

        "gate")
            echo "🌐 Building API Gateway..."
            if [ ! -d "$DOCKER_DIR/gate" ]; then
                echo "❌ Directory not found: $DOCKER_DIR/gate"
                return 1
            fi
            cd "$DOCKER_DIR/gate"
            docker build -t kiwi-gateway:latest .
            docker run -d --name kiwi-gateway \
                --network kiwi-network \
                -p 18004:18004 \
                -v "$DOCKER_DIR/gate/logs:/logs" \
                -e EUREKA_SERVER=http://kiwi-eureka:18000/eureka/ \
                kiwi-gateway:latest
            echo "✅ API Gateway deployed successfully"
            ;;

        "word")
            echo "📚 Building Word Service..."
            if [ ! -d "$DOCKER_DIR/word/biz" ]; then
                echo "❌ Directory not found: $DOCKER_DIR/word/biz"
                return 1
            fi
            cd "$DOCKER_DIR/word/biz"
            docker build -t kiwi-word-biz:latest .
            docker run -d --name kiwi-word-biz \
                --network kiwi-network \
                -p 18010:18010 \
                -v "$DOCKER_DIR/word/logs:/logs" \
                -v "$DOCKER_DIR/word/bizTmp:/bizTmp" \
                -e EUREKA_SERVER=http://kiwi-eureka:18000/eureka/ \
                kiwi-word-biz:latest
            echo "✅ Word Service deployed successfully"
            ;;

        "crawler")
            echo "🕷️ Building Crawler Service..."
            if [ ! -d "$DOCKER_DIR/crawler" ]; then
                echo "❌ Directory not found: $DOCKER_DIR/crawler"
                return 1
            fi
            cd "$DOCKER_DIR/crawler"
            docker build -t kiwi-word-crawler:latest .
            docker run -d --name kiwi-word-crawler \
                --network kiwi-network \
                -p 18011:18011 \
                -v "$DOCKER_DIR/crawler/logs:/logs" \
                -e EUREKA_SERVER=http://kiwi-eureka:18000/eureka/ \
                kiwi-word-crawler:latest
            echo "✅ Crawler Service deployed successfully"
            ;;

        "ai")
            echo "🤖 Building AI Service..."

            # Check if AI directories exist
            if [ ! -d "$DOCKER_DIR/ai" ]; then
                echo "❌ AI directory not found: $DOCKER_DIR/ai"
                echo "Creating AI directory structure..."
                mkdir -p "$DOCKER_DIR/ai/biz" "$DOCKER_DIR/ai/batch" "$DOCKER_DIR/ai/logs" "$DOCKER_DIR/ai/tmp"
            fi

            # Build AI Business Logic
            if [ -d "$DOCKER_DIR/ai/biz" ] && [ -f "$DOCKER_DIR/ai/biz/Dockerfile" ]; then
                echo "   → Building AI Business Logic component..."
                cd "$DOCKER_DIR/ai/biz"
                docker build -t kiwi-ai-biz:latest .

                echo "   → Starting AI Business Logic container..."
                docker run -d --name kiwi-ai-biz \
                    --network kiwi-network \
                    -p 18015:18015 \
                    -v "$DOCKER_DIR/ai/logs:/logs" \
                    -v "$DOCKER_DIR/ai/tmp:/ai-tmp" \
                    -e EUREKA_SERVER=http://kiwi-eureka:18000/eureka/ \
                    -e TZ=Pacific/Auckland \
                    kiwi-ai-biz:latest
            else
                echo "⚠️  AI Business Logic Dockerfile not found at $DOCKER_DIR/ai/biz/Dockerfile"
            fi

            # Build AI Batch
            if [ -d "$DOCKER_DIR/ai/batch" ] && [ -f "$DOCKER_DIR/ai/batch/Dockerfile" ]; then
                echo "   → Building AI Batch component..."
                cd "$DOCKER_DIR/ai/batch"
                docker build -t kiwi-ai-batch:latest .

                echo "   → Starting AI Batch container..."
                docker run -d --name kiwi-ai-batch \
                    --network kiwi-network \
                    -p 18016:18016 \
                    -v "$DOCKER_DIR/ai/logs:/logs" \
                    -v "$DOCKER_DIR/ai/tmp:/ai-tmp" \
                    -e EUREKA_SERVER=http://kiwi-eureka:18000/eureka/ \
                    -e TZ=Pacific/Auckland \
                    kiwi-ai-batch:latest
            else
                echo "⚠️  AI Batch Dockerfile not found at $DOCKER_DIR/ai/batch/Dockerfile"
            fi

            echo "✅ AI Service deployment completed"
            ;;

        "tools")  # NEW
            echo "🧰 Building Tools Service..."
            if [ ! -d "$DOCKER_DIR/tools" ]; then
                echo "❌ Directory not found: $DOCKER_DIR/tools"
                return 1
            fi
            cd "$DOCKER_DIR/tools"
            docker build -t kiwi-tools-biz:latest .
            docker run -d --name kiwi-tools-biz \
                --network kiwi-network \
                -p 18012:18012 \
                -v "$DOCKER_DIR/tools/logs:/logs" \
                -e EUREKA_SERVER=http://kiwi-eureka:18000/eureka/ \
                kiwi-tools-biz:latest
            echo "✅ Tools Service deployed successfully"
            ;;

        *)
            echo "❌ Unknown service: $service"
            return 1
            ;;
    esac

    # Return to original directory
    cd "$CURRENT_DIR/microservice-kiwi/"
}

# Function to perform selective deployment
selective_deployment() {
    local services="$1"

    echo "=============================================="
    echo "🎯 SELECTIVE SERVICE DEPLOYMENT"
    echo "=============================================="
    echo "Services to deploy: $services"
    echo "Working directory: $CURRENT_DIR"
    echo "=============================================="

    # Prefer docker compose to (re)create only the requested services
    if command -v docker-compose >/dev/null 2>&1; then
        COMPOSE_CMD="docker-compose"
    elif docker compose version >/dev/null 2>&1; then
        COMPOSE_CMD="docker compose"
    else
        COMPOSE_CMD=""
    fi

    # Before using compose, build the images for the selected services (tag :2.0)
    local DOCKER_KIWI_ROOT="$CURRENT_DIR/docker/kiwi"
    IFS=',' read -ra SERVICE_ARRAY <<< "$services"
    for service in "${SERVICE_ARRAY[@]}"; do
        case "$service" in
          eureka)
            [ -d "$DOCKER_KIWI_ROOT/eureka" ] && docker build -f "$DOCKER_KIWI_ROOT/eureka/Dockerfile" -t kiwi-eureka:2.0 "$DOCKER_KIWI_ROOT/eureka" || true ;;
          config)
            [ -d "$DOCKER_KIWI_ROOT/config" ] && docker build -f "$DOCKER_KIWI_ROOT/config/Dockerfile" -t kiwi-config:2.0 "$DOCKER_KIWI_ROOT/config" || true ;;
          upms)
            [ -d "$DOCKER_KIWI_ROOT/upms" ] && docker build -f "$DOCKER_KIWI_ROOT/upms/Dockerfile" -t kiwi-upms:2.0 "$DOCKER_KIWI_ROOT/upms" || true ;;
          auth)
            [ -d "$DOCKER_KIWI_ROOT/auth" ] && docker build -f "$DOCKER_KIWI_ROOT/auth/Dockerfile" -t kiwi-auth:2.0 "$DOCKER_KIWI_ROOT/auth" || true ;;
          gate)
            [ -d "$DOCKER_KIWI_ROOT/gate" ] && docker build -f "$DOCKER_KIWI_ROOT/gate/Dockerfile" -t kiwi-gate:2.0 "$DOCKER_KIWI_ROOT/gate" || true ;;
          word)
            [ -d "$DOCKER_KIWI_ROOT/word/biz" ] && docker build -f "$DOCKER_KIWI_ROOT/word/biz/Dockerfile" -t kiwi-word-biz:2.0 "$DOCKER_KIWI_ROOT/word" || true ;;
          crawler)
            [ -d "$DOCKER_KIWI_ROOT/crawler" ] && docker build -f "$DOCKER_KIWI_ROOT/crawler/Dockerfile" -t kiwi-crawler:2.0 "$DOCKER_KIWI_ROOT/crawler" || true ;;
          ai)
            [ -d "$DOCKER_KIWI_ROOT/ai/biz" ] && docker build -f "$DOCKER_KIWI_ROOT/ai/biz/Dockerfile" -t kiwi-ai-biz:2.0 "$DOCKER_KIWI_ROOT/ai/biz" || true
            [ -d "$DOCKER_KIWI_ROOT/ai/batch" ] && docker build -f "$DOCKER_KIWI_ROOT/ai/batch/Dockerfile" -t kiwi-ai-biz-batch:2.0 "$DOCKER_KIWI_ROOT/ai/batch" || true ;;
          tools)
            [ -d "$DOCKER_KIWI_ROOT/tools" ] && docker build -f "$DOCKER_KIWI_ROOT/tools/Dockerfile" -t kiwi-tools-biz:2.0 "$DOCKER_KIWI_ROOT/tools" || true ;;
          *)
            echo "⚠️  Unknown service key '$service' during build; skipping build" ;;
        esac
    done

    if [ -n "$COMPOSE_CMD" ]; then
        # Map short service keys to compose service names
        declare -A COMPOSE_SERVICES=(
            [eureka]="kiwi-eureka"
            [config]="kiwi-config"
            [upms]="kiwi-upms"
            [auth]="kiwi-auth"
            [gate]="kiwi-gate"
            [word]="kiwi-word-biz"
            [crawler]="kiwi-crawler"
            [ai]="kiwi-ai-biz"
            [tools]="kiwi-tools-biz"
        )

        # Split targets into base vs service lists
        BASE_TARGETS=()
        APP_TARGETS=()
        for service in "${SERVICE_ARRAY[@]}"; do
            case "$service" in
              eureka|config)
                BASE_TARGETS+=("${COMPOSE_SERVICES[$service]}")
                ;;
              upms|auth|gate|word|crawler|ai|tools)
                APP_TARGETS+=("${COMPOSE_SERVICES[$service]}")
                ;;
              *)
                echo "⚠️  Unknown service key '$service' for docker compose; skipping"
                ;;
            esac
        done

        # Compose files
        local BASE_YML="$CURRENT_DIR/microservice-kiwi/kiwi-deploy/docker/docker-compose-base.yml"
        local SERV_YML="$CURRENT_DIR/microservice-kiwi/kiwi-deploy/docker/docker-compose-service.yml"

        # Bring up base services (no deps)
        if [ ${#BASE_TARGETS[@]} -gt 0 ]; then
            if [ ! -f "$BASE_YML" ]; then
              echo "❌ Missing base compose file: $BASE_YML"; return 1
            fi
            echo "🧩 docker compose (project=kiwi-base): ${BASE_TARGETS[*]}"
            $COMPOSE_CMD --project-name kiwi-base -f "$BASE_YML" up -d --no-deps --force-recreate --remove-orphans "${BASE_TARGETS[@]}"
        fi

        # Bring up application services (no deps)
        if [ ${#APP_TARGETS[@]} -gt 0 ]; then
            if [ ! -f "$SERV_YML" ]; then
              echo "❌ Missing service compose file: $SERV_YML"; return 1
            fi
            echo "🧩 docker compose (project=kiwi-service): ${APP_TARGETS[*]}"
            $COMPOSE_CMD --project-name kiwi-service -f "$SERV_YML" up -d --no-deps --force-recreate --remove-orphans "${APP_TARGETS[@]}"
        fi
    else
        echo "ℹ️  docker compose not found; falling back to legacy per-service build/run"
        for service in "${SERVICE_ARRAY[@]}"; do
            echo ""
            deploy_single_service "$service"
        done
    fi

    echo ""
    echo "=============================================="
    echo "🎉 SELECTIVE DEPLOYMENT COMPLETED"
    echo "Deployed services: $services"
    echo "=============================================="

    # Show running containers
    echo ""
    docker ps --filter "name=kiwi-" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
}

# Core deployment operations (modified to skip the redundant stopAll call)
echo "=============================================="
echo "🚀 STARTING CONTAINER DEPLOYMENT:"
echo "=============================================="

# Safety guard: never proceed to autoDeploy in build-only mode
if [ "$ONLY_BUILD_MAVEN" = true ]; then
  echo "Guard: ONLY_BUILD_MAVEN=true detected before autoDeploy. Exiting now."
  exit 0
fi

# Build Docker images and deploy
if [ "$BUILD_ALL_SERVICES" = true ]; then
  # Safety guard: if somehow ONLY_BUILD_MAVEN is still true, do not continue
  if [ "$ONLY_BUILD_MAVEN" = true ]; then
    echo "OBM mode: skipping auto deployment."
    exit 0
  fi

  echo "🚀 Starting auto deployment for all services..."
  AUTO_DEPLOY_SCRIPT="$CURRENT_DIR/microservice-kiwi/kiwi-deploy/docker/autoDeploy.sh"
  if [ ! -f "$AUTO_DEPLOY_SCRIPT" ]; then
     echo "❌ autoDeploy script not found at: $AUTO_DEPLOY_SCRIPT"
     echo "Current directory: $(pwd)"
     ls -l "$CURRENT_DIR/microservice-kiwi/kiwi-deploy/docker/" || true
     exit 1
  fi
  chmod +x "$AUTO_DEPLOY_SCRIPT" || true
  echo "➡️  Invoking: $AUTO_DEPLOY_SCRIPT $MODE"
  echo "   (Logging to $CURRENT_DIR/autoDeploy.log)"
  # Ensure children also see mode flags
  KIWI_DEPLOY_MODE="$MODE" ONLY_BUILD_MAVEN="$ONLY_BUILD_MAVEN" bash -x "$AUTO_DEPLOY_SCRIPT" "$MODE" 2>&1 | tee "$CURRENT_DIR/autoDeploy.log"
  echo "✅ autoDeploy completed"
else
  # For selective deployment, we still need to stop/remove only selected services
  echo "🛑 Stopping selected services: $SELECTED_SERVICES"
  stop_selected_containers "$SELECTED_SERVICES"

  echo "🗑️  Removing selected containers: $SELECTED_SERVICES"
  remove_selected_containers "$SELECTED_SERVICES"

  # Selective deployment
  selective_deployment "$SELECTED_SERVICES"
fi

# AutoCheck Service Logic - Added at the end as requested
echo "=============================================="
echo "AUTO CHECK SERVICE CONFIGURATION:"
echo "=============================================="

if [ "$ENABLE_AUTO_CHECK" = true ]; then
  if ! pgrep -f "autoCheckService.sh" >/dev/null; then
    echo "🔄 Starting autoCheckService..."
    nohup "$CURRENT_DIR/microservice-kiwi/kiwi-deploy/docker/autoCheckService.sh" >"$CURRENT_DIR/autoCheck.log" 2>&1 &
    echo "✅ AutoCheckService started successfully"
    echo "🔄 AutoCheckService is now running in the background"
    echo "📄 Log file: $CURRENT_DIR/autoCheck.log"
  else
    echo "ℹ️  AutoCheckService is already running"
    echo "✅ AutoCheckService status: ACTIVE"
  fi
else
  echo "ℹ️  You are NOT using auto check mode this time"
  echo "❌ AutoCheckService will not be started"
  echo "💡 To enable AutoCheckService, add -c parameter to your command"
fi

echo "=============================================="
if [ "$BUILD_ALL_SERVICES" = false ]; then
  echo "🎉 SELECTIVE DEPLOYMENT COMPLETED SUCCESSFULLY!"
  echo "✅ Deployed services: $SELECTED_SERVICES"
else
  echo "🎉 FULL DEPLOYMENT COMPLETED SUCCESSFULLY!"
fi
echo "=============================================="

