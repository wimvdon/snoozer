package io.github.impca201.snoozer.ui.guide

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.core.content.ContextCompat
import io.github.impca201.snoozer.R
import io.github.impca201.snoozer.tile.SnoozeTileService
import android.graphics.drawable.Icon as PlatformIcon

@Composable
fun TileGuideScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var addResult by rememberSaveable { mutableStateOf<Int?>(null) }
    val tileName = stringResource(R.string.tile_label)
    val shortcutName = stringResource(R.string.shortcut_toggle_short)

    GuideScaffold(title = stringResource(R.string.guide_tile_title), onBack = onBack) {
        item(key = "intro") {
            GuideCallout(
                text = stringResource(R.string.guide_tile_intro),
                iconRes = R.drawable.ic_tile_grid,
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            item(key = "quick-add") {
                GuideCallout(
                    title = stringResource(R.string.guide_tile_quick_add_title),
                    text = stringResource(R.string.guide_tile_quick_add_body),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actions = {
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
                    },
                )
            }
        }
        item(key = "manual") {
            GuideSection(
                title = stringResource(R.string.guide_tile_manual_title),
                steps =
                    listOf(
                        stringResource(R.string.guide_tile_step1),
                        stringResource(R.string.guide_tile_step2),
                        stringResource(R.string.guide_tile_step3, tileName),
                        stringResource(R.string.guide_tile_step4),
                    ),
            )
        }
        item(key = "shortcut") {
            GuideSection(
                title = stringResource(R.string.guide_shortcut_title),
                steps =
                    listOf(
                        stringResource(R.string.guide_shortcut_step1, stringResource(R.string.app_name)),
                        stringResource(R.string.guide_shortcut_step2, shortcutName),
                        stringResource(R.string.guide_shortcut_step3),
                    ),
            )
        }
    }
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
