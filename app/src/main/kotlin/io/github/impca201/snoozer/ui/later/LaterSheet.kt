package io.github.impca201.snoozer.ui.later

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.impca201.snoozer.R
import io.github.impca201.snoozer.data.NotificationSnapshot
import io.github.impca201.snoozer.later.LaterWidget
import io.github.impca201.snoozer.ui.theme.AppShapes
import kotlinx.coroutines.launch

/** Bottom sheet listing the notifications in the shade; tapping one saves it for later. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaterSheet(
    hasAccess: Boolean,
    snoozing: Boolean,
    candidates: List<NotificationSnapshot>,
    onPick: (NotificationSnapshot) -> Unit,
    onAllowAccess: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
            Text(
                text = stringResource(R.string.later_sheet_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            when {
                !hasAccess ->
                    SheetMessage(
                        icon = R.drawable.ic_notifications_off,
                        title = stringResource(R.string.status_no_access_title),
                        body = stringResource(R.string.later_sheet_no_access),
                        action = stringResource(R.string.status_no_access_action),
                        onAction = onAllowAccess,
                    )
                candidates.isEmpty() ->
                    SheetMessage(
                        icon = R.drawable.ic_notifications,
                        title = stringResource(R.string.later_sheet_empty),
                        body = if (snoozing) stringResource(R.string.later_sheet_empty_snoozing) else null,
                    )
                else ->
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        items(candidates, key = { it.key }) { notification ->
                            CandidateRow(notification) {
                                scope.launch { sheetState.hide() }.invokeOnCompletion { onPick(notification) }
                            }
                        }
                    }
            }
        }
    }
}

@Composable
private fun CandidateRow(
    notification: NotificationSnapshot,
    onClick: () -> Unit,
) {
    val headline = notification.title.ifBlank { notification.text }
    val supporting = notification.text.takeIf { notification.title.isNotBlank() && it.isNotBlank() }
    ListItem(
        modifier = Modifier.clip(AppShapes.Item).clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        overlineContent = { Text(notification.appLabel, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        headlineContent = { Text(headline, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = supporting?.let { { Text(it, maxLines = 2, overflow = TextOverflow.Ellipsis) } },
        trailingContent = { Text(LaterWidget.postedTime(notification.postedAt).toString()) },
    )
}

/** Empty and setup states, laid out like the shared EmptyState (DESIGN.md). */
@Composable
private fun SheetMessage(
    @DrawableRes icon: Int,
    title: String,
    body: String?,
    action: String? = null,
    onAction: () -> Unit = {},
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(72.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 360.dp),
        )
        if (body != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 360.dp),
            )
        }
        if (action != null) {
            Spacer(Modifier.height(24.dp))
            Button(onClick = onAction) { Text(action) }
        }
    }
}
