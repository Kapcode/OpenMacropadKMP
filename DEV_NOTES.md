# Development Notes - 2025-11-12

This document outlines the technical challenges and solutions implemented during the development session.

## 1. Server-Client Connectivity

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

## 2. State Management on Desktop

### Challenge: Macro Switch State Was Not Persistent
- **Problem**: The "active" state of the macro switches in the desktop `MacroManagerScreen` would reset to `off` every time the application was relaunched.
- **Root Cause**: The `isActive` state for each macro was only stored in memory within the `MacroManagerViewModel`. It was not being saved to disk.
- **Solution**:
    1.  A `java.util.Properties` file (`.open-macropad-active-macros.properties`) is now created in the user's home directory to store the state of the switches.
    2.  The `MacroManagerViewModel` now loads these properties on startup.
    3.  Whenever a macro's `isActive` state is toggled, the new state is written to the properties file, ensuring persistence across application launches.

## 3. Android UI and Build Issues

### Challenge: UI Layout and Component Refactoring
- **Problem**: Several UI adjustments were needed, including adding ad banners, handling navigation bar overlap, and modifying the app bar.
- **Solution**:
    - Admob banner ads were added to `MainActivity` and `ClientActivity`.
    - The `MainActivity` banner was initially obscured by the system navigation bar. This was fixed by wrapping the `AdmobBanner` in a `BottomAppBar`, which correctly respects system insets provided by `enableEdgeToEdge`.
    - The `CommonAppBar` was refactored to accept a composable `navigationIcon` parameter, making it more flexible for different screens (e.g., showing a menu icon on `ClientActivity` and a back arrow on `MainActivity`'s settings screen).

### Challenge: Compilation Errors
- **Problem**: Refactoring the `CommonAppBar` and other changes introduced several compilation errors.
- **Solution**: The errors were resolved by updating the call sites in `MainActivity` and `ClientActivity` to match the new `CommonAppBar` signature and by replacing a deprecated Material icon (`Icons.Filled.ArrowBack`) with its modern, auto-mirrored equivalent.