# Known Security Issues & Vulnerability Backlog

This document tracks identified security risks that have not yet been fully mitigated. For resolved issues, see [SECURITY.md](SECURITY.md).

## 🟡 Medium Priority

### 7. Trust On First Use (TOFU) Gap
- **Description**: During the initial pairing of a new device, the client must "trust" the server's identity certificate without prior verification.
- **Status**: ✅ **Fixed**. Implemented out-of-band verification using a 6-digit code. The server generates a code and sends it to the client; both display it prominently during pairing for the user to verify.

### 8. Local Denial of Service (Keystore Corruption)
- **Description**: Although we have implemented "Self-Healing" with backups, a malicious local process can repeatedly delete or corrupt the `server_keystore.p12` file.
- **Abuse Scenario**: The user is forced into a loop of resetting their identity and re-pairing devices, rendering the application unusable.
- **Status**: ✅ **Fixed**. Strengthened the "Reset Identity" flow with a mandatory "RESET" string confirmation to prevent accidental or rapid-fire resets.

### 9. Keystore Password Brute Force
- **Description**: If the `server_keystore.p12` file is stolen, it can be subjected to offline brute-force or dictionary attacks.
- **Abuse Scenario**: An attacker guesses the password stored in `local.properties` or a weak user-defined password.
- **Status**: ✅ **Fixed**. Increased the auto-generated keystore password entropy to 64 bytes (Base64 encoded) and ensured it is stored in the OS-native secure keyring.

### 10. Security Audit of Pairing Process
- **Description**: With the introduction of "Sync (Fleet)" and multi-QR grids, the pairing process has increased in complexity. A formal security audit is needed to ensure no race conditions or unauthorized bypasses exist in the mass-provisioning flow.
- **Status**: 🔴 **Planned**.

---

## ✅ Resolved

### 11. Unsafe Deserialization (Vulnerability 1)
- **Description**: Network protocol used Java `ObjectInputStream`, which is vulnerable to remote code execution (RCE).
- **Status**: ✅ **Fixed**. Migrated to `kotlinx.serialization` (JSON).

### 12. ClientId Spoofing (Vulnerability 2)
- **Description**: Server relied on self-reported `clientId` for authentication.
- **Status**: ✅ **Fixed**. Implemented Cryptographic Challenge-Response (ECDSA).

### 13. Pairing Code Interception (Vulnerability 3)
- **Description**: 6-digit verification code was sent over the network to the client.
- **Status**: ✅ **Fixed**. Implemented Out-of-band Verification (Server UI only).

### 14. Authentication Bypass via Raw Frames
- **Description**: Text frames could bypass security checks.
- **Status**: ✅ **Fixed**. Hardened server/client to ignore `Frame.Text`.

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
- **Status**: ✅ **Fixed**. Implemented `EXECUTION_START`, `EXECUTION_COMPLETE`, and `EXECUTION_FAILED` feedback loop. The Android client now tracks execution state and only deducts tokens upon successful command submission, with visual feedback for successes and failures.

### 6. Inspector Screenshot Limit
- **Description**: No limit on active screenshots in the Inspector.
- **Status**: ✅ **Fixed**. Implemented a configurable screenshot limit (default 10) in `InspectorViewModel`.

---

## 🟢 Monitoring

- **Client ID Spoofing**: We assume the Identity Key provided during the handshake is unique. If an attacker clones a trusted device's Identity Key (requires root/physical access to the phone), they can impersonate that device.
    - **Future Mitigation**: Implement public/private key challenge-response during each session initiation to ensure the client possesses the private key corresponding to the registered ID.
- **UDP Discovery Flooding**: A malicious actor could flood the network with fake server discovery packets to confuse the client UI.
    - **Future Mitigation**: Implement rate-limiting and a "cooldown" period for processing incoming discovery packets in the `DiscoveryViewModel`.
