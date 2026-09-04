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
obhoy/
├── .github/
│   └── workflows/
│       └── android.yml       <-- GitHub Actions CI/CD pipeline
├── PRIVACY_POLICY.md        <-- Legal privacy policy
├── TERMS_OF_SERVICE.md      <-- Terms and liability disclaimers
├── SECURITY.md              <-- Vulnerability reporting rules
├── README.md                <-- Architecture and project overview
├── gradle.properties        <-- AndroidX, JVM heap & build performance flags
├── build.gradle.kts         <-- Root build script
├── settings.gradle.kts      <-- Dependency repository management & module includes
└── app/                     <-- Primary Android application module
    ├── build.gradle.kts     <-- Module dependencies (Room, SQLCipher, WorkManager)
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/obhoy/app/
        │   ├── ObhoyApplication.kt <-- Central Application class & dependency injection
        │   ├── data/
        │   │   ├── local/
        │   │   │   ├── ObhoyDatabase.kt <-- SQLCipher encrypted Room vault
        │   │   │   ├── dao/             <-- UserProfileDao, EmergencyContactDao, LocationHistoryDao
        │   │   │   └── entity/          <-- UserProfileEntity, EmergencyContactEntity, LocationHistoryEntity
        │   │   └── repository/          <-- Synchronous (*Sync) & suspend Repository implementations
        │   ├── service/
        │   │   └── ObhoyForegroundService.kt <-- Persistent foreground worker (Location & Mic types)
        │   ├── receiver/
        │   │   ├── ScreenToggleReceiver.kt  <-- Quad-click power button trigger listener
        │   │   └── BootReceiver.kt          <-- BOOT_COMPLETED service & WorkManager restarter
        │   ├── sensor/
        │   │   ├── GnssSatelliteEngine.kt   <-- Raw GNSS & satellite positioning engine
        │   │   └── BarometerElevationEngine.kt <-- Barometric pressure & floor level estimator
        │   ├── engine/
        │   │   ├── DispatchManager.kt      <-- Emergency alert pipeline orchestrator
        │   │   ├── LocationLoggerWorker.kt <-- WorkManager periodic location task
        │   │   └── SmsPayloadCompiler.kt   <-- Location URL & floor payload formatter
        │   ├── util/
        │   │   └── SmsDispatcher.kt        <-- Multipart SMS execution engine
        │   └── ui/                         <-- Onboarding, Active Escort, Dual-PIN Decoy views
        └── res/                            <-- Dynamic assets, strings, layouts, accessibility XML
---
```
## Prerequisites & Installation
​Android Hardware Requirements
​Android OS: API Level 26+ (Android 8.0 Oreo or higher)
​Hardware Sensors: GPS/GNSS, Atmospheric Barometer (Optional for floor estimation)
​Telephony: Active SIM card with standard SMS balance

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






