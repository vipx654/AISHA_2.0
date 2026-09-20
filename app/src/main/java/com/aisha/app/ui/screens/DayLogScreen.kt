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

/** LOCKED §3/§6 Day Log Viewer: calendar navigation, daily history, mood/bond/task context, export, delete. */
@Composable
fun DayLogScreen(nav: NavController) {
    AppScaffold(title = "Day Logs", onBack = { nav.popBackStack() }) {
        Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("Calendar + day detail will render here", style = MaterialTheme.typography.bodyMedium)
            Text("Finalized logs are encrypted at rest (§6)", style = MaterialTheme.typography.bodySmall)
        }
    }
}
