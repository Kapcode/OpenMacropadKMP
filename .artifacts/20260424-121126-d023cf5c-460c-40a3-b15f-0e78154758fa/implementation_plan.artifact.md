# Refactor Desktop Macro and Pack Manager UI

Refactor the `MacroManagerScreen` to provide dedicated, collapsible headers for "Packs Manager" and "Macro Manager", each with its own search bar, action buttons, and collapse/expand functionality. Additionally, enhance the visual styling of items with tiled backgrounds and updated iconography.

## Proposed Changes

### ViewModel and State
(Already implemented in previous step)

### UI Components

#### [MacroManagerScreen.kt](file:///mnt/512_nvme/Master_Projects/Android/OpenMacropadKMP/composeApp/src/jvmMain/kotlin/switchdektoptocompose/ui/MacroManagerScreen.kt)

- **Update `SectionHeader`**:
    - Use `Icons.Default.PostAdd` (Page with plus) for the "New Pack" icon.
    - Set background color to solid `surface` (effectively black in dark mode and white in light mode relative to the variant).
- **Update `PackItem` and `MacroItem`**:
    - Implement a `TiledBackground` modifier/composable that draws a repeating pattern.
    - **Macro Item**: Tiled keyboard key icon (`Icons.Default.Keyboard`).
    - **Pack Item**: Tiled pack icon (`Icons.AutoMirrored.Filled.LibraryBooks`).
    - Set the background of the items to `surfaceVariant` (grey/off-white) to contrast with the headers.
    - Background patterns use a subtle, fitting color for both dark and light modes (low alpha).

---

## Verification Plan

### Automated Tests
- Run `analyze_file` on `MacroManagerScreen.kt`.

### Manual Verification
- Visual inspection of the item backgrounds in both dark and light themes (if possible to toggle).
- Verify the "New Pack" icon is updated to `PostAdd`.
