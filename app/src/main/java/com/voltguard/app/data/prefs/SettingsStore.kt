package com.voltguard.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "voltguard")

/**
 * User-configurable monitoring settings (alert thresholds, persistence cadence).
 * Defaults tuned for a typical 5 V USB-C phone charger (~4.4–5.2 V VIN).
 */
data class MonitorSettings(
    val lowVinMv: Float = 4_600f,
    val highVinMv: Float = 5_400f,
    val lowVinAlertMv: Float = 4_200f,
    val highVinAlertMv: Float = 5_800f,
    val highTempWarnC: Float = 37f,
    val highTempAlertC: Float = 42f,
    val alertEnabled: Boolean = true,
    val soundEnabled: Boolean = false,
    val persistSeconds: Int = 10,
)

private val KEY_LOW = floatPreferencesKey("low_vin_mv")
private val KEY_HIGH = floatPreferencesKey("high_vin_mv")
private val KEY_LOW_ALERT = floatPreferencesKey("low_vin_alert_mv")
private val KEY_HIGH_ALERT = floatPreferencesKey("high_vin_alert_mv")
private val KEY_TEMP_WARN = floatPreferencesKey("temp_warn_c")
private val KEY_TEMP_ALERT = floatPreferencesKey("temp_alert_c")
private val KEY_ALERT = booleanPreferencesKey("alert_enabled")
private val KEY_SOUND = booleanPreferencesKey("sound_enabled")
private val KEY_PERSIST = intPreferencesKey("persist_seconds")

private fun Preferences.asSettings(): MonitorSettings {
    val def = MonitorSettings()
    return MonitorSettings(
        lowVinMv = this[KEY_LOW] ?: def.lowVinMv,
        highVinMv = this[KEY_HIGH] ?: def.highVinMv,
        lowVinAlertMv = this[KEY_LOW_ALERT] ?: def.lowVinAlertMv,
        highVinAlertMv = this[KEY_HIGH_ALERT] ?: def.highVinAlertMv,
        highTempWarnC = this[KEY_TEMP_WARN] ?: def.highTempWarnC,
        highTempAlertC = this[KEY_TEMP_ALERT] ?: def.highTempAlertC,
        alertEnabled = this[KEY_ALERT] ?: def.alertEnabled,
        soundEnabled = this[KEY_SOUND] ?: def.soundEnabled,
        persistSeconds = this[KEY_PERSIST] ?: def.persistSeconds,
    )
}

class SettingsStore(private val context: Context) {

    val settings: Flow<MonitorSettings> = context.dataStore.data.map { it.asSettings() }

    suspend fun update(transform: (MonitorSettings) -> MonitorSettings) {
        context.dataStore.edit { p ->
            val next = transform(p.asSettings())
            p[KEY_LOW] = next.lowVinMv
            p[KEY_HIGH] = next.highVinMv
            p[KEY_LOW_ALERT] = next.lowVinAlertMv
            p[KEY_HIGH_ALERT] = next.highVinAlertMv
            p[KEY_TEMP_WARN] = next.highTempWarnC
            p[KEY_TEMP_ALERT] = next.highTempAlertC
            p[KEY_ALERT] = next.alertEnabled
            p[KEY_SOUND] = next.soundEnabled
            p[KEY_PERSIST] = next.persistSeconds
        }
    }
}
