package com.kapcode.open.macropad.kmps.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
    isBlinking: Boolean = true,
    showText: Boolean = true
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        BlinkingCursor(isBlinking = isBlinking)
        if (showText) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "INITIALIZING",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 4.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun BlinkingCursor(isBlinking: Boolean = true) {
    val minAlpha = 20f / 255f
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val animatedAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = minAlpha,
        animationSpec = infiniteRepeatable(
            animation = tween(250, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )

    val alpha = if (isBlinking) animatedAlpha else 1f

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = ">",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .width(16.dp)
                .height(28.dp)
                .alpha(alpha)
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}

@Composable
fun ThreeDotsLoading(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    
    @Composable
    fun Dot(delayMillis: Int) {
        val scale by infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, delayMillis = delayMillis, easing = LinearOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "dotScale"
        )
        Box(
            modifier = Modifier
                .size(10.dp)
                .alpha(scale)
                .background(color = MaterialTheme.colorScheme.primary, shape = CircleShape)
        )
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Dot(0)
        Dot(200)
        Dot(400)
    }
}
