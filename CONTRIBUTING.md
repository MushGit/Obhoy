## Contributing to Obhoy (অভয়)
First off, thank you for taking the time to check out Obhoy! Whether you are a full-stack engineer, an Android developer, or a cybersecurity researcher, your contributions help make personal safety technology more transparent, secure, and accessible for everyone.
Obhoy is a privacy-first, offline-capable emergency SOS application built under the GNU General Public License v3.0 (GPLv3). Our goal is to maintain a zero-monetization, audit-friendly, and lightweight security tool for women, children, and anyone in high-risk environments.
## Tech Stack & Architecture Overview
Before making changes, here is a quick overview of how Obhoy is structured:
 * Language: 100% Kotlin
 * UI Framework: Android Jetpack, XML ViewBinding, Material Design 3
 * Local Persistence: Room Database (Offline-first, zero cloud tracking)
 * Asynchronous Operations: Kotlin Coroutines & Flow
 * Security & Cryptography: PinVerificationEngine with multi-round BCrypt hashing for local PIN verification
 * Hardware Sensors: GNSS (GPS, GLONASS, Galileo, BeiDou), Barometer (Atmospheric Pressure for floor estimation), and Power Button physical trigger receiver
## Getting Started locally
Prerequisites:
 * Android Studio: Ladybug (2024.2.1) or newer recommended
 * JDK: Version 17
 * Minimum SDK: API Level 24 (Android 7.0 Nougat)
 * Target SDK: API Level 34 / 35 (Android 14 / 15)
Build Instructions
 * Clone the Repository: (bash)
   git clone https://github.com/your-username/obhoy.git
cd obhoy

 * Open in Android Studio: Open the project directory and let Gradle sync dependencies.
 * Build the Debug APK: (bash)
   ./gradlew assembleDebug

 * Run Tests: (bash)
   ./gradlew test

## Core Security & Design Principles
When contributing code to Obhoy, please adhere strictly to these core tenets:
 * Zero Plaintext Credentials: Never log, print, or store user PINs in plaintext. All authentication logic must interface with PinVerificationEngine or secure local hashing.
 * Offline-First Data Flow: Obhoy must remain fully operational without an internet connection. Do not introduce dependencies or APIs that mandate external server connectivity for emergency features.
 * Stealth & Coercion Safeguards: Always respect the separation between the True PIN and the Duress (Stealth) PIN. The duress flow must visually mimic alert cancellation while maintaining silent background execution.
 * Zero Telemetry / Analytics: We do not collect user analytics, crash logs via third-party cloud services, or advertisement identifiers.
## How to Contribute
1. Reporting Bugs
If you encounter a bug, sensor mismatch, or hardware edge case:
 * Check the existing GitHub Issues tab to ensure it hasn't already been reported.
 * Open a new issue using the Bug Report template. Include your device model, Android OS version, and steps to reproduce.
2. Feature Requests & Enhancements
Have an idea to improve sensor accuracy, optimize battery consumption, or refine the stealth UI?
 * Open a GitHub issue labeled enhancement to discuss the proposed change before writing code.
 * Ensure feature proposals align with our core focus: low-latency emergency dispatch, privacy preservation, and hardware accessibility.
3. Submitting Pull Requests (PRs)
 * Fork the Repository and create a feature branch off main: (bash)
   git checkout -b feature/your-feature-name

 * Commit Your Changes: Keep commits atomic and messages descriptive: (bash)
   git commit -m "security: optimize BCrypt salt generation delay on lower-end devices"

 * Test Thoroughly: Ensure the project compiles without warnings and all local Unit/DAO tests pass.
 * Push & Open PR: Push your branch to your fork and submit a Pull Request targeting main. Describe your changes clearly in the PR template.
## License & Community Copyright
By contributing to Obhoy, you agree that your contributions will be licensed under the GNU General Public License v3.0 (GPLv3). This guarantees that Obhoy and all derived works remain free, open-source, and accessible to the public forever.
