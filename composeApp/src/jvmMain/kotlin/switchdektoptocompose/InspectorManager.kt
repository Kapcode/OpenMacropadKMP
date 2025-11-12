package switchdektoptocompose

import com.github.kwhat.jnativehook.GlobalScreen
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener
import java.awt.MouseInfo
import java.awt.Rectangle
import java.awt.Robot
import java.awt.Toolkit
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.swing.JFileChooser
import javax.swing.SwingUtilities

class InspectorManager(
    private val viewModel: InspectorViewModel,
    private val consoleViewModel: ConsoleViewModel
) : NativeKeyListener {

    private val robot = Robot()

    fun startListening() {
        if (!GlobalScreen.isNativeHookRegistered()) {
            GlobalScreen.registerNativeHook()
        }
        GlobalScreen.addNativeKeyListener(this)
        consoleViewModel.addLog(LogLevel.Info, "Inspector started listening.")
    }

    fun stopListening() {
        GlobalScreen.removeNativeKeyListener(this)
        consoleViewModel.addLog(LogLevel.Info, "Inspector stopped listening.")
    }

    override fun nativeKeyReleased(e: NativeKeyEvent) {
        val selectedFKey = viewModel.selectedFKey.value
        val pressedKey = NativeKeyEvent.getKeyText(e.keyCode)

        if (pressedKey.equals(selectedFKey, ignoreCase = true)) {
            inspect()
        }
    }

    private fun inspect() {
        val mousePos = MouseInfo.getPointerInfo().location
        val color = robot.getPixelColor(mousePos.x, mousePos.y)

        val hexColor = String.format("#%02x%02x%02x", color.red, color.green, color.blue)
        val argbColor = "ARGB(${color.alpha}, ${color.red}, ${color.green}, ${color.blue})"

        consoleViewModel.addLog(LogLevel.Info, "--- Inspector Info ---")
        consoleViewModel.addLog(LogLevel.Info, "Mouse Position: X=${mousePos.x}, Y=${mousePos.y}")
        consoleViewModel.addLog(LogLevel.Info, "Pixel Color (Hex): $hexColor")
        consoleViewModel.addLog(LogLevel.Info, "Pixel Color (ARGB): $argbColor")

        if (viewModel.screenshotOnPress.value) {
            takeScreenshot()
        }
    }
    
    private fun takeScreenshot() {
        try {
            val screenRect = getScreenshotRectangle()
            val screenshot = robot.createScreenCapture(screenRect)

            SwingUtilities.invokeLater {
                val fileChooser = JFileChooser()
                fileChooser.dialogTitle = "Save Screenshot"
                fileChooser.selectedFile = File("screenshot.png")
                if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                    val fileToSave = fileChooser.selectedFile
                    ImageIO.write(screenshot, "png", fileToSave)
                    consoleViewModel.addLog(LogLevel.Info, "Screenshot saved to: ${fileToSave.absolutePath}")
                }
            }
        } catch (ex: Exception) {
            consoleViewModel.addLog(LogLevel.Error, "Error taking screenshot: ${ex.message}")
            ex.printStackTrace()
        }
    }

    private fun getScreenshotRectangle(): Rectangle {
        val tlX = viewModel.topLeftX.value.toIntOrNull()
        val tlY = viewModel.topLeftY.value.toIntOrNull()
        val brX = viewModel.bottomRightX.value.toIntOrNull()
        val brY = viewModel.bottomRightY.value.toIntOrNull()

        return if (tlX != null && tlY != null && brX != null && brY != null) {
            Rectangle(tlX, tlY, brX - tlX, brY - tlY)
        } else {
            Rectangle(Toolkit.getDefaultToolkit().screenSize)
        }
    }
    
    override fun nativeKeyPressed(e: NativeKeyEvent) {}
    override fun nativeKeyTyped(e: NativeKeyEvent) {}
}