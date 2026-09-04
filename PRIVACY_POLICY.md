# Privacy Policy for Obhoy

**Last Updated: September 2026**

Obhoy ("the App") is an open-source, offline-first personal safety application. Your privacy and safety are our highest priorities.

### 1. Zero Cloud Data Collection
Obhoy operates strictly locally on your device. We do not own, maintain, or operate any remote servers. We do not track, collect, store, or sell any of your personal data, location history, contacts, or background recordings.

### 2. Local Device Permissions
The App requests the following device permissions solely to perform emergency functions on your command:
- **Location (GPS & Sensors):** To append your real-time coordinates and estimated floor level into emergency dispatches.
- **SMS (SmsManager):** To send emergency text messages directly to your designated emergency contacts over cellular networks.
- **Microphone:** To record local audio evidence locally to your device's encrypted storage during an active emergency event.
- **Contacts:** To allow you to select emergency contacts locally.

### 3. Data Storage & Encryption
All user profiles, PIN hashes, emergency contact lists, and cached satellite data are stored locally on your device inside an encrypted SQLite database (SQLCipher AES-256). No unencrypted data ever leaves your device unless explicitly dispatched by you via SMS.

### 4. Third-Party Access
Because we do not collect or transmit your data to any remote backend, no third parties have access to your information.
