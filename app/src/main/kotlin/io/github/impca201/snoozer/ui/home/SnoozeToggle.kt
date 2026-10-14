package io.github.impca201.snoozer.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.impca201.snoozer.R
import kotlin.math.roundToInt

val SnoozeToggleSize: Dp = 232.dp

private const val ROTATION_PERIOD_MS = 60_000
private const val BREATH_HALF_CYCLE_MS = 2_600

/**
 * The big snooze switch: a Material 3 Expressive cookie shape that morphs into a slowly turning
 * flower while snoozing. It shows the action a tap performs ("Snooze" / "Unsnooze") and gently
 * "breathes" while snoozing is active.
 */
@Composable
fun SnoozeToggle(
    snoozed: Boolean,
    enabled: Boolean,
    animate: Boolean,
    onSnoozeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val containerColor by animateColorAsState(
        targetValue =
            when {
                !enabled -> colors.onSurface.copy(alpha = 0.10f)
                snoozed -> colors.primary
                else -> colors.surfaceContainerHighest
            },
        label = "toggle-container",
    )
    val contentColor by animateColorAsState(
        targetValue =
            when {
                !enabled -> colors.onSurface.copy(alpha = 0.38f)
                snoozed -> colors.onPrimary
                else -> colors.primary
            },
        label = "toggle-content",
    )

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "toggle-press",
    )
    // Animated values below are read inside graphicsLayer {} (layer phase), not during
    // composition, so the running animations never recompose the toggle's content.
    val breathScale =
        if (animate && snoozed && enabled) {
            rememberInfiniteTransition(label = "breath").animateFloat(
                initialValue = 1f,
                targetValue = 1.03f,
                animationSpec = infiniteRepeatable(tween(BREATH_HALF_CYCLE_MS, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "breath-scale",
            )
        } else {
            null
        }

    // Expressive morph: cookie when awake, flower while snoozing, with a bouncy spring.
    val morphProgress =
        animateFloatAsState(
            targetValue = if (snoozed && enabled) 1f else 0f,
            animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow),
            label = "toggle-morph",
        )
    // While snoozing the shape turns very slowly; when woken it settles on a resting angle.
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(snoozed, enabled, animate) {
        if (snoozed && enabled && animate) {
            while (true) {
                rotation.animateTo(rotation.value + 360f, tween(ROTATION_PERIOD_MS, easing = LinearEasing))
                rotation.snapTo(rotation.value % 360f)
            }
        } else {
            val step = ExpressiveShapes.COOKIE_SYMMETRY_DEGREES
            rotation.animateTo((rotation.value / step).roundToInt() * step, spring(stiffness = Spring.StiffnessVeryLow))
            rotation.snapTo(rotation.value % 360f)
        }
    }
    val stateText = stringResource(if (snoozed) R.string.state_snoozed else R.string.state_not_snoozed)

    Box(
        modifier =
            modifier
                .size(SnoozeToggleSize)
                .graphicsLayer {
                    val scale = pressScale * (breathScale?.value ?: 1f)
                    scaleX = scale
                    scaleY = scale
                    shape = MorphShape(ExpressiveShapes.CookieToFlower, morphProgress.value, rotation.value)
                    clip = true
                    shadowElevation = if (enabled) 10.dp.toPx() else 0f
                }.background(containerColor)
                .toggleable(
                    value = snoozed,
                    interactionSource = interactionSource,
                    indication = ripple(),
                    enabled = enabled,
                    role = Role.Switch,
                    onValueChange = onSnoozeChange,
                ).semantics { stateDescription = stateText },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                painter = painterResource(if (snoozed) R.drawable.ic_notifications_paused else R.drawable.ic_notifications),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(72.dp),
            )
            Text(
                text = stringResource(if (snoozed) R.string.toggle_unsnooze else R.string.toggle_snooze),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                textAlign = TextAlign.Center,
            )
        }
    }
}
