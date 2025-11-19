# Development Notes

This document serves as a technical log for the challenges encountered and solutions implemented during the development of OpenMacropadKMP, particularly focusing on the Desktop Server component.

## Critical Technical Challenges & Solutions

### 1. JNativeHook and Swing Threading

**Problem:**
We initially observed severe input lag and "bursty" event delivery when using global hotkeys (e.g., pressing Escape repeatedly resulted in a 10-15 second silence followed by all triggers firing at once).
This was caused by two factors:
*   **EDT Blocking:** `GlobalScreen.setEventDispatcher(SwingDispatchService())` was forcing native events onto the Swing Event Dispatch Thread. If the UI (Compose/Swing) was busy rendering, the input hook would get backed up.
*   **Native Callback Blocking:** Performing heavy logic (like launching coroutines or logging synchronously) directly inside the `nativeKeyReleased` callback can stall the OS-level hook thread.

**Solution:**
*   **Removed Swing Dispatcher:** We removed the `SwingDispatchService`, allowing JNativeHook to run on its own dedicated thread.
*   **Offloaded Processing:** The `TriggerListener` now immediately offloads the trigger logic to a `listenerScope.launch { ... }` coroutine. The native callback returns almost instantly.
*   **Shutdown Hook:** We added a JVM shutdown hook to ensure `GlobalScreen.unregisterNativeHook()` is called even if the process is terminated forcefully, preventing zombie hooks that necessitate a system reboot.

### 2. Robot Automation Concurrency

**Problem:**
When multiple macros were triggered rapidly (or due to the JNativeHook burst mentioned above), we saw massive system congestion.
*   **Shared State:** A single `MacroPlayer` instance (and thus a single `java.awt.Robot` instance) was being shared across multiple concurrent coroutines.
*   **AutoDelay Conflict:** Different coroutines were modifying `robot.autoDelay` simultaneously, leading to erratic timing.
*   **Resource Contention:** Multiple threads were trying to move the physical mouse cursor simultaneously, causing the system to stutter or lock up.

**Solution:**
*   **State Isolation:** We now create a **new `MacroPlayer` instance** for every macro execution. This ensures each playback has its own clean `Robot` state.
*   **Serialization (Mutex):** We implemented a `Mutex` in `MacroManagerViewModel` with a **"Drop if Running"** policy (`tryLock()`). If a macro is triggered while another is running, the new trigger is ignored (and logged as a warning). This effectively prevents the "dogpile" effect where 20 macros try to run at once.

### 3. Window Focus and Robot Idle Waiting

**Problem:**
Macros would only execute *after* the application window lost focus.
This was caused by `robot.isAutoWaitForIdle = true` (default). The Robot was waiting for the application's event queue to be completely empty before proceeding. Since a Compose app in focus is constantly processing events (cursor blinks, repaints), the queue was never "idle," causing the Robot to hang until focus changed.

**Solution:**
*   **Disable Wait for Idle:** We explicitly set `isAutoWaitForIdle = false` in `MacroPlayer`. We now handle timing manually using coroutine `delay()`, which is non-blocking and independent of the UI thread state.

### 4. Safety Mechanisms (E-Stop)

**Problem:**
With the introduction of automated mouse movements and loops, there was a risk of the user losing control of their system if a macro went rogue or ran too long.

**Solution:**
*   **Global E-Stop:** We implemented a configurable Emergency Stop key (default F12).
*   **Implementation:** The `TriggerListener` intercepts this key at the highest level and calls `cancelAllMacros()`.
*   **SupervisorJob:** Macro playback runs within a `SupervisorJob`. The E-Stop command calls `cancelChildren()` on this job, instantly stopping all active playback coroutines while keeping the ViewModel alive for future triggers.

## 5. Server-Client Connectivity

### Challenge: Client Could Not Discover the Server
- **Problem**: After replacing the legacy `WifiServer` with the new Ktor-based `MacroKtorServer`, the Android client was unable to find the desktop server on the network.
- **Root Cause**: The new server implementation was missing the UDP broadcast discovery mechanism that the client relies on.
- **Solution**: A `ServerDiscoveryAnnouncer` class was created for the desktop application. This class periodically broadcasts a UDP packet on the local network containing a JSON payload with the server's name, port, and security status (`isSecure`). The Android client's `ClientDiscovery` service was updated to parse this new payload.

### Challenge: SSL/TLS Connection Failure
- **Problem**: Even after discovery was fixed, the Android client would attempt to connect, fail five times, and then exit. Logcat showed "certificate trust" errors.
- **Root Cause**: This was a multi-part issue:
    1.  **Incorrect Keystore Path**: The server was hardcoded to look for the `keystore.p12` file in the user's home directory (`~/.open-macropad/`), but the file was located in the project's root directory.
    2.  **Silent Server Failure**: When the keystore was not found, the server would fail to start its encrypted listener *silently* but would still broadcast that it was a secure server. The client would then try to connect to a non-existent secure port.
    3.  **Client Reconnection Logic**: The `MacroKtorClient` was incorrectly closing the entire shared `HttpClient` instance on every disconnect, which prevented the client from being able to attempt reconnection.
- **Solution**:
    1.  The path in `MacroKtorServer.kt` was corrected to point to the `keystore.p12` file in the project root.
    2.  The server was modified to throw a `RuntimeException` if it is configured to use encryption but cannot find the keystore file, preventing it from starting in a broken state.
    3.  The `client.close()` call was removed from the `MacroKtorClient`'s `close()` method, ensuring the underlying `HttpClient` persists for reconnection attempts.

## 6. State Management on Desktop

### Challenge: Macro Switch State Was Not Persistent
- **Problem**: The "active" state of the macro switches in the desktop `MacroManagerScreen` would reset to `off` every time the application was relaunched.
- **Root Cause**: The `isActive` state for each macro was only stored in memory within the `MacroManagerViewModel`. It was not being saved to disk.
- **Solution**:
    1.  A `java.util.Properties` file (`.open-macropad-active-macros.properties`) is now created in the user's home directory to store the state of the switches.
    2.  The `MacroManagerViewModel` now loads these properties on startup.
    3.  Whenever a macro's `isActive` state is toggled, the new state is written to the properties file, ensuring persistence across application launches.

## 7. Android UI and Build Issues

### Challenge: UI Layout and Component Refactoring
- **Problem**: Several UI adjustments were needed, including adding ad banners, handling navigation bar overlap, and modifying the app bar.
- **Solution**:
    - Admob banner ads were added to `MainActivity` and `ClientActivity`.
    - The `MainActivity` banner was initially obscured by the system navigation bar. This was fixed by wrapping the `AdmobBanner` in a `BottomAppBar`, which correctly respects system insets provided by `enableEdgeToEdge`.
    - The `CommonAppBar` was refactored to accept a composable `navigationIcon` parameter, making it more flexible for different screens (e.g., showing a menu icon on `ClientActivity` and a back arrow on `MainActivity`'s settings screen).

## Architecture Overview

*   **UI Layer:** Pure Jetpack Compose for Desktop (`DesktopApp`, `InspectorScreen`, `Console`, etc.).
*   **State Management:** `ViewModel` pattern (`DesktopViewModel`, `MacroManagerViewModel`, `SettingsViewModel`) using `StateFlow` to drive the UI.
*   **Input Handling:** `JNativeHook` for global keyboard listening.
*   **Automation:** `java.awt.Robot` for simulating input events.
*   **Persistence:** JSON for macro definitions, `Properties` file for application settings and active macro state.

## Future Considerations

*   **Wayland Support:** `java.awt.Robot` and `JNativeHook` have known limitations on Wayland (Linux). Future updates might need alternative automation strategies for Wayland environments.
*   **Mouse Coordinate Recording:** The current "Record Macro" button is a placeholder. Implementing real-time recording of input events would be a significant feature addition.
