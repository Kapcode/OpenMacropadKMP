# Known Security Issues & Vulnerability Backlog

This document tracks identified security risks that have not yet been fully mitigated. For resolved issues, see [SECURITY.md](SECURITY.md).

## 🟡 Medium Priority

### 1. Security Audit of Pairing Process
- **Description**: With the introduction of "Sync (Fleet)" and multi-QR grids, the pairing process has increased in complexity. A formal security audit is needed to ensure no race conditions or unauthorized bypasses exist in the mass-provisioning flow.
- **Status**: 🔴 **Planned**.

---

## ✅ Resolved

### 2. Trust On First Use (TOFU) Gap
- **Description**: During the initial pairing of a new device, the client must "trust" the server's identity certificate without prior verification.
- **Status**: ✅ **Fixed**. Implemented out-of-band verification using a 6-digit code. The server generates a code and sends it to the client; both display it prominently during pairing for the user to verify.

### 3. Local Denial of Service (Keystore Corruption)
- **Description**: Although we have implemented "Self-Healing" with backups, a malicious local process can repeatedly delete or corrupt the `server_keystore.p12` file.
- **Abuse Scenario**: The user is forced into a loop of resetting their identity and re-pairing devices, rendering the application unusable.
- **Status**: ✅ **Fixed**. Strengthened the "Reset Identity" flow with a mandatory "RESET" string confirmation to prevent accidental or rapid-fire resets.

### 4. Keystore Password Brute Force
- **Description**: If the `server_keystore.p12` file is stolen, it can be subjected to offline brute-force or dictionary attacks.
- **Abuse Scenario**: An attacker guesses the password stored in `local.properties` or a weak user-defined password.
- **Status**: ✅ **Fixed**. Increased the auto-generated keystore password entropy to 64 bytes (Base64 encoded) and ensured it is stored in the OS-native secure keyring.

### 5. Unsafe Deserialization (Vulnerability 1)
- **Description**: Network protocol used Java `ObjectInputStream`, which is vulnerable to remote code execution (RCE).
- **Status**: ✅ **Fixed**. Migrated to `kotlinx.serialization` (JSON).

### 6. ClientId Spoofing (Vulnerability 2)
- **Description**: Server relied on self-reported `clientId` for authentication.
- **Status**: ✅ **Fixed**. Implemented Cryptographic Challenge-Response (ECDSA).

### 7. Pairing Code Interception (Vulnerability 3)
- **Description**: 6-digit verification code was sent over the network to the client.
- **Status**: ✅ **Fixed**. Implemented Out-of-band Verification (Server UI only).

### 8. Authentication Bypass via Raw Frames
- **Description**: Text frames could bypass security checks.
- **Status**: ✅ **Fixed**. Hardened server/client to ignore `Frame.Text`.

### 9. Immutability of Keys in RAM (String Leakage)
- **Description**: Sensitive cryptographic keys, IVs, and the "Golden Key" password are often handled as `String` objects or converted to Strings for logging/Base64 encoding. 
- **Status**: ✅ **Fixed**. Sensitive data handling now uses `CharArray` and explicit zeroing to prevent RAM leakage.

### 10. Lack of Hardware-Backed Security on JVM
- **Description**: The JVM server previously stored identity keys in a software-encrypted file.
- **Status**: ✅ **Fixed**. Integrated `java-keyring` to store the server's keystore password in OS-native secure storage.

### 11. Dialog Minimization Behavior
- **Description**: Minimizing a `DialogWindow` caused main window issues.
- **Status**: ✅ **Fixed**. Transitioned to independent `Window` components (via `AppDialog`). Added `closeOnMinimize` logic and `ConsoleViewModel` integration to provide snackbar feedback when a dialog is automatically closed due to minimization.

### 12. Console Interaction & Padding
- **Description**: The Console text area lacked sufficient bottom padding.
- **Status**: ✅ **Fixed**. Increased bottom padding to `200.dp` in `Console.kt`.

### 13. Macro Currency Deduction Timing
- **Description**: Currency could be deducted even if a macro failed to execute.
- **Status**: ✅ **Fixed**. Implemented `EXECUTION_START`, `EXECUTION_COMPLETE`, and `EXECUTION_FAILED` feedback loop. The Android client now tracks execution state and only deducts tokens upon successful command submission, with visual feedback for successes and failures.

### 14. Inspector Screenshot Limit
- **Description**: No limit on active screenshots in the Inspector.
- **Status**: ✅ **Fixed**. Implemented a configurable screenshot limit (default 10) in `InspectorViewModel`.

---

## 🟢 Monitoring

### 15. Security Rating & Human Risk
- **Current Security Rating**: **High**. 
- **Analysis**: This project uses **TLS with Pinning**, **Hardware-backed EC Keys**, and **Cryptographic Challenge-Response Authentication**.
- **Primary Remaining Risk (User Error)**: The biggest vulnerability is accidental or social-engineered approval of a malicious device. If an attacker triggers a pairing request while the user is actively interacting with the Desktop UI, the user may inadvertently click "Allow" without verifying the device details.
- **Detriments of Compromise**: Once a device is trusted, it inherits the full capability to execute any configured macro. Since macros simulate keyboard input and system commands, a compromised trust relationship allows an attacker to remotely control the host computer with the same privileges as the active user. This could result in:
    - **Remote Command Execution**: Running terminal commands or scripts.
    - **Data Exfiltration**: Typing commands to upload files or leak sensitive information.
    - **Credential Theft**: Using simulated keystrokes to interact with password managers or login prompts.
- **Mitigation**: Users must strictly verify that the **Device Name** and **6-digit Pairing Code** displayed on their phone exactly match the Desktop prompt before approving. Future updates will include "Recent Connections" logs to help users audit device activity.

- **Client ID Spoofing**: We assume the Identity Key provided during the handshake is unique. If an attacker clones a trusted device's Identity Key (requires root/physical access to the phone), they can impersonate that device.
    - **Future Mitigation**: Implement public/private key challenge-response during each session initiation to ensure the client possesses the private key corresponding to the registered ID.
- **UDP Discovery Flooding**: A malicious actor could flood the network with fake server discovery packets to confuse the client UI.
    - **Future Mitigation**: Implement rate-limiting and a "cooldown" period for processing incoming discovery packets in the `DiscoveryViewModel`.
