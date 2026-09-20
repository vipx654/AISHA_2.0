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

/** LOCKED §3/§7 Relationship: current stage, milestones, gradual progression. */
@Composable
fun RelationshipScreen(nav: NavController) {
    AppScaffold(title = "Bond", onBack = { nav.popBackStack() }) {
        Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("Stage: Companion → Warmer → Romantic → Deep bond", style = MaterialTheme.typography.bodyMedium)
            Text("Milestones timeline will render here", style = MaterialTheme.typography.bodySmall)
        }
    }
}
