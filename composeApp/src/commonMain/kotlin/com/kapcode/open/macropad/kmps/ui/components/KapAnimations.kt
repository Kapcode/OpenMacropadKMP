package com.kapcode.`open`.macropad.kmps.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kapcode.`open`.macropad.kmps.*
import org.jetbrains.compose.resources.painterResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Global state for Kap animations.
 */
class KapAnimationManager {
    var balancePosition by mutableStateOf(Offset.Zero)
    val flyEvents = mutableStateListOf<FlyEvent>()

    fun triggerDeduction(targetPos: Offset) {
        if (balancePosition != Offset.Zero && targetPos != Offset.Zero) {
            flyEvents.add(FlyEvent(
                id = Random.nextLong(),
                startPos = balancePosition,
                endPos = targetPos,
                type = AnimationType.DEDUCTION
            ))
        }
    }

    fun triggerGraceSkip(targetPos: Offset) {
        if (balancePosition != Offset.Zero && targetPos != Offset.Zero) {
            flyEvents.add(FlyEvent(
                id = Random.nextLong(),
                startPos = balancePosition,
                endPos = targetPos,
                type = AnimationType.GRACE_SKIP
            ))
        }
    }

    fun triggerAward(startPos: Offset = Offset(500f, 1000f)) { // Default to center-bottom if unknown
        if (balancePosition != Offset.Zero) {
            // Award +100 text
            flyEvents.add(FlyEvent(
                id = Random.nextLong(),
                startPos = startPos,
                endPos = balancePosition,
                type = AnimationType.AWARD_TEXT
            ))
            
            // Multiple Kaps spread
            repeat(10) { i ->
                flyEvents.add(FlyEvent(
                    id = Random.nextLong(),
                    startPos = startPos,
                    endPos = balancePosition,
                    type = AnimationType.AWARD_KAP,
                    delay = i * 50
                ))
            }
        }
    }

    fun triggerFlight(startPos: Offset, endPos: Offset) {
        if (startPos != Offset.Zero && endPos != Offset.Zero) {
            flyEvents.add(FlyEvent(
                id = Random.nextLong(),
                startPos = startPos,
                endPos = endPos,
                type = AnimationType.DEDUCTION
            ))
        }
    }
}

enum class AnimationType {
    DEDUCTION, AWARD_KAP, AWARD_TEXT, GRACE_SKIP
}

data class FlyEvent(
    val id: Long,
    val startPos: Offset,
    val endPos: Offset,
    val type: AnimationType,
    val delay: Int = 0
)

val LocalKapAnimationManager = staticCompositionLocalOf { KapAnimationManager() }

@Composable
fun KapAnimationOverlay() {
    val manager = LocalKapAnimationManager.current

    Box(modifier = Modifier.fillMaxSize()) {
        manager.flyEvents.forEach { event ->
            key(event.id) {
                when (event.type) {
                    AnimationType.DEDUCTION -> DeductionAnimation(event) { manager.flyEvents.remove(it) }
                    AnimationType.AWARD_KAP -> AwardKapAnimation(event) { manager.flyEvents.remove(it) }
                    AnimationType.AWARD_TEXT -> AwardTextAnimation(event) { manager.flyEvents.remove(it) }
                    AnimationType.GRACE_SKIP -> GraceSkipAnimation(event) { manager.flyEvents.remove(it) }
                }
            }
        }
    }
}

@Composable
fun DeductionAnimation(event: FlyEvent, onFinish: (FlyEvent) -> Unit) {
    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
        )
        onFinish(event)
    }

    val currentX = lerp(event.startPos.x, event.endPos.x, animProgress.value)
    val currentY = lerp(event.startPos.y, event.endPos.y, animProgress.value)
    val scale = lerp(1.2f, 0.5f, animProgress.value)
    val alpha = lerp(1f, 0f, animProgress.value)

    Icon(
        painter = painterResource(Res.drawable.macropadIcon64),
        contentDescription = null,
        tint = Color.Unspecified,
        modifier = Modifier
            .offset { IntOffset(currentX.roundToInt(), currentY.roundToInt()) }
            .size(24.dp)
            .scale(scale)
            .alpha(alpha)
    )
}

@Composable
fun GraceSkipAnimation(event: FlyEvent, onFinish: (FlyEvent) -> Unit) {
    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000, easing = LinearEasing)
        )
        onFinish(event)
    }

    val t = animProgress.value
    
    // Start and End are the balance (wallet)
    val startX = event.startPos.x
    val startY = event.startPos.y
    
    // Target is the pressed item
    val targetX = event.endPos.x
    val targetY = event.endPos.y
    
    val midX = startX + 1.6f * (targetX - startX)
    val midY = startY + 1.6f * (targetY - startY)

    // Add a tiny bit of "width" to the U-turn by slightly offsetting the control point perpendicular to the path
    val dx = targetX - startX
    val dy = targetY - startY
    val len = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
    val perpX = -dy / len * 50f // 50 pixels offset
    val perpY = dx / len * 50f
    
    val currentX = quadraticBezier(startX, midX + perpX, startX, t)
    val currentY = quadraticBezier(startY, midY + perpY, startY, t)
    
    // Pulse scale at the turn
    val scale = if (t < 0.5f) lerp(1f, 1.4f, t * 2) else lerp(1.4f, 1f, (t - 0.5f) * 2)

    Icon(
        painter = painterResource(Res.drawable.macropadIcon64),
        contentDescription = null,
        tint = Color.Unspecified,
        modifier = Modifier
            .offset { IntOffset(currentX.roundToInt(), currentY.roundToInt()) }
            .size(24.dp)
            .scale(scale)
    )
}

@Composable
fun AwardKapAnimation(event: FlyEvent, onFinish: (FlyEvent) -> Unit) {
    val animProgress = remember { Animatable(0f) }
    val spreadX = remember { Random.nextFloat() * 400f - 200f }
    val spreadY = remember { Random.nextFloat() * 400f - 200f }

    LaunchedEffect(Unit) {
        delay(event.delay.toLong())
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
        onFinish(event)
    }

    // Arch path: Start -> Spread Point -> End
    val t = animProgress.value
    val midX = event.startPos.x + spreadX
    val midY = event.startPos.y + spreadY
    
    val currentX = quadraticBezier(event.startPos.x, midX, event.endPos.x, t)
    val currentY = quadraticBezier(event.startPos.y, midY, event.endPos.y, t)
    val scale = if (t < 0.5f) lerp(0f, 1.5f, t * 2) else lerp(1.5f, 0.5f, (t - 0.5f) * 2)

    Icon(
        painter = painterResource(Res.drawable.macropadIcon64),
        contentDescription = null,
        tint = Color.Unspecified,
        modifier = Modifier
            .offset { IntOffset(currentX.roundToInt(), currentY.roundToInt()) }
            .size(24.dp)
            .scale(scale)
    )
}

@Composable
fun AwardTextAnimation(event: FlyEvent, onFinish: (FlyEvent) -> Unit) {
    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000, easing = LinearOutSlowInEasing)
        )
        onFinish(event)
    }

    val currentX = event.startPos.x
    val currentY = lerp(event.startPos.y, event.startPos.y - 300f, animProgress.value)
    val alpha = if (animProgress.value < 0.8f) 1f else lerp(1f, 0f, (animProgress.value - 0.8f) * 5)

    Text(
        text = "+100",
        color = Color(0xFFFFD700),
        fontSize = 32.sp,
        fontWeight = FontWeight.Black,
        modifier = Modifier
            .offset { IntOffset(currentX.roundToInt(), currentY.roundToInt()) }
            .alpha(alpha)
    )
}

private fun lerp(start: Float, end: Float, fraction: Float): Float {
    return start + (end - start) * fraction
}

private fun quadraticBezier(p0: Float, p1: Float, p2: Float, t: Float): Float {
    return (1 - t) * (1 - t) * p0 + 2 * (1 - t) * t * p1 + t * t * p2
}

/**
 * Modifier to track the position of the balance icon.
 */
fun Modifier.trackBalancePosition(manager: KapAnimationManager): Modifier = this.onGloballyPositioned { coords ->
    manager.balancePosition = coords.positionInRoot()
}

/**
 * Modifier to track the position of a macro button.
 */
fun Modifier.trackWidgetPosition(onPositioned: (Offset) -> Unit): Modifier = this.onGloballyPositioned { coords ->
    onPositioned(coords.positionInRoot())
}
