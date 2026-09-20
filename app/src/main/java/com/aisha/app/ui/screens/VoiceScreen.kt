package com.aisha.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.aisha.app.ui.common.AppScaffold

/** LOCKED §3/§13 Voice: live conversation, speech controls, state feedback, lip-sync/expression. */
@Composable
fun VoiceScreen(nav: NavController) {
    AppScaffold(title = "Voice", onBack = { nav.popBackStack() }) {
        Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically)) {
            Text("🎙 Avatar + waveform will render here", style = MaterialTheme.typography.bodyLarge)
            Button(onClick = { /* start/stop listening — implementation phase */ }) { Text("Start listening") }
        }
    }
}
