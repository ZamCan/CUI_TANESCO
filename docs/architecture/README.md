# Architecture

## Module layout

Each top-level directory is a Gradle module, so the documented architecture and
the build structure are the same thing.

| Directory | Module | Kind | Android? |
|---|---|---|---|
| `core/` | `:core` | Kotlin/JVM library | no |
| `connection/` | `:connection` | Kotlin/JVM library | no |
| `data/` | `:data` | Kotlin/JVM library | no |
| `platform/` | `:platform` | Android library | yes |
| `app/` | `:app` | Android application | yes |
| `tests/` | `:tests` | Android library | yes |

`core`, `connection` and `data` are pure Kotlin on purpose:

- every domain rule can be unit tested on a plain JVM, with no emulator — which
  matters because instrumentation tests cannot run on the build machine at all;
- only three modules are Android builds, which keeps configuration time viable on
  a phone;
- iOS support means writing sibling implementations of the same interfaces, not
  porting Kotlin.

## Dependency rule

```
app ──▶ platform ──▶ data ──▶ core ◀── connection
 └────────────────────────────┘
```

Nothing points back up. `app` is the only module that may depend on
`:connection`, and even then only through the interface.

## Layers

### 1. Presentation — `app/`

Compose only. Screens are stateless: each takes a state object from a ViewModel
and a set of lambdas. No screen touches a repository, a SQL driver, or a
`MeterConnection`.

```
app/ui/
  designsystem/    theme, components  (no knowledge of any screen)
  navigation/      Destination, TanescoApp (the NavHost + bottom bar)
  common/          UiState, ScreenScaffold, ViewModel factory
  home/ meter/ metersetup/ token/ activity/ diagnostics/ settings/ about/
```

### 2. Application / domain — `core/`

```
core/model/      MeterProfile, TokenRecord, AppEvent, AppSettings, Measurement,
                 DomainResult, DomainError
core/meter/      MeterProfileValidator, MeterProfileService
core/token/      TokenNormalizer, TokenFormatProfile, TokenValidationEngine,
                 TokenService, MeterKeypadInput
core/validation/ FieldValidation, Validators
core/history/    EventRecorder
core/diagnostics/ DiagnosticsService, probes
core/ciu/        AssociationState, AssociationEngine, CiuMeterProfile
core/transport/  MeterTransport, TransportAvailability, ProtocolCompatibility
core/experiment/ ExperimentRecord, ObservedBehavior, Confidence
core/time/       Clock, IdGenerator
core/repository/ Ports: MeterRepository, TokenRepository, EventRepository, ...
```

### 3. Local data — `data/` + `platform/`

`data` owns the *contract*: schema, statements, row mapping, repositories.
`platform` supplies the Android implementation (SQLite, package manager,
dispatchers). See [0003](../decisions/0003-persistence.md).

### 4. Diagnostics / audit

The event log is append-only and is the reason the Activity and Diagnostics
screens can be trusted: every statement they make is backed by a row rather than
by transient UI state.

### 5. Platform services — `platform/`

The only module outside `app` allowed to import `android.*`.

### 6. Connection boundary — `connection/`, `core/ciu/`, `core/transport/`

See [CONNECTION-BOUNDARY](../connection/CONNECTION-BOUNDARY.md). This is the seam
that must never leak upward.

---

## State and error handling

`DomainResult<T>` is `Success<T> | Failure(DomainError)`. Failures are values
with a stable `code`, so every failure branch is handled at compile time and no
layer throws across a boundary. Coroutine cancellation is always rethrown.

Screens map that to `UiState<T>` = `Initial | Loading | Ready<T> | Failed`.
`Initial` is separate from `Loading` so "nothing has happened yet" is
distinguishable from "working".

---

## Where the Digital CIU architecture currently stands

| CIU requirement | Status |
|---|---|
| `MeterProfile` | ✅ `core/ciu/CiuMeterProfile.kt` — all CIU fields optional, provenance-tracked |
| Association engine | ✅ `core/ciu/AssociationState.kt` — 9 states, refusals as values |
| Association tests | ✅ `AssociationEngineTest`, 20 cases over the 12 required scenarios |
| Transport abstraction | ✅ `core/transport/MeterTransport.kt` — port + availability + registry |
| Transport availability vs compatibility | ✅ separate enums, `Unestablished` default |
| Experiment / evidence log | ✅ `core/experiment/ExperimentLog.kt` — confidence is mandatory |
| Manufacturer isolation | ✅ ADR [0008](../decisions/0008-model-specific-isolation.md) |
| Local meter record + token engine | ✅ `core/meter`, `core/token` |
| `MeterSession` engine | ⚠️ record exists (`MeterSession`); **lifecycle engine missing** |
| Capability services | ❌ `getMeterIdentity` / `getBalance` / … |
| Protocol adapter interface | ❌ |
| Virtual meter | ❌ |
| Simulator transport | ❌ |
| Hardware capability registry | ❌ |
| Android UI over the CIU engine | ❌ (existing UI is over the older domain) |

### Correct order from here

Per the engineering specification's phase list, and because each step is
independently testable:

1. `MeterSession` engine — `connect/initialize/discover/verifyIdentity/
   authenticateIfRequired/establish/maintain/disconnect/recover`, with timeout,
   retry, wrong-meter detection and reconnection. Pure state machine, so it is
   testable before any transport exists.
2. `MeterProtocolAdapter` interface — framed request/response, capability
   declaration. No frames invented.
3. **Virtual meter** — the primary development target. Labelled `SIMULATOR`
   everywhere; its evidence can never be `isProductionEvidence`.
4. Simulator transport + protocol adapter, so M1–M2 become reachable.
5. Capability services returning the structured result type.
6. Only then the Android UI moves onto the CIU engine.
7. Hardware capability inventory last — it informs feasibility, not design.

## Key decisions

| ADR | Subject |
|---|---|
| [0001](0001-toolchain.md) | Pinned toolchain |
| [0002](0002-build-script-dsl.md) | Groovy DSL; heap sized for the device |
| [0003](0003-persistence.md) | Platform SQLite behind a hand-written port |
| [0004](0004-dependency-injection.md) | Manual DI |
| [0006](0006-token-format.md) | Structural token checks only |
| [0007](0007-localisation.md) | English only, no machine translation |
| [0008](0008-model-specific-isolation.md) | Manufacturer behaviour stays isolated |