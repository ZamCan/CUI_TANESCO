# TANESCO Digital CIU

A native Android companion interface for studying and managing prepaid electricity meter interactions. The application is being developed in clearly separated layers so that the user interface can be built and tested before any physical meter protocol is validated.

## Current engineering status

- Repository baseline: initialized.
- Meter dashboard and interaction simulator: planned for the first implementation stage.
- Physical CIU/meter communications: **not implemented or verified**. Do not treat simulated readings, pairing, or token operations as real meter communication.
- Official TANESCO branding and meter-specific compatibility must be based on verified assets and device documentation; this project does not imply TANESCO endorsement.

## Development principles

1. Native Android UI, designed for phone-first development.
2. Keep presentation, meter-domain logic, and hardware/protocol adapters separate.
3. Make simulator mode visibly distinct from a real connected meter.
4. Never claim a token was sent, accepted, or a meter was paired unless the connected hardware reports a verified result.
5. Require authorized equipment and documented protocol details before implementing live communications.

## Build

This repository is being initialized. The Android Gradle project and build validation workflow will be added in subsequent commits. Build status must be reported only after a real build has run.

## Project name

TANESCO Digital CIU (repository: `CUI_TANESCO`).