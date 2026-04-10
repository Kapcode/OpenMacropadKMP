package switchdektoptocompose.utils

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.awt.Color
import java.awt.image.BufferedImage

object QrCodeGenerator {
    fun generateQrCode(content: String, size: Int = 512): ImageBitmap {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
        
        val width = bitMatrix.width
        val height = bitMatrix.height
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        
        for (x in 0 until width) {
            for (y in 0 until height) {
                image.setRGB(x, y, if (bitMatrix.get(x, y)) Color.BLACK.rgb else Color.WHITE.rgb)
            }
        }
        
        return image.toComposeImageBitmap()
    }

    private fun toByteArray(image: BufferedImage): ByteArray {
        val baos = java.io.ByteArrayOutputStream()
        javax.imageio.ImageIO.write(image, "png", baos)
        return baos.toByteArray()
    }
}
