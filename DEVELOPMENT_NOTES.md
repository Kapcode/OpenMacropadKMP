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

### Challenge: SSL/TLS Connection Failure & Keystore Management
- **Problem**: The desktop server would crash on startup in the production-packaged build, complaining about a missing or incorrect `keystore.p12` file.
- **Root Cause**: This was a multi-part issue related to how the SSL/TLS certificate was being created, located, and accessed.
    1.  **Hardcoded Path:** The code was initially looking for the keystore at a fixed file system path (e.g., `/home/kyle/keystore.p12`), which doesn't exist when the app is installed in `/opt/`.
    2.  **Password Mismatch:** The password used to open the keystore was hardcoded in `MacroKtorServer.kt`. If this password didn't exactly match the password used during the `.p12` file's creation, the server would crash with an `IOException: keystore password was incorrect`.
    3.  **Build-Time vs. Runtime:** The `.p12` file is a **runtime dependency**, not source code. It should never be committed to version control.
- **Solution & Workflow**:
    1.  **Generation:** A self-signed PKCS12 keystore (`.p12`) is created **once** using the `keytool` command. This requires setting a password and an alias.
        ```bash
        keytool -genkeypair -alias your-alias-name -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore keystore.p12 -validity 10000
        ```
    2.  **Placement:** The generated `keystore.p12` file is manually placed in the `composeApp/src/jvmMain/resources/` directory before building. This ensures it gets bundled into the application's resources.
    3.  **Code Access:** The `MacroKtorServer` was modified to load the keystore as a resource stream from the classpath (`this::class.java.classLoader.getResourceAsStream("keystore.p12")`). This works regardless of the installation directory.
    4.  **Configuration:** The alias and passwords used during generation are hardcoded into `MacroKtorServer.kt`. It is the developer's responsibility to ensure these values match the `.p12` file.
    5.  **Security:** The `keystore.p12` file is added to `.gitignore` to prevent the private key from ever being committed to version control.

## 6. Desktop Packaging & Distribution

### Challenge: JNativeHook "Permission Denied" on Linux
- **Problem:** After being packaged into a `.deb` or `.bin` installer and run from a system directory (like `/opt/`), the application would crash with `UnsatisfiedLinkError: ... (Permission denied)` when trying to initialize `JNativeHook`.
- **Root Cause:** This is a two-part Linux security issue:
    1.  **User Permissions:** Standard users are not allowed to listen to the global input device stream for security reasons.
    2.  **File Permissions:** The native library (`.so` file) for JNativeHook was being packaged without the "executable" permission bit set.
- **Solution:** A robust, multi-layered solution was implemented.
    1.  **User Group:** The user must be added to the `input` group, which has the rights to read system-wide input events. This is a one-time setup command for the user, followed by a mandatory logout/login. The `README.md` was updated with these user instructions.
        ```bash
        sudo usmod -a -G input $USER
        ```
    2.  **Gradle File Permissions:** To fix the file permission issue directly, a `doLast` block was added to the `packageDistributionForCurrentOS` task in `build.gradle.kts`. This script automatically runs `chmod +x` on the JNativeHook `.so` file after it's placed in the build directory, ensuring it's always executable in the final package.

### Challenge: Build Cache Issues
- **Problem:** During troubleshooting, changes made to files (like updating passwords in `MacroKtorServer.kt`) were not reflected in the final packaged application, causing the same error to repeat.
- **Root Cause:** Gradle's caching mechanism was reusing old, compiled outputs instead of rebuilding with the new code.
- **Solution:** When troubleshooting packaging issues, always run the build with the `clean` task to ensure a fresh build from source.
    ```bash
    ./gradlew clean :composeApp:createDistributable
    ```

## 7. State Management on Desktop

### Challenge: Macro Switch State Was Not Persistent
- **Problem**: The "active" state of the macro switches in the desktop `MacroManagerScreen` would reset to `off` every time the application was relaunched.
- **Root Cause**: The `isActive` state for each macro was only stored in memory within the `MacroManagerViewModel`. It was not being saved to disk.
- **Solution**:
    1.  A `java.util.Properties` file (`.open-macropad-active-macros.properties`) is now created in the user's home directory to store the state of the switches.
    2.  The `MacroManagerViewModel` now loads these properties on startup.
    3.  Whenever a macro's `isActive` state is toggled, the new state is written to the properties file, ensuring persistence across application launches.

## 8. Android UI and Build Issues

### Challenge: UI Layout and Component Refactoring
- **Problem**: Several UI adjustments were needed, including adding ad banners, handling navigation bar overlap, and modifying the app bar.
- **Solution**:
    - Admob banner ads were added to `MainActivity` and `ClientActivity`.
    - The `MainActivity` banner was initially obscured by the system navigation bar. This was fixed by wrapping the `AdmobBanner` in a `BottomAppBar`, which correctly respects system insets provided by `enableEdgeToEdge`.
    - The `CommonAppBar` was refactored to accept a composable `navigationIcon` parameter, making it more flexible for different screens (e.g., showing a menu icon on `ClientActivity` and a back arrow on `MainActivity`'s settings screen).

## 9. Compose for Desktop Dialogs

### Challenge: Dialogs Hidden by Swing Components
- **Problem**: Standard `AlertDialog` composables were being obscured by the Swing-based code editor component.
- **Root Cause**: The layering of standard dialogs was incompatible with the heavyweight Swing `ComposePanel`.
- **Solution**:
    - Replaced `AlertDialog` with `DialogWindow`. `DialogWindow` creates a separate, always-on-top window that is not affected by the layering of Swing components within the main application window. This ensures that dialogs like the "Rename Macro" dialog are always visible to the user.

## 10. Android Freemium Model

### Challenge: Implement a Rewarded Ad-Based Token System
- **Problem**: The app needed a way to monetize on Android while providing a free service. The chosen model was a freemium system where users spend "tokens" to execute macros and can earn more tokens by watching rewarded ads.
- **Root Cause**: This required state management for the token balance, integration with Google AdMob, and UI components to display the balance and prompt users.
- **Solution**:
    1.  **Centralized State:** A `TokenManager` class was created as a singleton. This ensures that all parts of the app (different Activities, UI components) access and modify a single, consistent token balance. It uses `SharedPreferences` for persistence.
    2.  **Configurable Economy:** A `BillingConstants.kt` file was created to hold the core values of the economy (`TOKENS_PER_REWARDED_AD`, `TOKENS_PER_MACRO_PRESS`, `STARTING_TOKENS`). This allows for easy tuning of the model without searching through the codebase.
    3.  **UI Integration:** The `CommonAppBar` was modified to include a token balance display. Tapping this display shows an `AlertDialog` (`GetTokensDialog`) which prompts the user to watch an ad.
    4.  **AdMob Integration:** A `RewardedAd.kt` file was created to encapsulate the logic for loading and showing a rewarded ad from AdMob. When a user successfully watches an ad, the `TokenManager` is called to award the specified number of tokens.
    5.  **Token Consumption:** The `ClientActivity` was updated to call `tokenManager.spendTokens()` before sending a macro command. If the user is out of tokens, a `Toast` message is shown instead.

## Architecture Overview

*   **UI Layer:** Pure Jetpack Compose for Desktop (`DesktopApp`, `InspectorScreen`, `Console`, etc.).
*   **State Management:** `ViewModel` pattern (`DesktopViewModel`, `MacroManagerViewModel`, `SettingsViewModel`) using `StateFlow` to drive the UI.
*   **Input Handling:** `JNativeHook` for global keyboard listening.
*   **Automation:** `java.awt.Robot` for simulating input events.
*   **Persistence:** JSON for macro definitions, `Properties` file for application settings and active macro state.

## Building from Source

### Prerequisites (if not using Android Studio)

1.  **JDK 11:** This project requires JDK version 11. You can download it from a provider like [Adoptium Temurin](https://adoptium.net/temurin/releases/?version=11).
2.  **Android SDK:** You need the Android SDK Command-Line Tools.
    *   Download the tools from the [Android Studio downloads page](https://developer.android.com/tools) (scroll down to "Command line tools only").
    *   Create a directory for your Android SDK (e.g., `~/Android/sdk` or `C:\Android\sdk`).
    *   Unzip the downloaded tools into the created directory.
3.  **Environment Variables:**
    *   Set `JAVA_HOME` to the installation path of your JDK 11.
    *   Set `ANDROID_HOME` (or `ANDROID_SDK_ROOT`) to the path of your Android SDK directory.
    *   Add the JDK `bin` directory and the Android SDK `cmdline-tools/latest/bin` and `platform-tools` directories to your system's `PATH`.

### Building the Desktop Application

Once the prerequisites are installed and configured, you can build the native desktop distribution from the project's root directory:

**For Windows:**
```bash
./gradlew :composeApp:packageDistributionForCurrentOs
```

**For macOS or Linux:**
```bash
./gradlew :composeApp:packageDistributionForCurrentOs
```

## Future Considerations

*   **Wayland Support:** `java.awt.Robot` and `JNativeHook` have known limitations on Wayland (Linux). Future updates might need alternative automation strategies for Wayland environments.
*   **Mouse Coordinate Recording:** The current "Record Macro" button is a placeholder. Implementing real-time recording of input events would be a significant feature addition.
