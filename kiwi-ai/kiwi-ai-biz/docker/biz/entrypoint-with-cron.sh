#!/bin/bash
set -euo pipefail

# This entrypoint starts cron for daily yt-dlp updates, then runs the Java app.
# Cron job runs at 00:00 UTC (adjust TZ for container if needed).

YTDLP_BIN="/usr/local/bin/yt-dlp"
export YTDLP_BIN

# Ensure log directory exists
mkdir -p /var/log

# Install cron job if not present
CRON_FILE="/etc/cron.d/yt-dlp-update"
if [ ! -f "$CRON_FILE" ]; then
  echo "0 0 * * * root /usr/local/bin/yt-dlp-auto-update >> /var/log/yt-dlp-update.log 2>&1" > "$CRON_FILE"
  chmod 0644 "$CRON_FILE"
fi

# Ensure cron is running
service cron start || (
  echo "Falling back to crond";
  if command -v crond >/dev/null 2>&1; then
    crond
  fi
)

# Run immediate update on container start to avoid waiting a day
/usr/local/bin/yt-dlp-auto-update || echo "Initial yt-dlp update failed; continuing"

# Launch application
exec java ${JAVA_OPTS:-""} -jar /app/app.jar

