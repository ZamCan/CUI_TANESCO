# TANESCO Digital CIU — Developer Preview

Independent native Android UI prototype for exploring a phone-first prepaid-meter companion. Package: `tz.zamcan.cui_tanesco.debug`; minimum Android 8.0 (API 26).

## Included
- Warm parchment/amber native UI and generic meter illustration.
- Local meter profile editor.
- Token keypad interface for layout and interaction testing.
- Local activity log and Settings/About.
- No network, Bluetooth, NFC, location or other hardware permissions.
- Debug application ID suffix to separate developer builds from a future production package.

## Critical boundary
**This is not a working Digital CIU connection.** No meter protocol, RF driver, pairing handshake, live readings, balance calculation or token submission is implemented. Token entries are local demo records and are never validated or transmitted. The illustration is generic and not an exact physical model. The wordmark is text only; no official TANESCO logo asset or endorsement is implied.

Do not treat demo entries as electricity payments. Enter purchased tokens through the meter/CIU's approved procedure and verify acceptance on the physical device.

## Build with GitHub Actions
Push to `main` to trigger `.github/workflows/android-build.yml`. On success, open **Actions → Android debug build → latest successful run → Artifacts** and download `tanesco-digital-ciu-debug-apk`.

## Build in Termux
With JDK 17, Android SDK platform/build tools 35, and Gradle 8.7 configured:
```bash
pkg update
pkg install git openjdk-17
git clone https://github.com/ZamCan/CUI_TANESCO.git
cd CUI_TANESCO
gradle --no-daemon assembleDebug
```
Expected APK: `app/build/outputs/apk/debug/app-debug.apk`. Tap it in a file manager to install; Android may ask permission to install unknown apps from that source.

## Next engineering gates
1. Recover/merge any larger local implementation not present on remote `main`.
2. Add automated tests and stronger persistence.
3. Confirm exact meter/CIU hardware and authorised interface/protocol evidence.
4. Implement a separate connection adapter with capability detection and explicit simulation state.
5. Test on target devices before enabling live features or release signing.

Use official TANESCO visual assets only when obtained from an authoritative source and their use is permitted.
