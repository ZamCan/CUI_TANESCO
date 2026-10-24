# TANESCO logo — provenance and use

**What is in this folder**

- `source/tanesco_logo_transparent.png` — transparent PNG of the genuine
  TANESCO logo artwork (green/yellow swoosh pair, lightning-bolt “N”,
  TANESCO wordmark). This is the recognizable official TANESCO mark.
- `tanesco_logo_official_transparent.png` — the same artwork, margin-trimmed
  and upscaled for display. The artwork itself was not redrawn, recoloured
  or re-proportioned.
- `tanesco_launcher_icon_1024.png` — the same artwork, unaltered, centred on
  a light ivory launcher tile by `tools/make_tanesco_launcher_assets.py`.

**Provenance (recorded honestly)**

The artwork was retrieved from a public logo-vector repository that hosts the
TANESCO (Tanzania Electric Supply Company Limited) logo. A direct download
from `tanesco.co.tz` was attempted during this work but the site did not
respond to requests from the build environment, and the repository contained
no official brand asset to use instead. This file is therefore **genuine
TANESCO logo artwork from a third-party source — it is not claimed to be an
official TANESCO-issued master file.**

**Action for the project owner**

1. Replace `source/tanesco_logo_transparent.png` with the highest-resolution
   logo obtained directly from TANESCO (Corporate Communications) when
   available, then re-run `tools/make_tanesco_launcher_assets.py`.
2. Obtain TANESCO's authorization before any public or commercial
   distribution of an application bearing their logo. The logo is TANESCO's
   trademark; this companion project must not present itself as an official
   TANESCO product.
