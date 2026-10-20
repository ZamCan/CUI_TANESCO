# 0007 — Localisation: English only, no machine translations

**Status:** accepted · **Date:** 2026-10-05

## Context

The application targets Tanzania, where Swahili and English are official
languages. `AppLanguage` exists, and `resourceConfigurations += ['en']` is set in
the app module.

## Decision

Ship **English only**. `AppLanguage` currently offers `System` and `English`.

Do not bundle machine-generated translations.

## Rationale

**A wrong translation of a safety-relevant string is worse than no translation.**
This application's strings include "Not sent to a meter", "Passed local checks",
"Not available", and the meter keypad legends `CANCEL` / `ENTER`. A mistranslation
of any of these could tell a customer their token was sent to the meter when it
was not, or that a value had been read when it had not. Machine translation
cannot be relied on for precisely this class of string.

**Meter key labels are hardware, not UI.** `CANCEL`, `ENTER` and the indicator
legend `STATUS` / `FAULT` / `CONTACT` reproduce the silkscreen on the physical
device. The user's muscle memory is keyed to those exact words. Translating them
would make the modelled keypad *less* faithful, not more accessible.

**Plural and formatting errors are easy to ship and hard to notice.** Kotlin
string templates and Android plurals need per-locale review.

## How localisation is added later, properly

1. Obtain reviewed translations of `res/values/strings.xml` from a native
   speaker, with domain context supplied: this is a prepaid electricity meter, the
   strings describe meter state, and several must not be mistranslated in a way
   that claims an operation succeeded.
2. Add `res/values-sw/strings.xml` and a `values-sw` test for the keypad labels,
   asserting they match the intended hardware legend.
3. Widen `resourceConfigurations` to include `sw`.
4. Add `AppLanguage.Swahili`.
5. Localise the *domain* strings that live in Kotlin — the validation messages in
   `core/validation/Validators.kt` and `core/token/TokenRule.kt` — as part of the
   same change, so validation copy is never left in English.

## Note on the domain-layer messages

Validation and status copy currently lives in Kotlin rather than in
`strings.xml`, in exchange for having one wording shared verbatim by Android and
any future iOS build. Step 5 above is the deliberate cost of that choice, and it
must not be skipped: the first real localisation change should move domain
messages behind an injected message source rather than adding parallel string
resources that can drift.

## Consequences

- The application is currently English-only. Acceptable for a foundation build.
- Swahili speakers get a working application in English rather than a partly
  wrong one in Swahili.
