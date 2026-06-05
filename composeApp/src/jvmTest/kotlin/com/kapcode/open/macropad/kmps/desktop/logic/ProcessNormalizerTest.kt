package com.kapcode.open.macropad.kmps.desktop.logic

import com.kapcode.open.macropad.kmps.desktop.model.ActiveProcessInfo
import kotlin.test.Test
import kotlin.test.assertEquals

class ProcessNormalizerTest {

    @Test
    fun testAndroidStudioNormalization() {
        val info = ActiveProcessInfo(
            name = "studio",
            processName = "studio64.exe",
            id = "1",
            pid = "123",
            windowId = "1",
            command = "N/A",
            windowTitle = "my_project [~/projects/my_project]"
        )

        val normalized = ProcessNormalizer.normalize(info)

        assertEquals("Android Studio", normalized.name)
        assertEquals("Android Studio - my_project [~/projects/my_project]", normalized.windowTitle)
    }

    @Test
    fun testVSCodeNormalization() {
        val info = ActiveProcessInfo(
            name = "Code",
            processName = "code",
            id = "2",
            pid = "456",
            windowId = "2",
            command = "N/A",
            windowTitle = "index.js - project"
        )

        val normalized = ProcessNormalizer.normalize(info)

        assertEquals("VS Code", normalized.name)
        assertEquals("VS Code - index.js - project", normalized.windowTitle)
    }

    @Test
    fun testPhotoshopNormalization() {
        val info = ActiveProcessInfo(
            name = "Adobe Photoshop 2024",
            processName = "Photoshop.exe",
            id = "3",
            pid = "789",
            windowId = "3",
            command = "N/A",
            windowTitle = "image.psd @ 50% (RGB/8)"
        )

        val normalized = ProcessNormalizer.normalize(info)

        assertEquals("Photoshop", normalized.name)
        assertEquals("Photoshop - image.psd @ 50% (RGB/8)", normalized.windowTitle)
    }

    @Test
    fun testExistingTitlePrefixNotDuplicated() {
        val info = ActiveProcessInfo(
            name = "slack",
            processName = "slack",
            id = "4",
            pid = "101",
            windowId = "4",
            command = "N/A",
            windowTitle = "Slack - KapCode"
        )

        val normalized = ProcessNormalizer.normalize(info)

        assertEquals("Slack", normalized.name)
        assertEquals("Slack - KapCode", normalized.windowTitle) // No duplicate prepending
    }

    @Test
    fun testAndroidStudioLinuxJavaNormalization() {
        val info = ActiveProcessInfo(
            name = "jetbrains-studio",
            processName = "java",
            id = "0x12345",
            pid = "999",
            windowId = "0x12345",
            command = "/opt/android-studio/jbr/bin/java -Xmx4096m ...",
            windowTitle = "MacroKapKMP [/home/user/MacroKapKMP]"
        )

        val normalized = ProcessNormalizer.normalize(info)

        assertEquals("Android Studio", normalized.name)
        assertEquals("Android Studio - MacroKapKMP [/home/user/MacroKapKMP]", normalized.windowTitle)
    }

    @Test
    fun testVSCodeLinuxNormalization() {
        val info = ActiveProcessInfo(
            name = "code",
            processName = "code",
            id = "0x67890",
            pid = "888",
            windowId = "0x67890",
            command = "/usr/share/code/code ...",
            windowTitle = "ProcessNormalizer.kt - MacroKapKMP"
        )

        val normalized = ProcessNormalizer.normalize(info)

        assertEquals("VS Code", normalized.name)
        assertEquals("VS Code - ProcessNormalizer.kt - MacroKapKMP", normalized.windowTitle)
    }
}
