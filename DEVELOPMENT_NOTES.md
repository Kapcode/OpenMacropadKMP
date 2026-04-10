# Development Notes

This document serves as a technical log for the challenges encountered and solutions implemented during the development of OpenMacropadKMP, particularly focusing on the Desktop Server component.

## Critical Technical Challenges & Solutions

### 1. Ktor 2.x to 3.x Migration & Server Refactor

**Problem:**
Upgrading from Ktor 2.3.x to 3.0.x in a Kotlin Multiplatform project introduced severe dependency resolution conflicts and compilation errors. Additionally, the initial `MacroKtorServer` implementation was tightly coupled and lacked robust state management.

**Solution:**
*   **Nuclear Alignment:** Aligned the project to a stable "Golden Trio": **Kotlin 2.1.0**, **Compose Multiplatform 1.7.3**, and **Ktor 3.0.3**.
*   **Global Exclusions:** Added `exclude(group = "io.ktor", module = "ktor-server-host-common")` to `build.gradle.kts` to banish legacy Ktor 2 metadata.
*   **Explicit JVM Artifacts:** Switched to explicit `-jvm` suffixes for Ktor server dependencies in the Desktop target to remove resolution ambiguity.
*   **Forced Resolution:** Used `resolutionStrategy.force` to ensure Ktor 3.0.3 and Coroutines 1.10.1 were used globally, preventing silent downgrades.
*   **Duration API:** Migrated all time-based logic (WebSockets, server timeouts) to use the native `kotlin.time.Duration` API. *Note: As of 3.0.x, `EmbeddedServer.stop()` still requires `Long` milliseconds.*
*   **Architecture Refactor:** Re-architected `MacroKtorServer` to use a single `ConnectedClient` state object and `ConcurrentHashMap`. Extracted protocol handling into specialized functions (`handleAuthResponse`, `handleUnauthenticatedMessage`, etc.) and implemented Dependency Injection for `AppSettings` and `TrustedDeviceManager`.
*   **Native Heartbeats:** Replaced manual `heartbeatJob` loops with Ktor's native WebSocket `pingPeriod` (15s) and `timeout` (30s) configurations, reducing overhead and improving reliability.


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

*   **UI Layer:** Jetpack Compose Multiplatform (Material 3). See [DESIGN_LANGUAGE.md](DESIGN_LANGUAGE.md) for detailed UI principles.
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

## 11. Security Hardening & Vulnerability Fixes

### Challenge: Unsafe Deserialization (Vulnerability 1)
- **Problem**: Use of Java `ObjectInputStream` for network communication was vulnerable to arbitrary code execution.
- **Solution**: Migrated the entire `DataModel` to **`kotlinx.serialization` (JSON)**. This provides a type-safe, multiplatform-compatible way to handle data without the risks associated with Java serialization.

### Challenge: ClientId Spoofing (Vulnerability 2)
- **Problem**: The server relied on a self-reported `clientId` for authentication, allowing an attacker to impersonate any trusted device.
- **Solution**: Implemented **Cryptographic Challenge-Response** authentication.
    - The server issues a random UUID challenge upon connection.
    - The client signs this challenge using its platform-specific private EC key (Hardware-backed on Android, password-protected PKCS12 on JVM).
    - **Identity Fingerprinting**: The `clientId` is now the full SHA-256 hex hash of the client's public key.
    - **Strict Verification**: The server strictly verifies that the public key provided in `AUTH_RESPONSE` matches the `clientId` used to establish the session.


### Challenge: Pairing Code Interception (Vulnerability 3)
- **Problem**: The 6-digit verification code was sent over the network to the client, allowing a passive attacker to see it.
- **Solution**: **Out-of-band Verification**. The code is now strictly displayed on the server UI (Desktop). The user must manually enter it on the client or scan it via QR code. The server never sends the code to the client; it only acknowledges if the client-submitted code is correct.

### Challenge: Authentication Bypass via Raw Frames
- **Problem**: Ktor's `webSocket` route processed both `Frame.Binary` and `Frame.Text`, allowing attackers to bypass the secure `DataModel` handler.
- **Solution**: Hardened `MacroKtorServer` and `MacroKtorClient` to **explicitly ignore `Frame.Text`**. All communication is now strictly binary, containing serialized JSON `DataModel` objects.

### Challenge: Man-in-the-Middle (MITM) via Unsafe SSL
- **Problem**: To support the server's self-signed certificate, the Android client was previously configured to trust all SSL certificates, leaving it vulnerable to MITM attacks.
- **Solution**: Implemented **Certificate Pinning**.
    - **Fingerprint Calculation**: The Desktop server calculates the SHA-256 fingerprint of its own certificate (`KeystoreUtils.kt`).
    - **Fingerprint Sharing**: The fingerprint is shared with the client via UDP discovery and the initial pairing handshake.
    - **Pinned Client**: The Android client now uses a custom `X509TrustManager` that verifies the server's certificate against the stored fingerprint. This replaces the "unsafe" client with a cryptographically hardened one for all subsequent connections.

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
    - **Smooth Transitions**: Implemented a quadratic ease-in animation that scales and moves the window toward the system tray area during minimize.
    - **Dynamic Menu Items**: Implemented a state-aware context menu that changes its labels (e.g., "Show Main Window" vs. "Hide to Tray") based on the current window visibility and minimized state.
    - **Robust Toggling**: Optimized the `onAction` handler to handle quick clicks, window restoration from a minimized taskbar state, and immediate cancellation of active hide animations if the user "catches" the window.
    - **Stability**: Set `animateToTray` to `false` by default in `AppSettings` to provide a more predictable out-of-the-box experience while remaining configurable.
    - **User Notification**: Created a dedicated `MinimizeToTrayDialog` with high-visibility Material 3 typography to inform the user when the application is continuing to run in the background. Included a "Don't show again" option that persists in `AppSettings`.
    - **High-Quality Assets**: Switched to a 512px icon to eliminate white fringing artifacts on dark system taskbars.

## 19. Comprehensive UI Theming & Accessibility

### Challenge: Inconsistent Component Theming (Swing + Compose)
- **Problem**: The JSON code editor (based on `RSyntaxTextArea`) did not follow the Compose application's theme, and dialogs often lacked proper visual affordances for scrolling.
- **Solution**:
    - **Dynamic JSON Theming**: Implemented a bridge between `SettingsViewModel` and `SwingCodeEditor`. The editor now dynamically loads `dark.xml` or `idea.xml` theme files whenever the user switches between "Dark Blue" and "Light Blue" themes.
    - **Accessibility Contrast**: Adjusted the "Running" status text color to a darker green (`0xFF008000`) in the Light Blue theme to ensure it meets contrast requirements against the Surface Variant background.
    - **Persistent Scrollbars**: Added thick (`8.dp`), high-visibility `VerticalScrollbar` components to all major dialogs (Settings, New Event, Record Macro). This ensures users can clearly see scrollable areas, especially on touchscreens or with hidden system scrollbars.
    - **Tooltip Optimization**: Removed the tooltip from the `SettingsDialog` close button to prevent it from obstructing the button itself during quick interactions.

## 20. Console Enhancements & Security

### Challenge: Debugging Macro Execution and Client Interaction
- **Problem**: The standard console log was hard to read due to lack of timing information and often scrolled past important events too quickly.
- **Solution**:
    - **Timestamps**: Added millisecond-precision timestamps (`HH:mm:ss.SSS`) to all log entries.
    - **Auto-scroll Control**: Implemented a toggle to enable/disable automatic scrolling to the latest log, allowing users to inspect older logs without being "snapped" back to the bottom.
    - **Log to File**: Added the ability to persist logs to a session-specific text file on disk for long-term debugging.

### Challenge: Privacy and Hardware Safety with Persistence
- **Problem**: Logging every macro event (including key/mouse events) to disk introduces security risks (plain-text passwords) and potential SSD wear.
- **Solution**:
    - **Informed Consent**: Implemented a mandatory security warning dialog that users must acknowledge before "Log to File" can be enabled.
    - **Temporary Nature**: The UI emphasizes that file logging should only be used temporarily for debugging and not left on during normal operation.

## 21. Desktop Architectural Refactoring

### Challenge: Maintainability and Dependency Sprawl
- **Problem**: As the desktop application grew, `ui/DesktopApp.kt` and `viewmodel/` were becoming cluttered with tightly coupled logic and duplicate declarations (e.g., `DesktopViewModels`).
- **Solution**: 
    1.  **Package Organization**: Refactored the `jvmMain` source into a clean, package-based structure:
        - `di/`: Centralized dependency management via `ViewModelFactory`.
        - `logic/`: Isolated business logic (Macro execution, settings persistence, server discovery).
        - `model/`: Shared data classes and state representations.
        - `ui/`: Pure Compose UI components and themes.
        - `viewmodel/`: Specialized ViewModels for state management.
    2.  **Centralized DI**: Implemented a `ViewModelFactory` that handles the instantiation and cross-wiring of all ViewModels, ensuring that circular dependencies (like `MacroManager` vs `MacroEditor`) are handled correctly using `remember` and late initialization.
    3.  **Refined Visibility**: Moved models like `ClientInfo` and `MacroFileState` to a common `model` package to resolve visibility issues across UI and ViewModel layers.
    4.  **Compose Optimization**: Cleaned up imports and property delegates across 20+ files to ensure consistent use of `by` and `collectAsState()`.

## 22. Repository Structure & Branching Strategy

### Challenge: Mental Load and Release Management
- **Problem**: Maintaining a single branch for both active development and stable releases made it difficult to track what code was "shippable" versus "experimental."
- **Solution**:
    - **Main Branch**: Reset to the exact state of the last stable release (`V-1`). This branch is now protected and only updated when a new version is ready for the public.
    - **Dev Branch**: Created as the primary workspace for all ongoing development. This allows for rapid iteration and testing without affecting the stability of the `main` branch.
    - **Protected Branches**: Enabled GitHub branch protection on `main` to prevent accidental force-pushes or deletions.

## 23. To-Do List & Future Improvements

### Android Client
- [x] **Optimize Dependency Initialization**: Transitioned from synchronous `ContentProvider`-based initialization to lazy, background-thread initialization for Firebase and AdMob.
- [x] **Extreme Android Startup Optimization**: Reduced cold start from ~20s to ~1.2s by enabling R8 in debug, removing blocking Content Providers, and using lazy initialization.
- [x] **UI Feedback**: Implemented `BlinkingCursor` and `ThreeDotsLoading` with `rememberInfiniteTransition` for reliable feedback during startup and discovery.
- [x] **Slam Fire Trigger Logic**: Implemented a "Slam Fire" hardware trigger system using the Android Proximity Sensor.
    - **Double Slam Detection**: Uses a 300ms (configurable) threshold to distinguish between single and double triggers.
    - **Contextual UI Control**: Integrated with the QR scanner (Single=Open, Double=Close) during the discovery phase.
    - **Persistence**: Built a `SettingsStorage` handler using `SharedPreferences` to persist Slam Fire bindings, thresholds, and overall app state across activity restarts.
    - **Toast Management**: Centralized `showSlamToast` to prevent UI "pileup" when triggering hardware inputs rapidly.
- [ ] **Background Connectivity**: Maintain a heartbeat connection with the desktop server while the app is in the background to avoid reconnect delays.
- [ ] **Customizable UI**: Allow users to rearrange macro buttons on the mobile interface.

### Desktop Server
- [x] **Architectural Refactoring**: Cleaned up the `jvmMain` package structure, separating UI, ViewModels, Models, Logic, and DI.
- [x] **Centralized DI**: Implemented `ViewModelFactory` to manage complex ViewModel dependencies and circular references.
- [x] **Platform Identifiers**: Implemented `DeviceInfo` (expect/actual) to provide stable, unique, and privacy-safe device names and IDs across Android and JVM.
- [x] **Amazon Tablet Fix**: Specifically improved device naming for Fire tablets by querying `Global` and `Secure` settings for `device_name`.
- [x] **Taskbar/Tray Polish**: Overhauled the system tray implementation with dynamic context menus, primary click toggling, and a user notification dialog on first minimize.
### 24. Physical Consent Pairing & QR Support

### Challenge: Physical Consent Pairing & QR Support
- **Problem**: Manually typing 6-digit PINs on a mobile device is error-prone and tedious, especially in landscape mode where the keyboard covers most of the UI.
- **Solution**: 
    - **QR Generation**: Integrated `ZXing` on the JVM to generate a QR code from the pairing PIN.
    - **QR Scanning**: Implemented `CameraX` and `ML Kit` on Android to scan and automatically submit the pairing code.
    - **Advanced Camera Controls**: Added pinch-to-zoom, tap-to-focus, and auto-exposure to the `QrCodeScanner` to improve reliability in various lighting conditions and distances.
    - **Protocol Hardening**: Removed the PIN from the initial `PAIRING_PENDING` network message. The client must now obtain the PIN out-of-band (QR/Manual) to prevent passive interception. Added a `PAIRING_CODE_MATCHED` state to notify the client that the PIN was accepted and it is now waiting for the user to click "Approve" on the desktop.

## 25. Stability & Connection Lifecycle

### Challenge: Disconnect Immediately After Pairing Approval
- **Problem**: Newly approved devices would often disconnect or fail to transition to the "Connected" state, requiring a manual restart of the connection.
- **Root Causes**:
    1.  **Race Conditions**: The server's `handleSession` finally block was aggressively removing clients from the active map, occasionally clearing a new successful retry session when an old one closed.
    2.  **Timeout Sensitivity**: The 20-30s heartbeat watchdog was too strict for the manual pairing process (PIN entry + approval), causing silent timeouts during the sensitive handshake.
    3.  **UI Notification Lag**: The server was only notifying the UI of a "connection" after a full cryptographic challenge-response, which doesn't happen during the initial manual pairing approval phase.
- **Solution**:
    1.  **Differentiated Watchdog**: Increased the pairing phase timeout to **5 minutes** while keeping the active session timeout at 60s.
    2.  **Session Identity Check**: Updated the server's cleanup logic to only remove a client from the map if the closing session is the *exact* one currently registered for that ID.
    3.  **Explicit Promotion**: Modified `authenticateClient` to explicitly trigger `onClientConnected` and reset the heartbeat timer. This ensures the UI updates immediately and the device gets a fresh timeout window the moment "Approve" is clicked.
    4.  **Heartbeat Relaxation**: Heartbeats are no longer strictly required to keep the socket alive during the PIN entry phase.

## 26. Pairing UI Optimization for Mobile

### Challenge: Visibility and Focus in Landscape Mode
- **Problem**: In landscape orientation, the software keyboard would often cover the 6-digit input field or the instructions, leading to a "guessing game" for the user.
- **Solution**:
    - **Top-Pinned Instructions**: Used `Modifier.weight(1f)` for the central status area (QR scanner or success icons), allowing instructions to stay pinned at the top and the input row to stay pinned at the absolute bottom.
    - **Keyboard Detection**: Utilized `WindowInsets.ime.getBottom` via `LocalDensity` to detect keyboard visibility and dynamically adjust the UI (e.g., hiding instructions or shrinking the text field) to keep critical elements visible.
    - **Focus Management**: Implemented `FocusRequester` and `LocalSoftwareKeyboardController` to ensure the input field is automatically focused when the pairing screen appears, and that the keyboard can be toggled manually via a dedicated button.
    - **Unified Iconography**: Standardized on concise Material icons (`Close` for cancel, `Done` for submit, `QrCodeScanner` for QR mode, and `KeyboardArrowUp/Down` for keyboard toggle) to maximize horizontal space.

## 27. Advanced JVM Security & UI Standardization

### Challenge: Identity Key Protection on Desktop
- **Problem**: The JVM identity keystore password was previously stored in plain text or relied on user memory, which is either insecure or prone to data loss.
- **Solution**: Integrated **`SecretManager`** to leverage native OS keyrings (macOS Keychain, Windows Credential Manager, and Linux Libsecret). `IdentityManager` now automatically retrieves or generates a secure 32-character password stored in the system's encrypted vault, providing hardware-level security semantics on Desktop.

### Challenge: Dialog Consistency and Layout Failures
- **Problem**: The custom `AppDialog` (built on `BaseDialog.kt`) does not support direct `width`/`height` parameters, leading to compilation errors when migrating legacy dialogs. Additionally, dialogs were often static-sized, causing overflow on smaller screens.
- **Solution**:
    - **State-Based Sizing**: Migrated all JVM dialogs (e.g., `ExitConfirmDialog`) to use `rememberWindowState(width = ..., height = ...)` passed into the `DialogWindow` call within `AppDialog`.
    - **Fleet Mode (Smart QR Grid)**: Implemented a dynamic grid calculation in `PairingRequestDialog`. The "Smart QR Grid" automatically adjusts rows and columns based on the window's aspect ratio and available space, ensuring that even with 10+ simultaneous pairing requests, the QR codes remain legible and do not overlap the central control area.

### Challenge: Redundant UI and Clutter
- **Problem**: `SettingsDialog.kt` contained four duplicate blocks of the "Default to QR Scanning" toggle due to copy-paste errors, confusing users.
- **Solution**: Conducted a "UI Hygiene" pass, removing all redundant toggles and ensuring the Security & Privacy section is concise and accurate.

## 28. Lifecycle Management

### Challenge: Application Restart Logic
- **Problem**: Users needed a way to restart the application after changing critical settings (like Identity reset) without manually closing and re-opening the binary.
- **Solution**: Implemented a `ProcessBuilder` based restart mechanism in `DesktopApp.kt`. The app identifies its own launch command (via `System.getProperty("sun.java.command")`) and spawns a new process before exiting the current one.


- [x] **Physical Consent & QR Support**: Implemented a "Physical Consent Pairing" security feature with **QR code scanning** for seamless setup. Untrusted devices must be manually approved on the server. Added support for persistent "Banning" and "Unpairing" with "Device Discovery" control and a "One-Time Approvals ONLY" mode.
- [x] **OS-Level Secret Vault**: Integrated native secure storage for identity keys using macOS Keychain, Windows Credential Manager, and Linux Libsecret via `SecretManager`.
- [x] **Smart QR Grid**: Implemented "Fleet Mode" for dynamic scaling of pairing requests.
- [x] **Stable Hardware Fingerprinting**: Replaced `FINGERPRINT` with a combination of `MANUFACTURER|MODEL|BOARD|HARDWARE` to ensure persistent device identity across Android OS updates.
- [x] **Gold Standard Currency Protocol**: Implemented a unified `currency_update` and `currency_spent` binary protocol to synchronize Android `TokenManager` balances with the Desktop's global ledger.
- [x] **Split Pane Desktop Layout**: Integrated `VerticalSplitPane` with custom **Pill-Shaped Pulltabs** for the Desktop console, providing a flexible layout for monitoring active sessions vs. connection history.
- [x] **Persistent Connection Auditing**: Added a "Recent Activity" sidebar to the Desktop console that persists connection history to disk, providing a clear audit trail of past access.
- [ ] **Macro Templates**: Add predefined templates for popular software (e.g., OBS, Photoshop, VS Code).
- [ ] **Automatic Updates**: Integrate a background update checker for the desktop client.


### Cross-Platform / Common
- [ ] **UDP Discovery Polish**: Improve the reliability of server discovery on complex local network topologies (e.g., multiple subnets).
- [ ] **End-to-End Testing**: Implement automated integration tests for the cryptographic handshake process.

### Ideas
- **Cursor Hotbar**: I have a prompt for this, but the general idea is to have a hotbar of cursor locations that you can scroll through, making UI navigation faster. It would be JSON backed, use the Macro Manager UI as well as use the Editor.
