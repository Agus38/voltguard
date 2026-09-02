package com.voltguard.app.data

import android.content.Context
import com.voltguard.app.data.db.AppDatabase
import com.voltguard.app.data.db.SampleEntity
import com.voltguard.app.data.prefs.MonitorSettings
import com.voltguard.app.data.prefs.SettingsStore
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Single source of truth for power telemetry.
 *
 * - [snapshot]  : the latest [PowerSnapshot], updated every [sampleIntervalMs].
 * - [settings]  : user monitoring configuration (thresholds etc.).
 * - [alerts]    : raised [AlertEvent]s (banner + notifications react to this).
 * - [history]   : persisted samples from Room (survives restarts).
 *
 * Only ONE owner should drive [start] at a time (the foreground service). The UI
 * observes the exposed flows; it may call [collectNow] once for instant first paint.
 */
class PowerRepository(
    private val app: Context,
    private val store: SettingsStore,
    private val db: AppDatabase,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val samplerMutex = Mutex()
    @Volatile private var sampler: Job? = null

    private val _snapshot = MutableStateFlow(PowerSnapshot(timestamp = 0L, level = 0, plugged = 0,
        status = 0, present = true, tech = "—", temperature = 0.0, voltage = 0f))
    val snapshot: StateFlow<PowerSnapshot> = _snapshot.asStateFlow()

    val settings: StateFlow<MonitorSettings>

    private val _alert = MutableSharedFlow<AlertEvent>(extraBufferCapacity = 1)
    val alerts: SharedFlow<AlertEvent> = _alert.asSharedFlow()

    private val _lastAlert = MutableStateFlow<AlertEvent?>(null)
    val lastAlert: StateFlow<AlertEvent?> = _lastAlert.asStateFlow()

    private val dao = db.sampleDao()
    val history = MutableStateFlow<List<SampleEntity>>(emptyList())

    private var lastPersist = 0L
    private var lastAlertAt = 0L
    private val sampleIntervalMs = 1_000L
    private val persistIntervalMs = 10_000L

    init {
        // Seed settings into a StateFlow so the rest of the app observes a single flow.
        settings = MutableStateFlow(MonitorSettings())
        scope.launch {
            store.settings.collect { settings.value = it }
        }
        scope.launch {
            dao.recent(300).let { history.value = it }
        }
        // Opportunistically prune old rows (keep ~7 days).
        scope.launch {
            dao.deleteOlderThan(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7))
        }
    }

    /** Kick the sampling loop (idempotent). Call from the service. */
    fun start() {
        samplerMutex.withLock {
            if (sampler?.isActive == true) return
            sampler = scope.launch {
                while (true) {
                    sampleAndEmit(persist = true)
                    delay(sampleIntervalMs)
                }
            }
        }
    }

    fun stop() {
        samplerMutex.withLock {
            sampler?.cancel()
            sampler = null
        }
    }

    /** Do a single read now (for instant UI paint / button press). */
    fun collectNow() = scope.launch { sampleAndEmit(persist = false) }

    private suspend fun sampleAndEmit(persist: Boolean) {
        try {
            val intent = app.currentBatteryIntent()
            val snap = PowerCollector.collect(app, intent)
            _snapshot.value = snap

            if (persist) {
                val now = System.currentTimeMillis()
                if (now - lastPersist >= persistIntervalMs) {
                    lastPersist = now
                    dao.insert(
                        SampleEntity(
                            ts = now,
                            level = snap.level,
                            status = snap.status,
                            plugged = snap.plugged,
                            temperature = snap.temperature.toFloat(),
                            voltage = snap.voltage,
                            vinVoltage = snap.vinVoltage,
                            inputCurrent = snap.inputCurrent,
                            chargeCurrent = snap.chargeCurrent,
                            power = snap.power,
                            isCharging = snap.isCharging,
                        )
                    )
                    val recent = dao.recent(300)
                    history.value = recent
                    // Keep the table bounded.
                    if (recent.size >= 300) dao.deleteOlderThan(recent.lastOrNull()?.ts ?: 0L)
                }
            }

            evaluateAlerts(snap)
        } catch (t: Throwable) {
            // Never let a sampling hiccup kill the loop.
        }
    }

    private fun evaluateAlerts(s: PowerSnapshot) {
        if (!settings.value.alertEnabled) return
        val cfg = settings.value
        val vin = s.vinVoltage?.takeIf { it > 0f } ?: s.voltage.takeIf { it > 0f }

        var level: AlertLevel? = null
        var title = ""
        var msg = ""

        if (vin != null) {
            when {
                vin < cfg.lowVinAlertMv -> { level = AlertLevel.ALERT; title = "Tegangan input sangat rendah"; msg = "${PowerFormatters.volt(vin)} — jauh di bawah normal." }
                vin > cfg.highVinAlertMv -> { level = AlertLevel.ALERT; title = "Tegangan input sangat tinggi"; msg = "${PowerFormatters.volt(vin)} — risiko kerusakan." }
                vin < cfg.lowVinMv -> { level = AlertLevel.WARN; title = "Tegangan input rendah"; msg = "${PowerFormatters.volt(vin)}." }
                vin > cfg.highVinMv -> { level = AlertLevel.WARN; title = "Tegangan input tinggi"; msg = "${PowerFormatters.volt(vin)}." }
            }
        }
        val t = PowerFormatters.classifyTemp(s.temperature)
        if (t == HealthState.ALERT && level != AlertLevel.ALERT) {
            level = AlertLevel.ALERT; title = "Suhu baterai panas"; msg = "${PowerFormatters.temp(s.temperature)}."
        } else if (t == HealthState.WARN && level == null) {
            level = AlertLevel.WARN; title = "Suhu baterai tinggi"; msg = "${PowerFormatters.temp(s.temperature)}."
        }

        level?.let { lv ->
            val now = System.currentTimeMillis()
            // Rate-limit: re-announce at most once per 60s.
            if (now - lastAlertAt > 60_000L) {
                lastAlertAt = now
                val ev = AlertEvent(lv, title, msg, now)
                _lastAlert.value = ev
                _alert.tryEmit(ev)
            } else {
                _lastAlert.value = AlertEvent(lv, title, msg, now)
            }
        } ?: run {
            // Clear when healthy again.
            if (_lastAlert.value != null) _lastAlert.value = null
        }
    }

    suspend fun refreshHistory() {
        history.value = dao.recent(300)
    }

    /** Apply a change to the user's monitoring settings. */
    fun updateSettings(transform: (MonitorSettings) -> MonitorSettings) {
        scope.launch { store.update(transform) }
    }

    /** Remove all persisted samples. */
    fun clearHistory() {
        scope.launch {
            dao.clear()
            history.value = emptyList()
        }
    }
}
