package com.kapcode.open.macropad.kmps.hardware

/**
 * A no-op implementation of [HardwareTriggerManager] for platforms that don't
 * support hardware triggers yet (like Desktop).
 */
class NoOpHardwareTriggerManager : HardwareTriggerManager {
    override fun start() {
        // No-op
    }

    override fun stop() {
        // No-op
    }

    override fun handleKeyDown(keyCode: Int): Boolean {
        // No-op
        return false
    }
}
