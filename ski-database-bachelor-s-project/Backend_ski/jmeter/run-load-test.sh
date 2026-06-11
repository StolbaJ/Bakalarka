#!/usr/bin/env bash
# Spustí JMeter test v CLI a vygeneruje HTML report.
# Použití: ./run-load-test.sh [ smoke | load | long ]
# Cíl (výchozí localhost:8080): BASE_URL=api.example.com PORT=443 PROTOCOL=https ./run-load-test.sh

set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

JMETER_CMD="${JMETER_CMD:-jmeter}"
RESULTS="${RESULTS:-results.jtl}"
REPORT_DIR="${REPORT_DIR:-report}"
BASE_URL="${BASE_URL:-127.0.0.1}"
PORT="${PORT:-8080}"
PROTOCOL="${PROTOCOL:-http}"

case "${1:-load}" in
  smoke)  JMX="ski-inventory-smoke.jmx" ;;
  long)   JMX="ski-inventory-long.jmx" ;;
  load|*) JMX="ski-inventory-load.jmx" ;;
esac

rm -f "$RESULTS"
rm -rf "$REPORT_DIR"

echo "Spouštím: $JMX"
echo "Cíl: $PROTOCOL://$BASE_URL:$PORT"
echo "Výstup: $RESULTS, report: $REPORT_DIR/"
echo ""

"$JMETER_CMD" -n -t "$JMX" -l "$RESULTS" -e -o "$REPORT_DIR" \
  -JBASE_URL="$BASE_URL" -JPORT="$PORT" -JPROTOCOL="$PROTOCOL"

echo ""
echo "Hotovo. Otevři: open $REPORT_DIR/index.html"
