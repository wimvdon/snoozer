package io.github.impca201.snoozer.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.impca201.snoozer.service.NotificationAccess
import io.github.impca201.snoozer.service.SnoozeIndicator
import io.github.impca201.snoozer.service.SnoozeMode
import io.github.impca201.snoozer.snoozerApp
import io.github.impca201.snoozer.ui.guide.AccessGuideScreen
import io.github.impca201.snoozer.ui.home.HomeScreen
import io.github.impca201.snoozer.ui.settings.SettingsActions
import io.github.impca201.snoozer.ui.settings.SettingsScreen
import io.github.impca201.snoozer.ui.settings.SettingsState
import kotlinx.coroutines.launch

private val BackStackSaver =
    listSaver<SnapshotStateList<Destination>, String>(
        save = { stack -> stack.map { it.name } },
        restore = { names -> names.map(Destination::valueOf).toMutableStateList() },
    )

@Composable
fun SnoozerNavigation(
    requested: Destination?,
    onRequestHandled: () -> Unit,
) {
    val app = LocalContext.current.snoozerApp
    val repository = app.repository
    val snoozed by repository.snoozeEnabled.collectAsStateWithLifecycle()
    val toastEnabled by repository.toastEnabled.collectAsStateWithLifecycle()
    val indicatorEnabled by repository.indicatorEnabled.collectAsStateWithLifecycle()
    val dimEnabled by repository.dimEnabled.collectAsStateWithLifecycle()
    val accessGranted = rememberNotificationAccessGranted()
    val context = LocalContext.current
    var canPostNotifications by remember { mutableStateOf(SnoozeIndicator.canPost(context)) }
    var hasPolicyAccess by remember { mutableStateOf(SnoozeMode.hasAccess(context)) }
    LifecycleResumeEffect(Unit) {
        // Permission or access may have changed in Settings while we were away.
        canPostNotifications = SnoozeIndicator.canPost(context)
        hasPolicyAccess = SnoozeMode.hasAccess(context)
        app.refreshSnoozeCues()
        onPauseOrDispose { }
    }

    // Android 13+ needs the notification permission for the persistent notification.
    var openSettingsIfDenied by remember { mutableStateOf(false) }
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            canPostNotifications = SnoozeIndicator.canPost(context)
            app.refreshSnoozeCues()
            // The system stops showing the dialog after two refusals; then only Settings can help.
            if (!granted && openSettingsIfDenied) openAppNotificationSettings(context)
        }

    fun ensureCanPostNotifications(fromSettingsItem: Boolean) {
        if (SnoozeIndicator.canPost(context)) return
        val permissionMissing =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
        if (permissionMissing) {
            openSettingsIfDenied = fromSettingsItem
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else if (fromSettingsItem) {
            // Permission granted but notifications switched off for the app in Settings.
            openAppNotificationSettings(context)
        }
    }

    val backStack = rememberSaveable(saver = BackStackSaver) { mutableStateListOf(Destination.Home) }

    fun push(destination: Destination) {
        if (backStack.last() != destination) backStack.add(destination)
    }

    fun pop() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }

    LaunchedEffect(requested) {
        if (requested != null) {
            if (requested == Destination.Home) {
                while (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
            } else {
                push(requested)
            }
            onRequestHandled()
        }
    }

    BackHandler(enabled = backStack.size > 1) { pop() }

    AnimatedContent(
        targetState = backStack.last(),
        transitionSpec = {
            val forward = targetState.ordinal > initialState.ordinal
            (slideInHorizontally { width -> if (forward) width / 4 else -width / 4 } + fadeIn())
                .togetherWith(slideOutHorizontally { width -> if (forward) -width / 4 else width / 4 } + fadeOut())
        },
        label = "screen",
    ) { destination ->
        when (destination) {
            Destination.Home -> {
                HomeScreen(
                    snoozed = snoozed == true,
                    accessGranted = accessGranted,
                    onSnoozeChange = { enabled ->
                        app.appScope.launch { repository.setSnoozeEnabled(enabled) }
                        if (enabled && indicatorEnabled) ensureCanPostNotifications(fromSettingsItem = false)
                    },
                    onOpenAccessGuide = { push(Destination.AccessGuide) },
                    onOpenSettings = { push(Destination.Settings) },
                )
            }

            Destination.AccessGuide -> {
                AccessGuideScreen(accessGranted = accessGranted, onBack = ::pop)
            }

            Destination.Settings -> {
                SettingsScreen(
                    state =
                        SettingsState(
                            toastEnabled = toastEnabled,
                            indicatorEnabled = indicatorEnabled,
                            indicatorBlocked = indicatorEnabled && !canPostNotifications,
                            dimSupported = SnoozeMode.isSupported(),
                            dimEnabled = dimEnabled,
                            dimBlocked = dimEnabled && !hasPolicyAccess,
                        ),
                    actions =
                        SettingsActions(
                            onToastEnabledChange = { enabled -> app.appScope.launch { repository.setToastEnabled(enabled) } },
                            onIndicatorEnabledChange = { enabled ->
                                app.appScope.launch { repository.setIndicatorEnabled(enabled) }
                                if (enabled) ensureCanPostNotifications(fromSettingsItem = true)
                            },
                            onDimEnabledChange = { enabled ->
                                app.appScope.launch { repository.setDimEnabled(enabled) }
                                // Normally already granted together with notification access.
                                if (enabled && !SnoozeMode.hasAccess(context)) openPolicyAccessSettings(context)
                            },
                        ),
                    onBack = ::pop,
                )
            }
        }
    }
}

private fun openPolicyAccessSettings(context: Context) {
    try {
        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
    } catch (_: ActivityNotFoundException) {
        NotificationAccess.openAppInfo(context)
    }
}

private fun openAppNotificationSettings(context: Context) {
    val intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        NotificationAccess.openAppInfo(context)
    }
}

/** Re-checked every time the app resumes, e.g. when returning from the system settings. */
@Composable
fun rememberNotificationAccessGranted(): Boolean {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(NotificationAccess.isGranted(context)) }
    LifecycleResumeEffect(Unit) {
        granted = NotificationAccess.isGranted(context)
        onPauseOrDispose { }
    }
    return granted
}
