#!/bin/bash

LOG_FILE="$HOME/wifi_check.log"
CONFIG_FILE="$HOME/.wifi_check_config"

if [ "$1" != "nohup-child" ]; then
  echo "Starting script in background with nohup, logging to $LOG_FILE"
  nohup "$0" nohup-child > "$LOG_FILE" 2>&1 &
  echo "Script is running in background (PID $!). Logs: $LOG_FILE"
  exit 0
fi

# No default values - user must provide credentials on first run

function connect_wifi() {
  local ssid=$1
  local password=$2
  local bssid=$3
  echo "$(date): Trying to connect to Wi-Fi: $ssid (BSSID: $bssid)"

  # Remove old connection if exists
  local old_conn
  old_conn=$(nmcli -t -f NAME,TYPE connection show | grep "^$ssid:wifi$" | cut -d: -f1)
  if [ -n "$old_conn" ]; then
    echo "$(date): Removing old connection profile: $old_conn"
    nmcli connection delete "$old_conn"
  fi

  nmcli dev wifi connect "$ssid" bssid "$bssid" password "$password"
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

function prompt_wifi_credentials() {
  echo "=== WiFi Configuration Setup ==="
  echo "Please enter your WiFi credentials:"

  read -p "WiFi SSID (network name): " input_ssid
  read -s -p "WiFi Password: " input_password
  echo
  read -p "WiFi BSSID (MAC address, e.g., 20:37:F0:9E:A2:D7): " input_bssid

  # Validate BSSID format (basic check)
  if [[ ! "$input_bssid" =~ ^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$ ]]; then
    echo "Warning: BSSID format may be incorrect. Expected format: XX:XX:XX:XX:XX:XX"
    read -p "Continue anyway? (y/N): " confirm
    if [[ ! "$confirm" =~ ^[Yy]$ ]]; then
      echo "Setup cancelled."
      exit 1
    fi
  fi

  # Save to config file
  echo "$input_ssid" > "$CONFIG_FILE"
  echo "$input_password" >> "$CONFIG_FILE"
  echo "$input_bssid" >> "$CONFIG_FILE"

  echo "Configuration saved to $CONFIG_FILE"

  # Set environment variables
  WIFI_SSID="$input_ssid"
  WIFI_PASS="$input_password"
  WIFI_BSSID="$input_bssid"
}

# Load or prompt for WiFi configuration
if [ ! -f "$CONFIG_FILE" ]; then
  echo "Config file not found. Setting up WiFi configuration..."
  prompt_wifi_credentials
else
  WIFI_SSID=$(sed -n '1p' "$CONFIG_FILE")
  WIFI_PASS=$(sed -n '2p' "$CONFIG_FILE")
  WIFI_BSSID=$(sed -n '3p' "$CONFIG_FILE")

  # Check if BSSID exists in config (for backward compatibility)
  if [ -z "$WIFI_BSSID" ]; then
    echo "BSSID not found in config file. Please add it:"
    read -p "WiFi BSSID (MAC address, e.g., 20:37:F0:9E:A2:D7): " input_bssid
    echo "$input_bssid" >> "$CONFIG_FILE"
    WIFI_BSSID="$input_bssid"
  fi
fi

# Export as environment variables
export WIFI_SSID
export WIFI_PASS
export WIFI_BSSID

echo "$(date): Using WiFi credentials - SSID: $WIFI_SSID, BSSID: $WIFI_BSSID"

# Initial connection attempt
until connect_wifi "$WIFI_SSID" "$WIFI_PASS" "$WIFI_BSSID" && sleep 5 && check_wifi_connected; do
  echo "$(date): Initial connection failed, retrying in 10 seconds..."
  sleep 10
done

echo "$(date): Wi-Fi connected successfully to $WIFI_SSID. Setting DNS..."

set_google_dns "$WIFI_SSID"

echo "$(date): Starting monitoring..."

# Main monitoring loop
while true; do
  if check_wifi_connected; then
    echo "$(date): Wi-Fi is connected."
  else
    echo "$(date): Wi-Fi disconnected. Trying to reconnect..."
    connect_wifi "$WIFI_SSID" "$WIFI_PASS" "$WIFI_BSSID"
    # After reconnecting, reset DNS again
    set_google_dns "$WIFI_SSID"
  fi
  sleep 20
done