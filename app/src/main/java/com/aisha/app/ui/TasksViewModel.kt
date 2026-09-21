package com.aisha.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aisha.app.AishaApplication
import com.aisha.core.AishaTask
import com.aisha.core.TaskValidationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Master §9 Daily Planner surface — user stays in control of all changes. */
class TasksViewModel(app: Application) : AndroidViewModel(app) {

    data class UiState(
        val pending: List<AishaTask> = emptyList(),
        val done: List<AishaTask> = emptyList(),
        val error: String? = null,
    )

    private val container = (app as AishaApplication).container
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            val all = container.taskEngine.all()
            _state.value = UiState(pending = all.filter { !it.completed }, done = all.filter { it.completed })
        }
    }

    fun create(title: String, dueAt: java.time.LocalDateTime?) {
        viewModelScope.launch {
            try {
                container.taskEngine.create(title, dueAt)
                _state.value = _state.value.copy(error = null)
                refresh()
            } catch (e: TaskValidationException) {
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }

    fun complete(id: String) { viewModelScope.launch { container.taskEngine.complete(id); refresh() } }
    fun delete(id: String) { viewModelScope.launch { container.taskEngine.delete(id); refresh() } }
}
