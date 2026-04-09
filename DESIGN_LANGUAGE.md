# Design Language

This document outlines the visual and interaction principles for OpenMacropadKMP, ensuring a consistent and accessible experience across Android and Desktop platforms.

## 1. Visual Identity

OpenMacropadKMP uses **Material Design 3 (M3)** as its foundational design system, customized with a specialized "Blue" color language.

### Color Palette

The application supports two primary themes: **Dark Blue** (default) and **Light Blue**.

| Role | Light Blue | Dark Blue |
| :--- | :--- | :--- |
| **Primary** | `#0061A4` | `#9ECAFF` |
| **Surface Variant** | `#DFE2EB` | `#43474E` |
| **Background** | `#FDFCFF` | `#1A1C1E` |
| **Status (Success)** | `#008000` (High Contrast) | `#00FF00` (Standard Green) |

**Key Principle**: Use `SurfaceVariant` for grouping related controls (e.g., Device List, Console background) to provide depth without using heavy shadows.

### Iconography
- **Library**: Material Symbols / Icons.
- **Directional Icons**: Use `AutoMirrored` variants (e.g., `ArrowBack`, `ExitToApp`) to support RTL layouts automatically.
- **Brand Icon**: A high-resolution 512px icon is used for desktop taskbars and Android splash screens to ensure crispness across all DPI levels.

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

### Desktop Motion
- **Minimize to Tray**: Uses a **Quadratic Ease-In** animation that scales and translates the window toward the system tray area.
- **Tray Interaction**: Single-click on the tray icon toggles window visibility; right-click provides an OS-native context menu.

## 3. Accessibility & Usability

### High-Visibility Scrollbars
To assist users on touchscreens or with hidden system scrollbars, all major scrollable areas (Settings, Macro Timeline, Event Dialogs) use:
- **Thickness**: `8.dp`
- **Visibility**: Persistent or high-contrast against the background.

### Contrast Requirements
- In the **Light Blue** theme, success/running indicators use a darkened green (`#008000`) instead of bright green to ensure readability against the light surface variant.

### Progressive Disclosure (Tooltips)
- **Settings**: Descriptions for complex security toggles (e.g., Device Discovery, One-Time Approvals ONLY) are moved into `TooltipArea` components. This reduces visual noise and "mental load" in the settings screen while keeping information available on-demand.

## 4. Platform-Specific Design

### Android
- **Splash Screen**: Follows the Android 12+ standard using `androidx.core:core-splashscreen`.
- **Adaptive Icons**: 512px source wrapped in a 192dp container to fit within the OS-enforced "safe circle" without clipping or "black ring" artifacts.

### Desktop (Swing/Compose Bridge)
- **JSON Editor**: The `RSyntaxTextArea` (Swing) component is dynamically themed to match the Compose UI.
  - Dark Blue Theme -> `dark.xml`
  - Light Blue Theme -> `idea.xml`
- **Dialogs**: All modal interactions use `DialogWindow` or custom `Surface`-based overlays to ensure they remain top-level over Swing-based components.
