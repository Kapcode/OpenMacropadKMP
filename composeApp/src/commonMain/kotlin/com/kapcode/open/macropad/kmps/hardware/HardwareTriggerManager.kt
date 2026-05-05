package com.kapcode.open.macropad.kmps.hardware

/**
 * Interface for managing hardware-based triggers like physical buttons
 * or proximity sensors across different platforms.
 */
interface HardwareTriggerManager {
    /**
     * Starts listening for hardware trigger events.
     */
    fun start()

    /**
     * Stops listening for hardware trigger events.
     */
    fun stop()

    /**
     * Handlers a key down event. Returns true if the event was consumed.
     * @param keyCode The platform-specific key code.
     */
    fun handleKeyDown(keyCode: Int): Boolean
}
