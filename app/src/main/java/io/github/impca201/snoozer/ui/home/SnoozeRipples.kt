package io.github.impca201.snoozer.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private const val RING_COUNT = 4
private const val CYCLE_MS = 6_000

/**
 * Slow, calm rings that drift outwards from the toggle, like ripples on still water at night.
 * With animations disabled system-wide, a static set of rings is drawn instead.
 */
@Composable
fun SnoozeRipples(
    color: Color,
    center: Offset?,
    innerRadius: Dp,
    animate: Boolean,
    modifier: Modifier = Modifier,
) {
    val progress =
        if (animate) {
            val transition = rememberInfiniteTransition(label = "ripples")
            transition
                .animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(CYCLE_MS, easing = LinearEasing)),
                    label = "ripple-progress",
                ).value
        } else {
            0.15f
        }

    Canvas(modifier) {
        val origin = center ?: this.center
        val minRadius = innerRadius.toPx()
        val maxRadius = maxOf(size.width, size.height) * 0.8f
        drawGlow(color, origin, minRadius)
        repeat(RING_COUNT) { index ->
            val phase = (progress + index.toFloat() / RING_COUNT) % 1f
            // Ease-out so rings slow down as they spread, which reads as "calming".
            val eased = 1f - (1f - phase) * (1f - phase)
            val radius = lerp(minRadius, maxRadius, eased)
            val alpha = (1f - phase) * 0.45f
            drawCircle(
                color = color.copy(alpha = alpha),
                radius = radius,
                center = origin,
                style = Stroke(width = lerp(6.dp.toPx(), 1.dp.toPx(), phase)),
            )
        }
    }
}

private fun DrawScope.drawGlow(
    color: Color,
    origin: Offset,
    minRadius: Float,
) {
    val glowRadius = minRadius * 2.2f
    drawCircle(
        brush =
            Brush.radialGradient(
                colors = listOf(color.copy(alpha = 0.28f), Color.Transparent),
                center = origin,
                radius = glowRadius,
            ),
        radius = glowRadius,
        center = origin,
    )
}

private fun lerp(
    start: Float,
    stop: Float,
    fraction: Float,
): Float = start + (stop - start) * fraction
