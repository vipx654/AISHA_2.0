package com.aisha.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aisha.app.AishaApplication
import com.aisha.app.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** MVVM shell over the Core Engine pipeline (spec §3 Chat, §5 pipeline). */
class ChatViewModel(app: Application) : AndroidViewModel(app) {

    private val container: AppContainer = (app as AishaApplication).container

    data class UiState(
        val messages: List<Bubble> = emptyList(),
        val busy: Boolean = false,
        val offline: Boolean = false,
    ) {
        data class Bubble(val fromUser: Boolean, val text: String, val degraded: Boolean = false)
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    init {
        // §9 Presence Engine — greeting from real state; absence derived from bookkeeping only
        val presence = com.aisha.core.PresenceEngine()
        val action = presence.onAppOpen(container.clock.now(), container.core.lastActiveDayOrNull())
        val text = (action as? com.aisha.core.PresenceAction.Greeting)?.text ?: "I'm here."
        _state.value = _state.value.copy(messages = listOf(Bubble(false, text)))
    }

    fun send(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _state.value.busy) return
        append(Bubble(true, trimmed))
        _state.value = _state.value.copy(busy = true)
        viewModelScope.launch {
            val result = container.core.handleUserTurn(trimmed)
            append(Bubble(false, result.text, result.degraded))
            _state.value = _state.value.copy(busy = false)
        }
    }

    private fun append(bubble: UiState.Bubble) {
        _state.value = _state.value.copy(messages = _state.value.messages + bubble)
    }
}

fun greetingFor(now: java.time.LocalDateTime): String = when (now.hour) {
    in 5..11 -> "Good morning ☀️"
    in 12..16 -> "Hey, good afternoon"
    in 17..21 -> "Good evening 🌙"
    else -> "Still up? 🌙"
}
