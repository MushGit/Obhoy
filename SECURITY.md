# Security Policy

## Offline Philosophy

Obhoy is designed with an offline-first architecture. Core emergency
operations — hardware trigger detection, location acquisition, floor
estimation, and SMS dispatch — operate entirely locally, with no
dependency on any server operated by the developers. The only network
calls the App makes are: (1) an optional weather API lookup used solely
to refine barometric floor estimates, which never blocks emergency SMS
dispatch, and (2) whatever network path the user's chosen app uses when
sharing a recording (e.g., WhatsApp, email).

## Encryption Architecture

| Data | At-rest protection | Mechanism |
|---|---|---|
| User profile, PINs (hashed), emergency contacts | Encrypted local database | SQLCipher, AES-256, via Room |
| Location history / cache | Encrypted local database | SQLCipher, AES-256, via Room |
| Audio recordings | Encrypted local vault | `EncryptedFile` (AndroidX Security), AES-256-GCM, key held in Android Keystore |
| True PIN / Decoy PIN | Never stored in plaintext | BCrypt (12 rounds) via `PinVerificationEngine` |

**Key management:** the database passphrase and audio encryption key are
generated on-device on first launch using a cryptographically secure
random generator, and are held only in Android's Keystore / app-private
secure storage. Neither key is ever transmitted, backed up, or
recoverable by the developers. This means:
- Encrypted data cannot be decrypted by anyone other than the app instance that created it, including the developers.
- If a device is factory reset, the App is uninstalled, or Keystore-backed keys are otherwise cleared, previously encrypted data (recordings, cached location, profile) is **permanently and intentionally unrecoverable**. We consider this the correct tradeoff for a personal-safety tool: a recoverable key would itself be an exploitable weakness.

**Known, deliberate exceptions to "always encrypted":**
- While a `MediaRecorder` session is actively capturing audio, it writes to a temporary plaintext file — a platform-level requirement, since `MediaRecorder` cannot write directly into an encrypted stream. This file is encrypted and the plaintext copy deleted within moments of the user tapping "Stop."
- When a user explicitly plays back or shares a saved recording, a short-lived decrypted temporary copy is created in app-private cache storage for that action only, and is deleted when the relevant screen closes. In the rare case the App process is killed by the OS before this cleanup runs, a temporary decrypted copy may persist briefly in app-private cache (not accessible to other apps) until cleared. We treat closing this last edge case as an open hardening item — see "Known Limitations" below.

## Stealth and Coercion-Defense Design

Several features exist specifically to protect users under coercion or
surveillance by an abuser. Because this is a core part of the App's
threat model, contributors should treat any regression here as a
high-severity issue, not a cosmetic one:

- Persistent foreground-service notifications (required by Android 8+) use deliberately generic text and channel names — never referencing the App's safety functions, "escort," "duress," or similar terms.
- The Active Escort timer's session state is not visible anywhere in the app UI once started; it can only be checked via a discreet secondary gesture.
- The Duress/Decoy PIN, when entered, presents a convincing "cancelled" state and routes to a disguised UI, while silently continuing background alerting.
- Any UI copy, log message, or notification that could reveal the existence of these features to someone other than the intended user is considered a security defect and should be reported as such.

## Hardware Trigger (Accessibility Service) Scope

Obhoy requests `AccessibilityService` — a permission commonly associated
with spyware — for one narrow, auditable purpose: detecting a 4-rapid-press
pattern on the physical power button, shaving one press off Android's
native 5-press SOS gesture. This permission is **not** used to read
screen content, capture keystrokes, or observe any other app's behavior.
Anyone auditing this code should be able to confirm that its logic is
scoped exactly this narrowly; if you find evidence otherwise, please
report it immediately as a critical issue (see below).

## Location Acquisition

At the moment of an emergency, the App requests a location fix directly
from the device's GPS hardware via Android's `LocationManager` /
`GPS_PROVIDER` — not routed through Google Play Services' Fused Location
Provider — so that it can function with zero network connectivity and on
devices without Google Play Services installed. If a live fix cannot be
obtained within the timeout window, the App falls back to the most recent
location in its encrypted local cache, which is refreshed periodically in
the background.

## Data Never Leaves the Device Unless You Choose

The App does not operate any backend server. No location, contact,
profile, or recording data is transmitted anywhere except:
- SMS dispatched to your own configured emergency contacts,
- A call placed to a national emergency number,
- A file you explicitly choose to share via Android's share sheet (e.g., sending a recording to police or a contact).

No analytics, telemetry, or crash-reporting SDK that transmits data
off-device is included in the App.

## Known Limitations

We believe in being transparent about what is *not* yet fully hardened,
rather than overstating the App's guarantees:

- **Temporary decrypted files**, as described above, could theoretically persist briefly in app-private cache if the process is killed mid-action. A defense-in-depth fix (purging the temp-share directory on every app launch, in addition to on-screen-close) is a planned hardening item.
- **No independent third-party security audit has been performed yet.** The App is in public beta specifically to invite this kind of scrutiny — see "Reporting Vulnerabilities" below.
- **Barometric floor estimation** can be affected by weather-driven pressure changes and building HVAC systems; a weather-API correction is used when available, with a standard-pressure fallback when offline, but floor estimates should be treated as approximate, not exact.
- **This project is maintained by a small, early-stage team**, not a company with a dedicated security response function. Response times to reports may vary; we ask for patience and will credit reporters who wish to be credited once an issue is resolved.

## Reporting Vulnerabilities

If you discover a security vulnerability, logic flaw, or any behavior
that could compromise a user's safety, privacy, or the integrity of the
stealth/coercion-defense features described above, please report it
responsibly:

- **Preferred:** open a GitHub Issue on this repository with clear reproduction steps. For anything you believe could put a user at immediate risk if made public before a fix ships, please indicate this clearly in your report so it can be prioritized, or contact the maintainer directly through the contact method listed in the repository profile rather than filing a public issue.
- Please include: the affected file/component, steps to reproduce, and — if applicable — the potential impact (e.g., "could allow a device inspector to detect an active Escort session").
- We ask reporters not to publicly disclose a vulnerability's exploit details until a fix has had reasonable time to ship, given the population this App is built to protect.

Given Obhoy's specific use case — supporting people in domestic violence,
stalking, and coercion situations — we treat privacy and stealth-defense
bugs with the same severity as data-loss or crash bugs, even when they
might otherwise be considered "cosmetic" in a typical app.

---
