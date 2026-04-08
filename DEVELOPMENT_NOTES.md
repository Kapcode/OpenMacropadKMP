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

## 13. Android Startup Performance

### Challenge: Excessive Cold Start Latency (18s+ Black Screen)
- **Problem**: The app experienced a massive delay before the first frame was drawn, resulting in a long black screen.
- **Solution**:
    - **Modern Splash Screen API**: Integrated `androidx.core:core-splashscreen` to provide an immediate system-level splash screen (black background with a high-res, centered app icon).
    - **Theme Hand-off**: Created a custom `Theme.App.Starting` theme that handles the transition from the OS splash to the app's internal theme seamlessly.
    - **Removed Artificial Delays**: Eliminated legacy code that introduced a 2-second `delay()` and a manual splash screen state in `MainActivity`, allowing the Compose UI to begin rendering as early as possible.
    - **High-Resolution Assets**: Used a `layer-list` XML drawable (`splash_icon_centered.xml`) to wrap the 512px PNG icon, ensuring it remains sharp and perfectly centered across all device densities without blurriness.

## 14. Extreme Android Startup Optimization (The "Nuclear" Fix)

### Challenge: 20-Second "Silent" Cold Start Delay
- **Problem**: Even with basic splash screen optimization, the app suffered a ~15-20 second delay on cold start (13.5s in logs, plus silent OS overhead).
- **Diagnosis**: 
    1.  **DEX Verification**: The Android Runtime (ART) was spending ~8 seconds verifying thousands of unused classes from **Ktor Server/Netty** bundled into the Android APK.
    2.  **Synchronous Init**: Firebase, AdMob, and Jetpack Startup providers were blocking the main process start before `Application.onCreate`.
    3.  **UI Thread Blocking**: Eager initialization of `ClientDiscovery` and `SettingsViewModel` blocked the initial Compose `setContent` call.

### Solution: Multi-Layered Optimization
- **R8 Minification (The Breakthrough)**: 
    - Enabled `isMinifyEnabled = true` for **debug** builds. 
    - Added specific `proguard-rules.pro` to strip out the massive Ktor-Server/Netty dependencies from the Android APK. 
    - This reduced the "Pre-Code" verification gap from **8.4s to 3.2s**.
- **Content Provider Removal**:
    - Used `tools:node="remove"` in `AndroidManifest.xml` to disable `FirebaseInitProvider`, `MobileAdsInitProvider`, and `androidx.startup.InitializationProvider`.
    - Manually initialized Firebase and AdMob on a background thread (`Dispatchers.IO`) only *after* the UI was visible.
- **Lazy Initialization**:
    - Converted `ClientDiscovery` and `SettingsViewModel` into `lazy` properties in `MainActivity`.
    - Delayed networking class loading until the user explicitly clicks "Scan".
- **SharedPreferences Warming**:
    - Triggered a dummy `getSharedPreferences` call in `MacroApplication.onCreate` to warm up the disk-to-memory cache for `TokenManager`.
- **UI Optimization**:
    - Removed redundant `MaterialTheme` nesting in `App.kt`, shaving ~800ms off the `setContent` phase.

### Result: 
**Total Startup Time reduced from ~20s to ~1.2s - 2.2s on modern hardware (e.g., Pixel 7, Pixel 9a), and ~8s on budget/older hardware like the Amazon Fire HD 8 (10th Gen).**

## 15. UI Feedback & Interactive Loading

### Challenge: Invisible Startup Progress
- **Problem**: During the 500ms splash screen and subsequent initialization, the UI appeared static, leading to a "frozen" feel despite the improved startup speed.
- **Solution**:
    - **BlinkingCursor**: Implemented a retro-style blinking terminal cursor (`> _`) using `rememberInfiniteTransition` for robust, frame-perfect animation even during heavy main-thread load.
    - **Minimum Alpha**: Set a `minAlpha` of 20/255 for the "off" state of the cursor, ensuring it remains slightly visible to maintain visual continuity.
    - **Optimized Timing**: Tuned the blink cycle to 400ms (200ms ON / 200ms OFF) to guarantee at least one full blink cycle occurs during the 500ms splash screen delay, returning the cursor to full visibility just as the transition ends.
    - **ThreeDotsLoading**: Added a staggered scaling animation for server discovery, providing immediate feedback when the UDP scan is active.
    - **Stop Scanning**: Added a manual "Stop" button (red 'X') to allow users to halt discovery, preventing unnecessary network traffic and battery drain once a server is found.

## 16. Amazon Fire Tablet Optimization

### Challenge: Generic Device Names (The "KFONWI" Problem)
- **Problem**: Amazon Fire tablets often report a generic model name like "KFONWI" instead of a user-defined name, making it hard to identify devices in the server list.
- **Solution**:
    - **Aggressive Name Lookup**: Updated `DeviceInfo.kt` on Android to query multiple system settings providers (`Global`, `Secure`, and `System`).
    - **Key Search**: Specifically looks for `device_name` and `bluetooth_name` keys, which often contain the user-friendly name set during tablet setup.
    - **Stability**: Combines the user-friendly name with a short (4-character) anonymous SHA-256 hash of the `ANDROID_ID` to create a stable, unique, and privacy-respecting identity: `Kyle's Fire-a1b2`.

## 17. Android Splash Screen Perfection (The "Zero-Ring" Transition)

### Challenge: Android 12+ Splash Screen "Black Ring" Artifact
- **Problem**: On Android 12 and above, the system automatically applies a circular mask to splash icons. If the icon has transparency or an inconsistent background, the OS adds a high-contrast "black ring" around it. Additionally, large high-res icons (512px) often suffered from downsampling artifacts during the early boot phase.
- **Solution**:
    - **True Adaptive Icon**: Created a proper `<adaptive-icon>` in `drawable-v26` with separate foreground and background layers.
    - **Safe-Zone Scaling**: Wrapped the icon in a `layer-list` with an explicit `192dp` container. This ensures the 512px source image fits perfectly within the "safe circle" (roughly 72dp of actual content) enforced by the OS, preventing clipping.
    - **Transparency Fix**: Set `windowSplashScreenIconBackgroundColor` to `@android:color/transparent` in `themes.xml`. This explicitly tells the Android OS not to generate a background "ring" for contrast.
    - **Pre-Downsized Assets**: Switched to a pre-scaled 192px PNG (`splash_icon_downsized.png`) for the system splash to avoid real-time interpolation shimmer.
    - **Visual Continuity**: Synchronized the Compose `SplashUI` to use the exact same `192dp` centering and icon-to-cursor spacing. This creates a seamless "hand-off" where the system icon remains perfectly still as the Compose UI takes over and begins the cursor blink animation.

## 18. Desktop Taskbar & System Tray Integration

### Challenge: Unreliable "Minimize to Tray" Behavior
- **Problem**: The initial implementation for iconifying the application to the system tray was unstable across different OS environments, lacking proper "Restore" behavior and clear user feedback.
- **Solution**:
    - **Native Tray Integration**: Leveraged Compose for Desktop's `Tray` API to provide a stable, OS-native icon and context menu.
    - **Dynamic Menu Items**: Implemented a state-aware context menu that changes its labels (e.g., "Show Main Window" vs. "Hide to Tray") based on the current window visibility.
    - **Primary Action Support**: Added an `onAction` handler (primary/left-click) to toggle the window state instantly, providing a familiar and snappy user experience.
    - **User Notification**: Created a dedicated `MinimizeToTrayDialog` with high-visibility Material 3 typography to inform the user when the application is continuing to run in the background. Included a "Don't show again" option that persists in `AppSettings`.
    - **Settings Toggle**: Added a "Minimize to System Tray" checkbox in the main Settings dialog, allowing users to choose between full exit and background mode on window close.

## 19. To-Do List & Future Improvements

### Android Client
- [x] **Optimize Dependency Initialization**: Transitioned from synchronous `ContentProvider`-based initialization to lazy, background-thread initialization for Firebase and AdMob.
- [x] **Extreme Android Startup Optimization**: Reduced cold start from ~20s to ~1.2s by enabling R8 in debug, removing blocking Content Providers, and using lazy initialization.
- [x] **UI Feedback**: Implemented `BlinkingCursor` and `ThreeDotsLoading` with `rememberInfiniteTransition` for reliable feedback during startup and discovery.
- [ ] **Background Connectivity**: Maintain a heartbeat connection with the desktop server while the app is in the background to avoid reconnect delays.
- [ ] **Customizable UI**: Allow users to rearrange macro buttons on the mobile interface.

### Desktop Server
- [x] **Platform Identifiers**: Implemented `DeviceInfo` (expect/actual) to provide stable, unique, and privacy-safe device names and IDs across Android and JVM.
- [x] **Amazon Tablet Fix**: Specifically improved device naming for Fire tablets by querying `Global` and `Secure` settings for `device_name`.
- [x] **Taskbar/Tray Polish**: Overhauled the system tray implementation with dynamic context menus, primary click toggling, and a user notification dialog on first minimize.
- [ ] **OS-Level Secret Vault**: Implement native secure storage for identity keys using Gnome Keyring (Linux), Keychain (macOS), and Credential Manager (Windows).
- [ ] **Macro Templates**: Add predefined templates for popular software (e.g., OBS, Photoshop, VS Code).
- [ ] **Automatic Updates**: Integrate a background update checker for the desktop client.

### Cross-Platform / Common
- [ ] **UDP Discovery Polish**: Improve the reliability of server discovery on complex local network topologies (e.g., multiple subnets).
- [ ] **End-to-End Testing**: Implement automated integration tests for the cryptographic handshake process.
