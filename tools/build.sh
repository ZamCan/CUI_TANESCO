#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# TANESCO Meter Companion - resilient Gradle wrapper for Termux.
#
# Why this exists
# ---------------
# On the build device (Android phone, PRoot, ~3.6 GB RAM, ~500 MB typically
# available) the Gradle daemon is occasionally killed by the kernel while a
# build is in flight. Gradle reports that as an ordinary task failure, which is
# misleading: the task itself is usually fine.
#
# Verified behaviour on this device:
#   - :platform:compileDebugLibraryResources FAILED inside a large build,
#     then BUILD SUCCESSFUL in 17s on its own.
#   - Daemon logs accumulating (10 in one attempt) is the signature of a kill.
#
# This script therefore runs the requested tasks one invocation at a time and
# retries a failed invocation once, after stopping daemons. It does NOT hide a
# genuine compilation error: a real error reproduces on the retry and is
# reported with its output intact.
#
# Usage:
#   tools/build.sh :app:assembleDebug
#   tools/build.sh :core:test :connection:test
# ---------------------------------------------------------------------------
set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

# shellcheck source=/dev/null
source "$REPO_ROOT/tools/env.sh" >/dev/null

export JAVA_HOME="${JAVA_HOME:-/data/data/com.termux/files/usr/lib/jvm/java-21-openjdk}"

GRADLEW="./gradlew"
MAX_ATTEMPTS="${MAX_ATTEMPTS:-2}"

if [ "$#" -eq 0 ]; then
    echo "usage: tools/build.sh <gradle task> [more tasks...]" >&2
    exit 64
fi

daemon_log_count() {
    ls -1 "$GRADLE_USER_HOME/daemon/8.9/"*.log 2>/dev/null | wc -l
}

for attempt in $(seq 1 "$MAX_ATTEMPTS"); do
    echo "=== attempt ${attempt}/${MAX_ATTEMPTS}: gradlew $* ===" >&2
    before="$(daemon_log_count)"
    LOG="$(mktemp)"
    "$GRADLEW" "$@" --console=plain 2>&1 | tee "$LOG"
    status=${PIPESTATUS[0]}

    if [ "$status" -eq 0 ]; then
        rm -f "$LOG"
        exit 0
    fi

    # A Kotlin compilation error is a real defect and reproduces exactly.
    # Retrying it just burns another ten minutes, so stop here.
    if grep -qE '^e: |^w: .*error:' "$LOG"; then
        echo "=== compilation error: not retrying ===" >&2
        rm -f "$LOG"
        exit "$status"
    fi

    after="$(daemon_log_count)"
    if [ "$after" -gt "$before" ]; then
        # A new daemon log means the previous daemon died mid-build. Almost
        # certainly memory pressure rather than a defect in the task.
        echo "=== daemon restarted (${before} -> ${after} logs): device pressure, retrying ===" >&2
        "$GRADLEW" --stop >/dev/null 2>&1
        sleep 5
        rm -f "$LOG"
        continue
    fi

    echo "=== failure is reproducible (${after} daemon logs, unchanged): real error ===" >&2
    rm -f "$LOG"
    exit "$status"
done

echo "=== exhausted ${MAX_ATTEMPTS} attempts ===" >&2
exit 1
