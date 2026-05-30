# MacroKapKMP Code Map

This document serves as a guide for AI assistants to navigate the project effectively.

## 📂 Project Structure

- **`composeApp/`**: The main module containing all code.
    - **`src/commonMain/kotlin/`**: Shared logic between Android and Desktop.
        - **`com.kapcode.open.macropad.kmps/`**: Shared root package.
            - `network/sockets/`: Authenticated & Encrypted WebSocket implementation (Client/Server/Model).
            - `models/`: Shared data models (GridModels, AutomationAST, TrustedServer).
            - `ui/components/`: Shared UI (CommonAppBar, ConnectionItem, LoadingIndicator, SplashScreen, GamepadStatusIndicator).
            - `settings/`: Shared settings ViewModels and Screens.
            - `DeviceInfo.kt` & `IdentityManager.kt`: Shared hardware/security interfaces.
            - `App.kt`: Main Compose entry point for shared UI.
        - **`MacroKTOR/`**: Ktor 3.x common client utilities (`MacroKtorClient.kt`).
    - **`src/jvmMain/kotlin/`**: Desktop Server implementation (Compose for Desktop).
        - **`switchdektoptocompose/`**: Main Desktop logic and UI.
            - `di/`: Centralized dependency injection (`ViewModelFactory`).
            - `logic/`: Core automation logic (MacroPlayer, TriggerListener, ServerDiscovery, KeyParser, ControllerManager).
            - `model/`: Desktop-specific state models (MacroModels, ClientInfo).
            - `ui/`: Desktop-specific Compose screens, themes, and specialized components.
                * `MacroManagerScreen.kt`: Collapsible `SectionHeader`s, independent search, and tiled item backgrounds.
            - `viewmodel/`: Desktop ViewModels for state management.
                * `MacroManagerViewModel.kt`: Manages macro/pack states, active process tracking, and intelligent notification routing.
                * `ServerViewModel.kt`: Handles WebSocket broadcasts and targeted client communication.
            - `main.kt`: JVM Application entry point.
        - **`MacroKTOR/`**: Ktor 3.x server implementation (`MacroKtorServer.kt`).
        - **`com.kapcode.open.macropad.kmps/`**: JVM implementations of `DeviceInfo` and `IdentityManager`.
            - `utils/KeystoreUtils.kt`: Secure local keystore management.
    - **`src/androidMain/kotlin/`**: Android Client implementation.
        - **`com.kapcode.open.macropad.kmps/`**: Android app logic.
            - `MainActivity.kt`: Server discovery and initial setup.
            - `ClientActivity.kt`: Remote control UI, tab navigation, and gesture handling.
            - `ClientViewModel.kt`: Central state management for search, tabs, and dashboard logic.
            - `SettingsStorage.kt`: SharedPreferences-backed persistence for user-curated dashboards.
            - `TokenManager.kt` & `RewardedAd.kt`: Monetization and persistent token storage logic.
            - `DeviceInfo.kt` & `IdentityManager.kt`: Android hardware-backed security.
            - `MacroApplication.kt`: Android Application class for initialization.

## 🧩 Core Components & Responsibilities

| Component | Responsibility | Location |
| :--- | :--- | :--- |
| **IdentityManager** | Provides persistent, platform-specific identity keys. Android uses Hardware-backed Keystore; JVM integrates with **native OS keyrings** (macOS Keychain, Windows Credential Manager, Linux Libsecret) via `SecretManager`. | `commonMain/com/.../IdentityManager.kt` |
| **DesktopWindowState** | Centralizes window visibility, tray transitions, and the three-option exit system (**Ask, Exit to Tray, Just Exit**). Shared between `main.kt` and `DesktopApp`. | `jvmMain/switchdektoptocompose/ui/DesktopWindowState.kt` |
| **KeystoreUtils** | Manages JVM-local EC keystores with automated backup, password rotation, and 600 permissions. Works with `SecretManager` for secure password retrieval. | `jvmMain/com/.../utils/KeystoreUtils.kt` |
| **Server** | Ktor 3.x WebSocket server & SecureSocket. Manages connections and macro execution requests. | `jvmMain/MacroKTOR/` & `commonMain/com/.../network/sockets/` |
| **DeviceInfo** | Provides stable, unique, and privacy-safe identifiers for the device (Expect/Actual). | `commonMain/com/.../DeviceInfo.kt` |
| **MacroPlayer** | Simulates mouse/keyboard input via `java.awt.Robot`. | `jvmMain/switchdektoptocompose/logic/MacroPlayer.kt` |
| **TriggerListener** | Listens for global hotkeys via `JNativeHook`. | `jvmMain/switchdektoptocompose/logic/TriggerListener.kt` |
| **Client** | Connects to server, spends tokens, and triggers macros. Supports **Rich Widgets** (Buttons, Toggles, Sliders) and **Google Play Billing** (Pro & Ad-Free). Android UI uses a 3-tab system: **[0: Dashboard, 1: Active Pack, 2: Marketplace]**. | `androidMain/com/.../ClientActivity.kt` & `commonMain/com/.../network/sockets/` |
| **TokenManager** | Manages local currency balance with 1000ms/500ms de-bouncing and server sync on connection. Integrates with **Google Play Billing** for purchases. | `androidMain/com/.../TokenManager.kt` |
| **BillingManager** | Manages Google Play Billing lifecycle, product details, and purchase flows for Android. | `androidMain/com/.../BillingManager.kt` |
| **IconMapper** | Maps string-based icon IDs from the server to Android Material Icons. | `androidMain/com/.../MacroButtonsScreen.kt` |
| **GridWidget** | Shared model for UI items. Defines type (Button, Toggle, Slider), color, position, and state. | `commonMain/com/.../models/GridModels.kt` |
| **AutomationAST** | Defines the logic for stateful automation (Triggers, Conditions, Actions). | `commonMain/com/.../models/AutomationAST.kt` |
| **VariablePicker** | Shared component for selecting system and user variables with live previews. | `commonMain/com/.../ui/components/VariablePicker.kt` |
| **VisualRoutineBuilder**| Block-based UI for creating automation routines (Shared). | `commonMain/com/.../ui/components/VisualRoutineBuilder.kt` |
| **RoutineEditorDialog** | Desktop-specific dialog for visual routine editing. | `jvmMain/switchdektoptocompose/ui/RoutineEditorDialog.kt` |
| **PackManager** | Manages active packs, layers, and context-aware switching on the server. | `jvmMain/switchdektoptocompose/logic/PackManager.kt` |
| **SequenceEvaluator** | Evaluates complex key sequences (Hold, Multi-tap) and chords for stateful triggers. | `jvmMain/switchdektoptocompose/logic/SequenceEvaluator.kt` |
| **ControllerManager** | Integrated Gamepad support via Jamepad. Polls hardware at 60fps and maps buttons to macro triggers. | `jvmMain/switchdektoptocompose/logic/ControllerManager.kt` |
| **GamepadStatusIndicator** | Shared UI component showing the connection state of the gamepad in the app bar. | `commonMain/com/.../ui/components/GamepadStatusIndicator.kt` |
| **AutomationDialogs** | Shared UI components for complex trigger and client configuration. | `jvmMain/switchdektoptocompose/ui/components/MacroDialogComponents.kt` |
| **SettingsStorage** | Manages Android persistence. Includes JSON-based storage for custom dashboards and migration logic. | `androidMain/com/.../SettingsStorage.kt` |
| **SecureSocket** | Authenticated Handshake with EC (secp256r1) and AES-GCM encryption. | `commonMain/com/.../network/sockets/model/` |
| **Discovery** | Togglable UDP-based server discovery (Announcer on Desktop, Discovery on Android). | `jvmMain/switchdektoptocompose/logic/ServerDiscoveryAnnouncer.kt` & `androidMain/com/.../ClientDiscovery.kt` |

## 📡 Communication Protocol (WebSocket)

The client and server communicate using **DataModel** objects serialized as JSON over WebSockets:

- **Common Message Types**:
    - `Text`: Raw string messages (Legacy support).
    - `Command`: Application-specific commands (e.g., `getMacros`, `play:[MacroName]`).
    - `Control`: Lifecycle and security messages (`PAIRING_REQUEST`, `PAIRING_PENDING`, `PAIRING_RESPONSE`, `PAIRING_CODE_MATCHED`, `PAIRING_APPROVED`, `BANNED`, `DISCONNECT`).
    - `Currency`: Syncs balances and spending metrics (`currency_update`, `currency_spent`).
    - `Data`: Transmits structured JSON payloads (`installed_packs`, `marketplace_items`).
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
