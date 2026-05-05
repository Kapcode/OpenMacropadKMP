Implementation Plan - Modularize Session Management and Hardware Trigger Logic
Goal
Modularize session management and hardware trigger logic by extracting platform-agnostic interfaces into commonMain to decouple business logic from platform-specific implementations.
Proposed Changes
commonMain
SessionManager.kt
•
Verify existing interface and models (AuthStatus, ConnectedClient).
[NEW] HardwareTriggerManager.kt
•
Define HardwareTriggerManager interface with platform-agnostic methods for sensor and key events.
jvmMain
MacroKtorServer.kt
•
Implement SessionManager.
•
Remove internal AuthStatus and ConnectedClient classes.
•
Update logic to use common types.
androidMain
SlamFireManager.kt
•
Refactor to implement HardwareTriggerManager.
•
Ensure Android-specific logic (Sensors, KeyEvents) is encapsulated within the implementation.
UI / ViewModels
SettingsViewModel.kt
•
Update to use the new interfaces and common types.
Verification Plan
Automated Tests
•
Run Gradle build for both Android and JVM targets:
◦
./gradlew :composeApp:assembleDebug (Android)
◦
./gradlew :composeApp:jvmJar (JVM)
Manual Verification
•
Review code changes for consistency and adherence to the modularization goal.