package io.github.impca201.snoozer.ui.guide

import android.os.Build
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import io.github.impca201.snoozer.R
import io.github.impca201.snoozer.service.NotificationAccess

@Composable
fun AccessGuideScreen(
    accessGranted: Boolean,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val steps =
        listOf(
            stringResource(R.string.guide_access_step1),
            stringResource(R.string.guide_access_step2, stringResource(R.string.app_name)),
            stringResource(R.string.guide_access_step3),
            stringResource(R.string.guide_access_step4),
        )

    GuideScaffold(title = stringResource(R.string.guide_access_title), onBack = onBack) {
        item(key = "status") {
            if (accessGranted) {
                GuideCallout(
                    title = stringResource(R.string.guide_access_granted_title),
                    text = stringResource(R.string.guide_access_granted_body),
                    iconRes = R.drawable.ic_check_circle,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actions = {
                        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.guide_access_back_home))
                        }
                    },
                )
            } else {
                GuideCallout(
                    title = stringResource(R.string.guide_access_why_title),
                    text = stringResource(R.string.guide_access_why_body),
                    iconRes = R.drawable.ic_notifications_off,
                )
            }
        }
        item(key = "steps") {
            GuideSection(
                title = stringResource(R.string.guide_how_title),
                steps = steps,
                footer = {
                    Button(
                        onClick = { NotificationAccess.openSettings(context) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(painterResource(R.drawable.ic_open_in_new), contentDescription = null)
                        Text(
                            text = stringResource(R.string.guide_access_open_settings),
                            modifier = Modifier.padding(start = ButtonIconSpacing),
                        )
                    }
                },
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            item(key = "restricted") {
                GuideCallout(
                    title = stringResource(R.string.guide_access_restricted_title),
                    text = stringResource(R.string.guide_access_restricted_body),
                    iconRes = R.drawable.ic_info,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    actions = {
                        OutlinedButton(
                            onClick = { NotificationAccess.openAppInfo(context) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.guide_access_open_app_info))
                        }
                    },
                )
            }
        }
        item(key = "privacy") {
            GuideText(
                title = stringResource(R.string.guide_access_privacy_title),
                text = stringResource(R.string.guide_access_privacy_body),
            )
        }
    }
}
