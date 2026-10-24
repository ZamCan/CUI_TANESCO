# TANESCO Meter Companion — Premium Glass design system

The flat, website-like page layout has been replaced by a native glassmorphic
design system ("TANESCO Glass"). No behaviour changed: the meter/CIU
connection layer is still deliberately unimplemented, no measurement is
displayed that a meter did not report, and the modelled meter panel's CONTACT
indicator remains permanently unlit.

## Engineering approach

The redesign was done at the **design-system layer**, not screen by screen.
`SurfacePanel`, `AccentPanel`, `SectionHeader`, buttons, fields, status pills,
`ScreenScaffold` and the app shell were re-engineered once — so Home, Meter,
Meter Setup, Token Center, Activity, Diagnostics, Settings and About all
inherited the new look with **zero ViewModel, domain, data or connection
changes**. New screens built from these primitives get the system for free.

## Colour relationships (from the real TANESCO logo)

| Role | Colour | Rule |
|---|---|---|
| TANESCO green | `#177A2E` (deep `#0B4D20`, luminous `#2DBC5E`) | Action + identity. The primary button is a green energy gradient |
| Sun yellow | `#FFCD08` | The logo's upper swoosh. Brand highlight only (aurora, selected-nav ring, logo) — **never a warning** |
| Attention amber | `#9A5900` / `#FFC266` | Deliberately burnt-orange so it can never be confused with brand yellow |
| Ready / problem / info | green / red / sky semantic set | Meaning only, never decoration |
| Bases | mint-mist `#EEF6EA` (light) · emerald ink `#04150B` (dark) | Cool night/day bases that keep the warm green+yellow pair luminous |

## Glass, without a blur library (ADR-worthy constraint)

The app must build offline on Termux and stay smooth on low-end phones, so
glass is layered translucency, tokenised in `GlassColors`
(`TanescoTheme.glass`): a static aurora wash (`PremiumBackground`:
sun-yellow top-right, energy-green left, sky low), translucent gradient
panel fills, gradient hairline borders (light-catch → brand deep), and soft
coloured shadows. `Modifier.glassSurface()` in
`designsystem/component/Glass.kt` is the single implementation.

## Chrome & iconography

- **Top bar:** floating inset glass bar with the PNG brand mark, a
  `⚡ TANESCO` eyebrow and the screen title. Content scrolls under it.
- **Bottom navigation:** floating glass pill (not an edge-to-edge web footer)
  with animated selection and a sun-yellow ring on the active tab.
- **Unicode glyph medallions** (`UnicodeBadge`) replace generic vector icons:
  `⌂` Home · `⚡` Meter · `▦` Tokens/keypad · `◷` Activity · `⚙` Settings ·
  `✚` Diagnostics · `i` About · `›` drill-in. Glyphs are limited to code
  points with reliable Android font coverage. Glyphs are real text: they
  scale with accessibility settings and carry proper content descriptions.

## Logo (PNG only, authentic artwork)

`res/drawable/ic_launcher_foreground.xml` is deleted. The shipped identity
uses the **genuine TANESCO logo artwork itself** (yellow upper swoosh, green
lower swoosh, lightning-bolt “N”, TANESCO wordmark), never redrawn or
re-proportioned: transparent `brand_logo.png` for display, the unaltered logo
centred on a light ivory tile for the launcher (the official green wordmark
is designed for light grounds), and PNG adaptive-icon foregrounds.
Regenerate with `tools/make_tanesco_launcher_assets.py`. Provenance,
limitations and the owner action to source an official master directly from
TANESCO are recorded in `docs/brand/SOURCE.md` — the project must not present
itself as an official TANESCO product.
