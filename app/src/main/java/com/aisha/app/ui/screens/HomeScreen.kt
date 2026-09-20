package com.aisha.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.aisha.app.ui.Routes

/**
 * LOCKED §3 Home: 3D/animated avatar, greeting/status, quick actions,
 * ambient non-intrusive presentation. Avatar visual lands in implementation phase.
 */
@Composable
fun HomeScreen(nav: NavController) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text("AISHA", style = MaterialTheme.typography.displayMedium)
        Text("Greeting / status will appear here", style = MaterialTheme.typography.bodyMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)) {
            Button(onClick = { nav.navigate(Routes.CHAT) }) { Text("Chat") }
            Button(onClick = { nav.navigate(Routes.VOICE) }) { Text("Voice") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)) {
            OutlinedButton(onClick = { nav.navigate(Routes.MOOD) }) { Text("Mood") }
            OutlinedButton(onClick = { nav.navigate(Routes.RELATIONSHIP) }) { Text("Bond") }
            OutlinedButton(onClick = { nav.navigate(Routes.DAY_LOGS) }) { Text("Logs") }
        }
        OutlinedButton(onClick = { nav.navigate(Routes.SETTINGS) }) { Text("Settings") }
    }
}
