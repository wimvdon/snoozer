package io.github.impca201.snoozer.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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

val SnoozeToggleSize: Dp = 220.dp

/**
 * The big round snooze switch. Shows the action it will perform ("Snooze" / "Unsnooze") and
 * gently "breathes" while snoozing is active.
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
    val breathScale =
        if (animate && snoozed && enabled) {
            rememberInfiniteTransition(label = "breath")
                .animateFloat(
                    initialValue = 1f,
                    targetValue = 1.035f,
                    animationSpec = infiniteRepeatable(tween(2_400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                    label = "breath-scale",
                ).value
        } else {
            1f
        }

    val stateText = stringResource(if (snoozed) R.string.state_snoozed else R.string.state_not_snoozed)

    Box(
        modifier =
            modifier
                .size(SnoozeToggleSize)
                .graphicsLayer {
                    val scale = pressScale * breathScale
                    scaleX = scale
                    scaleY = scale
                }.shadow(elevation = if (enabled) 10.dp else 0.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(containerColor)
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
