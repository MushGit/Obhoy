# Obhoy (অভয়) — Covert Safety Engine

An open-source, offline-first personal safety application engineered for low-connectivity environments and budget Android devices in Bangladesh.

Obhoy operates completely locally on-device with **zero external server dependencies**. It delivers reliable, stealthy emergency alert dispatches over standard GSM cellular networks without requiring active mobile data.

---

## Legal & Compliance Documents

* [Privacy Policy](PRIVACY_POLICY)
* [Terms of Service & Disclaimer](TERMS_OF_SERVICE)
* [Security Policy](SECURITY.md)

---

## Core System Capabilities

* **Hardware Power-Button Override:** Quadruple-click physical key trigger powered by a persistent `ForegroundService` and `AccessibilityService`.
* **Active Escort (Dead Man's Switch):** On-demand countdown timer with a 60-second grace period and dual-PIN coercion defense.
* **Coercion-Resistant Dual-PIN:** 
  * **True PIN:** Safely disarms active emergency state.
  * **Decoy PIN:** Renders a believable "Safe" UI while silently executing background emergency dispatches.
* **Offline Hot-Start GNSS:** Caches raw NMEA satellite ephemeris ($GPGGA$) locally to achieve 1–2 second satellite fixes without an internet connection.
* **Barometric Floor-Level Tracking:** Uses atmospheric pressure differentials ($\Delta h$) to translate altitude into human-readable floor estimates (`Ground`, `4th floor`, `2 floors underground`).
* **GSM Cellular SMS Backbone:** Packages coordinates, floor estimates, time, and identity into standard SMS segments—ensuring delivery over 2G voice channels.
* **Encrypted On-Device Persistence:** All local databases (profile data, PIN hashes, emergency contacts) are encrypted via SQLCipher (AES-256).

---

## System Architecture

```
 [ Hardware Triggers ]              [ User Input ]
   • Quad-Click Power                 • Active Escort Timer
   • Background Listener              • Decoy / Duress PIN
           │                                 │
           └────────────────┬────────────────┘
                            │
                            ▼
             [ SYSTEM STATE: CRITICAL_DISPATCH ]
                            │
           ┌────────────────┴────────────────┐
           ▼                                 ▼
 [ Telemetry Collection ]          [ Local Evidence ]
   • Hot-Start GNSS Lock             • Open MediaRecorder
   • Barometer Elevation               Encrypted AAC Stream
   • Fallback Location Buffer        • Store in Private Storage
           │
           ▼
 [ Payload Compiler & Floor Formatter ]
   • Elevation -> Floor Name
   • Standardized SMS String
           │
           ▼
 [ Outbound Dispatch Engine ]
   • GSM SMS (SmsManager) ──> Contacts (1-5)
   • Optional IP Uplink (If online)
---
```

## Repository Structure

```
Obhoy/
├── .github/
│   └── workflows/
│       └── android.yml            <-- GitHub Actions CI/CD pipeline
├── CONTRIBUTING.md                <-- Open-source contribution guidelines & code standards
├── PRIVACY_POLICY.md              <-- Legal privacy policy
├── TERMS_OF_SERVICE.md            <-- Terms and liability disclaimers
├── SECURITY.md                    <-- Vulnerability reporting rules
├── README.md                      <-- Architecture and project overview
├── gradle.properties              <-- AndroidX, JVM heap & build performance flags
├── build.gradle.kts               <-- Root build script
├── settings.gradle.kts            <-- Dependency repository management & module includes
└── app/                           <-- Primary Android application module
    ├── build.gradle.kts           <-- Module dependencies (Room, jBCrypt, WorkManager)
    └── src/main/
        ├── AndroidManifest.xml    <-- Service declarations, power triggers, standalone activities
        ├── java/com/obhoy/app/
        │   ├── ObhoyApplication.kt <-- Central Application class & Room database initialization
        │   ├── data/
        │   │   ├── local/
        │   │   │   ├── ObhoyDatabase.kt <-- Local Room database vault
        │   │   │   ├── dao/             <-- UserProfileDao, EmergencyContactDao
        │   │   │   └── entity/          <-- UserProfileEntity, EmergencyContactEntity
        │   │   └── repository/          <-- Data source abstractions
        │   ├── security/
        │   │   └── PinVerificationEngine.kt <-- BCrypt hashing & duress PIN validation
        │   ├── service/
        │   │   └── ObhoyForegroundService.kt <-- Persistent service (Location, Audio, 999 Recording)
        │   ├── receiver/
        │   │   ├── ScreenToggleReceiver.kt  <-- 4x Power button press hardware detector
        │   │   └── BootReceiver.kt          <-- Service restart on device boot
        │   ├── sensor/
        │   │   ├── GnssSatelliteEngine.kt   <-- Offline raw GNSS satellite location engine
        │   │   └── BarometerElevationEngine.kt <-- Atmospheric pressure & vertical floor estimator
        │   ├── engine/
        │   │   ├── DispatchManager.kt      <-- SOS dispatch & 999 trigger orchestrator
        │   │   └── SmsPayloadCompiler.kt   <-- Location URL & floor payload formatter
        │   ├── util/
        │   │   └── SmsDispatcher.kt        <-- Multipart SMS execution engine
        │   └── ui/                         <-- Application UI & stealth components
        │       ├── profile/
        │       │   ├── ProfileActivity.kt        <-- Primary profile & dashboard view
        │       │   ├── ManageContactsActivity.kt <-- Emergency contact management & priority ordering
        │       │   └── UpdatePinsActivity.kt     <-- Safe/Duress PIN hashing & update screen
        │       └── notes/                        <-- Covert "Fake Notes" stealth UI disguise
        └── res/                            <-- XML layouts, navigation drawer graph, values, drawables

```
## Prerequisites & Installation
​Android Hardware Requirements
​Android OS: API Level 26+ (Android 8.0 Oreo or higher)
​Hardware Sensors: GPS/GNSS, Atmospheric Barometer (Optional for floor estimation)
​Telephony: Active SIM card with standard SMS balance

---
### A Note on the AccessibilityService Permission

You'll notice Obhoy requests `AccessibilityService` — a permission often associated with 
spyware and banking trojans, so it's fair to ask why a privacy-first safety app uses it.

Android's built-in hardware SOS trigger requires **5 rapid presses** of the power button. 
In a genuine emergency — shaking hands, panic, limited time — that fifth press can be the 
difference between an alert going out and not. Obhoy uses `AccessibilityService` for exactly 
one narrow purpose: detecting a **4-click pattern within a 1.5-second window**, shaving off 
that one extra click when it matters most.

This permission is **not** used for screen reading, keylogging, overlay injection, or any 
form of surveillance. Its entire function is scoped to counting power-button presses. You can 
verify this yourself — the relevant logic lives in 
[`ScreenToggleReceiver.kt`](app/src/main/java/com/obhoy/app/receiver/ScreenToggleReceiver.kt), 
and we welcome anyone auditing it to confirm there's nothing more happening under the hood.
---

## Local Development Setup
1. Clone the repository: 
  bash: git clone [https://github.com/mushgit/Obhoy.git](https://github.com/mushgit/Obhoy.git)
2. Open the project in Android Studio.
3. Place your launcher icon asset in app/src/main/res/drawable/obhoy.png.
4. Build and deploy to a physical test device: 
  bash: ./gradlew assembleDebug
---
## License
​This project is licensed under the GNU General Public License v3.0 (GPLv3). See the LICENSE file for full terms.
---






