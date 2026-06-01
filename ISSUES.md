# Known Issues & Backlog

This document tracks identified bugs, security risks, and pending tasks.

## 🔴 High Priority
- [ ] **Background Connectivity (Android)**: Maintain a heartbeat connection while the app is in the background to avoid reconnect delays.
- [ ] **UDP Discovery Polish**: Improve reliability on complex local network topologies (multiple subnets).
- [ ] **iOS Client Port**: Initial porting to iOS using Compose Multiplatform.

## 🟡 Medium Priority
- [ ] **Macro Templates**: Predefined templates for popular software (OBS, Photoshop, VS Code).
- [ ] **Automatic Updates**: Background update checker for the desktop client.
- [ ] **Fleet Mode Enhancements**: Remote device status monitoring from the server.

---

## ✅ Resolved

### 1. Kap Balance Sync Delay
- **Fixed**: Updated `ClientActivity.kt` to send `currency_update` immediately upon connection.

### 2. Desktop "X" Close Button
- **Fixed**: Consistently applied `exitBehavior` across all exit triggers.

### 3. Routine Builder NPE
- **Fixed**: Implemented safe state-copying in the builder UI.

### 4. Tab Focus Traversal (Desktop)
- **Fixed**: Custom `tabFocus()` modifier for text fields.

### 5. Fragmented Ad Configurations
- **Fixed**: Centralized Ad IDs in `BillingConstants.kt`.

### 6. Security Audit (Pairing Process)
- **Fixed**: Brute-force protection (3-strikes), state hardening, and identity binding.

### 7. Trust On First Use (TOFU) Gap
- **Fixed**: Out-of-band verification with 6-digit PIN and QR code.

### 8. Keystore Password Protection
- **Fixed**: Entropy increased to 64 bytes and stored in OS-native secure keyring.

### 9. Unsafe Deserialization
- **Fixed**: Migrated to `kotlinx.serialization` (JSON).

### 10. ClientId Spoofing
- **Fixed**: Cryptographic Challenge-Response (ECDSA).

### 11. Authentication Bypass via Raw Frames
- **Fixed**: Hardened server/client to ignore `Frame.Text`.

### 12. Hardware Metadata Binding
- **Fixed**: Bound Client ID to stable hardware fingerprint.

### 13. Documentation Consistency
- **Fixed**: Comprehensive audit and update of all `.md` files for correctness and cross-referencing.
