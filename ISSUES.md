# Known Security Issues & Vulnerability Backlog

This document tracks identified security risks that have not yet been fully mitigated. For resolved issues, see [SECURITY.md](SECURITY.md).

## 🟡 Medium Priority

### 7. Trust On First Use (TOFU) Gap
- **Description**: During the initial pairing of a new device, the client must "trust" the server's identity certificate without prior verification.
- **Abuse Scenario**: A sophisticated Man-in-the-Middle (MitM) attacker could spoof the server's identity during the *very first* connection attempt before the identity is pinned.
- **Mitigation Strategy**: Optional out-of-band verification (e.g., displaying a 6-digit hash/QR code on the desktop that the user verifies on the mobile device).

### 8. Local Denial of Service (Keystore Corruption)
- **Description**: Although we have implemented "Self-Healing" with backups, a malicious local process can repeatedly delete or corrupt the `server_keystore.p12` file.
- **Abuse Scenario**: The user is forced into a loop of resetting their identity and re-pairing devices, rendering the application unusable.
- **Mitigation Strategy**: This is largely an OS-level permissions issue, but can be further mitigated by strengthening the "Reset" confirmation logic.

### 9. Keystore Password Brute Force
- **Description**: If the `server_keystore.p12` file is stolen, it can be subjected to offline brute-force or dictionary attacks.
- **Abuse Scenario**: An attacker guesses the password stored in `local.properties` or a weak user-defined password.
- **Mitigation Strategy**: Enforce minimum password complexity and use memory-hard key derivation functions (e.g., Argon2) if the format allows.

---

## ✅ Resolved

### 1. Immutability of Keys in RAM (String Leakage)
- **Description**: Sensitive cryptographic keys, IVs, and the "Golden Key" password are often handled as `String` objects or converted to Strings for logging/Base64 encoding. 
- **Status**: ✅ **Fixed**. Sensitive data handling now uses `CharArray` and explicit zeroing to prevent RAM leakage.

### 2. Lack of Hardware-Backed Security on JVM
- **Description**: The JVM server previously stored identity keys in a software-encrypted file.
- **Status**: ✅ **Fixed**. Integrated `java-keyring` to store the server's keystore password in OS-native secure storage.

### 3. Dialog Minimization Behavior
- **Description**: Minimizing a `DialogWindow` caused main window issues.
- **Status**: ✅ **Fixed**. Transitioned to independent `Window` components (via `AppDialog`). Added `closeOnMinimize` logic and `ConsoleViewModel` integration to provide snackbar feedback when a dialog is automatically closed due to minimization.

### 4. Console Interaction & Padding
- **Description**: The Console text area lacked sufficient bottom padding.
- **Status**: ✅ **Fixed**. Increased bottom padding to `200.dp` in `Console.kt`.

### 5. Macro Currency Deduction Timing
- **Description**: Currency could be deducted even if a macro failed to execute.
- **Status**: ✅ **Fixed**. Implemented `EXECUTION_START`, `EXECUTION_COMPLETE`, and `EXECUTION_FAILED` feedback loop.

### 6. Inspector Screenshot Limit
- **Description**: No limit on active screenshots in the Inspector.
- **Status**: ✅ **Fixed**. Implemented a configurable screenshot limit (default 10) in `InspectorViewModel`.

---

## 🟢 Monitoring

- **Client ID Spoofing**: We assume the Identity Key provided during the handshake is unique. If an attacker clones a trusted device's Identity Key (requires root/physical access to the phone), they can impersonate that device.
- **UDP Discovery Flooding**: A malicious actor could flood the network with fake server discovery packets to confuse the client UI.
