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

function get_best_bssid() {
  local ssid=$1
  echo "$(date): Scanning for best $ssid network..."

  # Scan and get all networks with the target SSID, sorted by signal strength (descending)
  local best_network
  best_network=$(nmcli -t -f BSSID,SIGNAL,FREQ dev wifi list | \
    grep -E "(2\.4|5\.)" | \
    while IFS=':' read -r bssid signal freq rest; do
      # Check if this BSSID belongs to our target SSID
      local network_ssid
      network_ssid=$(nmcli -t -f BSSID,SSID dev wifi list | grep "^$bssid:" | cut -d: -f2)
      if [ "$network_ssid" = "$ssid" ]; then
        # Prefer 5GHz networks by adding bonus to signal strength
        if [ "$freq" -gt 5000 ]; then
          signal=$((signal + 10))  # 5GHz bonus
        fi
        echo "$signal:$bssid:$freq"
      fi
    done | sort -rn | head -1)

  if [ -n "$best_network" ]; then
    local signal=$(echo "$best_network" | cut -d: -f1)
    local bssid=$(echo "$best_network" | cut -d: -f2)
    local freq=$(echo "$best_network" | cut -d: -f3)
    local band="2.4GHz"
    if [ "$freq" -gt 5000 ]; then
      band="5GHz"
      signal=$((signal - 10))  # Remove the bonus for logging
    fi

    echo "$(date): Best network found - BSSID: $bssid, Signal: $signal, Band: $band"
    echo "$bssid"
  else
    echo "$(date): No networks found for $ssid"
    echo ""
  fi
}

function connect_wifi() {
  local ssid=$1
  local password=$2
  local preferred_bssid=$3

  echo "$(date): Trying to connect to Wi-Fi: $ssid"

  # Remove old connection if exists
  local old_conn
  old_conn=$(nmcli -t -f NAME,TYPE connection show | grep "^$ssid:wifi$" | cut -d: -f1)
  if [ -n "$old_conn" ]; then
    echo "$(date): Removing old connection profile: $old_conn"
    nmcli connection delete "$old_conn"
  fi

  # Try to connect to specific BSSID if provided
  if [ -n "$preferred_bssid" ]; then
    echo "$(date): Connecting to specific BSSID: $preferred_bssid"
    nmcli dev wifi connect "$ssid" password "$password" bssid "$preferred_bssid"
  else
    echo "$(date): Connecting to any available $ssid network"
    nmcli dev wifi connect "$ssid" password "$password"
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

function get_current_connection_info() {
  local bssid=$(iwconfig 2>/dev/null | grep "Access Point" | awk '{print $6}' | grep -v "Not-Associated")
  local signal=$(iwconfig 2>/dev/null | grep "Signal level" | awk '{print $4}' | cut -d= -f2)
  local freq=$(iwconfig 2>/dev/null | grep "Frequency" | awk '{print $2}' | cut -d: -f2)

  if [ -n "$bssid" ] && [ -n "$signal" ] && [ -n "$freq" ]; then
    local band="2.4GHz"
    if echo "$freq" | grep -q "^5\."; then
      band="5GHz"
    fi
    echo "$(date): Current connection - BSSID: $bssid, Signal: $signal, Band: $band"
  fi
}

function should_switch_network() {
  local current_signal=$(iwconfig 2>/dev/null | grep "Signal level" | awk '{print $4}' | cut -d= -f2 | sed 's/dBm//')
  local current_freq=$(iwconfig 2>/dev/null | grep "Frequency" | awk '{print $2}' | cut -d: -f2)

  if [ -z "$current_signal" ] || [ -z "$current_freq" ]; then
    return 0  # Switch if we can't get current info
  fi

  # Convert signal to positive number for comparison
  current_signal=${current_signal#-}

  # Check if current connection is poor (signal worse than -75 dBm)
  if [ "$current_signal" -gt 75 ]; then
    echo "$(date): Current signal is weak ($current_signal dBm), looking for better network"
    return 0
  fi

  # If on 2.4GHz and signal is not great, look for 5GHz alternative
  if echo "$current_freq" | grep -q "^2\.4" && [ "$current_signal" -gt 60 ]; then
    echo "$(date): On 2.4GHz with moderate signal, checking for 5GHz alternative"
    return 0
  fi

  return 1  # Don't switch
}

function set_google_dns() {
  local ssid=$1
  echo "$(date): Setting Google DNS for connection: $ssid"
  nmcli connection modify "$ssid" ipv4.dns "8.8.8.8 8.8.4.4"
  nmcli connection modify "$ssid" ipv4.ignore-auto-dns yes
  nmcli connection up "$ssid"
}

# Load configuration
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

# Initial connection with optimization
echo "$(date): Starting optimized Wi-Fi connection..."
best_bssid=$(get_best_bssid "$WIFI_SSID")

until connect_wifi "$WIFI_SSID" "$WIFI_PASS" "$best_bssid" && sleep 5 && check_wifi_connected; do
  echo "$(date): Initial connection failed, retrying in 10 seconds..."
  sleep 10
  # Rescan for best network on retry
  best_bssid=$(get_best_bssid "$WIFI_SSID")
done

echo "$(date): Wi-Fi connected successfully to $WIFI_SSID. Setting DNS..."
get_current_connection_info
set_google_dns "$WIFI_SSID"

echo "$(date): Starting monitoring..."

# Main monitoring loop
check_count=0
while true; do
  if check_wifi_connected; then
    echo "$(date): Wi-Fi is connected."
    get_current_connection_info

    # Every 10 checks (about 3.3 minutes), evaluate if we should switch to a better network
    if [ $((check_count % 10)) -eq 0 ] && should_switch_network; then
      echo "$(date): Checking for better network..."
      best_bssid=$(get_best_bssid "$WIFI_SSID")

      if [ -n "$best_bssid" ]; then
        current_bssid=$(iwconfig 2>/dev/null | grep "Access Point" | awk '{print $6}' | grep -v "Not-Associated")

        if [ "$best_bssid" != "$current_bssid" ]; then
          echo "$(date): Found better network, switching..."
          connect_wifi "$WIFI_SSID" "$WIFI_PASS" "$best_bssid"
          set_google_dns "$WIFI_SSID"
        fi
      fi
    fi

    check_count=$((check_count + 1))
  else
    echo "$(date): Wi-Fi disconnected. Trying to reconnect to best available network..."
    best_bssid=$(get_best_bssid "$WIFI_SSID")
    connect_wifi "$WIFI_SSID" "$WIFI_PASS" "$best_bssid"
    set_google_dns "$WIFI_SSID"
  fi
  sleep 20
done