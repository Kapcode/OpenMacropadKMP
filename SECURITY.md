# Security Policy

This document outlines the security vulnerabilities identified in the current implementation of OpenMacropadKMP and the roadmap for addressing them.

## Identified Vulnerabilities

### 1. Unauthenticated Key Exchange (MitM)
*   **Status:** Identified
*   **Description:** The Diffie-Hellman (DH) exchange in `EncryptionManager.kt` is currently unauthenticated.
*   **Risk:** A Man-in-the-Middle (MitM) attacker can intercept and replace public keys, allowing them to decrypt and modify macro commands in real-time.
*   **Fix:** Implement Ed25519 signatures for the DH public keys to allow verification of the sender's identity.

### 2. Weak DH Key Size (Logjam Risk)
*   **Status:** Identified
*   **Description:** `EncryptionManager.kt` uses a 1024-bit key size for DH.
*   **Risk:** 1024-bit groups are vulnerable to discrete logarithm attacks (Logjam).
*   **Fix:** Increase `KEY_SIZE` to at least 2048-bit or migrate to X25519 (Elliptic Curve DH).

### 3. RSA 2048 and "Marvin" Timing Attacks
*   **Status:** Identified
*   **Description:** `IdentityManager.kt` uses RSA 2048 for client identity.
*   **Risk:** Potential vulnerability to timing attacks that can leak the private exponent.
*   **Fix:** Migrate to Ed25519 for identity keys, which is designed to be constant-time.

### 4. Hardcoded Keystore Credentials
*   **Status:** Identified
*   **Description:** Keystore password "changeit" is hardcoded in `IdentityManager.kt`.
*   **Risk:** Anyone with access to the APK or the filesystem can extract the private identity key.
*   **Fix:** Use the Android Keystore System for hardware-backed, non-exportable keys.

### 5. Lack of Input Validation on DH Public Keys
*   **Status:** Identified
*   **Description:** Peer public keys are not validated before use in `completeKeyExchange`.
*   **Risk:** Small subgroup attacks can make the resulting shared secret predictable.
*   **Fix:** Explicitly validate peer public keys and ensure usage of updated cryptographic libraries (Bouncy Castle 1.73+).

### 6. Data Persistence in ZRAM (Cold Storage Risk)
*   **Status:** Identified
*   **Description:** On systems like Pop!_OS, ZRAM can cause sensitive data (AES keys, private keys) to persist in compressed RAM swap longer than expected.
*   **Risk:** Sensitive data could be recovered from memory long after use.
*   **Fix:** Implement zeroing out of sensitive byte arrays immediately after use and explore memory pinning if applicable.

## Reporting a Vulnerability

If you discover a security vulnerability, please open an issue or contact the maintainers directly.
