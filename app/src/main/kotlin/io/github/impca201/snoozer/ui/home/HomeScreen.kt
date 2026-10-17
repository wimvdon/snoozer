package io.github.impca201.snoozer.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.impca201.snoozer.R
import io.github.impca201.snoozer.ui.theme.SnoozerTheme
import io.github.impca201.snoozer.ui.theme.rememberReducedMotion

/** Slow cross-fade into and out of the snoozing look. */
private const val GLOW_FADE_MS = 1_600

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    snoozed: Boolean,
    accessGranted: Boolean,
    onSnoozeChange: (Boolean) -> Unit,
    onOpenAccessGuide: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val active = snoozed && accessGranted
    val animate = !rememberReducedMotion()
    val colors = MaterialTheme.colorScheme
    val background by animateColorAsState(
        targetValue = if (active) lerp(colors.surface, colors.primaryContainer, 0.35f) else colors.surface,
        animationSpec = tween(GLOW_FADE_MS),
        label = "home-background",
    )

    var boxOrigin by remember { mutableStateOf(Offset.Zero) }
    var toggleCenter by remember { mutableStateOf<Offset?>(null) }

    Scaffold(
        containerColor = background,
        // contentColorFor() can't match the blended background and falls back to black, which is
        // unreadable in dark mode, so set the content color explicitly.
        contentColor = colors.onSurface,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(painterResource(R.drawable.ic_settings), contentDescription = stringResource(R.string.menu_settings))
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .onGloballyPositioned { boxOrigin = it.positionInRoot() },
        ) {
            AnimatedVisibility(
                visible = active,
                modifier = Modifier.matchParentSize(),
                enter = fadeIn(tween(GLOW_FADE_MS)),
                exit = fadeOut(tween(GLOW_FADE_MS)),
            ) {
                SnoozeGlow(
                    primary = colors.primary,
                    secondary = colors.tertiary,
                    center = toggleCenter?.minus(boxOrigin),
                    animate = animate,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            BoxWithConstraints(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                Column(
                    modifier =
                        Modifier
                            .verticalScroll(rememberScrollState())
                            .fillMaxWidth()
                            .heightIn(min = maxHeight)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.weight(1f))
                    SnoozeToggle(
                        snoozed = active,
                        enabled = accessGranted,
                        animate = animate,
                        onSnoozeChange = onSnoozeChange,
                        modifier =
                            Modifier
                                .padding(24.dp)
                                .onGloballyPositioned { toggleCenter = it.boundsInRoot().center },
                    )
                    StatusText(snoozed = active, accessGranted = accessGranted)
                    // Setup needed: the step that makes the toggle work, right under the message
                    // that asks for it (DESIGN.md, "Empty and setup states").
                    AnimatedVisibility(
                        visible = !accessGranted,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        Button(onClick = onOpenAccessGuide, modifier = Modifier.padding(top = 16.dp)) {
                            Text(stringResource(R.string.status_no_access_action))
                        }
                    }
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StatusText(
    snoozed: Boolean,
    accessGranted: Boolean,
) {
    val (title, body) =
        when {
            !accessGranted -> R.string.status_no_access_title to R.string.status_no_access_body
            snoozed -> R.string.status_snoozed_title to R.string.status_snoozed_body
            else -> R.string.status_active_title to R.string.status_active_body
        }
    Column(
        modifier = Modifier.widthIn(max = 360.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    SnoozerTheme {
        HomeScreen(
            snoozed = true,
            accessGranted = true,
            onSnoozeChange = {},
            onOpenAccessGuide = {},
            onOpenSettings = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenNoAccessPreview() {
    SnoozerTheme {
        HomeScreen(
            snoozed = false,
            accessGranted = false,
            onSnoozeChange = {},
            onOpenAccessGuide = {},
            onOpenSettings = {},
        )
    }
}
