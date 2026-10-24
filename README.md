# TANESCO Digital CIU

A phone-first native Android companion interface for prepaid electricity meter workflows. The interface is intentionally separated from the hardware layer so the app can be developed and tested without falsely claiming a connection to a physical meter.

## Repository status

- Native Android Gradle project: initialized.
- Initial dashboard: implemented in Kotlin using Android native views.
- Visual direction: warm parchment, walnut, and restrained amber accents.
- GitHub Actions debug-APK workflow: configured for pushes, pull requests, and manual runs.
- **Live CIU/meter communication: not implemented or verified.** The current screen is a baseline, not a working meter controller.

## Safety and accuracy

- Simulator/interface state must remain clearly distinct from live hardware state.
- Never report a meter paired, a balance read, or a token sent/accepted without a verified response from authorized compatible hardware.
- Do not include guessed radio frequencies, pairing sequences, keys, or undocumented commands as if they were verified protocol facts.
- Use official TANESCO marks and meter images only after confirming source and permitted use. This project does not imply TANESCO endorsement.

## Build locally

Requirements: JDK 17, Android SDK Platform 35, Android Build Tools, and Gradle 8.7.

From the repository root, run:

```sh
gradle --no-daemon assembleDebug
```

Expected debug APK path after a successful build:

```text
app/build/outputs/apk/debug/app-debug.apk
```

A successful source commit is not proof that the app compiles. Check the GitHub Actions run and its artifact before treating the APK as build-verified.

## Next engineering stages

1. Build and validate this baseline on Android/CI.
2. Improve dashboard, meter profile, history, and token-entry UX with explicit simulator labeling.
3. Add unit tests for meter-domain models and input validation.
4. Research compatible CIU/meter hardware and documented authorized protocols separately.
5. Implement a hardware adapter only after protocol and hardware behaviour are demonstrated and tested.

Project name: **TANESCO Digital CIU** (`CUI_TANESCO`).