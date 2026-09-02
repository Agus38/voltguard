package com.voltguard.app.data

import java.util.Locale
import kotlin.math.roundToInt

/**
 * A single, normalized snapshot of the battery + charger/power state.
 * All values are unit-normalized so the UI only has to format, never convert.
 */
data class PowerSnapshot(
    val timestamp: Long,

    // --- Battery core ---
    val level: Int,                       // 0..100
    val plugged: Int,                     // BatteryManager.BATTERY_PLUGGED_*
    val status: Int,                      // BatteryManager.BATTERY_STATUS_*
    val present: Boolean,
    val tech: String,                     // e.g. Li-ion
    val temperature: Double,              // Celsius

    // --- Voltages (in mV) ---
    val voltage: Float = 0f,              // current cell voltage (mV)
    val vinVoltage: Float? = null,        // input/charger voltage (mV), null if unavailable
    val maxVinVoltage: Float? = null,     // max designed input voltage (mV)
    val minVinVoltage: Float? = null,     // min designed input voltage (mV)

    // --- Currents / power ---
    val chargeNow: Float = 0f,            // instantaneous current (uA)
    val chargeCounter: Float = 0f,        // charge counter (uAh)
    val inputCurrent: Float? = null,      // input current (uA), null if unavailable
    val outputCurrent: Float? = null,     // output current (uA), null if unavailable
    val chargeCurrent: Float? = null,     // actual charge current (uA), null if unavailable
    val maxChargeCurrent: Float? = null,  // designed max charge current (uA)
    val maxInputCurrent: Float? = null,   // designed max input current (uA)
    val maxDischargeCurrent: Float? = null, // designed max discharge current (uA)
    val power: Double? = null,            // computed power (W), null if inputs missing
    val chargePower: Double? = null,      // computed charge power (W)

    // --- Health / misc ---
    val health: Int = 0,                  // BatteryManager.BATTERY_HEALTH_*
    val cycleCount: Int? = null,          // charge cycle count (rare)
    val capacity: Int? = null,            // relative max capacity as % (rare)
    val pluggedTypeText: String = "",     // friendly plug type
    val statusText: String = "",          // friendly charge status
    val isCharging: Boolean = false,
    val isDischarging: Boolean = false,
    val isUnpluggedFull: Boolean = false,
)

/** Computed metrics for the headline card. */
data class PowerDerived(
    val charging: Boolean,
    val discharging: Boolean,
    val status: String,
    val statusColorKey: Int,             // 0=ok, 1=warn, 2=alert
    val plugText: String,
    val estimatedTime: Long,             // ms to full / empty (or -1 unknown)
    val powerLabel: String,
)

enum class HealthState(val label: String) {
    GOOD("Baik"),
    WARN("Perlu perhatian"),
    ALERT("Waspada"),
    UNKNOWN("—"),
}

/**
 * Central place that converts raw [PowerSnapshot] fields into display strings and
 * health assessments. Keeping all conversions here makes the UI dumb and the rules testable.
 */
object PowerFormatters {

    const val BASELINE_MV = 4000f       // reference for a healthy Li-ion under load

    fun volt(mv: Float?): String =
        if (mv == null || mv == 0f) "—" else "%.1f".format(Locale.US, mv / 1000f) + " V"

    fun currentA(ua: Float?): String {
        if (ua == null) return "—"
        val a = ua / 1_000_000f
        return if (Math.abs(a) < 1) "%.2f mA".format(Locale.US, a * 1000) else "%.2f A".format(Locale.US, a)
    }

    fun power(w: Double?): String = if (w == null) "—" else "%.2f W".format(Locale.US, w)

    fun temp(c: Double): String = "%.1f°C".format(Locale.US, c)

    fun milliVolt(mv: Float?): String =
        if (mv == null || mv == 0f) "—" else "%.0f mV".format(Locale.US, mv)

    fun percent(v: Int): String = "$v%"

    /**
     * Classify the *input/charger* voltage relative to the device's designed range.
     * Uses VIN if present, otherwise the cell voltage as a proxy.
     */
    fun classifyVoltage(s: PowerSnapshot): HealthState {
        val v = s.vinVoltage?.takeIf { it > 0f } ?: s.voltage.takeIf { it > 0f }
        if (v == null) return HealthState.UNKNOWN
        val max = s.maxVinVoltage?.takeIf { it > 0f }
        val min = s.minVinVoltage?.takeIf { it > 0f }
        if (max != null && min != null) {
            if (v < min * 0.95f) return HealthState.ALERT
            if (v > max * 1.05f) return HealthState.ALERT
            if (v < min * 1.0f || v > max * 0.98f) return HealthState.WARN
            return HealthState.GOOD
        }
        // No design range: sanity band for a Li-ion cell.
        if (v < 3400f) return HealthState.ALERT
        if (v < 3600f) return HealthState.WARN
        return HealthState.GOOD
    }

    fun classifyTemp(t: Double): HealthState = when {
        t >= 42.0 -> HealthState.ALERT
        t >= 37.0 -> HealthState.WARN
        t <= 0.0 -> HealthState.WARN
        else -> HealthState.GOOD
    }

    fun overall(s: PowerSnapshot): HealthState {
        val a = listOf(classifyVoltage(s), classifyTemp(s.temperature))
        return if (a.any { it == HealthState.ALERT }) HealthState.ALERT
        else if (a.any { it == HealthState.WARN }) HealthState.WARN
        else a.maxOrNull() ?: HealthState.UNKNOWN
    }

    fun formatDuration(ms: Long): String {
        if (ms < 0) return "—"
        val totalMin = (ms / 60000.0).roundToInt()
        val h = totalMin / 60
        val m = totalMin % 60
        return if (h > 0) "${h}j ${m}m" else "${m}m"
    }

    fun estimatedTime(s: PowerSnapshot): Long {
        // Use current to project time-to-full or time-to-empty.
        val level = s.level
        val current = s.chargeCurrent ?: s.inputCurrent ?: return -1L
        if (current == 0f) return -1L
        return if (s.isCharging) {
            val remaining = (100 - level).coerceAtLeast(0)
            // 1% of battery ≈ (chargeCounter is unreliable); approximate via current fraction of max.
            val refA = s.maxChargeCurrent?.takeIf { it > 0f } ?: 1_500_000f
            val fullAsec = refA / 100f // micro-amp-seconds for 1%
            val fullSec = (fullAsec / Math.abs(current)).toDouble()
            (fullSec * remaining).toLong().coerceAtLeast(0)
        } else {
            val refA = s.maxDischargeCurrent?.takeIf { it > 0f } ?: 1_500_000f
            val fullAsec = refA / 100f
            val fullSec = (fullAsec / Math.abs(current)).toDouble()
            (fullSec * level).toLong().coerceAtLeast(0)
        }
    }
}
