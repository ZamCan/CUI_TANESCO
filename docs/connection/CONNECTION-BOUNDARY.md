# Connection boundary

**Status:** active · **Applies to:** `:connection`, and any future `:transport` / `:protocol` work

This document is the contract between the application and any physical meter. It
states exactly what is implemented, exactly what is not, and the rules that keep
the two from blurring. Read it before touching anything under `connection/`,
`transport/` or `protocol/`.

---

## 1. What this build implements

| Layer | Status | Where |
|---|---|---|
| Connection **interface** (`MeterConnection`, states, capabilities, results) | Implemented | `connection/` |
| Connection **coordinator** (session + event logging per attempt) | Implemented | `connection/ConnectionCoordinator.kt` |
| **Research adapter** (reports "not implemented") | Implemented | `connection/ResearchMeterConnection.kt` |
| Diagnostics probe for the boundary | Implemented | `connection/ConnectionDiagnosticsProbe.kt` |
| Association state machine | Implemented | `core/ciu/AssociationState.kt` |
| Transport **port** | Implemented | `core/transport/MeterTransport.kt` |
| Structured capability result type | Implemented | `core/ciu/` |
| Experiment / evidence log model | Implemented | `core/experiment/ExperimentLog.kt` |

## 2. What this build deliberately does **not** implement

- Any CIU or meter protocol. None is spoken, guessed at, or reverse engineered.
- Any physical transport. There is no Bluetooth, NFC, USB, Wi-Fi, sub-GHz RF or
  PLC code in the project.
- Any virtual meter yet (see §6).
- Meter authentication material. Nothing is extracted, stored or derived.
- Any claim that a meter accepted a token or a credit change.

## 3. The rules

These are enforced by the type system wherever possible, not only by convention.

1. **A reading cannot exist without provenance.** `Measurement<T>` requires a
   `Provenance`. Absent data is `null`, never `0`. There is no overload that
   accepts a raw number, so a placeholder reading cannot be introduced by
   accident.

2. **A snapshot is empty until a device supplies one.** `EnergySnapshot.Empty`
   is the only value the application can currently hold.

3. **Success is only reported by an adapter.** `ConnectionResult.Success` is
   constructed exclusively inside a `MeterConnection` implementation. The
   coordinator and every screen only ever forward what an adapter returned.

4. **Failure is a value.** `ConnectionFailure` / `DomainError` carry a stable
   code. Nothing throws across a layer boundary, and coroutine cancellation is
   always rethrown rather than converted into a failure.

5. **Association is not connection.** `AssociationState` separates *identified*,
   *associated*, *verified* and *communicating*. `MeterConnection.connect()`
   succeeding never implies the right meter was found.

6. **Transport availability and protocol compatibility are separate.**
   `ProtocolCompatibility` defaults to `Unestablished`. No transport claims to
   reach a real meter until an experiment proves it.

7. **Manufacturer behaviour stays isolated.** Per-family behaviour lives in an
   adapter. `ObservedBehavior.universal` cannot be set true until a behaviour is
   confirmed on two independent systems — asserted in `init`.

8. **Evidence carries its weight.** Every `ExperimentRecord` states a
   `Confidence`. Simulator evidence is never `isProductionEvidence`.

## 4. How a real adapter gets added

1. Implement `MeterTransport` for the physical link, reporting
   `ProtocolCompatibility.Unestablished` until experimentally proven.
2. Implement `MeterProtocolAdapter` for the meter's protocol family, built only
   from published documentation or authorised observation.
3. Register both in `AppContainer` — the single place any concrete adapter is
   named. No screen or service changes.
4. Record the supporting experiments in the experiment log with honest
   confidence levels.
5. Add tests for every new failure path, not just the success path.

## 5. What must never be added

Stated explicitly because these are the changes that would make the project
useless or unsafe:

- Undocumented protocol frames reverse engineered from a production meter.
- Any mechanism that extracts, stores, derives or reproduces meter keys.
- Bypassing or defeating authentication.
- Altering live credit without authorisation.
- Any code path that reports success the device did not confirm.
- Any screen text asserting a meter operation succeeded without adapter
  confirmation. Copy such as "Token sent to meter" is prohibited today.

## 6. Planned next boundary work

In the order the engineering specification requires:

1. **Virtual meter** — the primary development target until legitimate hardware
   exists. It is explicitly labelled `SIMULATOR` everywhere it appears, and its
   evidence can never be `isProductionEvidence`.
2. **Simulator transport** — `CommunicationTechnology.Simulated`, the only
   transport that will ever report `ProtocolCompatibility.Confirmed` without
   physical hardware, and only because it is by definition not hardware.
3. **Capability services** — `getMeterIdentity`, `getBalance`,
   `getInstantaneousData`, `getConsumptionHistory`, `getEvents`,
   `getMeterStatus`, `submitToken`, `getDiagnostics`, each returning a structured
   result rather than a string.
4. **Session engine** — `connect / initialize / discover / verifyIdentity /
   authenticateIfRequired / establish / maintain / disconnect / recover`, with
   timeout, retry, wrong-meter detection and reconnection.

## 7. Verifying this document

Two mechanisms keep it true:

- `AssociationEngineTest` — 20 tests over the association state machine,
  including wrong-meter, recovery, lost association and invalid requests.
- The diagnostics screen, which reports the connection boundary from the live
  adapter rather than from this document. If the two ever disagree, the screen is
  correct and this document is stale.
