package com.aisha.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aisha.app.AishaApplication
import com.aisha.core.DayLogData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * LOCKED §3/§6 — Day Log Viewer state. Decrypts authorized local records,
 * integrity-checked by the store before render (Master §4 Day Logs workflow).
 */
class DayLogViewModel(app: Application) : AndroidViewModel(app) {

    data class DayPreview(val dayId: String, val summary: String, val storedBytes: Int)

    data class UiState(
        val days: List<DayPreview> = emptyList(),
        val selected: DayLogData? = null,
        val error: String? = null,
    )

    private val container = (app as AishaApplication).container
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    fun refresh() {
        viewModelScope.launch {
            val previews = container.dayLogStore.listIds().mapNotNull { id ->
                try {
                    val day = container.dayLogStore.load(id) ?: return@mapNotNull null
                    DayPreview(id, day.summary ?: "(no summary)", day.conversations.size)
                } catch (e: Exception) {
                    _state.value = _state.value.copy(error = "A day log failed integrity check and was skipped")
                    null
                }
            }
            _state.value = _state.value.copy(days = previews.sortedByDescending { it.dayId })
        }
    }

    fun select(dayId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(selected = try { container.dayLogStore.load(dayId) } catch (e: Exception) { null })
        }
    }

    /** §18 — user-initiated deletion goes through protected trash, never hard delete. */
    fun deleteSelected(reason: String = "user request") {
        val day = _state.value.selected ?: return
        viewModelScope.launch {
            container.trashManager.moveToTrashEncrypted(day.dayId, reason)
            _state.value = _state.value.copy(selected = null)
            refresh()
        }
    }
}
