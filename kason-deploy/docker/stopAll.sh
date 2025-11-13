#!/bin/bash

# --- Permission bootstrap (self-healing) ---
{
  SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
  DEPLOY_ROOT="$(cd "$SCRIPT_DIR/.." && pwd 2>/dev/null || echo "$SCRIPT_DIR")"
  # Ensure all .sh under kason-deploy are executable
  if [ -d "$DEPLOY_ROOT" ]; then
    find "$DEPLOY_ROOT" -type f -name "*.sh" -exec chmod 777 {} \; 2>/dev/null || true
  fi
  # Also ensure common helper binaries are executable (yt-dlp_linux if present)
  ORIG_USER="${SUDO_USER:-$USER}"
  ORIG_HOME=$(eval echo "~$ORIG_USER")
  for f in "$ORIG_HOME"/docker/kason/ai/*/yt-dlp_linux "$ORIG_HOME"/docker/kason/ai/yt-dlp_linux; do
    [ -e "$f" ] && chmod +x "$f" || true
  done
} >/dev/null 2>&1 || true

# Stop autoCheckService if running
if pgrep -f "autoCheckService.sh" >/dev/null; then
  echo "Stopping autoCheckService..."
  pkill -f "autoCheckService.sh"
  echo "✅ AutoCheckService stopped"
else
  echo "ℹ️  AutoCheckService is not running"
fi

echo "=============================================="
echo "STOPPING CONTAINERS..."
echo "=============================================="

# Define the desired stop order
desired_order=("kason-tools-biz" "kason-crawler" "kason-ai-biz" "kason-ai-biz-batch" "kason-word-biz" "kason-tools-biz" "kason-upms" "kason-auth" "kason-gate" "kason-config" "kason-eureka")  # NEW: kason-tools-biz

# Stop containers in the specified order
for name in "${desired_order[@]}"; do
    container_id=$(docker ps -a --filter "name=$name" --format "{{.ID}}")
    if [ -n "$container_id" ]; then
        echo "Stopping container: $name (ID: $container_id)"
        # Use --timeout instead of deprecated --time flag
        docker stop --timeout=30 "$container_id"
        echo "✅ Stopped: $name"
    else
        echo "ℹ️  Container not found: $name"
    fi
done

echo "✅ All containers stopped successfully"

echo "=============================================="
echo "REMOVING CONTAINERS..."
echo "=============================================="

# Force remove containers if they exist
for name in "${desired_order[@]}"; do
    container_id=$(docker ps -a --filter "name=$name" --format "{{.ID}}")
    if [ -n "$container_id" ]; then
        echo "Removing container: $name (ID: $container_id)"
        docker rm -f "$container_id"
        echo "✅ Removed: $name"
    else
        echo "ℹ️  Container not found for removal: $name"
    fi
done

echo "=============================================="
echo "🎉 CLEANUP COMPLETED SUCCESSFULLY!"
echo "=============================================="

