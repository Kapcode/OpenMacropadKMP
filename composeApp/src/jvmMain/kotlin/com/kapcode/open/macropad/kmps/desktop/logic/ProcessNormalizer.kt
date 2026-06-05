package com.kapcode.open.macropad.kmps.desktop.logic

import com.kapcode.open.macropad.kmps.desktop.model.ActiveProcessInfo

object ProcessNormalizer {
    
    private data class NormRule(
        val canonicalName: String,
        val processPatterns: List<String> = emptyList(),
        val appNamePatterns: List<String> = emptyList(),
        val commandPatterns: List<String> = emptyList()
    )

    private val rules = listOf(
        // IDEs
        NormRule("Android Studio", listOf("studio", "studio64"), listOf("Android Studio", "jetbrains-studio"), listOf("android-studio", "studio.sh")),
        NormRule("IntelliJ IDEA", listOf("idea", "idea64"), listOf("IntelliJ IDEA", "jetbrains-idea"), listOf("idea.sh")),
        NormRule("VS Code", listOf("code", "code-insiders", "Code"), listOf("Visual Studio Code", "code"), listOf("visual-studio-code")),
        NormRule("WebStorm", listOf("webstorm", "webstorm64"), emptyList(), listOf("webstorm.sh")),
        NormRule("PyCharm", listOf("pycharm", "pycharm64"), emptyList(), listOf("pycharm.sh")),
        NormRule("CLion", listOf("clion", "clion64"), emptyList(), listOf("clion.sh")),
        NormRule("Visual Studio", listOf("devenv"), listOf("Visual Studio")),
        
        // Design / Editors
        NormRule("Photoshop", listOf("photoshop"), listOf("Adobe Photoshop")),
        NormRule("Illustrator", listOf("illustrator"), listOf("Adobe Illustrator")),
        NormRule("Premiere Pro", listOf("premiere"), listOf("Adobe Premiere Pro")),
        NormRule("After Effects", listOf("afterfx"), listOf("Adobe After Effects")),
        NormRule("DaVinci Resolve", listOf("resolve"), listOf("DaVinci Resolve")),
        NormRule("Figma", listOf("figma")),
        NormRule("GIMP", listOf("gimp")),
        
        // Productivity
        NormRule("Word", listOf("winword"), listOf("Microsoft Word")),
        NormRule("Excel", listOf("excel"), listOf("Microsoft Excel")),
        NormRule("PowerPoint", listOf("powerpnt"), listOf("Microsoft PowerPoint")),
        NormRule("Outlook", listOf("outlook"), listOf("Microsoft Outlook")),
        NormRule("OneNote", listOf("onenote")),
        NormRule("Slack", listOf("slack")),
        NormRule("Discord", listOf("discord")),
        NormRule("Teams", listOf("teams"), listOf("Microsoft Teams")),
        NormRule("Spotify", listOf("spotify")),
        NormRule("Chrome", listOf("chrome"), listOf("Google Chrome")),
        NormRule("Firefox", listOf("firefox")),
        NormRule("Edge", listOf("msedge"), listOf("Microsoft Edge"))
    )

    fun normalize(info: ActiveProcessInfo): ActiveProcessInfo {
        val rule = rules.find { rule ->
            val procMatch = rule.processPatterns.any { pattern -> 
                info.processName.contains(pattern, ignoreCase = true) || 
                info.processName.removeSuffix(".exe").equals(pattern, ignoreCase = true)
            }
            val appMatch = rule.appNamePatterns.any { pattern -> 
                info.name.contains(pattern, ignoreCase = true)
            }
            val cmdMatch = rule.commandPatterns.any { pattern ->
                info.command.contains(pattern, ignoreCase = true)
            }
            procMatch || appMatch || cmdMatch
        } ?: return info

        // If matched, ensure canonical name is in the window title
        val normalizedTitle = if (info.windowTitle.contains(rule.canonicalName, ignoreCase = true)) {
            info.windowTitle
        } else {
            "${rule.canonicalName} - ${info.windowTitle}"
        }

        return info.copy(
            name = rule.canonicalName,
            windowTitle = normalizedTitle
        )
    }
}
