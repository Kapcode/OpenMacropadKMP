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
- `durationMs`: String (Flexible: accepts numbers or strings)

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

Polymorphic type for actions. Uses `type` as the JSON discriminator.

### MacroAction
- `type`: "MacroAction"
- `macroId`: String

### ScriptAction
- `type`: "ScriptAction"
- `script`: String

### LayerShift
- `type`: "LayerShift"
- `layerId`: String
- `isMomentary`: Boolean

### KeyEvent
- `type`: "KeyEvent"
- `keyName`: String
- `actionType`: String ("PRESS", "RELEASE", "TYPE")

### MouseEvent
- `type`: "MouseEvent"
- `x`: String (Flexible)
- `y`: String (Flexible)
- `actionType`: String ("MOVE", "CLICK")
- `isAnimated`: Boolean (Default: false)

### MouseButtonEvent
- `type`: "MouseButtonEvent"
- `buttonNumber`: String (Flexible)
- `actionType`: String ("PRESS", "RELEASE", "CLICK")

### ScrollEvent
- `type`: "ScrollEvent"
- `amount`: String (Flexible)

### DelayEvent
- `type`: "DelayEvent"
- `durationMs`: String (Flexible)

### SetAutoDelay
- `type`: "SetAutoDelay"
- `delayMs`: String (Flexible)

### SetVariable
- `type`: "SetVariable"
- `name`: String
- `value`: String

### MouseKeyboard
- `type`: "MouseKeyboard"
- `actionType`: String
- `parameters`: Map<String, String>

### ControllerButton
- `type`: "ControllerButton"
- `button`: String
- `controllerIndex`: String (Default: "0")
- `actionType`: String (Default: "PRESS")

---
*Note: Fields marked as "Flexible" use `FlexibleStringSerializer`, which allows them to be parsed from either a JSON number or a JSON string for better compatibility.*
