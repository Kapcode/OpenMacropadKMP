package UI

import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.JLabel

class DynamicFontSizeLabel(private val defaultSize: Float, private val minSize: Float) : JLabel() {

    override fun paintComponent(g: Graphics) {
        println("--- paintComponent for label with text: '$text' ---")
        val g2d = g as Graphics2D
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        var currentFont = font.deriveFont(font.style, defaultSize)
        println("  Reset font to default size: ${currentFont.size2D} with style: ${currentFont.style}")

        var fontMetrics = g.getFontMetrics(currentFont)
        val textWidth = fontMetrics.stringWidth(text)
        val availableWidth = width - 6 // Padding

        println("  Available Width: $availableWidth, Text Width: $textWidth")

        if (textWidth > availableWidth) {
            println("  Text overflows. Shrinking font...")
            var newFontSize = defaultSize
            while (g.getFontMetrics(currentFont).stringWidth(text) > availableWidth && newFontSize > minSize) {
                newFontSize -= 1f
                currentFont = currentFont.deriveFont(font.style, newFontSize)
            }
            println("  New font size: ${currentFont.size2D}")
        } else {
            println("  Text fits. No font change needed.")
        }

        g.font = currentFont
        g.color = foreground

        val y = (height - g.getFontMetrics(g.font).height) / 2 + g.getFontMetrics(g.font).ascent
        g.drawString(text, 3, y)
        println("--- End paintComponent for label: '$text' ---\n")
    }
}