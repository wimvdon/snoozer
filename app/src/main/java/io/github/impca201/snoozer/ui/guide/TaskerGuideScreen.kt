package io.github.impca201.snoozer.ui.guide

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import io.github.impca201.snoozer.R

private const val TASKER_PACKAGE = "net.dinglisch.android.taskerm"

@Composable
fun TaskerGuideScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val taskerLaunchIntent = remember(context) { context.packageManager.getLaunchIntentForPackage(TASKER_PACKAGE) }
    val appName = stringResource(R.string.app_name)

    GuideScaffold(title = stringResource(R.string.guide_tasker_title), onBack = onBack) {
        item(key = "intro") {
            GuideCallout(
                text = stringResource(R.string.guide_tasker_intro, appName),
                iconRes = R.drawable.ic_automation,
                actions = {
                    Button(
                        onClick = {
                            if (taskerLaunchIntent != null) {
                                safeStart(context, taskerLaunchIntent)
                            } else {
                                openStoreListing(context)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(painterResource(R.drawable.ic_open_in_new), contentDescription = null)
                        Text(
                            text =
                                stringResource(
                                    if (taskerLaunchIntent != null) R.string.guide_tasker_open else R.string.guide_tasker_get,
                                ),
                            modifier = Modifier.padding(start = ButtonIconSpacing),
                        )
                    }
                },
            )
        }
        item(key = "steps") {
            GuideSection(
                title = stringResource(R.string.guide_tasker_steps_title),
                steps =
                    listOf(
                        stringResource(R.string.guide_tasker_step1),
                        stringResource(R.string.guide_tasker_step2, appName),
                        stringResource(R.string.guide_tasker_step3),
                        stringResource(R.string.guide_tasker_step4),
                    ),
            )
        }
        item(key = "example") {
            GuideSection(
                title = stringResource(R.string.guide_tasker_example_title),
                steps =
                    listOf(
                        stringResource(R.string.guide_tasker_example_step1),
                        stringResource(R.string.guide_tasker_example_step2, appName),
                        stringResource(R.string.guide_tasker_example_step3, appName),
                    ),
            )
        }
        item(key = "other-apps") {
            GuideCallout(
                title = stringResource(R.string.guide_tasker_other_title),
                text = stringResource(R.string.guide_tasker_other_body),
                iconRes = R.drawable.ic_info,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
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
