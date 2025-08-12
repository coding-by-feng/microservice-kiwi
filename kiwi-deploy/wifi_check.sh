#!/bin/bash

LOG_FILE="$HOME/wifi_check.log"
CONFIG_FILE="$HOME/.wifi_check_config"

# Check if script is already running under nohup (child)
if [ "$1" != "nohup-child" ]; then
  echo "Starting script in background with nohup, logging to $LOG_FILE"
  nohup "$0" nohup-child > "$LOG_FILE" 2>&1 &
  echo "Script is running in background (PID $!). Logs: $LOG_FILE"
  exit 0
fi

# The rest of your script runs here as the nohup-child

DEFAULT_SSID="SPARK-P8EJZP"
DEFAULT_PASS="JoyfulEmuUU58?"

function connect_wifi() {
  local ssid=$1
  local password=$2
  echo "$(date): Trying to connect to Wi-Fi: $ssid"

  nmcli connection show "$ssid" &>/dev/null
  if [ $? -ne 0 ]; then
    nmcli dev wifi connect "$ssid" password "$password"
  else
    nmcli connection up "$ssid"
  fi
}

function check_wifi_connected() {
  nmcli -t -f WIFI g | grep -q "enabled"
  if [ $? -ne 0 ]; then
    echo "$(date): Wi-Fi is disabled"
    return 1
  fi

  local connected_ssid
  connected_ssid=$(nmcli -t -f ACTIVE,SSID dev wifi | grep '^yes' | cut -d: -f2)
  if [ -z "$connected_ssid" ]; then
    echo "$(date): Not connected to any Wi-Fi"
    return 1
  fi

  ping -c 2 -W 2 8.8.8.8 >/dev/null 2>&1
  if [ $? -ne 0 ]; then
    echo "$(date): No network connectivity"
    return 1
  fi

  return 0
}

if [ ! -f "$CONFIG_FILE" ]; then
  echo "Config file not found, using default Wi-Fi credentials."
  WIFI_SSID="$DEFAULT_SSID"
  WIFI_PASS="$DEFAULT_PASS"
  echo "$WIFI_SSID" > "$CONFIG_FILE"
  echo "$WIFI_PASS" >> "$CONFIG_FILE"
else
  WIFI_SSID=$(sed -n '1p' "$CONFIG_FILE")
  WIFI_PASS=$(sed -n '2p' "$CONFIG_FILE")
fi

connect_wifi "$WIFI_SSID" "$WIFI_PASS"
sleep 5

if check_wifi_connected; then
  echo "$(date): Wi-Fi connected successfully to $WIFI_SSID. Starting monitoring..."
else
  echo "$(date): Failed to connect to Wi-Fi. Exiting."
  exit 1
fi

while true; do
  if check_wifi_connected; then
    echo "$(date): Wi-Fi is connected."
  else
    echo "$(date): Wi-Fi disconnected. Trying to reconnect..."
    connect_wifi "$WIFI_SSID" "$WIFI_PASS"
  fi
  sleep 30
done