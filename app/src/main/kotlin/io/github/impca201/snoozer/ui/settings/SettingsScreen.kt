package io.github.impca201.snoozer.ui.settings

import android.app.StatusBarManager
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import io.github.impca201.snoozer.R
import io.github.impca201.snoozer.bedtime.BedtimeMonitor
import io.github.impca201.snoozer.data.Appearance
import io.github.impca201.snoozer.data.ThemeMode
import io.github.impca201.snoozer.tile.SnoozeTileService
import io.github.impca201.snoozer.ui.theme.AppearanceSettings
import io.github.impca201.snoozer.ui.theme.ExpandableSetting
import io.github.impca201.snoozer.ui.theme.SettingItem
import io.github.impca201.snoozer.ui.theme.SettingsBlock
import io.github.impca201.snoozer.ui.theme.SettingsFooter
import io.github.impca201.snoozer.ui.theme.SettingsScaffold
import io.github.impca201.snoozer.ui.theme.SettingsSection
import io.github.impca201.snoozer.ui.theme.SwitchSetting
import io.github.impca201.snoozer.ui.theme.rememberAppVersion
import io.github.impca201.snoozer.ui.guide.ButtonIconSpacing
import io.github.impca201.snoozer.ui.guide.NumberedSteps
import android.graphics.drawable.Icon as PlatformIcon

private const val TASKER_PACKAGE = "net.dinglisch.android.taskerm"

/** Everything that can be configured or set up, as one list of sections. */
@Composable
fun SettingsScreen(
    state: SettingsState,
    actions: SettingsActions,
    onBack: () -> Unit,
) {
    SettingsScaffold(
        title = stringResource(R.string.menu_settings),
        backLabel = stringResource(R.string.action_back),
        onBack = onBack,
    ) {
        item(key = "snoozing") { SnoozingSection(state, actions) }
        item(key = "mode") { ModeSection(state, actions) }
        item(key = "bedtime") { BedtimeSection(state, actions) }
        item(key = "shortcuts") { ShortcutsSection() }
        item(key = "tasker") { TaskerSection() }
        item(key = "appearance") { AppearanceSection(state, actions) }
        item(key = "footer") {
            SettingsFooter("${stringResource(R.string.app_name)} ${rememberAppVersion()}")
        }
    }
}

data class SettingsState(
    val toastEnabled: Boolean,
    val indicatorEnabled: Boolean,
    val indicatorBlocked: Boolean,
    val modeSupported: Boolean,
    val modeEnabled: Boolean,
    val modeBlocked: Boolean,
    val modeName: String,
    val followBedtime: Boolean,
    val bedtimeReady: Boolean,
    val bedtimeStatus: BedtimeMonitor.Status?,
    val appearance: Appearance,
)

class SettingsActions(
    val onToastEnabledChange: (Boolean) -> Unit,
    val onIndicatorEnabledChange: (Boolean) -> Unit,
    val onModeEnabledChange: (Boolean) -> Unit,
    val onModeNameSave: (String) -> Unit,
    val onOpenModeSettings: () -> Unit,
    val onFollowBedtimeChange: (Boolean) -> Unit,
    val onBedtimeCheckNow: () -> Unit,
    val onThemeModeChange: (ThemeMode) -> Unit,
    val onDynamicColorChange: (Boolean) -> Unit,
)

@Composable
private fun AppearanceSection(
    state: SettingsState,
    actions: SettingsActions,
) {
    SettingsSection(title = stringResource(R.string.section_appearance)) {
        AppearanceSettings(
            themeLabel = stringResource(R.string.setting_theme_title),
            themeOptions = ThemeMode.entries.map { stringResource(it.labelRes()) },
            selectedTheme = state.appearance.themeMode.ordinal,
            onThemeSelect = { actions.onThemeModeChange(ThemeMode.entries[it]) },
            dynamicColorTitle = stringResource(R.string.setting_dynamic_color_title),
            dynamicColorSummary = stringResource(R.string.setting_dynamic_color_summary),
            dynamicColor = state.appearance.dynamicColor,
            onDynamicColorChange = actions.onDynamicColorChange,
            dynamicColorSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
        )
    }
}

@StringRes
private fun ThemeMode.labelRes(): Int =
    when (this) {
        ThemeMode.System -> R.string.theme_system
        ThemeMode.Light -> R.string.theme_light
        ThemeMode.Dark -> R.string.theme_dark
    }

/** What Snoozer shows while it is snoozing. */
@Composable
private fun SnoozingSection(
    state: SettingsState,
    actions: SettingsActions,
) {
    SettingsSection(title = stringResource(R.string.section_snoozing)) {
        SwitchSetting(
            title = stringResource(R.string.setting_toast_title),
            summary = stringResource(R.string.setting_toast_summary),
            checked = state.toastEnabled,
            onCheckedChange = actions.onToastEnabledChange,
        )
        SwitchSetting(
            title = stringResource(R.string.setting_indicator_title),
            summary = stringResource(R.string.setting_indicator_summary),
            checked = state.indicatorEnabled,
            onCheckedChange = actions.onIndicatorEnabledChange,
            warning = if (state.indicatorBlocked) stringResource(R.string.setting_indicator_blocked) else null,
            onWarningClick = { actions.onIndicatorEnabledChange(true) },
        )
    }
}

@Composable
private fun ModeSection(
    state: SettingsState,
    actions: SettingsActions,
) {
    val appName = stringResource(R.string.app_name)
    val displayName = state.modeName.ifBlank { appName }
    SettingsSection(title = stringResource(R.string.section_mode)) {
        SwitchSetting(
            title = stringResource(R.string.setting_mode_title),
            summary =
                stringResource(
                    if (state.modeSupported) R.string.setting_mode_summary else R.string.setting_mode_unsupported,
                ),
            checked = state.modeSupported && state.modeEnabled,
            enabled = state.modeSupported,
            onCheckedChange = actions.onModeEnabledChange,
            warning = if (state.modeSupported && state.modeBlocked) stringResource(R.string.setting_mode_blocked) else null,
            onWarningClick = { actions.onModeEnabledChange(true) },
        )
        if (state.modeSupported) {
            SettingsBlock {
                ModeNameField(current = displayName, onSave = actions.onModeNameSave)
                SectionText(stringResource(R.string.setting_mode_details))
            }
            if (state.modeEnabled && !state.modeBlocked) {
                SettingItem(
                    title = stringResource(R.string.setting_mode_open),
                    summary = null,
                    onClick = actions.onOpenModeSettings,
                    trailing = { OpensElsewhereIcon() },
                )
            }
        }
    }
}

@Composable
private fun ModeNameField(
    current: String,
    onSave: (String) -> Unit,
) {
    var text by rememberSaveable(current) { mutableStateOf(current) }
    val changed = text.isNotBlank() && text.trim() != current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it.take(MAX_MODE_NAME_LENGTH) },
            label = { Text(stringResource(R.string.setting_mode_name_label)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (changed) onSave(text.trim()) }),
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = { onSave(text.trim()) }, enabled = changed) {
            Text(stringResource(R.string.action_save))
        }
    }
}

private const val MAX_MODE_NAME_LENGTH = 40

@Composable
private fun BedtimeSection(
    state: SettingsState,
    actions: SettingsActions,
) {
    val context = LocalContext.current
    val commands = BedtimeMonitor.adbCommands(context.packageName).joinToString("\n")
    SettingsSection(title = stringResource(R.string.section_bedtime)) {
        SettingsBlock { InfoBanner(stringResource(R.string.bedtime_experimental)) }
        SwitchSetting(
            title = stringResource(R.string.setting_bedtime_title),
            summary = stringResource(R.string.setting_bedtime_summary),
            checked = state.followBedtime,
            onCheckedChange = actions.onFollowBedtimeChange,
            // Needs the adb permissions first; switching off always stays possible.
            enabled = state.bedtimeReady || state.followBedtime,
        )
        SettingsBlock {
            state.bedtimeStatus?.let { status ->
                Text(
                    text = stringResource(bedtimeStatusText(status)),
                    style = MaterialTheme.typography.bodyMedium,
                    color =
                        when (status) {
                            BedtimeMonitor.Status.NeedsSetup, BedtimeMonitor.Status.Unreadable -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.primary
                        },
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            OutlinedButton(onClick = actions.onBedtimeCheckNow, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.bedtime_check_now))
            }
        }
        ExpandableSetting(
            key = "bedtime-setup",
            title = stringResource(R.string.bedtime_setup_title),
            initiallyExpanded = !state.bedtimeReady,
        ) {
            NumberedSteps(
                listOf(
                    stringResource(R.string.bedtime_step1),
                    stringResource(R.string.bedtime_step2),
                    stringResource(R.string.bedtime_step3),
                    stringResource(R.string.bedtime_step4),
                    stringResource(R.string.bedtime_step5),
                ),
            )
            SelectionContainer {
                Text(
                    text = commands,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.shapes.medium)
                            .padding(12.dp),
                )
            }
            FilledTonalButton(onClick = { copyToClipboard(context, commands) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.bedtime_copy))
            }
            SectionText(stringResource(R.string.bedtime_setup))
        }
    }
}

@StringRes
private fun bedtimeStatusText(status: BedtimeMonitor.Status): Int =
    when (status) {
        BedtimeMonitor.Status.NeedsSetup -> R.string.bedtime_status_needs_setup
        BedtimeMonitor.Status.Unreadable -> R.string.bedtime_status_unreadable
        BedtimeMonitor.Status.On -> R.string.bedtime_status_on
        BedtimeMonitor.Status.Off -> R.string.bedtime_status_off
    }

private fun copyToClipboard(
    context: Context,
    text: String,
) {
    context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("adb", text))
}

@Composable
private fun ShortcutsSection() {
    val context = LocalContext.current
    val appName = stringResource(R.string.app_name)
    val tileName = stringResource(R.string.tile_label)
    val shortcutName = stringResource(R.string.shortcut_toggle_short)
    var addResult by rememberSaveable { mutableStateOf<Int?>(null) }

    SettingsSection(title = stringResource(R.string.guide_shortcuts_title)) {
        SettingsBlock { SectionText(stringResource(R.string.guide_shortcuts_intro)) }
        ExpandableSetting(key = "shortcuts-tile", title = stringResource(R.string.guide_tile_title)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                SectionText(stringResource(R.string.guide_tile_quick_add_body))
                Button(
                    onClick = { requestAddTile(context) { addResult = it } },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(painterResource(R.drawable.ic_add), contentDescription = null)
                    Text(
                        text = stringResource(R.string.guide_tile_quick_add_button),
                        modifier = Modifier.padding(start = ButtonIconSpacing),
                    )
                }
                addResult?.let { result ->
                    Text(
                        text = stringResource(tileResultMessage(result)),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
            }
            NumberedSteps(
                listOf(
                    stringResource(R.string.guide_tile_step1),
                    stringResource(R.string.guide_tile_step2),
                    stringResource(R.string.guide_tile_step3, tileName),
                    stringResource(R.string.guide_tile_step4),
                ),
            )
        }
        ExpandableSetting(key = "shortcuts-launcher", title = stringResource(R.string.guide_shortcut_title)) {
            NumberedSteps(
                listOf(
                    stringResource(R.string.guide_shortcut_step1, appName),
                    stringResource(R.string.guide_shortcut_step2, shortcutName),
                    stringResource(R.string.guide_shortcut_step3),
                ),
            )
        }
        ExpandableSetting(key = "shortcuts-gesture", title = stringResource(R.string.guide_gesture_title)) {
            InfoBanner(stringResource(R.string.guide_gesture_pixel_only))
            NumberedSteps(
                listOf(
                    stringResource(R.string.guide_gesture_step1),
                    stringResource(R.string.guide_gesture_step2),
                    stringResource(R.string.guide_gesture_step3, appName, shortcutName),
                    stringResource(R.string.guide_gesture_step4),
                ),
            )
            SectionText(stringResource(R.string.guide_gesture_other))
        }
    }
}

@Composable
private fun TaskerSection() {
    val context = LocalContext.current
    val appName = stringResource(R.string.app_name)
    val taskerLaunchIntent = remember(context) { context.packageManager.getLaunchIntentForPackage(TASKER_PACKAGE) }

    SettingsSection(title = stringResource(R.string.guide_tasker_title)) {
        SettingsBlock {
            SectionText(stringResource(R.string.guide_tasker_intro, appName))
        }
        SettingItem(
            title = stringResource(if (taskerLaunchIntent != null) R.string.guide_tasker_open else R.string.guide_tasker_get),
            summary = null,
            onClick = {
                if (taskerLaunchIntent != null) safeStart(context, taskerLaunchIntent) else openStoreListing(context)
            },
            trailing = { OpensElsewhereIcon() },
        )
        ExpandableSetting(key = "tasker-steps", title = stringResource(R.string.guide_tasker_steps_title)) {
            NumberedSteps(
                listOf(
                    stringResource(R.string.guide_tasker_step1),
                    stringResource(R.string.guide_tasker_step2, appName),
                    stringResource(R.string.guide_tasker_step3),
                    stringResource(R.string.guide_tasker_step4),
                ),
            )
        }
        ExpandableSetting(key = "tasker-other", title = stringResource(R.string.guide_tasker_other_title)) {
            SectionText(stringResource(R.string.guide_tasker_other_body))
        }
    }
}

/** Trailing icon of a row that opens another app or system settings. */
@Composable
private fun OpensElsewhereIcon() {
    Icon(painterResource(R.drawable.ic_open_in_new), contentDescription = null)
}

private fun requestAddTile(
    context: Context,
    onResult: (Int) -> Unit,
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    requestAddTileApi33(context, onResult)
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun requestAddTileApi33(
    context: Context,
    onResult: (Int) -> Unit,
) {
    val statusBarManager = context.getSystemService(StatusBarManager::class.java) ?: return
    try {
        statusBarManager.requestAddTileService(
            ComponentName(context, SnoozeTileService::class.java),
            context.getString(R.string.tile_label),
            PlatformIcon.createWithResource(context, R.drawable.ic_tile),
            ContextCompat.getMainExecutor(context),
        ) { result -> onResult(result) }
    } catch (_: RuntimeException) {
        // Some launchers / OEM System UIs do not support the request.
        onResult(TILE_REQUEST_FAILED)
    }
}

/** Local marker for "the request could not even be made"; all platform error codes are > 2. */
private const val TILE_REQUEST_FAILED = -1

@StringRes
private fun tileResultMessage(result: Int): Int =
    when (result) {
        StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED -> R.string.guide_tile_result_added
        StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED -> R.string.guide_tile_result_already
        StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_NOT_ADDED -> R.string.guide_tile_result_not_added
        else -> R.string.guide_tile_result_error
    }

private fun openStoreListing(context: Context) {
    val market = Intent(Intent.ACTION_VIEW, "market://details?id=$TASKER_PACKAGE".toUri())
    val web = Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=$TASKER_PACKAGE".toUri())
    if (!safeStart(context, market)) safeStart(context, web)
}

private fun safeStart(
    context: Context,
    intent: Intent,
): Boolean =
    try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
