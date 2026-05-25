package switchdektoptocompose.ui.pairing

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import switchdektoptocompose.model.ClientInfo

@Composable
fun QrGridBox(
    requests: List<ClientInfo>,
    bitmaps: Map<String, ImageBitmap>,
    rows: Int,
    cols: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = Color.White,
        shadowElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(rows) { r ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(cols) { c ->
                        val index = r * cols + c
                        QrItem(requests.getOrNull(index), bitmaps)
                    }
                }
            }
        }
    }
}

@Composable
fun QrItem(request: ClientInfo?, bitmaps: Map<String, ImageBitmap>) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        QrImage(request?.let { bitmaps[it.id] })
        Box(modifier = Modifier.width(150.dp).height(24.dp), contentAlignment = Alignment.Center) {
            if (request != null) {
                Text(
                    text = request.name.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    fontWeight = FontWeight.Black,
                    color = Color.Black,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun QrImage(bitmap: ImageBitmap?) {
    Box(
        modifier = Modifier
            .size(150.dp)
            .background(Color.White, MaterialTheme.shapes.small)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "QR Code",
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 3.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "SYNCING QR...",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
