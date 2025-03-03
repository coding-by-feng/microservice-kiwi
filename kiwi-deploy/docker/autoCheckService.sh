#!/bin/bash

RUNNING_CODE=200
CHECK_INTERVAL=15
RESTART_DELAY=60

while true; do
  crawler_code=$(curl -I -m 10 -o /dev/null -s -w "%{http_code}" http://kiwi-crawler:6001/actuator/info)
  current_time=$(date "+%Y-%m-%d %H:%M:%S")

  if [ "$crawler_code" != "$RUNNING_CODE" ]; then
    echo "$current_time Word-Crawler Service not running (code: $crawler_code), restarting..."
    crawler_id=$(podman ps -a -q --filter "name=kiwi-crawler")
    [ -n "$crawler_id" ] && podman start "$crawler_id" && echo "$current_time Word-Crawler restarted"
    sleep "$RESTART_DELAY"
  else
    echo "$current_time Word-Crawler Service is running (code: $crawler_code)"
    sleep "$CHECK_INTERVAL"
  fi
done