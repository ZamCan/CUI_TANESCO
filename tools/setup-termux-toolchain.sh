#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# TANESCO Meter Companion - Termux Android toolchain repair.
#
# THE PROBLEM
# -----------
# The Android SDK installed on this phone contains x86_64 build tools, but the
# phone is aarch64. Every native tool in build-tools/34.0.0 fails to exec:
#
#   $ readelf -l build-tools/34.0.0/aapt2 | grep interpreter
#     [Requesting program interpreter: /lib64/ld-linux-x86-64.so.2]
#
# AGP surfaces this as a confusing resource error:
#
#   Execution failed for task ':app:processDebugResources'
#     > AAPT2 aapt2-8.6.1-...-linux Daemon #0: Daemon startup failed
#   Caused by: java.io.IOException: Cannot run program ".../aapt2":
#              Exec failed, error: 2 (No such file or directory)
#
# "No such file or directory" here means "wrong architecture / missing ELF
# interpreter", not "the file is absent".
#
# THE FIX
# -------
# 1. aapt2  - AGP's own Maven AAPT2 is x86_64 too. Termux packages a correctly
#             built aapt2 against Android's loader (/system/bin/linker64). Point
#             AGP at it with android.aapt2FromMavenOverride (set in
#             gradle.properties).
# 2. zipalign - AGP resolves this from the SDK build-tools directory and offers
#             no override property, so the working binary has to *be* that file.
#             Installed from the Debian package `zipalign` inside PRoot, which is
#             built for aarch64 Linux.
# 3. d8 and apksigner are shell scripts that invoke java, so the x86_64 copies
#     are harmless. aidl is unused (the project has no AIDL interfaces).
#
# Idempotent: safe to re-run. Run once per device, or after an SDK update.
# ---------------------------------------------------------------------------
set -uo pipefail

SDK="${ANDROID_HOME:-/data/data/com.termux/files/home/android-sdk}"
BUILD_TOOLS_VERSION="${BUILD_TOOLS_VERSION:-34.0.0}"
BT="$SDK/build-tools/$BUILD_TOOLS_VERSION"
TERMUX_BIN="/data/data/com.termux/files/usr/bin"
BACKUP_SUFFIX=".x86_64-original"

say() { printf '%s\n' "$*"; }
ok()  { printf '  ok    %s\n' "$*"; }
warn(){ printf '  warn  %s\n' "$*"; }
bad() { printf '  FAIL  %s\n' "$*"; }

[ -d "$BT" ] || { bad "build-tools $BUILD_TOOLS_VERSION not found at $BT"; exit 1; }

say "==> Android toolchain repair ($BT)"

# --- arch probe -------------------------------------------------------------
arch_of() { readelf -h "$1" 2>/dev/null | grep -o 'AArch64\|X86-64'; }

for tool in aapt2 zipalign d8 apksigner; do
    [ -f "$BT/$tool" ] || { warn "$tool absent from build-tools (skipped)"; continue; }
    printf '  %-10s sdk=%s\n' "$tool" "$(arch_of "$BT/$tool" || echo unknown)"
done

# --- 1. aapt2 via gradle.properties ----------------------------------------
if [ -x "$TERMUX_BIN/aapt2" ] && "$TERMUX_BIN/aapt2" version >/dev/null 2>&1; then
    ok "aapt2 (Termux) runs: $("$TERMUX_BIN/aapt2" version | head -1)"
    if grep -q '^android.aapt2FromMavenOverride=' gradle.properties 2>/dev/null; then
        ok "gradle.properties overrides aapt2 to the Termux binary"
    else
        warn "add android.aapt2FromMavenOverride=$TERMUX_BIN/aapt2 to gradle.properties"
    fi
else
    bad "no working aapt2. Install the Termux 'aapt2' package."
fi

# --- 2. zipalign in the SDK build-tools directory ---------------------------
install_zipalign_if_missing() {
    if command -v zipalign >/dev/null 2>&1 && zipalign >/dev/null 2>&1; then
        return 0
    fi
    say "  installing Debian 'zipalign' package (aarch64) ..."
    apt-get install -y zipalign >/dev/null 2>&1
    command -v zipalign >/dev/null 2>&1
}

if install_zipalign_if_missing; then
    SRC="$(command -v zipalign)"
    if [ "$(arch_of "$SRC")" = "AArch64" ]; then
        # Replace the SDK copy with a symlink, keeping the original aside so the
        # change is visible and reversible rather than destructive.
        if [ ! -L "$BT/zipalign" ]; then
            [ -f "$BT/zipalign$BACKUP_SUFFIX" ] || mv "$BT/zipalign" "$BT/zipalign$BACKUP_SUFFIX"
            ln -sf "$SRC" "$BT/zipalign"
        fi
        ok "zipalign -> $SRC (aarch64)"
    else
        bad "the installed zipalign is not aarch64"
    fi
else
    bad "could not obtain a working zipalign"
fi

# --- 3. verify --------------------------------------------------------------
say "==> verification"
# `zipalign` with no arguments prints usage and exits non-zero by design, so
# presence of the usage banner is the correct functional signal here.
if "$BT/zipalign" 2>&1 | grep -q "Zip alignment utility"; then
    ok "zipalign executes"
else
    bad "zipalign does not execute"
fi
"$BT/d8" --help >/dev/null 2>&1 && ok "d8 executes" || warn "d8 check inconclusive (script wrapper)"
"$BT/apksigner" --help >/dev/null 2>&1 && ok "apksigner executes" || warn "apksigner check inconclusive"

say "==> done. Now run: tools/build.sh :app:assembleDebug"