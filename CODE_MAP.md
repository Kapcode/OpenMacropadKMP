# OpenMacropadKMP Code Map

This document serves as a guide for AI assistants to navigate the project effectively.

## 📂 Project Structure

- **`composeApp/`**: The main module containing all code.
    - **`src/commonMain/`**: Shared logic between Android and Desktop.
        - `network/`: Core networking logic and message models.
        - `ui/`: Shared UI components (CommonAppBar, ConnectionItem, etc.).
    - **`src/jvmMain/`**: Desktop Server implementation (Compose for Desktop).
        - `switchdektoptocompose/`: Main UI, ViewModels, and Macro logic.
        - `MacroKTOR/`: Ktor 3.x-based WebSocket server implementation.
    - **`src/androidMain/`**: Android Client implementation.
        - `MainActivity.kt`: Server discovery and initial setup.
        - `ClientActivity.kt`: Remote control UI and token management.

## 🧩 Core Components & Responsibilities

| Component | Responsibility | Location |
| :--- | :--- | :--- |
| **Server** | Ktor 3.x WebSocket server. Manages connections and macro execution requests. | `jvmMain/MacroKTOR/` |
| **MacroPlayer** | Simulates mouse/keyboard input via `java.awt.Robot`. | `jvmMain/switchdektoptocompose/` |
| **TriggerListener** | Listens for global hotkeys via `JNativeHook`. | `jvmMain/switchdektoptocompose/` |
| **Client** | Connects to server, spends tokens, and triggers macros. | `androidMain/ClientActivity.kt` |
| **Discovery** | UDP-based server discovery (Announcer on Desktop, Discovery on Android). | `jvmMain/ServerDiscoveryAnnouncer.kt` & `androidMain/ClientDiscovery.kt` |

## 📡 Communication Protocol (WebSocket)

The client and server communicate using string-based messages over WebSockets:

- **Client -> Server**:
    - `getMacros`: Requests the list of available macros.
    - `play:[MacroName]`: Triggers the execution of a specific macro.
- **Server -> Client**:
    - `macros:[Name1,Name2,...]`: Sends the list of available macros to the client.

## 🛠️ Key Technologies
- **Kotlin Multiplatform (KMP)**
- **Compose Multiplatform** (Android & Desktop)
- **Ktor 3.x** (WebSocket networking)
- **JNativeHook** (Desktop global hotkeys)
- **java.awt.Robot** (Desktop automation)
- **AdMob** (Android monetization)
