# Security Policy

This document outlines the security vulnerabilities identified in OpenMacropadKMP and the status of their mitigations.

## Status of Vulnerability Mitigations

### 1. Unsafe Deserialization (RCE)
*   **Status:** ✅ **Fixed**
*   **Description:** The network protocol previously used Java `ObjectInputStream`, which is vulnerable to remote code execution (RCE) through malicious serialized objects.
*   **Mitigation:** Migrated the entire `DataModel` to **`kotlinx.serialization` (JSON)**. This provides a type-safe, multiplatform-compatible way to handle data without the risks associated with Java serialization. All communication now uses binary-wrapped JSON over WebSockets.

### 2. ClientId Spoofing
*   **Status:** ✅ **Fixed**
*   **Description:** The server previously relied on a self-reported `clientId` for authentication, allowing any connected client to impersonate a trusted device.
*   **Mitigation:** Implemented **Cryptographic Challenge-Response** authentication.
    - The server issues a random UUID challenge upon connection.
    - The client signs this challenge using its platform-specific private EC key (secp256r1).
    - The server verifies the signature against the stored public key for that `clientId`. Trust is only granted if the signature is valid.
    - **Security Fix (Identity Mismatch):** ✅ **Fixed**. The server now strictly verifies that the public key provided in the `AUTH_RESPONSE` exactly matches the `clientId` used to establish the session. This prevents an attacker from signing a challenge with their own key but claiming to be a different, trusted user.

### 3. Pairing Code Interception
*   **Status:** ✅ **Fixed**
*   **Description:** The 6-digit verification code was previously sent over the network to the client, making it visible to passive network attackers.
*   **Mitigation:** **Out-of-band Verification**. The pairing code is now strictly displayed on the server's physical screen (Desktop UI). The user must manually enter it on the client or scan it via QR code. The server never sends the code to the client; it only acknowledges if the client-submitted code matches.

### 4. Authentication Bypass via Raw WebSocket Frames
*   **Status:** ✅ **Fixed**
*   **Description:** Ktor's `webSocket` route processed both `Frame.Binary` and `Frame.Text`, allowing an attacker to send unencrypted text commands to bypass security checks.
*   **Mitigation:** Both `MacroKtorServer` and `MacroKtorClient` have been hardened to **explicitly ignore `Frame.Text`**. All communication is strictly binary, containing serialized JSON `DataModel` objects.

### 5. Lack of Input Validation on DH Public Keys
*   **Status:** ✅ **Fixed**
*   **Description:** Peer public keys were not validated before use.
*   **Mitigation:** Implemented explicit public key validation using Bouncy Castle in `EncryptionManager.kt`. This prevents small subgroup attacks and invalid curve attacks.

### 6. Data Persistence in RAM (Cold Storage Risk)
*   **Status:** ✅ **Fixed**
*   **Description:** Sensitive data (AES keys, passwords) could previously persist in memory due to the use of immutable `String` objects.
*   **Mitigation:** Transitioned sensitive data handling to `CharArray` and `ByteArray` which are explicitly zeroed out (`.fill('\u0000')`) immediately after use. This minimizes the window of opportunity for memory scraping attacks.

### 7. Base64 API Compatibility (Android < 8.0)
*   **Status:** ✅ **Fixed**
*   **Description:** `java.util.Base64` required API level 26, causing crashes on Android 7.x (API 24/25).
*   **Mitigation:** Implemented a KMP-safe `Base64Utils` that uses `android.util.Base64` on Android and `java.util.Base64` on JVM.

### 8. Physical Consent Pairing & Device Management
*   **Status:** ✅ **Fixed**
*   **Description:** Any device could previously connect and send macro commands if they knew the server IP.
*   **Mitigation:**
    *   **Trust on First Use (TOFU) with Physical Consent:** ✅ **Fixed**. Untrusted devices trigger a modal pairing dialog on the server's physical screen. Implemented out-of-band verification using a random 6-digit code displayed on the server and a **QR Code** for scanning. A human must manually verify or scan the code and "Approve" the connection. **Note:** Sensitive PINs are no longer transmitted over the network in the `PAIRING_PENDING` message, preventing passive sniffing of pairing codes.
    *   **Persistent Whitelisting:** Approved devices are stored by their unique Device ID fingerprint in `trusted_devices.json`.
    *   **Banning System:** Malicious or spammy devices can be "Banned," adding them to a `banned_devices.json` blacklist. Banned devices are blocked at the network level and cannot trigger further pairing prompts.
    *   **Unpairing:** Users can "Unpair" a device, which removes its trusted status and forces a new pairing request upon the next connection attempt.
    *   **Macro Execution Feedback:** ✅ **Fixed**. Implemented `EXECUTION_START`, `EXECUTION_COMPLETE`, and `EXECUTION_FAILED` control loop. The mobile client provides real-time visual feedback (progress bars and error states) for macro execution, ensuring tokens are only deducted for valid requests and preventing "blind" execution errors.
    *   **Global Lockdown (Device Discovery):** A "Device Discovery" toggle in Settings controls whether the server's presence is broadcast via UDP. Disabling this makes the server invisible to mobile app scans, though manual connections by IP are still possible (but still subject to pairing approval).
    *   **One-Time Approvals ONLY:** An "Ask Every Time (One-Time Approvals ONLY)" mode can be enabled to force all new connections to be non-persistent. In this mode, the "Always Allow" option is removed from the pairing dialog, ensuring no new devices are added to the trusted list and a human must approve every session.
    *   **UI Safety:** All security-critical dialogs (Pairing, Settings, Ban) use `alwaysOnTop = true` to ensure they remain visible over other application windows.

### 9. Man-in-the-Middle (MITM) via Unsafe SSL
*   **Status:** ✅ **Fixed**
*   **Description:** The Android client previously trusted all SSL certificates to support the server's self-signed certificate, leaving it vulnerable to MITM attacks.
*   **Mitigation:** Implemented **Certificate Pinning**.
    - The Desktop server calculates its unique SHA-256 certificate fingerprint.
    - This fingerprint is shared with the client during UDP discovery and the initial pairing handshake.
    - Upon successful pairing, the client saves this fingerprint in secure storage (`ServerStorage`).
    - Every subsequent connection uses a hardened `OkHttpClient` that validates the server's certificate against the pinned fingerprint. If the fingerprints do not match, the connection is immediately terminated.

## Reporting a Vulnerability

If you discover a security vulnerability, please open an issue or contact the maintainers directly.
