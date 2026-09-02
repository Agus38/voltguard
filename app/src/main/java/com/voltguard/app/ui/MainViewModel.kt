package com.voltguard.app.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.voltguard.app.VoltGuardApp
import com.voltguard.app.data.AlertEvent
import com.voltguard.app.data.PowerFormatters
import com.voltguard.app.data.HealthState
import com.voltguard.app.data.PowerSnapshot
import com.voltguard.app.data.db.SampleEntity
import com.voltguard.app.data.prefs.MonitorSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A UI-ready derived state derived from the raw snapshot. */
data class UiPower(
    val snap: PowerSnapshot,
    val displayMv: Float,
    val displayVinMv: Float?,
    val health: HealthState,
    val hasOemVin: Boolean,
    val estimatedMs: Long,
)

class MainViewModel(app: VoltGuardApp) : ViewModel() {

    private val repo = app.repository

    val snapshot: StateFlow<PowerSnapshot> = repo.snapshot
    val history: StateFlow<List<SampleEntity>> = repo.history
    val settings: StateFlow<MonitorSettings> = repo.settings
    val lastAlert: StateFlow<AlertEvent?> = repo.lastAlert

    val ui: StateFlow<UiPower> = repo.snapshot.map { s ->
        val health = PowerFormatters.overall(s)
        UiPower(
            snap = s,
            displayMv = (s.voltage).coerceAtLeast(0f),
            displayVinMv = s.vinVoltage?.takeIf { it > 0f },
            health = health,
            hasOemVin = s.vinVoltage != null && s.vinVoltage > 0f,
            estimatedMs = PowerFormatters.estimatedTime(s),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = UiPower(
            snap = s_blank,
            displayMv = 0f,
            displayVinMv = null,
            health = HealthState.UNKNOWN,
            hasOemVin = false,
            estimatedMs = -1L,
        ),
    )

    fun collectNow() = repo.collectNow()
    fun refreshHistory() { viewModelScope.launch { repo.refreshHistory() } }
    fun clearHistory() { repo.clearHistory() }

    fun updateSettings(transform: (MonitorSettings) -> MonitorSettings) {
        repo.updateSettings(transform)
    }

    companion object {
        private val s_blank = PowerSnapshot(
            timestamp = 0L, level = 0, plugged = 0, status = 0,
            present = true, tech = "—", temperature = 0.0, voltage = 0f,
        )
    }
}

class ViewModelFactory(private val app: VoltGuardApp) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            MainViewModel(app) as T
        } else {
            super.create(modelClass)
        }
}
