#!/bin/bash

CONFIG_FILE="$HOME/.wifi_check_config"

function connect_wifi() {
  local ssid=$1
  local password=$2
  echo "Trying to connect to Wi-Fi: $ssid"

  # Check if connection profile exists
  nmcli connection show "$ssid" &>/dev/null
  if [ $? -ne 0 ]; then
    # Add new Wi-Fi connection profile
    nmcli dev wifi connect "$ssid" password "$password"
  else
    # Try to bring connection up
    nmcli connection up "$ssid"
  fi
}

function check_wifi_connected() {
  # Check if any wifi device is connected and has an IP
  # You can also check connectivity by pinging Google DNS
  nmcli -t -f WIFI g | grep -q "enabled"
  if [ $? -ne 0 ]; then
    echo "Wi-Fi is disabled"
    return 1
  fi

  # Check if connected to the specified SSID
  local connected_ssid
  connected_ssid=$(nmcli -t -f ACTIVE,SSID dev wifi | grep '^yes' | cut -d: -f2)
  if [ -z "$connected_ssid" ]; then
    echo "Not connected to any Wi-Fi"
    return 1
  fi

  # Ping to confirm Internet access
  ping -c 2 -W 2 8.8.8.8 >/dev/null 2>&1
  if [ $? -ne 0 ]; then
    echo "No network connectivity"
    return 1
  fi

  return 0
}

if [ ! -f "$CONFIG_FILE" ]; then
  # First run: ask for Wi-Fi details
  read -p "Enter Wi-Fi SSID: " WIFI_SSID
  read -s -p "Enter Wi-Fi Password: " WIFI_PASS
  echo
  # Save to config file
  echo "$WIFI_SSID" > "$CONFIG_FILE"
  echo "$WIFI_PASS" >> "$CONFIG_FILE"
else
  WIFI_SSID=$(sed -n '1p' "$CONFIG_FILE")
  WIFI_PASS=$(sed -n '2p' "$CONFIG_FILE")
fi

# Try to connect first
connect_wifi "$WIFI_SSID" "$WIFI_PASS"
sleep 5

if check_wifi_connected; then
  echo "Wi-Fi connected successfully to $WIFI_SSID. Starting monitoring..."
else
  echo "Failed to connect to Wi-Fi. Exiting."
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