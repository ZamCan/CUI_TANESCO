#!/usr/bin/env bash
set -uo pipefail
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"
source "$REPO_ROOT/tools/env.sh" >/dev/null
export JAVA_HOME="${JAVA_HOME:-/data/data/com.termux/files/usr/lib/jvm/java-21-openjdk}"
GRADLEW="./gradlew"
MAX_ATTEMPTS="${MAX_ATTEMPTS:-2}"
TERMUX_AAPT2="/data/data/com.termux/files/usr/bin/aapt2"

if [ "$#" -eq 0 ]; then
  echo "usage: tools/build.sh <gradle task> [more tasks...]" >&2
  exit 64
fi

daemon_log_count() { ls -1 "$GRADLE_USER_HOME/daemon/8.9/"*.log 2>/dev/null | wc -l; }
for attempt in $(seq 1 "$MAX_ATTEMPTS"); do
  echo "=== attempt ${attempt}/${MAX_ATTEMPTS}: gradlew $* ===" >&2
  before="$(daemon_log_count)"
  LOG="$(mktemp)"
  if [ -x "$TERMUX_AAPT2" ]; then
    "$GRADLEW" -Dandroid.aapt2FromMavenOverride="$TERMUX_AAPT2" "$@" --console=plain 2>&1 | tee "$LOG"
  else
    "$GRADLEW" "$@" --console=plain 2>&1 | tee "$LOG"
  fi
  status=${PIPESTATUS[0]}
  if [ "$status" -eq 0 ]; then rm -f "$LOG"; exit 0; fi
  if grep -qE '^e: |^w: .*error:' "$LOG"; then
    echo "=== compilation error: not retrying ===" >&2; rm -f "$LOG"; exit "$status"
  fi
  after="$(daemon_log_count)"
  if [ "$after" -gt "$before" ]; then
    echo "=== daemon restarted (${before} -> ${after} logs): possible device pressure, retrying ===" >&2
    "$GRADLEW" --stop >/dev/null 2>&1; sleep 5; rm -f "$LOG"; continue
  fi
  echo "=== failure is reproducible (${after} daemon logs, unchanged): real error ===" >&2
  rm -f "$LOG"; exit "$status"
done
echo "=== exhausted ${MAX_ATTEMPTS} attempts ===" >&2
exit 1
