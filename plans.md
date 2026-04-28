# Plans - Session/Day

## 1. Unified Shortcut Settings (Desktop App Only) [DONE]
Create a centralized section in the Desktop settings to manage all keyboard shortcuts.
- **Global Shortcuts:** System-wide hotkeys like the **E-Stop** key.
- **In-App Shortcuts:** Shortcuts specific to the desktop application, such as **Copy Console Output**, etc.
- **Modifier Keys:** User-configurable modifier keys for splitter interaction (Alt/Ctrl/Shift) to prevent conflicts with other applications.
- **Requirement:** A singular, intuitive UI for viewing and remapping these shortcuts.
- **Access Points:**
    - Settings -> "Configure Global & App Shortcuts" button.
    - Main Dashboard Hamburger Menu -> "Shortcuts & Keymap".
    - System Tray Right-click -> "Shortcuts & Keymap".

## 2. Connected Clients Configuration (Desktop App Only) [DONE]
Implement a "Connected Clients" section in the Desktop settings to manage and sync configurations across all connected mobile/remote clients.
- **Push Settings:** Ability to push a set of "Server-Mandated" settings to every client upon connection.
- **Initial Syncable Settings:**
    - **Theme:** Synchronize the visual style (Light/Dark/Custom).
    - **Analytics:** Toggle analytics collection on or off for all clients.
    - **Slam Fire:** Configure the trigger button or action for "Slam Fire" functionality.

## 3. UI/UX Refactor [DONE]
- **Ghost Image Feedback:** Implement visual "ghost images" during pane swapping.
- **Panel Shading:** Apply unique background shades to nested panels for better visual hierarchy.
- **Splitter Enhancements:** Add hover/press highlights and persist positions/swapped states.
- **Console UI:** Improve toolbar with horizontal scrolling and visible scrollbar.

## 4. Layout Persistence [DONE]
- **Persistence:** Save and restore all splitter positions and swapped states to `AppSettings`.
- **Ghost Image Density:** Ensure ghost images are sized correctly across different screen densities.

# Professional-Grade Automation Suite Goal
Transform the project into a professional-grade automation suite. Desktop serves as the Creator/Marketplace Hub, and Android serves as the Dynamic Control Surface with both context-aware (Active Pack) and user-defined (Custom Grid) layouts.

## Phase 1: Shared Data Models (KMP commonMain) [DONE]
Core structures used by both Desktop (Server) and Android (Client).
- [DONE] GridWidget: Data class for a single button/element (ID, macroId, label, color, icon, position).
- [DONE] MacroPack: Data class for a collection of widgets + metadata (Author, Version, Target Process Name e.g., photoshop.exe).
- [DONE] MarketplaceItem: Wrapper for packs with download URLs and descriptions.

## Phase 2: Desktop "Hub" & Marketplace (JVM) [DONE]
The Desktop app becomes the central management point.
- [DONE] Marketplace UI:
    - [DONE] Create a Marketplace entry point in MacroManagerScreen.
    - [DONE] Implement a Grid/List view of available Macro Packs.
    - [DONE] "No-Monetization" Policy banner.
    - [DONE] Download/Install Logic: Save packs locally for Android sync.
- [DONE] Active Window Broadcaster:
    - [DONE] Link InspectorManager to DesktopViewModel.
    - [DONE] Emit `ACTIVE_PROCESS_CHANGED` packet to Android clients.

## Phase 3: Android "Dual-Mode" Interface [DONE]
The Android app gains a dynamic, searchable interface for managing macros and packs.
- [DONE] Navigation Bar: Implement `NavigationHeader` with "Active Pack" and "My Dashboard" tabs.
- [DONE] Search Bar: Implement real-time filtering of the macro grid based on search query.
- [DONE] Dashboard Persistence: Save curated macros to local storage via `SharedPreferences`.
- [DONE] Long-Press Management: Toggle macro presence in "My Dashboard" via long-press on any button.
- [DONE] Dynamic Grid Logic (Active Pack): Load associated `MacroPack` based on `active_process` broadcast.
- [DONE] Editable User Grid (Static):
    - [DONE] Edit Mode: Implement drag-and-drop or reordering logic for macro buttons.
    - [DONE] Add Item (FAB): A picker to add any available macro to the custom grid.
    - [DONE] Customization Settings: Add Rows/Columns configuration in Android settings.

## Phase 4: Communication & Sync [DONE]
- [DONE] Protocol Update:
    - [DONE] `ACTIVE_PROCESS_CHANGED` (String)
    - [DONE] `SYNC_INSTALLED_PACKS` (List of MacroPacks)
    - [DONE] `TRIGGER_MACRO` (Existing)

## Phase 5: Rich Interactive Controls [DONE]
- [DONE] Stateful Widgets: Implement Toggles and Sliders (Horizontal/Vertical) in addition to Buttons.
- [DONE] Slider Logic: Support `LIVE` and `ON_RELEASE` update modes to optimize network traffic.
- [DONE] Material Icon Resolver: Enable string-to-icon mapping for server-defined visual styles.
- [DONE] Drag-to-Trash: Interactive widget removal in Edit Mode with collision feedback.
- [DONE] Multi-Step Picker: Support choosing widget variants (Button vs. Slider) when adding to the dashboard.
- [DONE] Persistence Migration: Upgrade SharedPreferences storage to support complex `GridWidget` objects with backward compatibility.

## Phase 6: Advanced Automation & Scripting [DONE]
Transition the project into a stateful, logic-driven ecosystem.
- [x] Automation AST: Defined sealed classes for complex triggers (Hold, Multi-tap, Sequence) and logic blocks.
- [x] GraalVM Integration: Integrated GraalVM JS engine into the JVM server for high-performance scripting.
- [x] Visual Routine Builder: Block-based editor UI (shared) for building complex automation routines.
- [x] Window Context: Support for `current_window_title` and `ActiveWindowTitleIs` conditions.
- [x] Multi-Trigger Stacks: Support for multiple independent triggers per routine.
- [x] Live Variable Inspector: Real-time dashboard for mouse, window, and system state.
- [x] Polling Controls: Granular settings for Window, Input, and System refresh rates.
- [x] Variable Picker: Categorized dropdown with live previews for routine building.
- [x] State Machine Triggers: Refactored TriggerListener for AST-based complex triggers.
- [x] Context Watcher 2.0: Enhanced X11 context watching using `xprop` for better compatibility.

## Phase 7: Validation, Hardening & Controller Support [IN PROGRESS]
- [x] Android Reconnection: Fix 'Connecting' freeze and duplicate server history buttons.
- [x] Performance: Optimize `java.awt.Robot` (singleton) and implement per-pulse `variableCache` for routines.
- [x] UI/UX Clarity: Add tooltips for Pack Activation states and live window focus.
- [x] Input Validation: Live red-highlighting and suggestions for keyboard input fields.
- [/] Controller Support:
    - [x] Core Infrastructure: Integrate Jamepad (SDL2) and implement background polling.
    - [x] Navigation Modes: Implement Standard Traversal and Virtual Cursor logic.
    - [x] Automation Integration: Add Controller Button triggers and actions to the AST.
    - [ ] Visual Feedback: Render the 'Virtual Cursor' dot in the Desktop UI overlay.
    - [ ] Movement Safety: Implement screen/window boundary clamping for the virtual cursor.
- [ ] Security Audit:
    - [ ] GraalVM Sandbox: Verify `allowHostClassLookup { false }` effectively blocks file system and network access from JS.
    - [ ] Log Cleanup: Verify if AWT clipboard warnings can be further suppressed if they persist.

## Phase 8: Backward Compatibility & Legacy Support [NOT STARTED]
Ensure the current server remains compatible with older client versions (v1.0 / last stable release).
- [ ] Version Detection: Server identifies client protocol version during handshake.
- [ ] Legacy Protocol Handler: Implement fallback logic for v1.0 clients (e.g., handling non-AST macro requests).
- [ ] Compatibility Testing: Clone the `main` branch (v1.0) into a separate directory to run side-by-side tests with the current `dev` server.
- [ ] Graceful Degradation: Ensure new features (like complex routines) either fallback safely or are hidden from older clients.
