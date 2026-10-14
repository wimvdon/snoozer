package io.github.impca201.snoozer.tasker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.impca201.snoozer.R
import io.github.impca201.snoozer.tasker.TaskerContract.Command
import io.github.impca201.snoozer.ui.theme.SnoozerAppTheme

/**
 * Configuration screen Tasker opens when the user adds or edits the Snoozer plug-in action.
 * The chosen command is returned in the Locale bundle; the blurb is what Tasker shows in the task.
 */
class TaskerEditActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val previous =
            try {
                TaskerContract.commandFrom(intent?.getBundleExtra(TaskerContract.EXTRA_BUNDLE))
            } catch (_: RuntimeException) {
                null
            }

        setContent {
            SnoozerAppTheme {
                TaskerEditScreen(
                    initial = previous ?: Command.SNOOZE,
                    onSave = ::saveAndFinish,
                    onCancel = ::finish,
                )
            }
        }
    }

    private fun saveAndFinish(command: Command) {
        val result =
            Intent()
                .putExtra(TaskerContract.EXTRA_BUNDLE, TaskerContract.bundleFor(command))
                .putExtra(TaskerContract.EXTRA_STRING_BLURB, getString(command.label))
        setResult(RESULT_OK, result)
        finish()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskerEditScreen(
    initial: Command,
    onSave: (Command) -> Unit,
    onCancel: () -> Unit,
) {
    var selected by rememberSaveable { mutableStateOf(initial) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tasker_edit_title)) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(painterResource(R.drawable.ic_close), contentDescription = stringResource(R.string.action_cancel))
                    }
                },
            )
        },
        bottomBar = {
            Button(
                onClick = { onSave(selected) },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp),
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.tasker_edit_description),
                style = MaterialTheme.typography.bodyLarge,
            )
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Column(Modifier.selectableGroup().padding(vertical = 8.dp)) {
                    Command.entries.forEach { command ->
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = command == selected,
                                        onClick = { selected = command },
                                        role = Role.RadioButton,
                                    ).padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            RadioButton(selected = command == selected, onClick = null)
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(command.label), style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = stringResource(command.description),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
