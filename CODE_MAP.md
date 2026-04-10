# OpenMacropadKMP Code Map

This document serves as a guide for AI assistants to navigate the project effectively.

## 📂 Project Structure

- **`composeApp/`**: The main module containing all code.
    - **`src/commonMain/kotlin/`**: Shared logic between Android and Desktop.
        - **`com.kapcode.open.macropad.kmps/`**: Shared root package.
            - `network/sockets/`: Authenticated & Encrypted WebSocket implementation (Client/Server/Model).
            - `ui/components/`: Shared UI (CommonAppBar, ConnectionItem, LoadingIndicator, SplashScreen).
            - `settings/`: Shared settings ViewModels and Screens.
            - `DeviceInfo.kt` & `IdentityManager.kt`: Shared hardware/security interfaces.
            - `App.kt`: Main Compose entry point for shared UI.
        - **`MacroKTOR/`**: Ktor 3.x common client utilities (`MacroKtorClient.kt`).
    - **`src/jvmMain/kotlin/`**: Desktop Server implementation (Compose for Desktop).
        - **`switchdektoptocompose/`**: Main Desktop logic and UI.
            - `di/`: Centralized dependency injection (`ViewModelFactory`).
            - `logic/`: Core automation logic (MacroPlayer, TriggerListener, ServerDiscovery, KeyParser).
            - `model/`: Desktop-specific state models (MacroModels, ClientInfo).
            - `ui/`: Desktop-specific Compose screens, themes, and specialized components (SwingCodeEditor).
            - `viewmodel/`: Desktop ViewModels for state management.
            - `main.kt`: JVM Application entry point.
        - **`MacroKTOR/`**: Ktor 3.x server implementation (`MacroKtorServer.kt`).
        - **`com.kapcode.open.macropad.kmps/`**: JVM implementations of `DeviceInfo` and `IdentityManager`.
            - `utils/KeystoreUtils.kt`: Secure local keystore management.
    - **`src/androidMain/kotlin/`**: Android Client implementation.
        - **`com.kapcode.open.macropad.kmps/`**: Android app logic.
            - `MainActivity.kt`: Server discovery and initial setup.
            - `ClientActivity.kt`: Remote control UI and token management.
            - `TokenManager.kt` & `RewardedAd.kt`: Monetization and persistent storage logic.
            - `DeviceInfo.kt` & `IdentityManager.kt`: Android hardware-backed security.
            - `MacroApplication.kt`: Android Application class for initialization.

## 🧩 Core Components & Responsibilities

| Component | Responsibility | Location |
| :--- | :--- | :--- |
| **IdentityManager** | Provides persistent, platform-specific identity keys. Android uses Hardware-backed Keystore; JVM integrates with **native OS keyrings** (macOS Keychain, Windows Credential Manager, Linux Libsecret) via `SecretManager`. | `commonMain/com/.../IdentityManager.kt` |
| **KeystoreUtils** | Manages JVM-local EC keystores with automated backup, password rotation, and 600 permissions. Works with `SecretManager` for secure password retrieval. | `jvmMain/com/.../utils/KeystoreUtils.kt` |
| **Server** | Ktor 3.x WebSocket server & SecureSocket. Manages connections and macro execution requests. | `jvmMain/MacroKTOR/` & `commonMain/com/.../network/sockets/` |
| **DeviceInfo** | Provides stable, unique, and privacy-safe identifiers for the device (Expect/Actual). | `commonMain/com/.../DeviceInfo.kt` |
| **MacroPlayer** | Simulates mouse/keyboard input via `java.awt.Robot`. | `jvmMain/switchdektoptocompose/logic/MacroPlayer.kt` |
| **TriggerListener** | Listens for global hotkeys via `JNativeHook`. | `jvmMain/switchdektoptocompose/logic/TriggerListener.kt` |
| **Client** | Connects to server, spends tokens, and triggers macros. | `androidMain/com/.../ClientActivity.kt` & `commonMain/com/.../network/sockets/` |
| **SecureSocket** | Authenticated Handshake with EC (secp256r1) and AES-GCM encryption. | `commonMain/com/.../network/sockets/model/` |
| **Discovery** | Togglable UDP-based server discovery (Announcer on Desktop, Discovery on Android). | `jvmMain/switchdektoptocompose/logic/ServerDiscoveryAnnouncer.kt` & `androidMain/com/.../ClientDiscovery.kt` |

## 📡 Communication Protocol (WebSocket)

The client and server communicate using **DataModel** objects serialized as JSON over WebSockets:

- **Common Message Types**:
    - `Text`: Raw string messages (Legacy support).
    - `Command`: Application-specific commands (e.g., `getMacros`, `play:[MacroName]`).
    - `Control`: Lifecycle and security messages (`PAIRING_REQUEST`, `AUTH_CHALLENGE`, `AUTH_RESPONSE`, `BANNED`, `DISCONNECT`).
    - `Response`: Success/Failure acknowledgments with optional data (`String?`).
    - `Heartbeat`: Connection health checks.
- **Security Handshake**:
    - **Challenge-Response**: Servers issue a random challenge that clients must sign using their private EC key to prove identity and prevent `clientId` spoofing.
    - **Physical Consent Pairing**: Untrusted devices must be manually approved on the server UI. Verification codes are displayed on the server and must be entered on the client (or scanned via QR) to prevent pairing code leakage.
    - **Binary-Only Protocol**: Raw `Frame.Text` messages are ignored; all communication must use the serialized `DataModel` over `Frame.Binary`.

## 🛠️ Key Technologies
- **Kotlin Multiplatform (KMP)**
- **Compose Multiplatform** (Android & Desktop)
- **Ktor 3.x** (WebSocket networking)
- **JNativeHook** (Desktop global hotkeys)
- **java.awt.Robot** (Desktop automation)
- **AdMob** (Android monetization)
