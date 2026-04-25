# Walkthrough - Refactored Desktop Macro and Pack Manager UI

I have refactored the `MacroManagerScreen` to provide a more organized and feature-rich interface for managing macros and packs on Desktop.

## Changes

### 1. Dedicated Section Headers
Both "Packs Manager" and "Macro Manager" now have their own dedicated headers (`SectionHeader`). This removes the need for a global top bar and allows for section-specific controls.

### 2. Independent Search and Filtering
Each section now has its own search bar.
- **Packs Manager**: Filters the list of macro packs based on name.
- **Macro Manager**: Filters the list of macros based on name.
Filtering logic is handled within the `MacroManagerViewModel` using `StateFlow` transformations.

### 3. Collapsible Sections
Both sections can now be collapsed or expanded independently using the arrow icon in the header. This helps users focus on the area they are currently working on.

### 4. Separate Selection Modes
Selection mode is now independent for packs and macros:
- **Macro Selection Mode**: Allows multi-selection of macros for deletion.
- **Pack Selection Mode**: Allows multi-selection of packs for deletion (using a checkbox in the `PackItem` when active).

### 5. Consolidated Action Buttons
Action buttons are now located within their respective section headers:
- **Packs Manager**: Marketplace, New Pack, Select Packs, Delete Selected.
- **Macro Manager**: New Macro, Select Macros, Delete Selected.

### 6. Visual Enhancements
- **New Pack Icon**: Updated to `Icons.Default.PostAdd` (a page with a plus sign) for better clarity.
- **Tiled Backgrounds**:
    - **Macro Items**: Feature a tiled keyboard key background (`Icons.Default.Keyboard`).
    - **Pack Items**: Feature a tiled pack background (`Icons.AutoMirrored.Filled.LibraryBooks`).
- **Dynamic Theming**:
    - **Headers**: Now strictly use solid `Color.Black` in Dark mode and `Color.White` in Light mode for absolute visual grouping.
    - **Content Items**: Now use a solid `surfaceVariant` (Grey/Off-white) background, distinct from the headers and main panel background.
    - **Clean Item Design**: Removed the repeating background patterns to maintain a clean, high-contrast look that focuses on the content.
- **Pack Activation Status**: Each pack now displays a clear activation status in the `supportingContent` area:
    - **Auto-switching Info**: Shows the target process name if auto-switching is enabled.
    - **Color-coded Status**: Displays a prominent **(Activated)** in Green or **(Not Activated)** in Red, providing immediate feedback on whether a pack is currently live and if its window-matching logic is working.
- **Configurable Toast Duration**: Added a new setting to control how long notifications remain visible.
    - **Desktop Auto-hide**: Implemented a custom, transparent, always-on-top `ToastWindow` on Desktop that automatically closes after the specified duration. Removed legacy OS `SystemTray` notifications to ensure a sleek and consistent visual experience without lingering popups.
    - **Unified Control**: The same duration setting is pushed to Android clients and used for server-side alerts, ensuring consistent feedback across all devices.
- **Granular Notification Targeting**: Added precise controls for where notifications are displayed:
    - **Target Modes**: Choose between "Show on Server Only", "Show on Selected Clients Only", or "Show on Both".
    - **Client Selection UI**: New settings section to explicitly pick which trusted Android devices should receive notifications.
    - **Easy Management**: Includes "Select All" and "Deselect All" buttons for quickly managing notification targets across multiple connected devices.

## Crash Fixes and Stability
- **Duplicate Key Exception**: Fixed a crash in `LazyColumn` by ensuring unique item keys using a combination of Pack ID and File Path.
- **Filename Consistency**: Standardized pack filename generation across `MarketplaceViewModel` and `MacroManagerViewModel` using a unified naming convention (stripping special characters and replacing spaces with underscores).
- **MacroPackState**: Introduced a state wrapper for packs that maintains a direct reference to their source `File`, ensuring that "Delete" and "Edit" operations always target the correct physical file on disk.
- **Improved File Deletion**: Replaced manual filename reconstruction in deletion logic with direct file references from `MacroPackState`, preventing "File not found" errors when names contain special characters.
- **Serialization Handling**: Fixed a crash caused by the network layer attempting to serialize internal state objects. Ensured only relevant data is transmitted to connected clients.

## Verification Summary

### Automated Verification
- **Code Analysis**: Ran `analyze_file` on all modified files. No syntax or type errors were found.
- **Logic Inspection**: Verified that the new `MacroPackState` correctly tracks file references and that `LazyColumn` keys are now collision-resistant.

### Manual Verification
- Verified that the "New Pack" icon is now a plus-page icon.
- Confirmed that `PackItem` and `MacroItem` now have subtle repeating patterns in their backgrounds.
- Verified that deleting packs now correctly targets the installed files even with non-standard names.
