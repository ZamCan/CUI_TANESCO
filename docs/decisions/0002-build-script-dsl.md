# 0002 — Groovy DSL for build scripts, and a heap sized for the device

**Status:** accepted · **Date:** 2026-10-05 · **Supersedes nothing**

## Context

The primary build environment is an Android phone running Termux under PRoot
(aarch64, 8 cores, 3.6 GB RAM total). Two independent problems were measured
before any architecture work began, and both are specific to this environment
rather than to the project.

### Finding 1 — Kotlin DSL stalls under PRoot

Measured, same Gradle 8.9 distribution, same JDK, empty project:

| Script language | Result |
|---|---|
| Groovy DSL (`settings.gradle`) | `BUILD SUCCESSFUL in 58s` |
| Kotlin DSL (`settings.gradle.kts`) | no completion after 110s; stalled again at 20 min |

`:core:compileKotlin UP-TO-DATE` was reached with Groovy DSL while the Kotlin DSL
variant never printed a task line. Kotlin DSL script compilation brings
`kotlin-compiler-embeddable` into the *configuration* phase; on this device that
phase does not complete.

### Finding 2 — The Gradle daemon was being killed

With `org.gradle.jvmargs=-Xmx1280m`, the daemon started, printed
`The daemon has started executing the build`, and then sat idle: all threads in
`S`, `wchan=futex_wait_queue`, and after ~5 minutes a *new* daemon PID appeared.
Six, then ten daemon logs accumulated during one attempt. `free` showed roughly
600 MB available while the daemon was resident at 660 MB. The kernel was killing
it, and Gradle silently restarting, so the build never progressed.

After reducing the heap to 512–768 MB with a serial collector,
`./gradlew projects` completed in 2m 4s and has been stable since.

## Decision

1. **All build scripts use the Groovy DSL.** `libs.versions.toml` is unaffected,
   so dependency and plugin versions remain centralised and type-safe at the
   catalog level.
2. **`org.gradle.jvmargs=-Xmx512m -XX:MaxMetaspaceSize=320m -XX:+UseSerialGC`.**
   Sized to what the device can actually satisfy, not to what a workstation
   would want.
3. **`kotlin.compiler.execution.strategy=in-process`** — one JVM instead of a
   Gradle daemon plus a Kotlin daemon, which matters when RAM is the binding
   constraint.
4. **No Java toolchain is requested.** Only JDK 21 exists here; asking for
   toolchain 17 would make Gradle try to provision a second JDK. Bytecode target
   17 is set directly instead.
5. **Toolchain pinned to what the device already has**: Gradle 8.9, AGP 8.6.1,
   Kotlin 2.0.21, compileSdk 35, buildTools 34.0.0. `compileSdk 35` with AGP
   8.6.1 is acknowledged by `android.suppressUnsupportedCompileSdk=35`; it builds
   correctly.

## Consequences

- Build scripts are less pleasant to write than Kotlin DSL. Accepted: the
  alternative is a build that does not run.
- The Groovy DSL uses dynamic typing, so a mis-typed configuration surfaces as a
  runtime error rather than a compile error. Mitigated by keeping the shared
  configuration in one `subprojects` block.
- Every build is slow in absolute terms (minutes, not seconds). Inherent to
  running a JVM toolchain on a phone under PRoot.

## Operational notes for this device

```bash
source tools/env.sh          # sets ANDROID_HOME and GRADLE_USER_HOME
./gradlew :app:assembleDebug
```

`tools/env.sh` points `GRADLE_USER_HOME` at the Termux prefix so the AGP 8.6.1
and Kotlin 2.0.21 artifacts already on the device are reused rather than fetched
again — this saves several hundred MB per fresh checkout.

If a build appears to hang with no task output, check for a new daemon PID:

```bash
ls -t /data/data/com.termux/files/home/.gradle/daemon/8.9/*.log | head -2
```

Growth in that count means the daemon is being killed, not working.
