package io.github.impca201.snoozer.ui

import androidx.activity.compose.BackHandler
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
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.impca201.snoozer.service.NotificationAccess
import io.github.impca201.snoozer.snoozerApp
import io.github.impca201.snoozer.ui.guide.AccessGuideScreen
import io.github.impca201.snoozer.ui.guide.ShortcutsGuideScreen
import io.github.impca201.snoozer.ui.guide.TaskerGuideScreen
import io.github.impca201.snoozer.ui.home.HomeScreen
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
    val accessGranted = rememberNotificationAccessGranted()

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
                    toastEnabled = toastEnabled,
                    onSnoozeChange = { enabled -> app.appScope.launch { repository.setSnoozeEnabled(enabled) } },
                    onToastEnabledChange = { enabled -> app.appScope.launch { repository.setToastEnabled(enabled) } },
                    onOpenAccessGuide = { push(Destination.AccessGuide) },
                    onOpenShortcutsGuide = { push(Destination.ShortcutsGuide) },
                    onOpenTaskerGuide = { push(Destination.TaskerGuide) },
                )
            }

            Destination.AccessGuide -> {
                AccessGuideScreen(accessGranted = accessGranted, onBack = ::pop)
            }

            Destination.ShortcutsGuide -> {
                ShortcutsGuideScreen(onBack = ::pop)
            }

            Destination.TaskerGuide -> {
                TaskerGuideScreen(onBack = ::pop)
            }
        }
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
