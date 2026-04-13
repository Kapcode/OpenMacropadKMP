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
