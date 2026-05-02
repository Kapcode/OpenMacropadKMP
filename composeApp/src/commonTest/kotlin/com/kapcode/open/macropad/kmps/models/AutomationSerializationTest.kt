package com.kapcode.open.macropad.kmps.models

import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertEquals

class AutomationSerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    @Test
    fun testKeyEventSerialization() {
        val action: AutomationAction = AutomationAction.KeyEvent("A", "PRESS")
        val jsonString = json.encodeToString(action)
        
        // Check that the discriminator is "type" and value is "KeyEvent"
        assert(jsonString.contains("\"type\": \"KeyEvent\""))
        assert(jsonString.contains("\"keyName\": \"A\""))
        assert(jsonString.contains("\"actionType\": \"PRESS\""))

        val decoded = json.decodeFromString<AutomationAction>(jsonString)
        assertEquals(action, decoded)
    }

    @Test
    fun testMouseEventSerialization() {
        val action: AutomationAction = AutomationAction.MouseEvent("100", "200", "MOVE")
        val jsonString = json.encodeToString(action)
        
        assert(jsonString.contains("\"type\": \"MouseEvent\""))
        assert(jsonString.contains("\"x\": \"100\""))
        assert(jsonString.contains("\"y\": \"200\""))
        assert(jsonString.contains("\"actionType\": \"MOVE\""))

        val decoded = json.decodeFromString<AutomationAction>(jsonString)
        assertEquals(action, decoded)
    }

    @Test
    fun testAutomationRoutineSerialization() {
        val routine = AutomationRoutine(
            id = "test-id",
            name = "Test Routine",
            triggers = listOf(AutomationTrigger.KeyHold("Ctrl", "500")),
            logicBlocks = listOf(
                LogicBlock(
                    condition = AutomationCondition.ActiveWindowIs("chrome.exe"),
                    actions = listOf(
                        AutomationAction.KeyEvent("T", "PRESS"),
                        AutomationAction.DelayEvent("100")
                    )
                )
            )
        )

        val jsonString = json.encodeToString(routine)
        
        // Verify structure
        assert(jsonString.contains("\"id\": \"test-id\""))
        assert(jsonString.contains("\"type\": \"KeyEvent\""))
        assert(jsonString.contains("\"type\": \"DelayEvent\""))

        val decoded = json.decodeFromString<AutomationRoutine>(jsonString)
        assertEquals(routine, decoded)
    }

    @Test
    fun testBackwardCompatibilityWithFlexibleString() {
        // Simulating JSON where durationMs is a number instead of a string
        val jsonWithNumber = """
            {
                "type": "DelayEvent",
                "durationMs": 500
            }
        """.trimIndent()

        val decoded = json.decodeFromString<AutomationAction>(jsonWithNumber)
        assert(decoded is AutomationAction.DelayEvent)
        assertEquals("500", (decoded as AutomationAction.DelayEvent).durationMs)
    }
}
