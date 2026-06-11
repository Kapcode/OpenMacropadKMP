# MacroKapKMP Code Map

This document serves as a guide for AI assistants to navigate the project effectively.

## 📂 Project Structure

- **`composeApp/`**: The main module containing all code.
    - **`src/commonMain/kotlin/`**: Shared logic between Android and Desktop.
        - **`com.kapcode.open.macropad.kmps/`**: Shared root package.
            - `BillingConstants.kt`: Centralized source of truth for all monetization constants (Ad IDs, Product IDs).
            - `network/sockets/`: Authenticated & Encrypted WebSocket implementation.
                - `model/`: Shared network data models.
                - `MacroKtorClient.kt`: Shared Ktor 3.x client logic.
            - `models/`: Shared data models (GridModels, AutomationAST, TrustedServer, LunchMenuItem).
            - `ui/`: Shared UI components and navigation.
                - `components/`: KeyValidationField, ConnectionItem, LoadingIndicator, SplashScreen, GamepadStatusIndicator, VariablePicker, VisualRoutineBuilder.
                - `navigation/`: Screen definitions and routing logic.
                - `theme/`: Color and Theme definitions.
            - `settings/`: Shared settings ViewModels and Screens.
            - `hardware/`: Shared hardware interfaces (HardwareTriggerManager).
            - `lunch/`: Lunch Menu feature (Screen, ViewModel).
            - `utils/`: HashUtils, FlexibleStringSerializer, ClipboardManager, Base64Utils.
            - `DeviceInfo.kt` & `IdentityManager.kt`: Shared hardware/security interfaces.
            - `App.kt`: Main Compose entry point for shared UI.
            - `ProjectConfig.kt`: Application-wide configuration constants.
    - **`src/jvmMain/kotlin/`**: Desktop Server implementation (Compose for Desktop).
        - **`com.kapcode.open.macropad.kmps.desktop/`**: Main Desktop logic and UI.
            - `di/`: Centralized dependency injection (`ViewModelFactory`).
            - `logic/`: Core automation logic (MacroPlayer, TriggerListener, ServerDiscovery, KeyParser, ControllerManager, PackManager, SequenceEvaluator, ProcessWatcher, ProAccessManager, TrustedDeviceManager).
            - `model/`: Desktop-specific state models (MacroModels, ClientInfo, ActiveProcessInfo, MacroFileState, EditorTabState, LogLevel).
            - `ui/`: Desktop-specific Compose screens, themes, and specialized components.
                * `MacroManagerScreen.kt`: Collapsible `SectionHeader`s, independent search, and tiled item backgrounds.
                * `components/RedrawFix.kt`: Aggressive GPU buffer refresh logic (1-pixel resize trick) to resolve window "smearing" and transparency glitches.
                * `pairing/`: UI for physical consent pairing and QR code display.
                * `settings/`: Desktop-specific settings dialogs.
            - `viewmodel/`: Desktop ViewModels for state management (MacroManagerViewModel, ServerViewModel, ConsoleViewModel, MacroEditorViewModel, PairingViewModel, etc.).
            - `utils/`: QrCodeGenerator, ProjectPaths.
            - `network/`: Desktop-specific server implementation (`MacroKtorServer.kt`).
            - `main.kt`: JVM Application entry point.
        - **`com.kapcode.open.macropad.kmps/`**: JVM implementations of `DeviceInfo`, `IdentityManager`, and `Platform`.
            - `utils/KeystoreUtils.kt`: Secure local keystore management.
            - `utils/SecretManager.kt`: Native OS keyring integration.
    - **`src/androidMain/kotlin/`**: Android Client implementation.
        - **`com.kapcode.open.macropad.kmps/`**: Android app logic.
            - `ui/`: Android-specific screens and components (ClientScreen, MarketplaceScreen, QrCodeScanner).
                - `components/`: KapAnimations, CommonAppBar, ProPurchaseDialog, RewardConfirmDialog, LegalDialog.
            - `network/`: Android-specific network logic (`ClientRepository.kt`).
            - `settings/`: Android-specific settings sections.
            - `utils/`: Android-specific utilities.
            - `MainActivity.kt`: Server discovery and initial setup.
            - `ClientActivity.kt`: Remote control UI, tab navigation, and gesture handling.
            - `ClientViewModel.kt`: Central state management for search, tabs, and dashboard logic.
            - `SettingsStorage.kt`: SharedPreferences-backed persistence for user-curated dashboards.
            - `KapManager.kt` & `RewardedAd.kt`: Monetization and persistent Kap storage logic.
            - `BillingManager.kt`: Google Play Billing integration.
            - `SlamFireManager.kt`: Proximity sensor trigger logic.
            - `AnalyticsManager.kt` & `ConsentManager.kt`: Analytics and user consent tracking.
            - `DeviceInfo.kt` & `IdentityManager.kt`: Android hardware-backed security.
            - `MacroApplication.kt`: Android Application class for initialization.

## 🧩 Core Components & Responsibilities

| Component | Responsibility | Location |
| :--- | :--- | :--- |
| **IdentityManager** | Provides persistent, platform-specific identity keys. Android uses Hardware-backed Keystore; JVM integrates with **native OS keyrings** (macOS Keychain, Windows Credential Manager, Linux Libsecret) via `SecretManager`. | `commonMain/com/.../IdentityManager.kt` |
| **DesktopWindowState** | Centralizes window visibility, tray transitions, and the three-option exit system (**Ask, Exit to Tray, Just Exit**). | `jvmMain/com.kapcode.open.macropad.kmps.desktop/ui/DesktopWindowState.kt` |
| **KeystoreUtils** | Manages JVM-local EC keystores with automated backup and password rotation. Works with `SecretManager`. | `jvmMain/com/.../utils/KeystoreUtils.kt` |
| **MacroKtorServer** | Ktor 3.x WebSocket server. Manages connections and macro execution requests. | `jvmMain/com.kapcode.open.macropad.kmps.desktop/network/MacroKtorServer.kt` |
| **MacroKtorClient** | Ktor 3.x WebSocket client implementation. | `commonMain/com/.../network/sockets/MacroKtorClient.kt` |
| **DeviceInfo** | Provides stable, unique, and privacy-safe identifiers for the device. | `commonMain/com/.../DeviceInfo.kt` |
| **MacroPlayer** | Simulates mouse/keyboard input via `java.awt.Robot`. | `jvmMain/com.kapcode.open.macropad.kmps.desktop/logic/MacroPlayer.kt` |
| **TriggerListener** | Listens for global hotkeys via `JNativeHook`. | `jvmMain/com.kapcode.open.macropad.kmps.desktop/logic/TriggerListener.kt` |
| **BillingManager** | Manages Google Play Billing lifecycle and purchase flows for Android. | `androidMain/com/.../BillingManager.kt` |
| **SlamFireManager** | Manages high-speed physical triggers via the device's Proximity Sensor. | `androidMain/com/.../SlamFireManager.kt` |
| **KapManager** | Manages local currency balance with server sync. | `androidMain/com/.../KapManager.kt` |
| **AutomationAST** | Defines the logic for stateful automation (Triggers, Conditions, Actions). | `commonMain/com/.../models/AutomationAST.kt` |
| **VisualRoutineBuilder**| Block-based UI for creating automation routines (Shared). | `commonMain/com/.../ui/components/VisualRoutineBuilder.kt` |
| **ControllerManager** | Integrated Gamepad support via Jamepad. Polls hardware at 60fps. | `jvmMain/com.kapcode.open.macropad.kmps.desktop/logic/ControllerManager.kt` |
| **SecureSocket** | Authenticated Handshake with EC (secp256r1) and AES-GCM encryption. | `commonMain/com/.../network/sockets/model/` |
| **Discovery** | UDP-based server discovery (Announcer on Desktop, Discovery on Android). | `jvmMain/com.kapcode.open.macropad.kmps.desktop/logic/ServerDiscoveryAnnouncer.kt` & `androidMain/com/.../ClientDiscovery.kt` |

## 📡 Communication Protocol (WebSocket)

The client and server communicate using **DataModel** objects serialized as JSON over WebSockets:

- **Security Handshake**:
    - **Challenge-Response**: Servers issue a random challenge that clients must sign using their private EC key to prove identity.
    - **Physical Consent Pairing**: Untrusted devices must be manually approved on the server UI via a 6-digit verification code or **QR Code**.
    - **Binary-Only Protocol**: All communication must use the serialized `DataModel` over `Frame.Binary`. `Frame.Text` is ignored.

## 🛠️ Key Technologies
- **Kotlin Multiplatform (KMP)**
- **Compose Multiplatform** (Android & Desktop)
- **Ktor 3.x** (WebSocket networking)
- **JNativeHook** (Desktop global hotkeys)
- **java.awt.Robot** (Desktop automation)
- **AdMob** (Android monetization)
- **Google Play Billing** (Android purchases)
- **Jamepad** (Gamepad support)
- **GraalVM** (JavaScript scripting)
