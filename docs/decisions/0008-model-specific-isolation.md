# 0008 — Model-specific behaviour is isolated, never promoted

**Status:** accepted · **Date:** 2026-10-05

## Context

The Digital CIU specification is induced from observed behaviour across several
meter families. Those observations are *evidence of architectural patterns*, not
a single protocol:

- **EDMI HD16 / Mk10D / SER100** — RF CIU communication, FSK/GFSK-type RF,
  meter-specific addressing and channel behaviour, bidirectional framed
  communication, serial and meter-data queries, dedicated RF modem architecture.
- **CLOU** — UIU/meter association and pairing including documented on-site
  pairing, RF / PLC / wired variants, meter-address verification.
- **Wasion** — CIU-based prepaid systems, RF / PLC / wired by model, readings,
  consumption information, security/authentication, DLMS/COSEM in applicable
  products.

The risk these create is specific and easy to fall into: building the
application around one family's behaviour and calling it "the meter protocol".

## Decision

Manufacturer-specific behaviour lives behind `MeterProtocolAdapter` and
`MeterTransportAdapter`. No screen, service or repository may reference a
manufacturer, a frequency, a channel index, or a frame layout.

Concretely:

1. **`ProtocolFamily` stays coarse.** It has four values and none of them is a
   manufacturer. A new family does not require editing this enum.
2. **`CiuMeterProfile` carries manufacturer and model per meter.** The model is
   described by data, not by a branch in code.
3. **Optional everywhere.** Every CIU-specific field on `CiuMeterProfile` is
   nullable, because no two families expose the same facts. `null` means *not
   known* — never "default", never zero.
4. **`CommunicationTechnology` and `ProtocolFamily` are independent axes.** A
   meter may be reachable over more than one link, and a given link may carry
   more than one protocol. Collapsing them produces the false claim "this phone
   has Bluetooth, therefore it can talk to this meter".
5. **Universality is machine-checked.** `ObservedBehavior.universal` cannot be
   `true` unless the behaviour cites two or more independent observation sites
   *and* carries `Confidence.Confirmed`. This is asserted in `init`, so the
   invariant cannot be violated by a careless constructor call.
6. **Confidence is capped by independence.** `ObservedBehavior.effectiveConfidence`
   demotes a single-site observation from `Confirmed` to `StronglySupported`,
   because confirmation requires independence.

## Consequences

**Accepted costs**

- More interfaces, and more indirection, than a single "meter protocol" class.
  Every capability call now passes through an adapter that may decline.
- The first adapter is a simulator, so the indirection earns nothing real until a
  second adapter exists. This is deliberate: the cost is paid once, up front,
  rather than repeatedly when the second and third families arrive.
- Capability questions must be asked of the adapter
  (`MeterCapabilityProvider`), so a screen has to handle "not supported" for
  operations that a different family would have supported.

**Gains**

- A new manufacturer is a new adapter plus an experiment record, not a
  refactor of the application.
- The distinction between "we have not implemented this" and "this meter cannot
  do this" stays explicit, so the UI can say the right thing.
- The simulator is clearly labelled everywhere, and its evidence can never be
  mistaken for production behaviour.

## Alternatives rejected

**One `TanescoMeterProtocol` class with model branches.**
Rejected: it is the failure mode the specification explicitly warns about. Every
new family would add branches, and no branch could be independently tested.

**A generic `when (manufacturer)` in the service layer.**
Rejected for the same reason, and harder to test than an interface, because the
branches would be spread across services.

**Promoting EDMI's RF behaviour to a universal default.**
Rejected on evidence: only one family has been observed using sub-GHz FSK with
configurable channels, and CLOU and Wasion deployments include PLC and wired
variants. It is recorded as `StronglySupported` for that family, not universal.

## Related

- `docs/connection/CONNECTION-BOUNDARY.md` — what is and is not implemented
- `core/ciu/CiuMeterProfile.kt`, `core/experiment/ExperimentLog.kt`
