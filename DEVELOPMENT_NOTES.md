# Development Notes

This document serves as a technical log for the challenges encountered and solutions implemented during the development of OpenMacropadKMP, particularly focusing on the Desktop Server component.

## Critical Technical Challenges & Solutions

### 1. Ktor 2.x to 3.x Migration (Multiplatform)

**Problem:**
Upgrading from Ktor 2.3.x to 3.0.x in a Kotlin Multiplatform project introduced severe dependency resolution conflicts and compilation errors.
*   **Version Mismatch:** The compiler reported `actual type is Duration, but Long was expected` for the `server.stop()` function.
*   **Classpath Pollution:** Legacy Ktor 2 artifacts (specifically `ktor-server-host-common`) were being transitively included, forcing old method signatures even when the version catalog was set to 3.0.x.
*   **Plugin Conflicts:** The `composeHotReload` plugin was incompatible with Kotlin 2.0 compiler flags, causing `Unsupported plugin option` crashes.

**Solution:**
*   **Nuclear Alignment:** Aligned the project to a stable "Golden Trio": **Kotlin 2.1.0**, **Compose Multiplatform 1.7.3**, and **Ktor 3.0.3**.
*   **Global Exclusions:** Added `exclude(group = "io.ktor", module = "ktor-server-host-common")` to `build.gradle.kts` to banish legacy Ktor 2 metadata.
*   **Explicit JVM Artifacts:** Switched to explicit `-jvm` suffixes for Ktor server dependencies in the Desktop target to remove resolution ambiguity.
*   **Forced Resolution:** Used `resolutionStrategy.force` to ensure Ktor 3.0.3 and Coroutines 1.10.1 were used globally, preventing silent downgrades.
*   **Duration API:** Migrated all time-based logic (WebSockets, server timeouts) to use the native `kotlin.time.Duration` API. *Note: As of 3.0.x, `EmbeddedServer.stop()` still requires `Long` milliseconds.*

### 2. JNativeHook and Swing Threading

**Problem:**
We initially observed severe input lag and "bursty" event delivery when using global hotkeys.
*   **EDT Blocking:** `GlobalScreen.setEventDispatcher(SwingDispatchService())` was forcing native events onto the Swing Event Dispatch Thread.

**Solution:**
*   **Removed Swing Dispatcher:** We removed the `SwingDispatchService`, allowing JNativeHook to run on its own dedicated thread.
*   **Offloaded Processing:** The `TriggerListener` now immediately offloads the trigger logic to a `listenerScope.launch { ... }` coroutine.

### 3. Robot Automation Concurrency

**Problem:**
Multiple macros triggered rapidly led to system congestion and `java.awt.Robot` state conflicts.

**Solution:**
*   **State Isolation:** We now create a **new `MacroPlayer` instance** for every macro execution.
*   **Serialization (Mutex):** Implemented a `Mutex` in `MacroManagerViewModel` with a **"Drop if Running"** policy (`tryLock()`). If a macro is triggered while another is running, the new trigger is ignored.

### 4. Window Focus and Robot Idle Waiting

**Problem:**
Macros would only execute *after* the application window lost focus.

**Solution:**
*   **Disable Wait for Idle:** We explicitly set `isAutoWaitForIdle = false` in `MacroPlayer`. We now handle timing manually using coroutine `delay()`.

### 5. Safety Mechanisms (E-Stop)

**Problem:**
Automated macros could cause loss of system control if they ran too long or went rogue.

**Solution:**
*   **Global E-Stop:** Implemented a configurable Emergency Stop key (default F12).
*   **Implementation:** The `TriggerListener` intercepts this key and calls `cancelAllMacros()`, which triggers `cancelChildren()` on the playback `SupervisorJob`.

## 6. Server-Client Connectivity

### Challenge: SSL/TLS Connection Failure & Keystore Management
- **Problem**: Desktop server crash in packaged builds due to missing/incorrect `keystore.p12`.
- **Solution**: 
    1.  **Generation**: Created a self-signed PKCS12 keystore via `keytool`.
    2.  **Resource Loading**: Modified `MacroKtorServer` to load the keystore as a resource stream from the classpath, ensuring it works in `/opt/` or other system directories.

### Challenge: Client Identification and Device Naming
- **Problem**: Devices appeared as "Unknown" due to inconsistent query parameters.
- **Solution**: Updated `MacroKtorServer` to check both `name` and `deviceName` parameters and generate unique `UUID`s for persistent client tracking.

## 7. Desktop Packaging & Distribution

### Challenge: JNativeHook "Permission Denied" on Linux
- **Problem:** Packaging into `.deb` or `/opt/` caused `UnsatisfiedLinkError` (Permission denied).
- **Solution:** 
    1.  **User Group**: Instructions added to add the user to the `input` group.
    2.  **Executable Bit**: Added a `doLast` Gradle task to `packageDistributionForCurrentOS` that runs `setExecutable(true)` on the native `.so` files during packaging.

## 8. State Management on Desktop

### Challenge: Macro Switch State Was Not Persistent
- **Problem**: Active/Inactive states for macros reset on app restart.
- **Solution**: Implemented a `.open-macropad-active-macros.properties` file in the user's home directory to store and reload switch states.

## 9. Compose for Desktop Dialogs

### Challenge: Dialogs Hidden by Swing Components
- **Problem**: Standard `AlertDialog` was obscured by the Swing-based code editor.
- **Solution**: Replaced `AlertDialog` with `DialogWindow` to ensure dialogs are always top-level and visible.

## 10. Android Freemium Model

### Challenge: Rewarded Ad-Based Token System
- **Problem**: Monetization without a paywall.
- **Solution**: Created a `TokenManager` singleton using `SharedPreferences` to manage a token-based economy where users earn tokens by watching AdMob rewarded ads.

## Architecture Overview

*   **UI Layer:** Jetpack Compose Multiplatform (Material 3).
*   **State Management:** `ViewModel` pattern using `StateFlow`.
*   **Input Handling:** `JNativeHook` for global keyboard listening.
*   **Automation:** `java.awt.Robot` for input simulation.
*   **Persistence:** JSON for macro definitions, `Properties` for app state.

## Development & Building Instructions

### General Prerequisites
*   **JDK 17 or higher** (e.g., Eclipse Temurin).
*   **JAVA_HOME** must be set to the JDK path.

### Building the Desktop App
```bash
# Runnable JAR
./gradlew clean :composeApp:packageUberJarForCurrentOS
./gradlew :composeApp:stripSignaturesFromUberJar

# Distribution (.deb, .msi, .dmg)
./gradlew :composeApp:packageDistributionForCurrentOS
```

## 11. Security Hardening & KMP Cryptography

### Challenge: Authenticating the Handshake (MitM Protection)
- **Problem**: The initial Diffie-Hellman exchange was anonymous, making it vulnerable to interception.
- **Solution**: Implemented an authenticated handshake in `SecureSocket.kt`. 
    - **Identity**: Migrated from RSA-2048 to **Elliptic Curve (secp256r1)** for faster and more secure signatures.
    - **Signature**: During the handshake, both parties sign their ephemeral DH public keys. The peer verifies the signature using the other party's long-term public identity key before deriving the shared AES secret.

### Challenge: Platform-Specific Key Storage
- **Problem**: Storing private keys securely across different platforms.
- **Solution**: 
    - **Android**: Integrated with the **Android Keystore System**. Private keys are generated within the hardware-backed TEE (Trusted Execution Environment) or StrongBox, ensuring they cannot be exported even if the device is rooted.
    - **JVM**: Currently uses a local file storage with future plans for OS-level secret vault integration (e.g., Gnome Keyring, Windows Credential Manager).

### Challenge: Android 7.0 (API 24) Compatibility
- **Problem**: `java.util.Base64` was introduced in API 26, causing crashes on older Android devices.
- **Solution**: Created a KMP-safe `Base64Utils` object.
    - `androidMain` uses `android.util.Base64`.
    - `jvmMain` uses `java.util.Base64`.
    - This allows `commonMain` code (like `DataModel`) to remain platform-agnostic while supporting older Android versions.

### Challenge: Memory Security (Fix 6)
- **Problem**: Sensitive cryptographic keys persisting in RAM.
- **Solution**: Updated `EncryptionManager.kt` to explicitly call `.fill(0)` on `ByteArray` objects containing keys and IVs immediately after their use in cryptographic operations.

## 12. UI Modernization & Code Standards

### Challenge: Material 3 Component Migrations
- **Problem**: Compiler warnings from deprecated Material 3 components and experimental APIs.
- **Solution**:
    - **Dividers**: Replaced `Divider` with `HorizontalDivider`.
    - **Dropdown Menus**: Updated `menuAnchor()` to `menuAnchor(MenuAnchorType.PrimaryNotEditable)` for read-only fields to comply with the latest M3 API.
    - **Icons**: Switched to `AutoMirrored` variants for directional icons (e.g., `Icons.AutoMirrored.Filled.ArrowBack`) to support RTL layouts automatically.

### Challenge: Package Naming Conventions
- **Problem**: Non-standard uppercase package names (`Model`, `Client`, `Server`) in the `network.sockets` module.
- **Solution**: Refactored the entire `sockets` hierarchy to use lowercase package names: `com.kapcode.open.macropad.kmps.network.sockets.[model|client|server]`. This aligns with Kotlin's official naming conventions and improves project consistency.

### Challenge: Experimental API Management
- **Problem**: Use of `ExperimentalSplitPaneApi` in the JVM target required repeated `@OptIn` annotations.
- **Solution**: Standardized the use of `@OptIn(ExperimentalSplitPaneApi::class)` where necessary to maintain build stability and clear intent for experimental component usage.
