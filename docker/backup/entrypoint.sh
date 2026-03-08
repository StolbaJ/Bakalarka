#!/bin/sh
# První záloha po START_DELAY s (aby stack dojel), pak každých 24 h
INTERVAL_SEC="${BACKUP_INTERVAL_SEC:-86400}"
START_DELAY="${BACKUP_START_DELAY:-120}"

run_backup() {
  /backup.sh
}

echo "[$(date '+%Y-%m-%dT%H:%M:%S')] Backup služba startuje (první záloha za ${START_DELAY}s, pak každých ${INTERVAL_SEC}s)"
sleep "$START_DELAY"
run_backup

while true; do
  sleep "$INTERVAL_SEC"
  run_backup
done
