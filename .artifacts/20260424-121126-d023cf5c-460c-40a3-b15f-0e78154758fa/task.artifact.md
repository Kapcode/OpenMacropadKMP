# Task Management

- [/] Refactor Desktop Macro and Pack Manager UI
    - [x] Update ViewModel and State
    - [x] Update UI Components
    - [x] Verification
- [/] Notification System Refinement
    - [x] Add Activation Status to Pack Items
    - [x] Implement Live Activation UI Logic
    - [x] Implement OS-wide and Android Client Notifications
    - [x] Add Granular Notification Settings
    - [/] Implement Configurable Toast Duration
        - [ ] Add `toastDurationMs` to `AppSettings` (JVM)
        - [ ] Add `toastDurationMs` to `SettingsViewModel` (JVM & Common)
        - [ ] Update `BehaviorSettings` (JVM) with duration UI
        - [ ] Implement custom `ToastWindow` on Desktop for auto-hide
        - [ ] Update Android client to respect duration (using custom overlay if needed)
        - [ ] Sync duration via `PUSH_SETTINGS`
