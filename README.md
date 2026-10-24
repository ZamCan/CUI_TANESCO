# TANESCO Meter Companion

Native-first electricity meter companion application for Android.

The application is **local-first**: meter profiles, token history, activity and
settings are stored in an on-device database, and every feature works with no
network connection and no meter in reach.

The meter/CIU **connection layer is intentionally isolated** and, at this stage,
deliberately unimplemented. See
[docs/connection/CONNECTION-BOUNDARY.md](docs/connection/CONNECTION-BOUNDARY.md).

---

## Status

| | |
|---|---|
| APK | Fresh artifact pending the repaired GitHub Actions build; use the latest successful workflow artifact. |
| Package | `com.tanesco.metercompanion` (versionCode 1, targetSdk 35, minSdk 24) |
| Build | Hosted CI verification pending after separating Termux-only AAPT2 configuration from desktop runners. |
| Tests | Re-run by GitHub Actions after the APK assembly stage; confirm the latest workflow result before relying on the count. |
| Git | clean tree; milestones committed individually |

### Interface — TANESCO Glass

A premium glassmorphic design system built from the real TANESCO identity
(deep green + sun-yellow, from the logo): translucent glass panels over an
aurora wash, a floating glass top bar and bottom navigation, Unicode glyph
medallions for navigation/shortcuts, and a PNG-only logo set derived from the
official mark. It is implemented once in the design-system layer
(`Glass.kt`, `Panel.kt`, shells) so every screen inherits it. See
[docs/ui/PREMIUM-GLASS-DESIGN.md](docs/ui/PREMIUM-GLASS-DESIGN.md).

### What the application does today

- **Home** — application status, configured meter, connection state with its
  reason, recent activity, shortcuts.
- **Meter** — identity, profile, connection state, measurement placeholders that
  read *Not available* rather than zeros, session state, diagnostics link.
- **Meter Setup** — create or edit a meter record, with per-field validation.
- **Token Center** — token entry on a **faithful reproduction of the physical
  meter's front panel**: seven-segment LCD, STATUS/FAULT/CONTACT indicators, and
  the hardware keypad layout including the hardware's CANCEL and ENTER keys. A
  keyboard entry field is offered alongside it.
- **Activity** — the append-only local audit trail, filterable by category.
- **Diagnostics** — subsystem health, distinguishing *Ready*, *Available*,
  *Research*, *Unavailable* and *Error*.
- **Settings** — meters, default meter, appearance, units, language, connection
  attempts, developer details, local data.
- **About** — version, technical information, and an explicit statement of what
  the application does not do.

### What it deliberately does not do

No meter communication of any kind. No token is sent anywhere. No measurement is
displayed that a meter did not report. The CONTACT indicator on the modelled
meter panel is permanently unlit, and its legend says why.

---

## Build on Termux

The build machine is an Android phone running Termux. Three environment-specific
findings are baked into the configuration and documented in
[ADR 0002](docs/decisions/0002-build-script-dsl.md) and
[ADR 0009](docs/decisions/0009-termux-toolchain-repair.md):

1. **Kotlin DSL stalls under PRoot.** All build scripts use the Groovy DSL.
2. **A large Gradle heap gets the daemon killed.** The heap is 512 MB.
3. **The installed Android SDK is x86_64 on an aarch64 phone.** Every native
   build tool fails to exec. `aapt2` is taken from Termux and `zipalign` from an
   aarch64 package.

### One-time setup

```bash
cd ~/TANESCO-Meter-Companion
bash tools/setup-termux-toolchain.sh    # repairs aapt2 + zipalign
source tools/env.sh                    # sets ANDROID_HOME and GRADLE_USER_HOME
```

### Build

```bash
source tools/env.sh
tools/build.sh :app:assembleDebug      # retries device-pressure failures only
tools/build.sh :core:test              # unit tests
```

`tools/build.sh` distinguishes a genuinely broken build (a compilation error,
reported immediately) from a daemon killed by memory pressure (retried).

### Install

```bash
tools/install.sh
```

The script verifies the APK, copies it to shared storage and attempts every
available install route.

**Installing an APK on the device from inside the Termux sandbox is not
possible without root.** `pm`, `am` and `cmd` are blocked by the PRoot exec
filter, and `app_process` aborts without the shell UID. The APK is therefore
staged where the Android installer can reach it:

> **Open the Files app → Internal storage → Download → tap
> `TANESCO-Meter-Companion-debug.apk`.**

With a host machine attached over adb:

```bash
adb install -r -g app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.tanesco.metercompanion/.MainActivity
```

---

## Architecture

```
core/        :core        pure Kotlin domain — no Android dependency
connection/  :connection  pure Kotlin meter-connection boundary
data/        :data        pure Kotlin persistence contract and repositories
platform/    :platform    Android library: SQLite, package manager, dispatchers
app/         :app         Android application: Compose presentation layer
tests/       :tests       cross-module verification
docs/        architecture, ui, connection, testing, decisions
```

`core`, `connection` and `data` are pure Kotlin on purpose, so every domain rule
is unit-testable on a plain JVM — instrumentation tests cannot run on the build
machine at all.

See [docs/architecture/README.md](docs/architecture/README.md) for the full layer
map, the current status of the Digital CIU architecture, and the correct order
of the remaining work.

### Digital CIU engineering

The project is being re-engineered as a reusable Digital CIU platform. Implemented
so far:

- **Association engine** — nine states separating *identified*, *associated*,
  *verified* and *communicating*, with refusals as values. Covered by 20 tests
  across the twelve required scenarios.
- **`CiuMeterProfile`** — every CIU-specific field optional, with a
  `MeterProfileSource` recording whether a profile is a simulator, a test meter
  or an authorised one.
- **Transport port** — transport *availability* and protocol *compatibility* are
  separate axes, so "the phone has Bluetooth" can never imply "this phone can
  talk to that meter".
- **Experiment log** — confidence is mandatory on every observation, and a
  behaviour cannot be declared universal until it is confirmed on two
  independent systems.

Not yet implemented: the session engine, the protocol adapter interface, the
virtual meter, the simulator transport, the capability services, and the hardware
capability inventory. The correct order is in
[docs/architecture/README.md](docs/architecture/README.md).

---

## Documentation

| Path | Contents |
|---|---|
| [architecture/](docs/architecture/README.md) | Layer map, CIU status, next steps |
| [connection/CONNECTION-BOUNDARY.md](docs/connection/CONNECTION-BOUNDARY.md) | What is implemented, what is not, and the rules |
| [decisions/](docs/decisions/) | ADRs: toolchain, DSL, persistence, DI, tokens, localisation, model isolation |
| [ui/](docs/ui/) | Design system rationale |
| [testing/](docs/testing/) | Test strategy and coverage |

## Licence

Proprietary — TANESCO.
