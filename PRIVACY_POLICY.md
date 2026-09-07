# Privacy Policy for Obhoy (অভয়)

**Last Updated: September 2026**

Obhoy ("the App," "we," "us") is a free, open-source, offline-first personal
safety application. This Policy explains what data the App handles, how,
and why. It is written to reflect the App's actual behavior as implemented
in its publicly auditable source code.

---

## 1. Zero Cloud Data Collection

Obhoy operates strictly on your device. The developers do not own,
maintain, or operate any remote server that receives, stores, or processes
your personal data. We do not track, collect, sell, or transmit your
location history, contacts, audio recordings, or usage data to ourselves
or any third party.

The only data that ever leaves your device is data **you actively
dispatch** — sent directly from your device to destinations **you
choose** (e.g., SMS to your own emergency contacts, sharing a recording
via an app of your choice, or a call to 999) — never to the developers or
any server we control.

## 2. Device Permissions and Why We Need Them

| Permission | Purpose |
|---|---|
| **Location (GPS/GNSS, Barometer)** | To include your coordinates and estimated floor level in emergency dispatches, and to maintain a locally cached, encrypted location fix so the App can respond quickly even without an active internet connection. |
| **SMS (SmsManager)** | To send emergency alerts directly from your device to your chosen emergency contacts over standard cellular SMS. |
| **Microphone** | To record audio locally, at your explicit request, using the App's standalone Audio Recorder feature. Recordings are encrypted immediately after you stop recording (see Section 4). |
| **Contacts** | To let you select existing contacts as emergency contacts. We do not read, upload, or process your contact list beyond what you explicitly select. |
| **Accessibility Service** | Used **exclusively** to detect a 4-rapid-press pattern on the physical power button, shortening Android's native 5-press SOS trigger by one press for faster activation in an emergency. This permission is not used to read screen content, log keystrokes, or monitor any other app. The relevant code is publicly auditable in the App's GitHub repository. |

## 3. Location Data: Acquisition, Caching, and Encryption

To provide a location in an emergency SMS as quickly and accurately as
possible, the App uses a layered approach:

1. **At the moment of an emergency**, the App attempts a live GPS fix
   directly from your device's GPS hardware, which can succeed even with
   no internet or cellular data connection.
2. **If a live fix cannot be obtained in time** (e.g., poor sky visibility,
   being indoors), the App falls back to the most recent location stored
   in its local, encrypted database.
3. **To keep that fallback reasonably fresh**, the App periodically
   (approximately every 15 minutes) attempts to record your location into
   this same encrypted local database, independent of whether an
   emergency is active.

All location data — live and cached — is stored exclusively inside the
App's encrypted local database (SQLCipher, AES-256). No unencrypted
location file is created by the App at any point. You may disable this
periodic caching at any time by revoking the App's location permission in
your device's system settings, though doing so may reduce the speed or
accuracy of location data included in an emergency dispatch.

## 4. Audio Recordings: When They're Made and How They're Protected

The App's Audio Recorder is a **manually operated, standalone feature** —
it does not start automatically when an emergency or Active Escort session
begins. Recording starts only when you tap "Start Recording," and stops
only when you tap "Stop" or close the recording screen.

While actively recording, audio is briefly written to a temporary,
app-private location on your device — this is a technical requirement of
Android's audio recording system. **The instant you stop a recording, it
is encrypted** using a key held in your device's secure hardware-backed
Keystore (AES-256-GCM), and the temporary unencrypted copy is deleted
immediately. From that point on, the recording exists on your device only
in encrypted form.

**Playback and sharing:** when you choose to play back or share a saved
recording, the App decrypts a short-lived temporary copy for that specific
purpose only — for playback, this copy is deleted when playback ends or
the screen closes; for sharing, it is handed to the app you choose (e.g.,
your messaging app, email, or file-sharing app) via a secure, restricted
file-sharing mechanism, so that the recipient receives an actual playable
audio file rather than encrypted data they cannot open. In both cases, the
temporary decrypted copy is deleted from the App's private storage when
you leave the relevant screen; in rare cases where the App is forcibly
closed by the operating system before this cleanup completes, a temporary
decrypted copy may persist briefly in the App's private cache until it is
cleared automatically.

**Important:** the encryption key for your recordings is generated and
held only on your device, tied to this specific App installation. If you
uninstall the App, factory reset your device, or the device's secure
key storage is otherwise cleared, previously encrypted recordings become
permanently unrecoverable. There is no backup key or recovery mechanism,
by design — a recoverable key would itself be a security weakness.

## 5. Data Storage and Encryption Summary

| Data | Storage | Encryption |
|---|---|---|
| User profile, PIN hashes, emergency contacts | Local Room database | SQLCipher, AES-256 |
| Location history / cache | Local Room database | SQLCipher, AES-256 |
| Audio recordings (at rest) | Local encrypted vault | Android Keystore, AES-256-GCM |
| Audio recordings (briefly, during active recording or active playback/sharing) | App-private temporary storage | Not encrypted (technical necessity; deleted immediately after use) |

No unencrypted copy of your data is created by the App except for the
brief, necessary exceptions described above, and none of it is ever
transmitted anywhere unless you explicitly choose to send or share it.

## 6. Data Retention and Deletion

- Data persists on your device only for as long as the App remains installed and you have not manually deleted it.
- You may delete individual emergency contacts, recordings, or your full profile from within the App at any time.
- Uninstalling the App removes its local database and encrypted vault in accordance with standard Android app-data behavior.
- We recommend performing a factory reset before selling, discarding, or transferring a device that has stored sensitive App data, as with any sensitive application.

## 7. Children's Privacy

Obhoy is designed to be usable by minors as part of its child-safety
mission. Because the App collects no data on any remote server, there is
no remote profile of a child user for us to access, retain, or disclose.
We are not able to verify the age of any user, consistent with the App's
zero-account, zero-server design. Parents or guardians configuring the
App on a minor's behalf should review this Policy and the Terms of
Service before doing so, and may act as an emergency contact or
co-administrator of the App's settings if they choose.

If you are a parent or guardian with concerns about a minor's use of this
App, please refer to the contact information in Section 10.

## 8. No Third-Party Data Sharing

Because the App does not transmit data to any backend we control, no
third party receives your information through our systems. Data you
choose to dispatch or share (SMS, phone calls, shared recordings) is
transmitted via your mobile carrier or the app you select, in the same
way any message, call, or file you send through those channels is —
subject to those services' own practices, which this Policy does not
cover.

## 9. Changes to This Policy

This Policy may be updated as the App's features change. Material changes
will be reflected in the "Last Updated" date above and noted in the
project's GitHub repository release notes. Continued use of the App after
an update constitutes acceptance of the revised Policy.

## 10. Governing Context and Contact

This App is developed with users in Bangladesh as its primary intended
audience, and this Policy is written with that context in mind. For
privacy questions, concerns, or to report a suspected issue, please open
an issue on the project's GitHub repository or refer to the contact
method listed in `SECURITY.md`.
