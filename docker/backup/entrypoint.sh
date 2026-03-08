#!/bin/sh
# První záloha hned, pak každých 24 h
INTERVAL_SEC="${BACKUP_INTERVAL_SEC:-86400}"

run_backup() {
  /backup.sh
}

echo "[$(date '+%Y-%m-%dT%H:%M:%S')] Backup služba startuje (interval ${INTERVAL_SEC}s = 24h)"
run_backup

while true; do
  sleep "$INTERVAL_SEC"
  run_backup
done
