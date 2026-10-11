package io.github.impca201.snoozer.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.impca201.snoozer.R
import io.github.impca201.snoozer.ui.components.rememberReducedMotion
import io.github.impca201.snoozer.ui.theme.SnoozerTheme

/** Slow cross-fade into and out of the snoozing look. */
private const val GLOW_FADE_MS = 1_600

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    snoozed: Boolean,
    accessGranted: Boolean,
    toastEnabled: Boolean,
    indicatorEnabled: Boolean,
    indicatorBlocked: Boolean,
    onSnoozeChange: (Boolean) -> Unit,
    onToastEnabledChange: (Boolean) -> Unit,
    onIndicatorEnabledChange: (Boolean) -> Unit,
    onOpenAccessGuide: () -> Unit,
    onOpenShortcutsGuide: () -> Unit,
    onOpenTaskerGuide: () -> Unit,
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
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                actions = {
                    GuidesMenu(onOpenShortcutsGuide = onOpenShortcutsGuide, onOpenTaskerGuide = onOpenTaskerGuide)
                    SettingsMenu(
                        toastEnabled = toastEnabled,
                        indicatorEnabled = indicatorEnabled,
                        indicatorBlocked = indicatorBlocked,
                        onToastEnabledChange = onToastEnabledChange,
                        onIndicatorEnabledChange = onIndicatorEnabledChange,
                    )
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
                    AnimatedVisibility(
                        visible = !accessGranted,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        AccessBanner(onClick = onOpenAccessGuide)
                    }
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

@Composable
private fun AccessBanner(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ),
        modifier =
            Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(painterResource(R.drawable.ic_notifications_off), contentDescription = null)
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.banner_access_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.banner_access_body), style = MaterialTheme.typography.bodyMedium)
            }
            Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null)
        }
    }
}

@Composable
private fun GuidesMenu(
    onOpenShortcutsGuide: () -> Unit,
    onOpenTaskerGuide: () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(painterResource(R.drawable.ic_guide), contentDescription = stringResource(R.string.menu_guides))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            MenuHeader(stringResource(R.string.menu_guides))
            DropdownMenuItem(
                text = { Text(stringResource(R.string.guide_shortcuts_menu)) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_tile_grid), contentDescription = null) },
                onClick = {
                    expanded = false
                    onOpenShortcutsGuide()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.guide_tasker_menu)) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_automation), contentDescription = null) },
                onClick = {
                    expanded = false
                    onOpenTaskerGuide()
                },
            )
        }
    }
}

@Composable
private fun SettingsMenu(
    toastEnabled: Boolean,
    indicatorEnabled: Boolean,
    indicatorBlocked: Boolean,
    onToastEnabledChange: (Boolean) -> Unit,
    onIndicatorEnabledChange: (Boolean) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(painterResource(R.drawable.ic_settings), contentDescription = stringResource(R.string.menu_settings))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            MenuHeader(stringResource(R.string.menu_settings))
            SwitchMenuItem(
                title = stringResource(R.string.setting_toast_title),
                summary = stringResource(R.string.setting_toast_summary),
                iconRes = R.drawable.ic_toast,
                checked = toastEnabled,
                onCheckedChange = onToastEnabledChange,
            )
            SwitchMenuItem(
                title = stringResource(R.string.setting_indicator_title),
                summary =
                    stringResource(
                        if (indicatorBlocked) R.string.setting_indicator_blocked else R.string.setting_indicator_summary,
                    ),
                summaryIsWarning = indicatorBlocked,
                iconRes = R.drawable.ic_notifications_paused,
                checked = indicatorEnabled,
                onCheckedChange = onIndicatorEnabledChange,
                // Tapping a blocked, enabled item asks for the permission again.
                onClickOverride = if (indicatorBlocked) ({ onIndicatorEnabledChange(true) }) else null,
            )
        }
    }
}

@Composable
private fun SwitchMenuItem(
    title: String,
    summary: String,
    iconRes: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    summaryIsWarning: Boolean = false,
    onClickOverride: (() -> Unit)? = null,
) {
    val stateText = stringResource(if (checked) R.string.toast_setting_on else R.string.toast_setting_off)
    DropdownMenuItem(
        text = {
            Column(Modifier.widthIn(max = 220.dp)) {
                Text(title)
                Text(
                    summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (summaryIsWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        leadingIcon = { Icon(painterResource(iconRes), contentDescription = null) },
        trailingIcon = { Switch(checked = checked, onCheckedChange = null) },
        onClick = onClickOverride ?: { onCheckedChange(!checked) },
        modifier =
            Modifier.semantics {
                role = Role.Switch
                stateDescription = stateText
            },
    )
}

@Composable
private fun MenuHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier =
            Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .semantics { heading() },
    )
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    SnoozerTheme(dynamicColor = false) {
        HomeScreen(
            snoozed = true,
            accessGranted = true,
            toastEnabled = false,
            indicatorEnabled = true,
            indicatorBlocked = false,
            onSnoozeChange = {},
            onToastEnabledChange = {},
            onIndicatorEnabledChange = {},
            onOpenAccessGuide = {},
            onOpenShortcutsGuide = {},
            onOpenTaskerGuide = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenNoAccessPreview() {
    SnoozerTheme(dynamicColor = false) {
        HomeScreen(
            snoozed = false,
            accessGranted = false,
            toastEnabled = true,
            indicatorEnabled = true,
            indicatorBlocked = true,
            onSnoozeChange = {},
            onToastEnabledChange = {},
            onIndicatorEnabledChange = {},
            onOpenAccessGuide = {},
            onOpenShortcutsGuide = {},
            onOpenTaskerGuide = {},
        )
    }
}
