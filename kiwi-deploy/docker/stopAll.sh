#!/bin/bash

# Stop autoCheckService if running
if pgrep -f "autoCheckService.sh" >/dev/null; then
  echo "Stopping autoCheckService..."
  pkill -f "autoCheckService.sh"
fi

echo "Stopping containers..."

# Stop all kiwi-* containers with one command
for container in $(podman ps -a | grep -E "kiwi-crawler|kiwi-word-biz|kiwi-upms|kiwi-auth|kiwi-gate|kiwi-config|kiwi-eureka" | awk '{print $1}'); do
    [ -n "$container" ] && podman stop "$container"
done

echo "Containers stopped successfully"