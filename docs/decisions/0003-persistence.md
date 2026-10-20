# 0003 — Persistence: platform SQLite behind a hand-written port

**Status:** accepted · **Date:** 2026-10-05

## Context

The application must work offline and store meters, tokens, events, sessions and
settings. The build runs on a phone under PRoot with a daemon that is killed when
memory tightens (see [0002](0002-build-script-dsl.md)).

The Android-native options are Room, or the framework's own `SQLiteOpenHelper`.

## Decision

Use **Android's built-in SQLite** (`SQLiteOpenHelper`) behind a hand-written
port, `SqlDriver`. Room is not used.

## Rationale

**Room's cost lands in configuration.** Room requires KSP, an annotation
processor, which runs during the configuration phase. Given that the Kotlin DSL
configuration phase already fails to complete on this device
([0002](0002-build-script-dsl.md)), adding a second processor to that phase is
the wrong risk to take for code generation.

**The abstraction we actually need is small.** The repositories use six
operations: execute, insert, update, delete, query, transaction. Room's value is
compile-time verification of queries and entity mapping — genuinely useful, but
achievable here by a narrower port with plain JVM tests over the repository logic.

**Testability without an emulator.** `:data` is a pure Kotlin/JVM module, so
repository behaviour — duplicate token detection, event ordering, settings
defaults — is tested with fast JVM tests. Instrumentation tests cannot run on the
build machine at all.

**Schema control.** Migrations are an explicit `DatabaseSchema.Migration` list,
and an unregistered starting version throws rather than being upgraded
best-effort. Silent "best effort" migration of a user's meter and token data is
not acceptable.

## Consequences

**Accepted costs**

- Queries are hand-written strings, so a malformed statement is a runtime error
  rather than a compile error. Mitigated by deriving the SELECT column list from
  the same constant used to build positional row reads, so the two cannot drift.
- Entity mapping is manual. Mitigated by keeping row mappers as single top-level
  functions.
- No Flow-based invalidation from the database. Repositories publish changes via a
  revision counter (`ChangeSignal`) and re-query. Acceptable at this data volume;
  a driver-level change feed could replace it without touching a repository.

## Two Android API constraints found while building this

Both are handled explicitly rather than worked around silently.

**`rawQuery` accepts only `String[]` arguments.** There is no typed-binding
equivalent for `SELECT`, so numeric binds are converted to their canonical
decimal form. This is safe because of SQLite's type-affinity rules: a TEXT value
compared against a column declared INTEGER is converted to a number before
comparison, and every numeric column in the schema is declared INTEGER. `LIMIT`
is *not* bound this way — it is interpolated from an `Int` that the query type
validates as positive.

**`SQLiteDatabase.update`/`delete` take a table name, not a statement.** They
cannot express a join or an `ON CONFLICT` clause, both of which the repositories
use. DML therefore runs through `execSQL` and the affected-row count is read back
from SQLite's own `changes()`.

## Forward compatibility

Enums are stored **by name**, not ordinal, so reordering an enum can never
reinterpret existing rows. Every decode tolerates an unknown value and falls back
to a documented safe default — never to a *passing* state — so a database written
by a newer build cannot crash an older one, and cannot make a failure look like a
success.

The token validation report is stored as a single encoded column with Base64 on
each free-text field, so a message containing the field separator cannot corrupt
the row.
