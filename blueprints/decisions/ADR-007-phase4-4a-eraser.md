# ADR-007 — Phase 4 slice 4A (eraser, cloud-fallback), 2026-11-24

## Context
BP-04 `S4A` lists as candidate "on-device TFLite RMBG/U2-Net" and its
own spec'd fallback: "explicit-consent cloud API (keys in device prefs
only, never repo). Offline-first rule: eraser must degrade to
'unavailable offline' rather than silently upload."

## Decision (2026-11-24)
- Implement the cloud-fallback eraser first (no new deps, no models —
  consistent with ADR-002): `EraseService` POSTs the image to a user
  configured endpoint (SettingsStore `eraserEndpoint`, device-prefs
  only, never repo, never logged).
- Consent is explicit (SettingsStore `eraserConsent` + in-screen checkbox);
  without consent: `changed=false`, warnings: "consent required —
  enable in Settings".
- Offline degradation: no endpoint or unreachable host → receipt
  `verified=false`, warnings note the mode; source untouched.
- TFLite RMBG on-device (slice 4A original) — deferred further; no
  dep added.

## Consequences
- APK unchanged. Data never leaves the device except to the user's
  configured endpoint; nothing is uploaded implicitly (network was
  FULL ALLOWed repo-wide, but upload is opt-in by design).
