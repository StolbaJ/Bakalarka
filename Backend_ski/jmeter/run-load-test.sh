#!/usr/bin/env bash
# Spustí JMeter test v CLI a vygeneruje HTML report.
# Použití: ./run-load-test.sh [ smoke | load | long ]
# Před spuštěním musí běžet backend.

set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

JMETER_CMD="${JMETER_CMD:-jmeter}"
RESULTS="${RESULTS:-results.jtl}"
REPORT_DIR="${REPORT_DIR:-report}"

case "${1:-load}" in
  smoke)  JMX="ski-inventory-smoke.jmx" ;;
  long)   JMX="ski-inventory-long.jmx" ;;
  load|*) JMX="ski-inventory-load.jmx" ;;
esac

rm -f "$RESULTS"
rm -rf "$REPORT_DIR"

echo "Spouštím: $JMX"
echo "Výstup: $RESULTS, report: $REPORT_DIR/"
echo "Backend musí běžet na 127.0.0.1:8080"
echo ""

"$JMETER_CMD" -n -t "$JMX" -l "$RESULTS" -e -o "$REPORT_DIR"

echo ""
echo "Hotovo. Otevři: open $REPORT_DIR/index.html"
