#!/bin/sh
# wait-for-config.sh
# Usage: sh wait-for-config.sh [HEALTH_URL] [TIMEOUT_SECONDS]
# Defaults: HEALTH_URL=http://localhost:7771/actuator/health, TIMEOUT_SECONDS=0 (infinite)

HEALTH_URL="${1:-http://localhost:7771/actuator/health}"
TIMEOUT="${2:-0}"
WAIT=0

echo "Waiting for config server to be healthy at ${HEALTH_URL} ..."

while : ; do
  code=$(curl -s -o /dev/null -w '%{http_code}' "${HEALTH_URL}" 2>/dev/null || echo 000)
  if [ "${code}" = "200" ] || [ "${code}" = "401" ] || [ "${code}" = "403" ]; then
    echo "Config server is healthy (HTTP ${code})."
    exit 0
  fi

  WAIT=$((WAIT+1))
  if [ "${TIMEOUT}" -gt 0 ] && [ "${WAIT}" -ge "${TIMEOUT}" ]; then
    echo "Timed out waiting for config server after ${WAIT}s (last HTTP ${code})." >&2
    exit 1
  fi

  if [ $((WAIT % 5)) -eq 0 ]; then
    echo "Still waiting for config server... (${WAIT}s)"
  fi
  sleep 1
done

