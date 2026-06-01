# Automation JSON Schema

This document describes the JSON structure for `AutomationRoutine` and its components used in the MacroKap KMP project.

## AutomationRoutine

The root container for an automation routine.

| Field | Type | Description |
| :--- | :--- | :--- |
| `id` | String | Unique identifier for the routine. |
| `name` | String | Human-readable name of the routine. |
| `triggers` | List<[AutomationTrigger](#automationtrigger)> | List of triggers that can activate this routine. |
| `logicBlocks` | List<[LogicBlock](#logicblock)> | List of logic blocks containing conditions and actions. |

## AutomationTrigger

Polymorphic type used to define what triggers an automation.

### KeyHold
- `type`: "KeyHold"
- `keyName`: String
- `durationMs`: String (Flexible)

### MultiTap
- `type`: "MultiTap"
- `keyName`: String
- `tapCount`: String (Flexible)
- `windowMs`: String (Flexible)

### Sequence
- `type`: "Sequence"
- `keys`: List<String>
- `windowMs`: String (Flexible)

### OnConditionMet
- `type`: "OnConditionMet"
- `condition`: [AutomationCondition](#automationcondition)

### ControllerButton
- `type`: "ControllerButton"
- `button`: String
- `controllerIndex`: String (Default: "0")

## AutomationCondition

Polymorphic type for logical conditions.

### ActiveWindowIs
- `type`: "ActiveWindowIs"
- `processName`: String

### ActiveWindowTitleIs
- `type`: "ActiveWindowTitleIs"
- `windowTitle`: String

### Equals / GreaterThan / LessThan / Contains
- `type`: "Equals" | "GreaterThan" | "LessThan" | "Contains"
- `variable`: String
- `value`: String (or `substring` for Contains)

## LogicBlock

Groups actions with an optional condition.

| Field | Type | Description |
| :--- | :--- | :--- |
| `condition` | [AutomationCondition](#automationcondition)? | Optional condition to be met before executing actions. |
| `actions` | List<[AutomationAction](#automationaction)> | List of actions to execute. |

## AutomationAction

Polymorphic type for actions. Uses **`kind`** as the JSON discriminator (to avoid conflicts with `type` property in subclasses).

### MacroAction
- `kind`: "MacroAction"
- `macroId`: String

### ScriptAction
- `kind`: "ScriptAction"
- `script`: String

### ScriptEvent
- `kind`: "ScriptEvent"
- `script`: String

### LayerShift
- `kind`: "LayerShift"
- `layerId`: String
- `isMomentary`: Boolean

### KeyEvent
- `kind`: "KeyEvent"
- `keyName`: String
- `actionType`: String ("PRESS", "RELEASE", "TYPE")

### MouseEvent
- `kind`: "MouseEvent"
- `x`: String (Flexible)
- `y`: String (Flexible)
- `actionType`: String ("MOVE", "CLICK")
- `isAnimated`: Boolean (Default: false)

### MouseButtonEvent
- `kind`: "MouseButtonEvent"
- `buttonNumber`: String (Flexible)
- `actionType`: String ("PRESS", "RELEASE", "CLICK")

### ScrollEvent
- `kind`: "ScrollEvent"
- `amount`: String (Flexible)

### DelayEvent
- `kind`: "DelayEvent"
- `durationMs`: String (Flexible)

### SetAutoDelay
- `kind`: "SetAutoDelay"
- `delayMs`: String (Flexible)

### SetVariable
- `kind`: "SetVariable"
- `name`: String
- `value`: String

### MouseKeyboard
- `kind`: "MouseKeyboard"
- `actionType`: String
- `parameters`: Map<String, String>

### ControllerButton
- `kind`: "ControllerButton"
- `button`: String
- `controllerIndex`: String (Default: "0")
- `actionType`: String (Default: "PRESS")

---
*Note: Fields marked as "Flexible" use `FlexibleStringSerializer`, which allows them to be parsed from either a JSON number or a JSON string for better compatibility.*
