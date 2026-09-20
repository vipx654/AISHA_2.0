package com.aisha.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.aisha.app.ui.common.AppScaffold

/** LOCKED §3 Settings: Hindi/English, privacy, voice, theme, backup/cloud, notifications, avatar/display, data controls. */
@Composable
fun SettingsScreen(nav: NavController) {
    AppScaffold(title = "Settings", onBack = { nav.popBackStack() }) {
        Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("Language · Privacy · Voice · Theme · Backup · Notifications · Avatar · Data controls", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
