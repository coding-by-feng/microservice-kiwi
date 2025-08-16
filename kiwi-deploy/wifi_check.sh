#!/bin/bash

LOG_FILE="$HOME/wifi_check.log"
CONFIG_FILE="$HOME/.wifi_check_config"

if [ "$1" != "nohup-child" ]; then
  echo "Starting script in background with nohup, logging to $LOG_FILE"
  nohup "$0" nohup-child > "$LOG_FILE" 2>&1 &
  echo "Script is running in background (PID $!). Logs: $LOG_FILE"
  exit 0
fi

DEFAULT_SSID="SPARK-P8EJZP"
DEFAULT_PASS="JoyfulEmuUU58?"

function connect_wifi() {
  local ssid=$1
  local password=$2
  echo "$(date): Trying to connect to Wi-Fi: $ssid"

  # Remove old connection if exists
  local old_conn
  old_conn=$(nmcli -t -f NAME,TYPE connection show | grep "^$ssid:wifi$" | cut -d: -f1)
  if [ -n "$old_conn" ]; then
    echo "$(date): Removing old connection profile: $old_conn"
    nmcli connection delete "$old_conn"
  fi

  nmcli dev wifi connect "$ssid" password "$password"
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

function set_google_dns() {
  local ssid=$1
  echo "$(date): Setting Google DNS for connection: $ssid"
  nmcli connection modify "$ssid" ipv4.dns "8.8.8.8 8.8.4.4"
  nmcli connection modify "$ssid" ipv4.ignore-auto-dns yes
  nmcli connection up "$ssid"
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

until connect_wifi "$WIFI_SSID" "$WIFI_PASS" && sleep 5 && check_wifi_connected; do
  echo "$(date): Initial connection failed, retrying in 10 seconds..."
  sleep 10
done

echo "$(date): Wi-Fi connected successfully to $WIFI_SSID. Setting DNS..."

set_google_dns "$WIFI_SSID"

echo "$(date): Starting monitoring..."

while true; do
  if check_wifi_connected; then
    echo "$(date): Wi-Fi is connected."
  else
    echo "$(date): Wi-Fi disconnected. Trying to reconnect..."
    connect_wifi "$WIFI_SSID" "$WIFI_PASS"
    # After reconnecting, reset DNS again
    set_google_dns "$WIFI_SSID"
  fi
  sleep 20
done