# OpenMacropadKMP

This is a Kotlin Multiplatform project, focusing on creating an open-source Macropad solution.

## Project Structure

-   `composeApp`: Contains the shared code for Android and Desktop platforms using Compose Multiplatform, as well as platform-specific implementations.

## Network Library

This project uses a custom TCP socket implementation for client-server communication. It establishes a secure, end-to-end encrypted channel for exchanging `DataModel` objects.

The handshake protocol features a Diffie-Hellman key exchange where the server generates and sends its DH parameters to the client. This ensures that both parties use compatible parameters for generating the shared secret key. During the handshake, the server and client also exchange device names for identification in the UI.

## Desktop UI (Legacy Swing)

The current desktop application is built using Java Swing.

**Developer Note:** The next major step for the desktop application is to migrate the UI from Swing to Compose for Desktop. The information below pertains to the legacy Swing implementation.

### UI Layout and Theming Fixes

During development of the Swing UI, two main issues were addressed: missing UI elements and the lack of a modern theme.

-   **Issue #1: Missing UI Elements (`TabbedUI` / `MacroManagerUI`)**
    -   **Problem:** The `TabbedUI` and `MacroManagerUI` components were being created but not added to the main `JFrame`. The frame only contained a `JSplitPane` with other status panels.
    -   **Solution:** A new root `JSplitPane` was created to hold both the `TabbedUI` and the existing `mainSplitPane`. This `rootSplitPane` was then added to the `JFrame`, ensuring all components are part of the layout hierarchy.

-   **Issue #2: Dark Mode Not Applied**
    -   **Problem:** Swing defaults to a basic, OS-dependent Look and Feel (LaF).
    -   **Solution:** The [FlatLaf](https://www.formdev.com/flatlaf/) library was added to the project dependencies. The `FlatDarkLaf` theme is now set programmatically at the start of the `main()` function, before any UI components are created, providing a modern dark theme for the application.
