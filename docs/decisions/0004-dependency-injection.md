# 0004 — Manual dependency injection

**Status:** accepted · **Date:** 2026-10-05

## Context

The object graph is small: five repositories, four domain services, one
connection coordinator, one diagnostics service. It is assembled in one place,
`AppContainer`, and screens obtain ViewModels through a single factory function.

The build runs on a phone (see [0002](0002-build-script-dsl.md)), where
configuration time is already minutes rather than seconds.

## Decision

Construct the graph by hand in `AppContainer`. Use
`androidx.lifecycle.viewmodel.viewModelFactory` for ViewModels.

## Rationale

**The cost is paid in the wrong place.** Hilt or Dagger adds an annotation
processor to a build that already cannot keep a 768 MB daemon alive. On this
device that is not a marginal cost — the daemon is killed outright when memory
tightens, and a configuration-phase processor makes that materially more likely.

**The graph is more reviewable.** The entire application's object graph is one
readable function. That has a concrete benefit here: it makes the project's
central architectural rule *visible*. `AppContainer` is the only place in the
codebase that names a concrete `MeterConnection`:

```kotlin
val connectionProvider: SingleAdapterMeterConnectionProvider by lazy {
    SingleAdapterMeterConnectionProvider.withResearchAdapter()
}
```

Swapping in a real adapter later is a change to this expression and to nothing
else. With an annotation-processed graph, that fact would be spread across
modules, bindings and generated code, and the rule would be enforced only by
review discipline.

**Dependencies are explicit at the point of use.** Each ViewModel's constructor
lists exactly what it needs. `viewModelFactory` makes that list the only place
wiring exists, and the compiler catches a missing dependency.

**Testability does not suffer.** Services take their collaborators as
constructor parameters, so a JVM unit test constructs one with fakes. Nothing
about these classes depends on a container existing.

## Consequences

- No scoped bindings, so a ViewModel cannot accidentally receive a
  request-scoped object. Nothing here is request-scoped, so this costs nothing.
- Adding a dependency means editing the factory. For a graph this size that is
  an advantage: the change is visible in review.
- Constructor injection means longer constructor signatures. Accepted; it is
  visible rather than hidden.
- `AppContainer` must be released explicitly (`close()`), and `onTerminate` only
  fires on emulators. Accepted, and documented at the call site.

## When to revisit

Revisit if any of the following becomes true:

- the graph grows past roughly thirty bindings,
- per-`Activity` or per-screen scoping is genuinely needed,
- build configuration time stops mattering, i.e. the primary build machine is
  no longer a phone.

At that point Hilt is the right answer, and the migration is mechanical because
every dependency is already constructor-injected.
