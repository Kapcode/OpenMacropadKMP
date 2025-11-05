package UI

import com.kitfox.svg.SVGUniverse
import java.awt.Component
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.net.URI
import javax.swing.Icon

object SvgIconRenderer {

    private val svgUniverse = SVGUniverse()

    fun getIcon(resourcePath: String, width: Int, height: Int): Icon? {
        val resourceUrl = this.javaClass.getResource(resourcePath)
        if (resourceUrl == null) {
            System.err.println("SVG resource not found: $resourcePath")
            return null
        }

        return try {
            // This can throw a NumberFormatException if the SVG path data is malformed.
            // We catch it here to prevent a crash and allow the app to continue.
            val uri = svgUniverse.loadSVG(resourceUrl)
            SvgIcon(uri, width, height)
        } catch (e: Exception) {
            System.err.println("ERROR: Could not parse SVG file at $resourcePath. It may be invalid.")
            e.printStackTrace()
            null // Return null so the UI can fall back to text or nothing.
        }
    }

    private class SvgIcon(private val svgUri: URI, private val width: Int, private val height: Int) : Icon {

        override fun paintIcon(c: Component?, g: Graphics?, x: Int, y: Int) {
            // Synchronize on the shared universe to prevent threading-related race conditions.
            synchronized(svgUniverse) {
                val g2d = g?.create() as? Graphics2D ?: return

                try {
                    val diagram = svgUniverse.getDiagram(svgUri)
                    if (diagram == null) {
                        System.err.println("Could not get diagram for URI: $svgUri")
                        return
                    }

                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)

                    // Save the original graphics transform to restore it later.
                    val oldTransform = g2d.transform

                    // Translate to the icon's position on the component.
                    g2d.translate(x, y)

                    // Explicitly calculate the scale required to fit the desired dimensions.
                    val scaleX = width / diagram.width.toDouble()
                    val scaleY = height / diagram.height.toDouble()
                    g2d.scale(scaleX, scaleY)

                    // Render the diagram using the transformed graphics context.
                    diagram.render(g2d)

                    // Restore the original transform so we don't affect other painting operations.
                    g2d.transform = oldTransform

                } catch (e: Exception) {
                    System.err.println("Error rendering SVG icon: $svgUri")
                    e.printStackTrace()
                } finally {
                    g2d.dispose()
                }
            }
        }

        override fun getIconWidth(): Int = width

        override fun getIconHeight(): Int = height
    }
}