#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="${1:-.}"
KOVER_REPORT="$ROOT_DIR/app/build/reports/kover/reportDebug.xml"
TEST_RESULTS_DIR="$ROOT_DIR/app/build/test-results/testDebugUnitTest"
LINT_REPORT="$ROOT_DIR/app/build/reports/lint-results-debug.xml"
DETEKT_REPORT="$ROOT_DIR/app/build/reports/detekt/detekt.xml"
OUTPUT_DIR="$ROOT_DIR/app/build/reports/quality"
OUTPUT_FILE="$OUTPUT_DIR/quality-summary.md"

mkdir -p "$OUTPUT_DIR"

line_cov_pct="N/A"
line_cov_detail="N/A"
if [[ -f "$KOVER_REPORT" ]]; then
  line_counter="$(grep '<counter type="LINE"' "$KOVER_REPORT" | tail -n 1 || true)"
  if [[ -n "$line_counter" ]]; then
    covered="$(echo "$line_counter" | sed -E 's/.*covered="([0-9]+)".*/\1/')"
    missed="$(echo "$line_counter" | sed -E 's/.*missed="([0-9]+)".*/\1/')"
    total=$((covered + missed))
    if [[ "$total" -gt 0 ]]; then
      line_cov_pct="$(awk -v c="$covered" -v t="$total" 'BEGIN { printf "%.2f", (c/t)*100 }')%"
      line_cov_detail="$covered/$total"
    fi
  fi
fi

tests="N/A"
failures="N/A"
errors="N/A"
skipped="N/A"
if compgen -G "$TEST_RESULTS_DIR/*.xml" > /dev/null; then
  tests="$(grep -h -oE 'tests="[0-9]+"' "$TEST_RESULTS_DIR"/*.xml | awk -F\" '{sum += $2} END {print sum + 0}')"
  failures="$(grep -h -oE 'failures="[0-9]+"' "$TEST_RESULTS_DIR"/*.xml | awk -F\" '{sum += $2} END {print sum + 0}')"
  errors="$(grep -h -oE 'errors="[0-9]+"' "$TEST_RESULTS_DIR"/*.xml | awk -F\" '{sum += $2} END {print sum + 0}')"
  skipped="$(grep -h -oE 'skipped="[0-9]+"' "$TEST_RESULTS_DIR"/*.xml | awk -F\" '{sum += $2} END {print sum + 0}')"
fi

lint_total="N/A"
lint_fatal="N/A"
lint_error="N/A"
lint_warning="N/A"
lint_info="N/A"
if [[ -f "$LINT_REPORT" ]]; then
  lint_total="$(grep -c '<issue ' "$LINT_REPORT" || true)"
  lint_fatal="$( (grep -o 'severity="Fatal"' "$LINT_REPORT" || true) | wc -l | tr -d ' ' )"
  lint_error="$( (grep -o 'severity="Error"' "$LINT_REPORT" || true) | wc -l | tr -d ' ' )"
  lint_warning="$( (grep -o 'severity="Warning"' "$LINT_REPORT" || true) | wc -l | tr -d ' ' )"
  lint_info="$( (grep -o 'severity="Informational"' "$LINT_REPORT" || true) | wc -l | tr -d ' ' )"
fi

detekt_total="N/A"
detekt_error="N/A"
detekt_warning="N/A"
detekt_info="N/A"
if [[ -f "$DETEKT_REPORT" ]]; then
  detekt_total="$(grep -c '<error ' "$DETEKT_REPORT" || true)"
  detekt_error="$( (grep -oE 'severity="error"' "$DETEKT_REPORT" || true) | wc -l | tr -d ' ' )"
  detekt_warning="$( (grep -oE 'severity="warning"' "$DETEKT_REPORT" || true) | wc -l | tr -d ' ' )"
  detekt_info="$( (grep -oE 'severity="info"' "$DETEKT_REPORT" || true) | wc -l | tr -d ' ' )"
fi

{
  echo "# Quality Summary"
  echo
  echo "Generated at: $(date -u +"%Y-%m-%d %H:%M:%S UTC")"
  echo
  echo "| Metric | Value |"
  echo "| --- | --- |"
  echo "| Test total | $tests |"
  echo "| Test failures | $failures |"
  echo "| Test errors | $errors |"
  echo "| Test skipped | $skipped |"
  echo "| Line coverage | $line_cov_pct ($line_cov_detail) |"
  echo "| Lint total issues | $lint_total |"
  echo "| Lint fatal | $lint_fatal |"
  echo "| Lint error | $lint_error |"
  echo "| Lint warning | $lint_warning |"
  echo "| Lint info | $lint_info |"
  echo "| Detekt total findings | $detekt_total |"
  echo "| Detekt error | $detekt_error |"
  echo "| Detekt warning | $detekt_warning |"
  echo "| Detekt info | $detekt_info |"
  echo
  echo "Sources:"
  echo "- $KOVER_REPORT"
  echo "- $TEST_RESULTS_DIR"
  echo "- $LINT_REPORT"
  echo "- $DETEKT_REPORT"
} > "$OUTPUT_FILE"

echo "Generated $OUTPUT_FILE"
