#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# TANESCO Meter Companion - install the built APK on this device.
#
# The primary build machine *is* the target device (an Android phone running
# Termux), but Termux itself is not a package installer. This script therefore:
#
#   1. locates and verifies the freshly built APK
#   2. copies it to shared storage, so it is reachable from the Android file
#      manager and from any file manager the user already uses
#   3. attempts an automated install through whichever route is available
#      (adb with a device attached, or a root-capable pm)
#   4. prints the exact manual command when no automated route exists
#
# Usage:
#   tools/install.sh              # install the debug build
#   tools/install.sh --release    # install the release build
# ---------------------------------------------------------------------------
set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

VARIANT="debug"
[ "${1:-}" = "--release" ] && VARIANT="release"

APK="app/build/outputs/apk/$VARIANT/app-$VARIANT.apk"
SHARED_DIR="${SHARED_DIR:-/sdcard/Download}"

say() { printf '%s\n' "$*"; }
ok()  { printf '  ok    %s\n' "$*"; }
bad() { printf '  FAIL  %s\n' "$*"; }

# --- locate -----------------------------------------------------------------
if [ ! -f "$APK" ]; then
    bad "no APK at $APK"
    say "        build it first:  tools/build.sh :app:assembleDebug"
    exit 1
fi
ok "APK: $APK ($(du -h "$APK" | cut -f1))"

# --- verify -----------------------------------------------------------------
# Reading the package name and version from the APK itself proves the artifact
# is a well-formed, installable package rather than a truncated file.
if command -v aapt2 >/dev/null 2>&1; then
    BADGING="$(aapt2 dump badging "$APK" 2>/dev/null)"
    PKG="$(printf '%s' "$BADGING" | sed -n "s/^package: name='\([^']*\)'.*/\1/p" | head -1)"
    VER="$(printf '%s' "$BADGING" | sed -n "s/^package:.*versionCode='\([^']*\)'.*/\1/p" | head -1)"
    if [ -n "$PKG" ]; then
        ok "package: $PKG (versionCode $VER)"
    else
        bad "could not read the package manifest from the APK"
    fi
else
    bad "aapt2 not on PATH; cannot verify the APK"
fi

# Signature check: an APK whose signature does not verify will not install.
if command -v apksigner >/dev/null 2>&1; then
    if apksigner verify --print-certs "$APK" >/dev/null 2>&1; then
        ok "signature verifies"
    else
        bad "signature does NOT verify - the APK will not install"
    fi
fi

# --- copy to shared storage -------------------------------------------------
if [ -d "$SHARED_DIR" ] && [ -w "$SHARED_DIR" ]; then
    DEST="$SHARED_DIR/TANESCO-Meter-Companion-$VARIANT.apk"
    if cp "$APK" "$DEST" 2>/dev/null; then
        ok "copied to $DEST"
    else
        bad "could not copy to $SHARED_DIR"
    fi
else
    DEST="$APK"
    bad "$SHARED_DIR is not writable; install from $APK"
fi

# --- attempt an automated install ------------------------------------------
say "== attempting automated install =="

# Route 1: adb with a device attached (wireless debugging or USB).
if command -v adb >/dev/null 2>&1; then
    DEVICES="$(adb devices 2>/dev/null | awk 'NR>1 && $2=="device" {print $1}')"
    if [ -n "$DEVICES" ]; then
        for device in $DEVICES; do
            if adb -s "$device" install -r -g "$APK" 2>&1 | tail -3; then
                ok "installed via adb on $device"
                exit 0
            fi
        done
    else
        say "  adb: no device attached"
    fi
fi

# Route 2: a root-capable package manager on the device.
if command -v pm >/dev/null 2>&1 && pm path android >/dev/null 2>&1; then
    if pm install -r "$DEST" 2>&1 | tail -3; then
        ok "installed via pm"
        exit 0
    fi
fi

say "  no automated install route available on this device"
say ""
say "== install manually =="
say "  Option A - file manager (no root needed):"
say "    1. Open the Files app and go to Internal storage > Download."
say "    2. Tap TANESCO-Meter-Companion-$VARIANT.apk"
say "    3. Allow 'install from unknown sources' for the app you use to open it."
say ""
say "  Option B - from Termux, with a host machine attached over adb:"
say "    adb install -r -g $APK"
say ""
say "  Option C - from Termux, on a rooted device:"
say "    su -c 'pm install -r $DEST'"
say ""
say "  Then launch with:"
say "    adb shell am start -n com.tanesco.metercompanion/.MainActivity"
exit 0
