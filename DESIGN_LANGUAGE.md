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

**Key Principle**: Use `SurfaceVariant` for grouping related controls to provide depth without using heavy shadows. All currency-related text and icons (CurrencyExchange) strictly use the **Gold Standard** (`#FFFFD700`) to denote monetization and value.

### Iconography
- **Library**: Material Symbols / Icons.
- **Directional Icons**: Use `AutoMirrored` variants (e.g., `ArrowBack`, `ExitToApp`) to support RTL layouts.
- **Brand Icon**: A high-resolution 512px icon is used for desktop taskbars and Android splash screens.

## 2. Shared Interaction & Feedback

### Terminal-Inspired Aesthetics
- **Blinking Cursor (`> _`)**: Used during initialization or as a "heartbeat" indicator.
  - **Animation**: 400ms cycle (200ms ON / 200ms OFF).
- **Three-Dot Progress**: Used specifically for active network scanning or discovery.
  - **Animation**: Staggered scaling (600ms per dot).

### Status Feedback
- **Color Logic**:
    - **Success/Active**: Standard Green in Dark Theme, High-Contrast Green (`#008000`) in Light Theme.
    - **Error/Stopped**: Material 3 Error color (`#BA1A1A`).
    - **Grace Period**: Distinct Blue (`#2196F3`) used for the timer bar.
- **Toasts & Notifications**:
    - **Desktop**: Custom transparent overlay at `BottomCenter`.
    - **Android**: Native `Toast` API.

### Desktop Motion & Layout
- **Root Split Pane**: Primary horizontal split between Sidebar and Main Workspace.
- **Macro Manager**: Collapsible `SectionHeader`s for "Packs" and "Macros" with tiled item backgrounds.
- **Split Pane Feedback**: "Ghost Image" during dragging and dynamic transparency for handles (5% idle, 20% hover, 40% dragging).
- **Exit Behavior**: Three strategies (**Ask**, **Tray**, **Exit**).
- **Minimize Animation**: Quadratic Ease-In scaling and translation toward the system tray.

## 3. Accessibility & Usability

### High-Visibility Scrollbars
- **Thickness**: `8.dp`
- **Visibility**: Persistent or high-contrast against the background.

### Contrast Requirements
- In **Light Blue** theme, success indicators use darkened green (`#008000`) for accessibility.

## 4. Platform-Specific Design

### Android
- **Splash Screen**: Android 12+ standard with proper adaptive icon (512px source in 192dp container).
- **Navigation**: 3-tab system: **[0] My Dashboard**, **[1] Active Pack**, **[2] Marketplace**.

### Dashboard & Edit Mode (Android)
- **Long-Press**: Activates "Edit Mode" with item scaling (`1.1f`) and subtle rotation (`2 degrees`).
- **Drag-and-Drop**: Real-time grid reordering.
- **Drag-to-Trash**: A red, scaling trash can icon appears at the bottom during drags.
- **Macro Picker**: Stateful two-step dialog (Select Macro -> Select Variant).

### Mobile Pairing & QR Scanning
- **Keyboard-Aware**: Layout adjusts dynamically using `WindowInsets.ime`.
- **CameraX**: Supports pinch-to-zoom, tap-to-focus, and auto-exposure.

### Dynamic Kap Animations (Android)
- **Deduction**: Linear flight (600ms) from balance to button.
- **Boomerang**: 1000ms quadratic Bezier U-turn for grace-period executions (peaks at 80% distance).
- **Reward Spread**: Multiple Kaps burst and arch toward balance with floating gold "+100" text.

### Grace Timer Bar (Android)
- **Thickness**: `8.dp`
- **Thumb**: Sliding Kap icon.
- **Color**: Solid Blue (`#2196F3`) progress.

## 5. Gamepad & Controller Interaction

### Connectivity & Status
- **GamepadStatusIndicator**: Persistent in `TopAppBar`.
- **States**: Semi-transparent (Disconnected), Primary/Success Color (Connected), Pulse (Active Input).

## 6. Monetization & Marketplace UI

### Purchase Cards & Ads
- **Sticky Ad Footers**: Banner ads at the bottom of major screens, suppressed for Pro/Ad-Free users.
- **Badges**:
    - **WATCH AD**: For free rewarded options.
    - **$ Tags**: Relative price indicators ($ to $$$$$).
    - **ACTIVE**: For owned tiers.
- **Dynamic Prices**: Localized prices from Google Play shown in purchase dialogs.
