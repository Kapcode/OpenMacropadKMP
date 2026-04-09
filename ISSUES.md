# Known Security Issues & Vulnerability Backlog

This document tracks identified security risks that have not yet been fully mitigated. For resolved issues, see [SECURITY.md](SECURITY.md).

## 🔴 High Priority

### 1. Immutability of Keys in RAM (String Leakage)
- **Description**: Sensitive cryptographic keys, IVs, and the "Golden Key" password are often handled as `String` objects or converted to Strings for logging/Base64 encoding. In the JVM/Android runtime, `String` objects are immutable and cannot be zeroed out of memory after use.
- **Abuse Scenario**: An attacker with local access or a specialized exploit could perform a memory dump (RAM/ZRAM) of the running application and recover sensitive plain-text keys.
- **Mitigation Strategy**: Transition sensitive data handling to `ByteArray`, `CharArray`, or `ByteBuffer` which can be explicitly filled with zeros (`.fill(0)`) immediately after use.

### 2. Lack of Hardware-Backed Security on JVM
- **Description**: While the Android client uses the hardware-backed TEE/StrongBox, the JVM server stores its long-term identity keys in a software-encrypted `.p12` file on the filesystem.
- **Abuse Scenario**: If the user's OS account is compromised or the physical disk is stolen (without full disk encryption), the identity key can be exfiltrated.
- **Mitigation Strategy**: Integrate with OS-native secret management APIs: **Windows Credential Manager**, **macOS Keychain**, and **Gnome Keyring/KSecretService** for Linux.

---

## 🟡 Medium Priority

### 3. Dialog Minimization Behavior
- **Description**: Minimizing a `DialogWindow` (e.g., Settings, Pairing Request) causes both the dialog and the main application window to minimize simultaneously.
- **Current Behavior**: The dialogs use `alwaysOnTop = true`. When the minimize button on the dialog is pressed, the entire application stack is minimized by the OS window manager.
- **Expected Behavior**: Minimizing a dialog should either be disabled (since they are modal-like) or only minimize the dialog itself without affecting the main window.

### 4. Console Interaction & Padding
- **Description**: The Console text area lacks sufficient bottom padding, making it difficult to read the latest logs or interact with the UI on vertical monitor setups or when using auto-hiding taskbars.
- **Requested Fix**: Add significant bottom padding (e.g., `100.dp` or a large spacer) to the end of the console scrollable area to ensure the last log line is never obstructed by OS UI elements.

### 5. Trust On First Use (TOFU) Gap
- **Description**: During the initial pairing of a new device, the client must "trust" the server's identity certificate without prior verification.
- **Abuse Scenario**: A sophisticated Man-in-the-Middle (MitM) attacker could spoof the server's identity during the *very first* connection attempt before the identity is pinned.
- **Mitigation Strategy**: Optional out-of-band verification (e.g., displaying a 6-digit hash/QR code on the desktop that the user verifies on the mobile device).

### 6. Local Denial of Service (Keystore Corruption)
- **Description**: Although we have implemented "Self-Healing" with backups, a malicious local process can repeatedly delete or corrupt the `server_keystore.p12` file.
- **Abuse Scenario**: The user is forced into a loop of resetting their identity and re-pairing devices, rendering the application unusable.
- **Mitigation Strategy**: This is largely an OS-level permissions issue, but can be further mitigated by strengthening the "Reset" confirmation logic.

### 7. Keystore Password Brute Force
- **Description**: If the `server_keystore.p12` file is stolen, it can be subjected to offline brute-force or dictionary attacks.
- **Abuse Scenario**: An attacker guesses the password stored in `local.properties` or a weak user-defined password.
- **Mitigation Strategy**: Enforce minimum password complexity and use memory-hard key derivation functions (e.g., Argon2) if the format allows.

---

## 🟢 Monitoring

- **Client ID Spoofing**: We assume the Identity Key provided during the handshake is unique. If an attacker clones a trusted device's Identity Key (requires root/physical access to the phone), they can impersonate that device.
- **UDP Discovery Flooding**: A malicious actor could flood the network with fake server discovery packets to confuse the client UI.
