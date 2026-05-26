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

**R8 vs. No-R8 Comparison (Amazon Fire 8 10th Gen):**
- **Non-minified (No R8):** ~15 seconds to first frame.
- **Minified (R8 Enabled):** ~8.7 seconds to first frame.
- **Improvement:** ~42% faster startup.

> **Note**: R8 is now configurable in development via the `debugR8` build variant.

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

### Challenge: Unreliable "Minimize to Tray" & Inconsistent Exit Behavior
- **Problem**: The initial implementation for iconifying the application to the system tray was unstable across different OS environments, lacked proper "Restore" behavior, and had inconsistent exit logic between the Window [X] button and the Tray "Exit" menu item.
- **Solution**:
    - **Native Tray Integration**: Leveraged Compose for Desktop's `Tray` API to provide a stable, OS-native icon and context menu.
    - **Three-Option Exit System**: Refactored the exit behavior into a configurable system in `AppSettings`:
        1. **ASK**: Shows a confirmation dialog (Ask every time).
        2. **TRAY**: Minimizes the window to the system tray (Exit to Tray).
        3. **EXIT**: Closes the application immediately (Just Exit).
    - **Context-Aware Logic**: 
        - The **Window [X]** button respects the `exitBehavior` setting (e.g., will minimize to tray if "TRAY" is selected).
        - **Manual "Exit" Buttons** (Tray Menu and UI Header) are treated as explicit shutdown requests. If behavior is "ASK", the dialog is shown; otherwise, the app exits immediately, bypassing the "TRAY" setting to ensure users can always fully quit the app without changing settings.
    - **Shared Window State**: Resolved state desync by sharing a single `DesktopWindowState` instance between `main.kt` and `DesktopApp`. This ensures that tray animations and visibility toggles affect the actual application window consistently.
    - **Cleanup**: Fully removed the deprecated `MinimizeToTrayDialog` and consolidated settings into the `exitBehavior` property.
    - **Smooth Transitions**: Maintained the quadratic ease-in animation that scales and moves the window toward the system tray area during minimize.
    - **High-Quality Assets**: Uses a 512px icon to eliminate white fringing artifacts on dark system taskbars.

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

## 36. Battery Drain & Scanner Optimizations

**Problem**: Long-running scanner sessions (e.g., when the device is mounted and left on for hours) cause significant battery drain, sometimes exceeding the charging rate of slow chargers.

**Solution**:
1. **Configurable Timeout**: Implemented a "Scanner Timeout" (1-48 hours) slider in Settings. The scanner automatically stops and shows a "Timed Out" overlay to save power.
2. **Low Power Scanner Mode**: After 1 hour of continuous scanning, the UI transitions to a "Low Power Mode":
    - **FPS Reduction**: Camera capture is throttled to 5-10 FPS via `CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE`.
    - **Lower Resolution**: ML Kit target resolution is dropped to 640x480.
    - **Throttled Metering**: Focus/Exposure updates are slowed from 2.5s to 5s intervals.
    - **Visual Feedback**: The scanner overlay dims to signal the state.
3. **State Reset**: Closing the scanner or manually resuming resets the power-saving timers.

## 37. Rich Widget Ecosystem (Toggles, Sliders, & Icons)

### Challenge: Beyond Simple Buttons
- **Problem**: Users needed more than just "fire-and-forget" buttons. Controlling volume, toggling lights, or adjusting brightness required stateful widgets.
- **Solution**:
    - **Stateful Models**: Expanded `GridWidget` to include `WidgetType`, `state` (boolean), and `value` (float).
    - **Specialized Components**:
        - **Toggles**: Optimized for On/Off states with visual `Switch` feedback.
        - **Sliders**: Implemented both Horizontal and Vertical orientations. Added two communication modes: `LIVE` (updates as you drag) and `ON_RELEASE` (updates only when lifted) to balance network traffic vs. responsiveness.
    - **Material Icon Resolver**: Created an `IconMapper` to map server-provided string IDs (e.g., "volume_up") to Android's `Icons.Default` library, allowing the server to define the visual style.
    - **Dynamic Contrast**: Implemented a luminance-based calculation to automatically switch between White and Black text/icons depending on the custom background color of the widget.

### Challenge: Dashboard Migration & Complex Persistence
- **Problem**: Moving from a simple `List<String>` (macro names) to a complex `List<GridWidget>` for the dashboard threatened to break existing user data.
- **Solution**: 
    - **JSON Persistence**: Migrated `SettingsStorage` to use `kotlinx.serialization` to store the full `GridWidget` list as a JSON blob.
    - **Migration Path**: Added a "Lazy Migration" block that detects old string-based dashboard lists, converts them into basic `GridWidget` objects, saves the new format, and cleans up the old key.

## 38. UI Polish & Interaction Design

### Challenge: Intuitive Dashboard Management
- **Problem**: Removing items from the dashboard via just a long-press felt disconnected and lacked visual impact.
- **Solution**:
    - **Drag-to-Trash**: Implemented a "Trash Can" UI at the bottom of the screen that only appears in Edit Mode when a drag operation starts.
    - **Collision Logic**: Used the pointer offset to detect when a widget is hovered over the trash can.
    - **Animated Feedback**: The trash can uses `animateColorAsState` and `animateFloatAsState` to scale up and turn red when an item is "caught," making the deletion action feel satisfying and intentional.

### Challenge: Multi-Step Configuration
- **Problem**: Adding a macro as a specific variant (e.g., as a Slider instead of a Button) was difficult with a single-click picker.
- **Solution**:
    - **Two-Step Macro Picker**: Refactored the `MacroPicker` into a stateful, two-step dialog. Step 1 selects the macro from the list; Step 2 presents the available widget variants with descriptive icons. This ensures users can configure their dashboard without needing a separate "Edit Properties" screen.

## 29. De-bouncing and Token Sync Reliability

### Challenge: Multiple Token Deductions per Macro
- **Problem**: Users reported 2 to 4 tokens being removed for a single macro press.
- **Root Causes**:
    1.  **UI Ghost Touches**: Rapid accidental taps or proximity sensor fluctuations triggered multiple commands.
    2.  **Duplicate Logic**: Tokens were being deducted both in the initial `sendMacro` call AND in the `onExecutionStart` callback.
- **Solution**:
    1.  **Button De-bouncing**: Added a 1000ms cooldown to `MacroButton` in `MacroButtonsScreen.kt`.
    2.  **Slam Fire De-bouncing**: Added an `isHandlingSlam` flag with a 500ms cooldown in `ClientActivity.kt`.
    3.  **Single-Source Truth**: Moved all token deduction logic strictly to the `onExecutionStart` callback. The `sendMacro` function now only sends the request; tokens are only spent once the server confirms the macro has actually started.
    4.  **Automatic Refunds**: If a macro fails after starting, the client now automatically refunds the tokens and notifies the server to decrement the global "Total Spent" metric.

### Challenge: Currency Balance Not Syncing on Connection
- **Problem**: The Desktop console would show "0" tokens for a connected client until they executed a macro, even if they had a balance.
- **Solution**:
    - Updated `ClientActivity.kt` to send an initial `currency_update` message as soon as the connection transitions to the `"Connected"` state (specifically after the macro list is received). This ensures the server's global ledger is immediately populated with the correct local balance.

## 30. Build System Stability

### Challenge: R8 InterruptedException in Debug Builds
- **Problem**: Enabling R8 minification for debug builds (to strip Ktor-Server bloat) caused sporadic `java.lang.InterruptedException` and hung builds due to resource exhaustion during the shrinking phase.
- **Solution**: Disabled `isMinifyEnabled` for the `debug` build type in `composeApp/build.gradle.kts`. While this increases the debug APK size, it restores build reliability and developer velocity. Minification remains enabled for `release` builds to ensure production APKs are optimized.

## 31. Deployment and Distribution

### JAR Packaging
- **UberJar Strategy**: The project uses the `packageUberJarForCurrentOS` task to bundle all dependencies into a single, executable JAR file.
- **Signature Stripping**: A custom `stripSignaturesFromUberJar` task is used to remove security signatures from dependency JARs (like `META-INF/*.SF`, `*.DSA`, `*.RSA`) that would otherwise cause `SecurityException` when running the merged UberJar.

### Docker Deployment
- **Containerization**: The Desktop Server is designed to be containerized using a `Dockerfile` based on an OpenJDK JRE.
- **Headless Mode Support**: While the primary UI is Compose-based, the server logic is decoupled to allow running in "Headless" or "Service" mode within a Docker container.
- **Volume Mapping**: Critical data (keystores, settings, macro definitions) is stored in the user's home directory (e.g., `~/.open-macropad/`) and should be mapped to a persistent volume for Docker deployments.
- **Network Configuration**: Containers require `--net=host` or specific port mapping for UDP Discovery (Port 8888) and WebSocket communication (Port 8080).

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
- [x] **Dashboard Persistence**: Implemented local persistence for the "My Dashboard" tab using `SharedPreferences` and Flow-based sync.
- [x] **Context-Aware Resolution**: Implemented automatic "Active Pack" switching based on the Desktop's active process broadcast.
- [x] **Advanced Gestures**: Refactored macro buttons to use `pointerInput` for reliable long-press detection.
- [ ] **Background Connectivity**: Maintain a heartbeat connection with the desktop server while the app is in the background to avoid reconnect delays.
- [x] **Editable User Grid**: Implemented reordering logic using `detectDragGesturesAfterLongPress` and a custom `MacroPicker` for adding any available macro to the dashboard.

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
- [x] **Pairing Brute-Force Protection**: Added a "3-strikes" rule for the 6-digit verification code. Exceeding 3 incorrect attempts results in an automatic device ban, preventing exhaustive search attacks.
- [x] **OS-Level Secret Vault**: Integrated native secure storage for identity keys using macOS Keychain, Windows Credential Manager, and Linux Libsecret via `SecretManager`.
- [x] **Smart QR Grid**: Implemented "Fleet Mode" for dynamic scaling of pairing requests.
- [x] **Stable Hardware Fingerprinting**: Replaced `FINGERPRINT` with a combination of `MANUFACTURER|MODEL|BOARD|HARDWARE` to ensure persistent device identity across Android OS updates.
- [x] **Gold Standard Currency Protocol**: Implemented a unified `currency_update` and `currency_spent` binary protocol to synchronize Android `TokenManager` balances with the Desktop's global ledger.
- [x] **Split Pane Desktop Layout**: Integrated `VerticalSplitPane` with custom **Pill-Shaped Pulltabs** for the Desktop console, providing a flexible layout for monitoring active sessions vs. connection history.
- [x] **Persistent Connection Auditing**: Added a "Recent Activity" sidebar to the Desktop console that persists connection history to disk, providing a clear audit trail of past access.
- [ ] **Macro Templates**: Add predefined templates for popular software (e.g., OBS, Photoshop, VS Code).
- [ ] **Automatic Updates**: Integrate a background update checker for the desktop client.

## 50. Compose for Desktop: The "Smeared UI" Glitch

### Challenge: Dialogs and Windows Appearing as Transparent "Smeared" Frames
- **Problem**: Opening dialogs (like Sync or Factory Reset) would occasionally result in a visual mess where the window content didn't render, or trailed behind the cursor as a "smear" of the main window.
- **Root Causes**:
    1.  **Missing Solid Background**: In certain JVM environments (especially Linux/X11), Compose for Desktop requires an explicit solid color at the root of a window. Without it, the GPU may not clear the buffer, causing transparency or "ghosting" effects.
    2.  **Layout Loops**: Reading window dimensions (e.g., `windowState.size`) inside a composition that *also* modifies those dimensions creates a feedback loop, stalling the renderer.
- **Solution**:
    *   **Surface Enforcement**: Updated `BaseDialog.kt` to explicitly set `color = MaterialTheme.colorScheme.background` on the root `Surface`.
    *   **Stable Sizing**: Wrapped window dimension reads in `remember(windowState.size)` to prevent rapid-fire recompositions during window initialization.
    *   **Safe Resizing**: Added equality checks (`if (windowState.size != targetSize)`) in `LaunchedEffect` blocks to prevent redundant OS-level window resize calls.

## 52. Google Play Billing Integration

### Challenge: Modernizing the Freemium Model
- **Problem**: The app relied solely on rewarded ads for tokens, which lacked a permanent solution for power users who wanted to remove ads or gain permanent "Pro" access.
- **Solution**:
    - **Google Play Billing Library 7.x**: Integrated the latest Billing Library to support In-App Products (one-time) and Subscriptions.
    - **Product Suite**:
        - `pro_pass_one_time`: Permanent Pro access.
        - `pro_subscription`: Monthly/Yearly Pro access.
        - `ad_free_one_time`: Permanent banner ad removal.
        - `ad_free_subscription`: Monthly/Yearly ad removal.
    - **BillingManager**: Created a dedicated, lifecycle-aware `BillingManager` that handles the connection to Google Play, queries product details (prices, titles), and manages the purchase/acknowledgment flow.
    - **Ad Suppression**: Updated `ClientScreen.kt` to hide `AdmobBanner` if `isPro` or `isAdFree` states are active, ensuring a clean UI for paying users.

### Challenge: Testing Billing Flows without Sandbox Overhead
- **Problem**: Testing real Google Play Billing requires a signed APK, a licensed tester account, and internal distribution, which is slow for UI iteration.
- **Solution**:
    - **Memory-Only Developer Mode**: Implemented a `isDeveloperMode` flag in `SettingsViewModel`. 
    - **Instant Simulation**: When enabled, the `BillingManager` (via `CommonAppBar`) bypasses the Google Play Store and immediately grants the requested status (Pro or Ad-Free) in RAM. 
    - **Security**: The flag is intentionally memory-only and not persisted to settings, making it harder to exploit while remaining highly useful for development.

## 53. Desktop Window Management

### Challenge: Non-Maximized Window on Init
- **Problem**: The desktop application would start in a small "Floating" window even if the monitor index was set, requiring users to manually maximize it every time.
- **Solution**:
    - **Forced Maximization**: Updated `DesktopWindowState.kt`'s `applyInitialPlacement` function to set `windowState.placement = WindowPlacement.Maximized` immediately upon initialization.
    - **Monitor Awareness**: The app still respects monitor selection (Primary, Index, or Cursor-based) but now ensures it fills the target screen from the first frame.

## 42. Advanced Automation & Scripting Suite (Phase 6)

### Challenge: GraalVM Dependency Bloat
- **Problem**: Integrating GraalVM JS engine significantly increases the server's binary size and memory footprint.
- **Solution**: 
    - **Context Isolation**: Restricted GraalVM context to `js` language only and disabled host class lookup to minimize security risks.
    - **Lazy Initialization**: The JS engine is initialized only when the first `ScriptAction` is executed.

### Challenge: Complex Trigger State Machine
- **Problem**: Detecting "Hold" vs "Multi-tap" vs "Sequence" requires precise timing and key-state tracking across multiple OS threads.
- **Solution**:
    - **SequenceEvaluator**: Implemented a dedicated evaluator that tracks `pressedKeys` and `tapHistory` using `ConcurrentHashMap`.
    - **Native Hook Refactor**: Updated `TriggerListener` to pipe both `nativeKeyPressed` and `nativeKeyReleased` into the evaluator, allowing it to distinguish between a single tap and a long hold.

### Challenge: X11 Context Reliability
- **Problem**: `xdotool` is sometimes missing or fails on certain window managers (e.g., tiling WMs).
- **Solution**: 
    - **xprop Fallback**: Implemented a primary context watcher using `xprop -root _NET_ACTIVE_WINDOW` which is the standard X11 protocol for active window tracking. 
    - **Multi-tool Strategy**: If `xprop` fails or returns `0x0`, the system automatically falls back to `xdotool` for better compatibility across different Linux distributions.

### Challenge: GUI Confirmation vs. Physical Speed
- **Problem**: Physical key sequences are often too fast for a GUI dialog to feel "natural" as an intermediate step.
- **Solution**: Implemented a stateful pause in the `SequenceEvaluator` that holds the matched trigger in a "Pending" state, allowing the user to maintain their workflow and confirm when convenient.

### Challenge: Sequence History Management
- **Problem**: The key sequence history could grow indefinitely or contain stale data from auto-repeat events.
- **Solution**: Added auto-repeat filtering (ignoring consecutive duplicates) and implemented a sliding window of 20 keys for matching, ensuring the state machine stays efficient and accurate.

## 43. Scripting & Advanced Routines (Phase 6 Completion)

### Challenge: Scripting Context and Host API
- **Problem**: GraalVM scripts needed a way to interact with the host system (Robot, Logging, Active Process) without exposing dangerous internal classes.
- **Solution**:
    - **KapHostApi**: Created a dedicated `KapHostApi` inner class in `MacroPlayer` that exposes safe methods (`pressKey`, `moveMouse`, `delay`, `log`, `getActiveProcess`, `playMacro`, `notify`, `getClipboardText`, `setClipboardText`).
    - **Context Isolation**: Scripts are restricted to the `js` language and cannot look up Java classes directly.

### Challenge: Visual Routine Building
- **Problem**: Complex routines with triggers, conditions, and actions were difficult to build via raw JSON.
- **Solution**:
    - **Shared UI Component**: Ported and enhanced the `VisualRoutineBuilder` to `commonMain`. It now features a full-featured block-based editor with dropdowns for triggers, conditions, and action types.
    - **Desktop Integration**: Added a "Advanced Routines" section to the `PackEditorDialog` on Desktop, allowing users to visually create and edit routines within a Macro Pack.

### Challenge: Interleaving Scripts and Recorded Actions
- **Problem**: Users wanted to add custom logic (e.g., "if window is X, do Y") inside a recorded macro.
- **Solution**:
    - **ScriptEvent**: Added a new `ScriptEvent` type to the `MacroEventState` hierarchy.
    - **Timeline Support**: Updated the Macro Timeline to support adding and editing JavaScript blocks as individual steps within a macro sequence.
    - **Standalone JS Macros**: Updated `MacroManagerViewModel` to recognize `.js` files as standalone macros that execute directly via the script engine when triggered.


### Cross-Platform / Common
- [ ] **UDP Discovery Polish**: Improve the reliability of server discovery on complex local network topologies (e.g., multiple subnets).
- [ ] **End-to-End Testing**: Implement automated integration tests for the cryptographic handshake process.

## 32. Android Dashboard Persistence & Context Awareness

### Challenge: Persisting User-Curated Macro Lists
- **Problem**: Users wanted a "My Dashboard" tab that persists their favorite macros even after the app restarts or the server disconnects.
- **Solution**:
    - **SharedPrefs + JSON**: Implemented `SettingsStorage` on Android to serialize the list of dashboard macro IDs into a JSON string stored in `SharedPreferences`.
    - **Flow-Based Sync**: Used a `MutableStateFlow` in `SettingsStorage` to broadcast changes. The `ClientViewModel` binds to this Flow, ensuring the UI stays in sync with disk state in real-time.
    - **Atomic Toggles**: Implemented a `toggleDashboardMacro` logic that prevents duplicates and handles atomic updates to the underlying storage.

### Challenge: Active Process to MacroPack Resolution
- **Problem**: The Android client receives a raw process name (e.g., "photoshop.exe") from the Desktop but needs to map this to a specific UI layout.
- **Solution**:
    - **Metadata Mapping**: Added a `targetProcess` field to `MacroPack`.
    - **Resolution Engine**: Updated `ClientViewModel` to scan all installed/synced packs whenever the `activeProcess` changes, automatically promoting the matching pack to the "Active Pack" tab.

## 33. PointerInput for Advanced Gestures

### Challenge: Reliable Long-Press in Scrollable Containers
- **Problem**: Standard `Modifier.combinedClickable` often conflicted with the scroll behavior of the macro grid, leading to missed long-presses or "janky" scrolling.
- **Solution**:
    - **Custom PointerInput**: Migrated `MacroButton` to use `Modifier.pointerInput(Unit) { detectTapGestures(onLongPress = { ... }) }`. This provides more granular control over gesture consumption and improved reliability when managing the dashboard (adding/removing items via long-press).

## 34. Robust ViewModel-to-Storage Synchronization

### Challenge: Initialization Race Conditions & Context Crashes
- **Problem**: Initializing the `ClientViewModel` before `SettingsStorage` was ready, or showing Toasts from background threads, led to `NullPointerException` or "Window Leaks."
- **Solution**:
    - **Explicit Binding**: Created a `bindViewModel` pattern where the `MainActivity` explicitly connects the storage layer to the VM after both are fully initialized.
    - **Lifecycle-Aware Toasts**: Wrapped all Toast calls in `MainScope().launch` and ensured they use the `Activity` context rather than a potentially stale `Application` context for UI-bound feedback.

## 35. Android Edit Mode & Reordering

### Challenge: Drag-and-Drop Reordering in a LazyVerticalGrid
- **Problem**: Jetpack Compose's `LazyVerticalGrid` does not have built-in support for drag-and-drop reordering, and maintaining smooth visual feedback during swaps is complex.
- **Solution**:
    - **Long-Press Detection**: Used `detectDragGesturesAfterLongPress` to initiate the drag state.
    - **Index Calculation**: Implemented a hit-test logic that calculates the currently hovered item index based on pointer offset and `gridState.layoutInfo`.
    - **Smooth Swapping**: Integrated an `IntOffset` for the dragging item and used `graphicsLayer` scaling and elevation to provide depth.
    - **Atomic List Updates**: Created a `moveDashboardMacro` function in `ClientViewModel` that performs a stable list swap, which is then persisted to `SharedPreferences` via `SettingsStorage`.

## 39. MacroPack Synchronization Implementation

### Challenge: Missing Widget Collection Sync
- **Problem**: While `MacroPack` models were defined, the logic to synchronize them from the Desktop server (where packs are installed) to the Android client was missing.
- **Solution**:
    - **Desktop Discovery**: Updated `MacroManagerViewModel` to identify files ending in `_pack.json` and decode them as `MacroPack` objects using `kotlinx.serialization`.
    - **Push Mechanism**: Modified `ClientCommunicationViewModel` to send an `installed_packs` data message immediately after pairing approval or upon request for macros (if trusted).
    - **Android Reception**: Updated `ClientRepository` to listen for the `installed_packs` key and trigger a callback. The `ClientViewModel` then updates its state, making the widgets available in the "Active Pack" and "My Dashboard" tabs.

## 40. Navigation Lifecycle and Connection Safety

### Challenge: Zombie Connections and Trust Inheritance
- **Problem**: Navigating back from the client activity didn't explicitly close the WebSocket connection. If a device was approved with "Trust Once," a subsequent connection attempt could "inherit" that trust if the old session hadn't timed out or if it was replaced by a duplicate session.
- **Solution**:
    - **Explicit Disconnect**: Implemented explicit `disconnect()` calls in Android's `ClientActivity` and `ClientScreen` `BackHandler`. This ensures the socket is closed the moment the user leaves the pairing or control screens.
    - **Trust Revocation**: Hardened `MacroKtorServer.kt` to explicitly remove the client ID from `temporaryTrustedDevices` whenever a session is replaced or ends. This guarantees that "Trust Once" is strictly bound to a single, continuous session.

## 44. Window Context & State-Based Triggers

### Challenge: Window Title Pattern Matching
- **Problem**: Users needed to trigger actions based on specific window titles (e.g., a specific website) rather than just the application name.
- **Solution**: 
    - **ActiveWindowTitleIs**: Added a new condition type to the AST that performs partial string matching (`contains`) on the current focused window's title.
    - **ProcessWatcher Extension**: Enhanced the JVM watcher to track `activeTitle` and `lastTitle` alongside process names.

### Challenge: UI Blocking & Dialog Interference
- **Problem**: Opening the Settings dialog would occasionally prevent routines from triggering in the background.
- **Solution**: 
    - **State Pulse**: Implemented a periodic background coroutine in `MacroManagerViewModel` that pulses every 1s. This ensures triggers (like clipboard changes) are processed even if the UI thread is busy or a modal dialog is open.
    - **Edge Detection**: Added a `lastTriggeredRoutine` map to the state engine. Triggers now fire only on the *rising edge* (when a condition becomes true), preventing infinite execution loops while a condition remains met.

### Challenge: Variable Persistence & Live Feedback
- **Problem**: Building coordinate-based or pixel-based routines required manually typing values from the inspector into the builder.
- **Solution**: 
    - **Variable Picker Field**: Created a shared component that combines text input with a categorized dropdown of system variables.
    - **Live Values**: Integrated real-time resolvers into the picker dropdown. Users can now see their current `mouse_x` or `pixel_color_at_cursor` updating live in the menu before selecting them.
    - **Click-to-Copy**: Updated the Inspector UI and Variable Picker to support instant clipboard capture of any system variable.

## 46. Settings UI Layout Standardization

### Challenge: UI Overlaps in Settings Screen
- **Problem**: Sub-components within the Settings screen lacked root layout containers (like `Column`), causing them to stack on top of each other when placed inside a parent `Box`.
- **Solution**:
    - **Root Column Wrapper**: Standardized all settings components (`VariableSettings`, `ClientSettings`, `NetworkSettings`, `UISettings`, `ThemeSettings`, `BehaviorSettings`, `ControllerSettings`, and `DeviceManagement`) to be wrapped in a root `Column(modifier = Modifier.fillMaxWidth())`.
    - **Consistent Formatting**: Cleaned up excessive whitespace and ensured all sub-components follow a predictable vertical flow, preventing visual overlaps and ensuring future components are easier to integrate.

## 47. Integrated Gamepad Support (Jamepad)

### Challenge: Low-Latency Controller Polling
- **Problem**: Standard event-driven input libraries often introduce latency or missed frames when handling rapid analog movements or button mashes.
- **Solution**:
    - **Jamepad Integration**: Leveraged Jamepad (SDL2 wrapper) for robust, cross-platform controller support on JVM.
    - **High-Frequency Polling**: Implemented a dedicated `ControllerManager` that polls the gamepad state at ~60fps using a coroutine-based loop (`Dispatchers.Default`).
    - **State Flow**: Exposed `isConnected` and `activeButton` states via `StateFlow` to ensure the UI and Macro Manager can react instantly to hardware changes.
    - **Macro Mapping**: Integrated with `MacroManagerViewModel` to allow gamepad buttons to trigger macros directly, bypassing the standard keyboard/mouse event loop for lower overhead.

## 48. Automation Serialization & Polymorphism

### Challenge: Overlapping Property Names in Sealed Classes
- **Problem**: In `AutomationAction`, several subclasses (like `KeyEvent`, `MouseEvent`, and `MouseButtonEvent`) use a property named `type`. This caused an `IllegalStateException` during `kotlinx.serialization` because `type` is the default class discriminator used for polymorphic serialization of sealed classes.
- **Solution**:
    - **Custom Discriminator**: Applied `@JsonClassDiscriminator("kind")` to the `AutomationAction` sealed class. This moves the polymorphism metadata to a new JSON field (`kind`), freeing up the `type` property for use by the data models.
    - **Experimental API Opt-In**: Added `@file:OptIn(ExperimentalSerializationApi::class)` to `AutomationAST.kt` to enable the use of the custom discriminator annotation.

## 54. Compiler Warning Cleanup & Performance Regressions

### Challenge: JVM System Crawl after "Style" Cleanups
- **Problem**: A batch of standard compiler warning fixes (adding named arguments, clarifying parentheses, and moving lambdas) caused the application to slow down the entire host virtual machine to a crawl, despite low CPU usage.
- **Root Cause**: Likely an interaction between the Kotlin compiler's bytecode generation for certain constructs (like `mutableStateOf` with named arguments) and the JIT compiler or VM runtime in performance-critical loops (Heartbeat watchdog or JS bridge).
- **Solution**: 
    - **Cautious Re-application**: Reverted all changes and re-applied only unambiguous static cleanups.
    - **Functional Fixes ONLY**: Restricted JVM cleanups to unused import removal, unused exception parameters (`_`), and removing unused loop variables.
    - **Avoided Syntactic Sugar**: Specifically avoided adding named arguments or extra parentheses in the `MacroKtorServer` watchdog and `MacroPlayer` execution loops to maintain original bytecode performance characteristics.
    - **Incremental Verification**: Adopted a "Small Batch & Monitor" strategy for all future IDE-suggested cleanups.

## 55. Billing Implementation & Marketplace Placeholder

### Challenge: Finishing the Billing Flow
- **Problem**: Several purchase options (One-time Pro, Subscription Pro, Ad-Free) needed to be unified and wired to the Google Play Billing Library.
- **Solution**: 
    - **Unified Dialog**: Completed the `ProPurchaseDialog` with all five purchase/reward options.
    - **Product ID Standardization**: Updated `BillingConstants.kt` to include `openmacropadkmp_pro_subscription` and `openmacropadkmp_ad_free_subscription`.
    - **Simulation Support**: Ensured "Developer Mode" in `CommonAppBar` correctly bypasses Play Store for local testing of all tiers.
    - **Lifecycle Integration**: Verified `BillingManager` correctly queries and handles both In-App and Subscription product types.

### Challenge: Communicating Marketplace Status
- **Problem**: The Marketplace tab was empty, which could be confusing for new users.
- **Solution**: Added a "Marketplace Coming Soon" placeholder to `MarketplaceScreen.kt` on Android, featuring a `Storefront` icon and a descriptive message about future community macro packs.

## 56. Marketplace Tab Fix & Communication Bridge
- **Problem**: The Marketplace tab on Android was stuck on a loading spinner because the server wasn't handling the `getMarketplace` command.
- **Solution**:
    - **Command Wiring**: Updated `ClientCommunicationViewModel.kt` (JVM) to handle the `getMarketplace` command and return a response.
    - **DI Reference**: Wired `MarketplaceViewModel` into the communication bridge via `ViewModelFactory`.
    - **Loading Safety**: Added a 5-second timeout in `ClientViewModel.kt` (Android) to ensure the loading indicator is cleared even if the network fails.
    - **Coming Soon Trigger**: Set the server to return an empty list for now, which triggers the newly added "Coming Soon" UI on the Android client.

## 57. Security & Billing Standards: Developer Mode
- **Rule**: "Developer Mode" (used for bypassing Google Play Billing and other simulations) MUST only be toggled via direct source code modification.
- **Rationale**: To prevent accidental activation or exploitation by end-users, this flag should never be exposed as a setting in the UI, even in an "Advanced" section.
- **Implementation**: 
    - The `_isDeveloperMode` Flow in `SettingsViewModel.kt` defaults to `false` and should remain so in the repository.
    - **UI Control**: Visibility of sensitive developer-only UI (like the Pro switch in the Marketplace) MUST be bound to the `isDeveloperMode` flag.

## 58. Troubleshooting Google Play Billing: "Version Not Configured"

### Challenge: Local APK Mismatch
- **Problem**: Receiving a Google Play error: "The version of this application is not configured for billing through google play."
- **Diagnosis**: This occurs when the APK installed on the device is not recognized by the Play Store as a valid testing version.
- **Mandatory Fixes**:
    1.  **Release Signing**: The app MUST be built as a **Signed Release APK** using the production keystore. Local debug builds signed with the auto-generated debug key will always fail this check.
    2.  **Internal Testing Track**: The signed APK (matching the current `versionCode`) MUST be uploaded to an **Internal Testing** or **Closed Testing** track in the Google Play Console.
    3.  **Tester Opt-in**: The device's primary Google account MUST be added as a tester in the track, and the user MUST navigate to the opt-in URL provided by the Console to accept the test.
    4.  **License Testing**: In the Play Console (Setup > License Testing), ensure the tester's email is added to allow test purchases.
