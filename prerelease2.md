# Prerelease Plan 2

This document outlines the tasks for the upcoming prerelease phase, focusing on project audit, Firebase integration, Billing features, and AI agent optimization.

## Tasks

- [x] **Audit Documentation**: Audit all `.md` files for consistency and clarity. Ensure `AGENTS.md`, `CODE_MAP.md`, `README.md`, etc., are up-to-date and cross-referenced correctly.
- [ ] **Code Quality Scan**: Scan the project for compiler warnings and runtime errors. Address critical issues that could affect stability.
- [ ] **Firebase Opt-in Logic**: Implement "Opt-in" logic for Firebase Analytics and Crashlytics. This must be a user-controlled toggle in the settings, defaulting to off (or as per privacy best practices).
    - Target: `SettingsViewModel.kt` and associated UI.
- [ ] **Billing Integration**: Add Billing dependencies and permissions.
    - *Prerequisite*: Google Play Store signing reset approval.
    - *Includes*: $25 one-time removal, $2.99/mo subscription.
- [ ] **Monetization UI/Logic**: Develop UI and logic for:
    - Rewarded ads for tokens.
    - Premium subscription management.
- [ ] **AI Optimization**: Enhance `AGENTS.md` and `CODE_MAP.md` for better AI tool navigation and context provision.

## Status Tracking
- **Documentation Audit**: Completed
- **Code Quality Scan**: Pending
- **Firebase Opt-in**: Pending
- **Billing Integration**: Pending
- **Monetization UI**: Pending
- **AI Optimization**: Pending
