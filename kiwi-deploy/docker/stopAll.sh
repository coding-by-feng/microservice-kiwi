#!/bin/bash

# Stop autoCheckService if running
if pgrep -f "autoCheckService.sh" >/dev/null; then
  echo "Stopping autoCheckService..."
  pkill -f "autoCheckService.sh"
fi

echo "Stopping containers..."

# Stop all kiwi-* containers with one command
docker stop $(docker ps -a -q --filter "name=kiwi-") 2>/dev/null || true
podman stop $(podman ps -a -q --filter "name=kiwi-") 2>/dev/null || true

echo "Containers stopped successfully"