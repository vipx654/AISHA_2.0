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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.aisha.app.ui.DayLogViewModel
import com.aisha.app.ui.common.AppScaffold

/** LOCKED §3 Day Log Viewer: date navigation, daily history, context, delete. */
@Composable
fun DayLogScreen(nav: NavController) {
    val vm: DayLogViewModel = viewModel()
    val state by vm.state.collectAsState()
    LaunchedEffect(Unit) { vm.refresh() }

    AppScaffold(title = "Day Logs", onBack = { nav.popBackStack() }) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall) }

            if (state.selected == null) {
                if (state.days.isEmpty()) {
                    Text("No finalized days yet. AISHA finalizes each day at midnight (encrypted at rest).",
                        style = MaterialTheme.typography.bodyMedium)
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.days) { d ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text(d.dayId, style = MaterialTheme.typography.titleMedium)
                                Text(d.summary, style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2)
                                Text("${d.storedBytes} messages stored encrypted",
                                    style = MaterialTheme.typography.labelSmall)
                                OutlinedButton(onClick = { vm.select(d.dayId) }) { Text("Open") }
                            }
                        }
                    }
                }
            } else {
                val d = state.selected!!
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        Card { Column(Modifier.padding(12.dp)) {
                            Text("DAY ${d.dayId}", style = MaterialTheme.typography.titleMedium)
                            Text(d.summary ?: "(no summary)", style = MaterialTheme.typography.bodySmall)
                        } }
                    }
                    items(d.events) { e ->
                        Card { Column(Modifier.padding(12.dp)) {
                            Text("[${e.importance}] ${e.type}", style = MaterialTheme.typography.labelMedium)
                            Text(e.description, style = MaterialTheme.typography.bodySmall)
                        } }
                    }
                    items(d.conversations) { c ->
                        Text("${c.role.name.lowercase()}: ${c.text}", style = MaterialTheme.typography.bodySmall)
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { vm.select(d.dayId); vm.refresh() }) { Text("Close") }
                            Button(onClick = { vm.deleteSelected() }) { Text("Delete (to protected trash)") }
                        }
                    }
                }
            }
        }
    }
}
