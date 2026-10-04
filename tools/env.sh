#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# TANESCO Meter Companion - Termux build environment
#
# Source this before invoking Gradle:
#     source tools/env.sh && ./gradlew :app:assembleDebug
#
# It resolves the Android SDK from the Termux prefix, reusing the SDK and the
# Gradle module cache that already exist on this device instead of
# re-downloading the toolchain.
# ---------------------------------------------------------------------------

TERMUX_HOME="${TERMUX_HOME:-/data/data/com.termux/files/home}"
SDK_ROOT="${ANDROID_HOME:-$TERMUX_HOME/android-sdk}"

if [ ! -d "$SDK_ROOT/platforms" ]; then
  echo "env.sh: Android SDK not found at $SDK_ROOT" >&2
  echo "env.sh: set ANDROID_HOME to your SDK location and re-run." >&2
  return 1 2>/dev/null || exit 1
fi

export ANDROID_HOME="$SDK_ROOT"
export ANDROID_SDK_ROOT="$SDK_ROOT"

# Reuse the populated Gradle home so AGP / Kotlin artifacts already on the
# device are reused rather than fetched again.
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-$TERMUX_HOME/.gradle}"

# PRoot + low-memory device: keep the JVM small and single-purpose.
export GRADLE_OPTS="${GRADLE_OPTS:--Xmx512m -Dfile.encoding=UTF-8}"

echo "ANDROID_HOME   = $ANDROID_HOME"
echo "GRADLE_USER_HOME = $GRADLE_USER_HOME"
