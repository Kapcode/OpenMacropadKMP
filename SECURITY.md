# Security Policy

This document outlines the security vulnerabilities identified in OpenMacropadKMP and the status of their mitigations.

## Status of Vulnerability Mitigations

### 1. Unauthenticated Key Exchange (MitM)
*   **Status:** ✅ **Fixed**
*   **Description:** The Diffie-Hellman (DH) exchange was previously unauthenticated.
*   **Mitigation:** Implemented an authenticated handshake in `SecureSocket.kt`. Both client and server now sign their DH public keys using their long-term identity keys (EC secp256r1). The peer verifies the signature before completing the exchange, preventing Man-in-the-Middle (MitM) attacks.

### 2. Weak DH Key Size (Logjam Risk)
*   **Status:** ✅ **Fixed**
*   **Description:** Previously used a 1024-bit key size for DH.
*   **Mitigation:** Increased `KEY_SIZE` to 2048-bit in `EncryptionManager.kt` to protect against discrete logarithm attacks.

### 3. RSA 2048 and "Marvin" Timing Attacks
*   **Status:** ✅ **Fixed**
*   **Description:** `IdentityManager.kt` used RSA 2048 for client identity.
*   **Mitigation:** Migrated from RSA to Elliptic Curve (ECDSA with `secp256r1`) in `IdentityManager.kt`. This provides better security with smaller keys and improved resistance to certain timing attacks.

### 4. Hardcoded Keystore Credentials
*   **Status:** ✅ **Fixed**
*   **Description:** Keystore credentials were previously hardcoded in the source code.
*   **Mitigation:** 
    *   **Android:** Migrated to the hardware-backed **Android Keystore System**. Keys are now non-exportable and protected by the TEE/StrongBox.
    *   **JVM:** 
        *   **Local Storage:** Migrated to an automatic, local-only keystore generation in `KeystoreUtils.kt`. The keystore is stored in `~/.openmacropad/` and is never committed to Git.
        *   **Secret Management:** Passwords are now managed via `local.properties` (machine-local) and injected at build time via JVM System Properties (`-Dkeystore.password`).
        *   **Safety Net:** Implemented a non-destructive recovery path. If the password fails, the app prompts the user for consent. If the user chooses to reset, the old keystore is backed up with a timestamp (e.g., `server_keystore.p12.20231027_120000.bak`) instead of being deleted.
        *   **File Permissions:** The application automatically attempts to set the keystore file permissions to `600` (Owner Read/Write only) on POSIX-compliant systems (Linux/macOS) to prevent local privilege escalation.
        *   **Handshake Unification:** The server's SSL certificate and its long-term identity keys are now unified within the same password-protected keystore.
        *   **Next Steps:** Full integration with OS-level secret storage (Windows Credential Manager / macOS Keychain) is planned for production releases.

### 5. Lack of Input Validation on DH Public Keys
*   **Status:** ✅ **Fixed**
*   **Description:** Peer public keys were not validated before use.
*   **Mitigation:** Implemented explicit public key validation using Bouncy Castle in `EncryptionManager.kt`. This prevents small subgroup attacks and invalid curve attacks.

### 6. Data Persistence in ZRAM (Cold Storage Risk)
*   **Status:** 🔄 **In Progress**
*   **Description:** Sensitive data (AES keys) can persist in memory.
*   **Mitigation:** Updated `EncryptionManager.kt` to explicitly zero out sensitive `ByteArray` objects after use. Note: Java's `String` immutability still poses a risk if keys are converted to Strings (see `Base64Utils` usage).

### 7. Base64 API Compatibility (Android < 8.0)
*   **Status:** ✅ **Fixed**
*   **Description:** `java.util.Base64` required API level 26, causing crashes on Android 7.x (API 24/25).
*   **Mitigation:** Implemented a KMP-safe `Base64Utils` that uses `android.util.Base64` on Android and `java.util.Base64` on JVM.

## Reporting a Vulnerability

If you discover a security vulnerability, please open an issue or contact the maintainers directly.
