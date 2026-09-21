package com.aisha.app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aisha.app.ui.screens.ChatScreen
import com.aisha.app.ui.screens.DayLogScreen
import com.aisha.app.ui.screens.HomeScreen
import com.aisha.app.ui.screens.MoodScreen
import com.aisha.app.ui.screens.RelationshipScreen
import com.aisha.app.ui.screens.SettingsScreen
import com.aisha.app.ui.screens.TasksScreen
import com.aisha.app.ui.screens.VoiceScreen

/** LOCKED §3 — Application Interface System: 7 screens. */
object Routes {
    const val HOME = "home"
    const val CHAT = "chat"
    const val VOICE = "voice"
    const val DAY_LOGS = "daylogs"
    const val MOOD = "mood"
    const val RELATIONSHIP = "relationship"
    const val SETTINGS = "settings"
    const val TASKS = "tasks"
}

@Composable
fun AishaNavHost() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) { HomeScreen(nav) }
        composable(Routes.CHAT) { ChatScreen(nav) }
        composable(Routes.VOICE) { VoiceScreen(nav) }
        composable(Routes.DAY_LOGS) { DayLogScreen(nav) }
        composable(Routes.MOOD) { MoodScreen(nav) }
        composable(Routes.RELATIONSHIP) { RelationshipScreen(nav) }
        composable(Routes.SETTINGS) { SettingsScreen(nav) }
        composable(Routes.TASKS) { TasksScreen(nav) }
    }
}
