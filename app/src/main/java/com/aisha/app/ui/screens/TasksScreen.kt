package com.aisha.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.aisha.app.ui.TasksViewModel
import com.aisha.app.ui.common.AppScaffold
import java.time.LocalDateTime

/** Master §9 — Tasks/Planner: create (validated), complete, delete; reminders auto-scheduled. */
@Composable
fun TasksScreen(nav: NavController) {
    val vm: TasksViewModel = viewModel()
    val state by vm.state.collectAsState()
    var draft by remember { mutableStateOf("") }

    AppScaffold(title = "Tasks", onBack = { nav.popBackStack() }) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall) }
            OutlinedTextField(value = draft, onValueChange = { draft = it },
                modifier = Modifier.fillMaxWidth(), placeholder = { Text("New task…") })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { vm.create(draft, null); draft = "" }, enabled = draft.isNotBlank()) { Text("Add") }
                OutlinedButton(onClick = { vm.create(draft, LocalDateTime.now().plusMinutes(30)); draft = "" },
                    enabled = draft.isNotBlank()) { Text("+30 min") }
                OutlinedButton(onClick = { vm.create(draft, LocalDateTime.now().plusHours(1)); draft = "" },
                    enabled = draft.isNotBlank()) { Text("+1 hour") }
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.pending) { t ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = false, onCheckedChange = { vm.complete(t.id) })
                            Column(Modifier.weight(1f)) {
                                Text(t.title, style = MaterialTheme.typography.bodyMedium)
                                Text(t.dueAt?.let { "reminds ${it.toLocalTime()}" } ?: "no reminder",
                                    style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(onClick = { vm.delete(t.id) }) { Text("✕") }
                        }
                    }
                }
                items(state.done) { t ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = true, onCheckedChange = null)
                            Text(t.title, style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
