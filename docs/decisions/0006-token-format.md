# 0006 — Token format: structural checks only, no invented checksum

**Status:** accepted · **Date:** 2026-10-05

## Context

TANESCO prepaid meters take a numeric token. The token layout for a given meter
generation is specified per utility and per meter family, and that specification
is **not published**.

The application must let a customer record a token, check it for transcription
errors, keep a history, and detect duplicates — all locally, offline.

## Decision

Ship a `TokenFormatProfile` that applies **structural** checks only, and label it
as such everywhere it appears:

| Check | Behaviour |
|---|---|
| Not blank | Error |
| Character set | Digits only; names the offending character |
| Length | Exactly 20 for the shipped profile |
| Uniform digits | Rejects `1111…1` — a classic fat-finger |
| Integrity check | **`null`. No implementation ships.** |

The profile is `structural-20-digit-v1`, described as: *"Checks length, digits
and obvious input mistakes. It does not confirm that a meter will accept the
token."*

## Rationale

**A checksum that is not specified is a fabrication.** Implementing Luhn, mod-97
or any similar rule would produce tokens that display a green "valid" tick while
meaning nothing. That is worse than no check, because it converts an honest "I
don't know" into a confident wrong answer. The `TokenIntegrityCheck` port exists
so that adding a real, specified check later is a small local change.

**The most valuable check is the one people actually make.** Almost every real
error in transcribing a 20-digit token from a printed label is a swapped, dropped
or repeated digit. The uniform-digit rule and the character-set check catch the
large majority, and neither claims to know anything about the token's meaning.

**The wording is the product.** `TokenStatus` values are
`StructurallyValid`, `Rejected`, `Duplicate`, `Recorded` — and the enum's own
documentation states that `StructurallyValid` "says nothing about meter
acceptance". `TokenTransferState` exists precisely so that the hand-off to a
meter is modelled but cannot be advanced past `NotAttempted` by any local code.

## Consequences

- A user who enters a well-formed but invalid token sees "Passed local checks".
  That is accurate and intentionally weaker than what they want. The Token Center
  states the limitation above the entry field rather than hiding it.
- `TokenFormatProfileRegistry.resolve()` falls back to the default profile for an
  unknown id instead of throwing, so a settings row written by a newer build
  cannot crash an older one.
- Duplicate detection compares canonical (separator-stripped, upper-cased)
  values, so `1234 5678…` and `12345678…` are the same token.

## What would change this decision

An authoritative specification from the utility, or an experimentally
characterised checksum for a specific meter family. Until then this profile
stands, and `TokenIntegrityCheck` is the seam where a real rule would go.

## Related

`core/token/TokenFormatProfile.kt`, `core/token/TokenService.kt`,
`core/token/MeterKeypadInput.kt`
