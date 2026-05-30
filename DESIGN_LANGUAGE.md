# Design Language

This document outlines the visual and interaction principles for MacroKapKMP, ensuring a consistent and accessible experience across Android and Desktop platforms.

## 1. Visual Identity

MacroKapKMP uses **Material Design 3 (M3)** as its foundational design system, customized with a specialized "Blue" color language.

### Color Palette

The application supports two primary themes: **Dark Blue** (default) and **Light Blue**.

| Role | Light Blue | Dark Blue |
| :--- | :--- | :--- |
| **Primary** | `#0061A4` | `#9ECAFF` |
| **Surface Variant** | `#DFE2EB` | `#43474E` |
| **Background** | `#FDFCFF` | `#1A1C1E` |
| **Status (Success)** | `#008000` (High Contrast) | `#00FF00` (Standard Green) |
| **Gold Standard** | `#FFFFD700` | `#FFFFD700` |

**Key Principle**: Use `SurfaceVariant` for grouping related controls (e.g., Device List, Console background) to provide depth without using heavy shadows. All currency-related text and icons (CurrencyExchange) strictly use the **Gold Standard** (`#FFFFD700`) to denote monetization and value.

### Iconography
- **Library**: Material Symbols / Icons.
- **Directional Icons**: Use `AutoMirrored` variants (e.g., `ArrowBack`, `ExitToApp`) to support RTL layouts automatically.
- **Brand Icon**: A high-resolution 512px icon is used for desktop taskbars and Android splash screens to ensure crispness across all DPI levels.

### Rich Widget Components
- **Macro Buttons**: Fixed-height (90dp) tiles with centered labels and icons.
- **Toggles**: Feature a built-in `Switch` component. The background color or elevation shift denotes the "On" state.
- **Sliders**: 
    - **Horizontal**: Standard width with integrated slider.
    - **Vertical**: Double-height (180dp) tiles for fine-grained vertical control.
- **Dynamic Contrast**: Text and icons automatically adjust between White (`#FFFFFF`) and Black (`#000000`) based on the calculated luminance of the widget's background color to ensure accessibility on custom-colored buttons.

## 2. Shared Interaction & Feedback

### Terminal-Inspired Aesthetics (Cross-Platform)
To create a unified "tech-forward" feel, both platforms use terminal-inspired animations for background processes:
- **Blinking Cursor (`> _`)**: Used during initialization or as a "heartbeat" indicator.
  - **Component**: `BlinkingCursor` (Common).
  - **Animation**: 400ms cycle (200ms ON / 200ms OFF).
  - **Color**: `MaterialTheme.colorScheme.primary`.
- **Three-Dot Progress**: Used specifically for active network scanning or discovery.
  - **Component**: `ThreeDotsLoading` (Common).
  - **Animation**: Staggered scaling (600ms per dot).

### Status Feedback
- **Color Logic**:
    - **Success/Active**: Standard Green in Dark Theme, High-Contrast Green (`#008000`) in Light Theme.
    - **Error/Stopped**: Material 3 Error color (`#BA1A1A`).
- **Snackbars**: Standardized across both platforms for macro execution feedback (Start, Finish, E-Stop).
- **Unified Notification System (Toasts)**:
    - **Desktop Custom Overlay**: A transparent, always-on-top window positioned at `BottomCenter` for non-intrusive alerts. Replaces standard OS notifications for precise timing control.
    - **Android Native Toasts**: Leverages the Android `Toast` API for consistent mobile feedback.
    - **Granular Targeting**: Alerts can be routed to the **Server Only**, **Selected Clients Only**, or **Both**, managed via individual device checkboxes in settings.
    - **Contextual Awareness**: Option to include application names (e.g., "Floorp") in activation messages.
    - **Configurable Persistence**: A master "Toast Duration" setting (ms) synchronizes display times across all platforms.

### Desktop Motion & Layout
- **Root Split Pane**: The Desktop layout uses a primary horizontal split between the **Left Sidebar** (Console/Inspector) and the **Main Workspace** (Connections/Macros).
- **Macro Manager Refactoring**: The Macro Manager screen utilizes collapsible, high-contrast headers (`SectionHeader`) for "Packs Manager" and "Macro Manager".
    - **Headers**: Use solid `Surface` color (Black in Dark mode, White in Light mode) for prominent visual grouping.
    - **Content Items**: Use a subtle `SurfaceVariant` background with tiled patterns (Keyboard keys for Macros, Library books for Packs) to distinguish from headers.
    - **Search**: Independent search bars are integrated into each header for local filtering.
- **Nested Split Panes**: Within the Main Workspace, a vertical split separates **Connections** (left) from **Macros** (right).
- **Ghost Image Resizing**: To provide real-time visual feedback, splitters display a "Ghost Image" (a semi-transparent representation of the new divider position) during active dragging.
- **Dynamic Splitter Highlighting**: All split panes use custom-styled "pill" handles (pulltabs). These handles feature dynamic transparency:
    - **Idle**: 5% alpha.
    - **Hover**: 20% alpha.
    - **Pressed/Dragging**: 40% alpha.
- **Configurable Exit Behavior**: The application provides three exit strategies:
    - **Ask**: Displays a three-way confirmation dialog (Exit to Tray, Just Exit, or Cancel).
    - **Exit to Tray**: Automatically minimizes the application to the system tray.
    - **Just Exit**: Terminates the process immediately.
- **Minimize Animation**: Uses a **Quadratic Ease-In** animation that scales and translates the window toward the system tray area when "Exit to Tray" is triggered (respecting the `animateToTray` setting).
- **Tray Interaction**: Single-click on the tray icon toggles window visibility; right-click provides an OS-native context menu. Manual "Exit" triggers from the tray or UI header bypass the "Tray" setting to ensure the user can always fully quit the application.

### Integrated Inspection & Context Awareness
- **Live Variable Inspector**: The Inspector pane provides a real-time, scrolling dashboard of system variables:
    - **Window Context**: Current/Last process names and full window titles.
    - **Input Context**: Real-time mouse coordinates and pixel colors.
    - **System Context**: Clipboard text and system timers.
- **Visual Routine Builder**: A shared, block-based UI for creating complex automation.
    - **Triggers**: Supports a "stack" of multiple triggers per routine.
    - **Actions**: Provides dedicated editors for low-level keyboard/mouse events, scripts, and macro execution.
    - **Variables**: Integrated `VariablePickerField` allows selecting system variables with live previews directly in the dropdown.

## 3. Accessibility & Usability

### High-Visibility Scrollbars
To assist users on touchscreens or with hidden system scrollbars, all major scrollable areas (Settings, Macro Timeline, Event Dialogs, Console Toolbar) use:
- **Thickness**: `8.dp`
- **Visibility**: Persistent or high-contrast against the background.
- **Console Toolbar**: Specifically uses a `HorizontalScrollbar` with `onPointerEvent` for mouse-wheel scrolling support.

### Contrast Requirements
- In the **Light Blue** theme, success/running indicators use a darkened green (`#008000`) instead of bright green to ensure readability against the light surface variant.

### Progressive Disclosure (Tooltips)
- **Settings**: Descriptions for complex security toggles (e.g., Device Discovery, One-Time Approvals ONLY) are moved into `TooltipArea` components. This reduces visual noise and "mental load" in the settings screen while keeping information available on-demand.

## 4. Platform-Specific Design

### Android
- **Splash Screen**: Follows the Android 12+ standard using `androidx.core:core-splashscreen`.
- **Adaptive Icons**: 512px source wrapped in a 192dp container to fit within the OS-enforced "safe circle" without clipping or "black ring" artifacts.
- **Navigation Order**: The main navigation uses a bottom-bar or top-tab system with the following order:
    1.  **[0] My Dashboard**: The primary, user-curated view.
    2.  **[1] Active Pack**: Context-aware macros based on the current desktop process.
    3.  **[2] Marketplace**: Full-screen overlay or dedicated tab for downloading new packs.

### Dashboard & Edit Mode (Android)
To allow users to customize their experience, the "My Dashboard" tab features an interactive grid:
- **Long-Press Activation**: Users initiate "Edit Mode" by long-pressing any macro in the dashboard.
- **Visual Feedback (Dragging)**:
    - **Scaling**: The dragged item scales to `1.1f` to appear "lifted" from the surface.
    - **Rotation**: A subtle `2-degree` rotation is applied to create a "loose" or "floating" feel.
    - **Shadows**: Elevation is increased while dragging to distinguish the active item from the grid.
- **Dynamic Reordering**: The grid automatically shifts items to fill gaps in real-time as the dragged macro moves over other positions.
- **Macro Picker**: A prominent **Floating Action Button (FAB)** opens a stateful, two-step dialog:
    1.  **Macro Selection**: Choose the macro from the server.
    2.  **Variant Selection**: Choose the widget type (Button, Toggle, or Slider).
- **Drag-to-Trash**: 
    - When a drag operation starts in Edit Mode, a **Trash Can** icon appears at the bottom center of the screen.
    - **Collision Awareness**: The trash can scales up (`1.5x`) and turns red (`MaterialTheme.colorScheme.error`) when a widget is hovered over it.
    - **Confirmation**: Dropping an item onto the trash can immediately removes it from the dashboard.
- **Persistence**: Any additions, removals, or reordering are immediately persisted to local storage using a JSON-based schema in `SharedPreferences`.

### Mobile Pairing & QR Scanning
To ensure secure and ergonomic device pairing on mobile:
- **Keyboard-Aware Layout**: Use `WindowInsets.ime` to detect keyboard state. Pinned instructions are placed at the top, and critical action fields (like Manual IP entry) are pinned to the bottom. Central content (QR Scanner) uses `Modifier.weight(1f)` to shrink gracefully when the keyboard is visible.
- **CameraX Interactions**:
    - **Pinch-to-Zoom**: Enabled for scanning at a distance.
    - **Tap-to-Focus**: Allows users to manually focus on poorly lit or distant screens.
    - **Auto-Exposure**: Continuously adjusts to varying monitor brightness levels.
- **Standardized Icons**:
    - `Icons.Default.QrCodeScanner`: Toggle QR scanning mode.
    - `Icons.Default.KeyboardArrowUp/Down`: Toggle manual entry keyboard.
    - `Icons.Default.Done`: Confirm/Submit pairing.
    - `Icons.Default.Close`: Cancel/Exit pairing.

### Desktop (Swing/Compose Bridge)
- **JSON Editor**: The `RSyntaxTextArea` (Swing) component is dynamically themed to match the Compose UI.
  - Dark Blue Theme -> `dark.xml`
  - Light Blue Theme -> `idea.xml`
- **Dialogs**: All modal interactions use `Window` (replacing `DialogWindow` for better minimization behavior) or custom `Surface`-based overlays to ensure they remain top-level over Swing-based components.

## 5. Gamepad & Controller Interaction

### Connectivity & Status
To provide immediate feedback on controller status, the application utilizes a persistent **GamepadStatusIndicator** in the `TopAppBar`.

- **Visual States**:
    - **No Controllers**: The gamepad icon is semi-transparent (`0.38f` alpha) or matches the `onSurfaceVariant` color to indicate inactivity.
    - **Controller Connected**: The icon switches to `MaterialTheme.colorScheme.primary` or the **Success (Green)** status color (respecting theme-specific contrast).
    - **Active Input**: During button presses or stick movement, the indicator can provide a subtle "pulse" or highlight to confirm the application is receiving signals.

### Focus & Navigation (Common)
The UI is designed to be fully navigable via D-Pad and Left Stick:
- **Focus Rings**: Focused elements MUST display a high-contrast focus ring (using `MaterialTheme.colorScheme.primary`).
- **Dead Zones**: Software-level dead zones are applied to analog sticks to prevent "drift" during UI navigation.
- **Button Mapping**:
    - **Accept**: Bottom face button (e.g., 'A' / Cross).
    - **Back/Cancel**: Right face button (e.g., 'B' / Circle).
    - **Menu**: 'Start' or 'Menu' button.

## 6. Monetization & Marketplace UI

### Purchase Cards & Ads
- **Highlighting**: Use `primaryContainer` for tiers the user *doesn't* yet own to draw attention.
- **Sticky Ad Footers**: All banner ads on scrollable screens (Marketplace, Settings) MUST be implemented as sticky footers. They stay visible at the bottom of the screen while content scrolls behind/above them.
- **Universal Ad Logic**: Ads MUST be hidden if any of the following are true:
    - `isPro` (local purchase) is active.
    - `isAdFree` (local purchase) is active.
    - `isServerProActive` (shared status from server) is active.
- **Badges**:
    - **WATCH AD**: Used for free rewarded options.
    - **$ Tags**: Relative price indicators ($ to $$$$$) pinned to the top-right of purchase cards.
    - **ACTIVE**: Primary-colored badge for currently owned tiers.
- **Benefit Lists**: Displayed as small-text bullet points with check icons (`Icons.Default.Check`) below the product description.
- **Currency**: Tokens are represented by `Icons.Default.CurrencyExchange` in `GoldCurrencyColor` (0xFFFFD700).

### Marketplace Status
- **Marketplace Coming Soon**: Displays a `Storefront` icon with a description about future community macro packs.
