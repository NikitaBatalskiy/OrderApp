#!/bin/sh
# wait-for-url.sh URL [TIMEOUT_SECONDS]
url="$1"
timeout="${2:-120}"

echo "Waiting for $url (timeout ${timeout}s)..."

start_time=$(date +%s)
end_time=$((start_time + timeout))

while [ $(date +%s) -lt $end_time ]; do
  if wget -q --spider "$url" 2>/dev/null; then
    echo "URL is reachable."
    exit 0
  fi
  sleep 1
done

echo "Timeout waiting for $url"
exit 1