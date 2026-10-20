# 0009 — Termux toolchain repair: wrong-architecture SDK binaries

**Status:** accepted · **Date:** 2026-10-20 · **Applies to:** the build device only

## Context

The Android SDK installed on the build phone contains **x86_64** build tools,
but the phone is **aarch64**. Every native tool in `build-tools/34.0.0` therefore
fails to exec:

```
$ readelf -l build-tools/34.0.0/aapt2 | grep interpreter
  [Requesting program interpreter: /lib64/ld-linux-x86-64.so.2]
$ readelf -h build-tools/34.0.0/aapt2 | grep Machine
  Machine: Advanced Micro Devices X86-64
```

The failure surfaces in Gradle as something that looks like a missing file:

```
Execution failed for task ':app:processDebugResources'
  > AAPT2 aapt2-8.6.1-11315950-linux Daemon #0: Daemon startup failed
Caused by: java.io.IOException: Cannot run program
  ".../aapt2-8.6.1-11315950-linux/aapt2": Exec failed, error: 2 (No such file or directory)
```

`error: 2` here means **wrong architecture or missing ELF interpreter**, not a
missing file. Diagnosing it as a dependency problem leads nowhere.

## Audit

Every native binary in the SDK build-tools directory, and what it needs:

| Tool | Architecture | Usable? | Replacement |
|---|---|---|---|
| `aapt2` | x86_64 | no | Termux `aapt2` (aarch64, `/system/bin/linker64`) |
| `zipalign` | x86_64 | no | Debian `zipalign` package (aarch64 Linux) |
| `aapt` | x86_64 | no | not used by AGP 8 |
| `aidl` | x86_64 | no | not used — the project has no AIDL interfaces |
| `dexdump`, `split-select`, `bcc_compat` | x86_64 | no | not used |
| `d8` | shell script → `java` | **yes** | none needed |
| `apksigner` | shell script → `java` | **yes** | none needed |

Note that `d8` and `apksigner` are *scripts*, so the x86_64 copies are harmless.
Only two tools actually block the build.

## Decision

1. **aapt2** — point AGP at the Termux build with
   `android.aapt2FromMavenOverride` in `gradle.properties`. AGP's own Maven
   artifact is x86_64-only, so the override is the only route. Verified:

   ```
   $ aapt2 version
   Android Asset Packaging Tool (aapt) 2.20-android-16.0.0_r4
   ```

2. **zipalign** — AGP resolves this from the SDK build-tools directory and
   offers **no** override property, so the working binary has to *be* that file.
   The Debian `zipalign` package is built for aarch64 Linux, and the SDK copy is
   replaced with a symlink to it. The original is kept as
   `zipalign.x86_64-original` so the change is visible and reversible.

3. **Everything is scripted** in `tools/setup-termux-toolchain.sh`, which is
   idempotent, prints the architecture of every tool, and verifies that each one
   actually executes.

## Why not the alternatives

**Rebuild the SDK for aarch64.** Expensive, and it would still have been
`aapt2` from Maven inside AGP — the override is needed regardless.

**Write a Java zipalign.** Alignment is a well-understood algorithm, but the
platform already ships a correct, tested implementation. Reimplementing it would
add a component that must be trusted for APK validity.

**Disable alignment.** AGP offers no supported way, and an unaligned APK is
rejected by some distribution channels.

**Build AOSP from source.** Disproportionate for a build-tool problem.

## Verification

```bash
$ aapt2 version
Android Asset Packaging Tool (aapt) 2.20-android-16.0.0_r4
$ build-tools/34.0.0/zipalign -f -v 4 in.zip out.zip
Verifying alignment of out.zip (4)...
      64 f.txt (OK)
Verification successful
$ ./gradlew :app:assembleDebug
BUILD SUCCESSFUL
```

## Note

`android.aapt2FromMavenOverride` is marked experimental by AGP and prints a
warning. It is the only supported mechanism for this situation, and the warning
is acknowledged rather than suppressed.
