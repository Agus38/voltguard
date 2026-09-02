package com.voltguard.app.data

import android.content.Context
import android.content.Intent
import android.os.BatteryManager

/**
 * Turns the system battery broadcast + public BatteryManager reads into a normalized
 * [PowerSnapshot]. Defensive: every optional read degrades to null instead of crashing.
 *
 * Note: Android only exposes a small public surface for battery/charger telemetry.
 * Fields with no public constant (input/charge current, max currents, VIN) are read
 * best-effort from the underlying Intent extras by their well-known string keys, which
 * are populated by the framework on most devices.
 */
object PowerCollector {

    // Public BatteryManager property ids.
    private const val PROP_CURRENT_NOW = BatteryManager.BATTERY_PROPERTY_CURRENT_NOW
    private const val PROP_CHARGE_COUNTER = BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER
    private const val PROP_CAPACITY = BatteryManager.BATTERY_PROPERTY_CAPACITY

    fun collect(context: Context, intent: Intent): PowerSnapshot {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager

        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1).let { if (it in 0..100) it else 0 }
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        val present = intent.getBooleanExtra(BatteryManager.EXTRA_PRESENT, true)
        val tech = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "—"
        val tempC = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10.0
        val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0).toFloat()
        val health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)

        // Public BatteryManager reads.
        val chargeNow = safeInt(bm, PROP_CURRENT_NOW)?.toFloat() ?: 0f
        val chargeCounter = safeInt(bm, PROP_CHARGE_COUNTER)?.toFloat() ?: 0f
        val cycleCount = intentInt(intent, BatteryManager.EXTRA_CYCLE_COUNT)
        val capacity = safeInt(bm, PROP_CAPACITY)?.takeIf { it in 0..100 }

        // Best-effort reads from framework extras (uA) by their string keys.
        val inputCurrent = intentInt(intent, "input_current")?.toFloat()
        val outputCurrent = intentInt(intent, "output_current")?.toFloat()
        val chargeCurrent = intentInt(intent, "charge_current")?.toFloat() ?: chargeNow.takeIf { it != 0f }
        val maxChargeCurrent = intentInt(intent, "max_charging_current")?.toFloat()
        val maxInputCurrent = intentInt(intent, "max_charging_current")?.toFloat()
        val maxDischargeCurrent = intentInt(intent, "max_discharging_current")?.toFloat()

        val vinVoltage = readOemVin(intent)
        val maxVinVoltage = readOemVinMax(intent)
        val minVinVoltage = readOemVinMin(intent)

        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING
        val isDischarging = status == BatteryManager.BATTERY_STATUS_DISCHARGING
        val isUnpluggedFull = plugged == 0 && status == BatteryManager.BATTERY_STATUS_FULL

        val v = voltage
        val power = if (isCharging && v > 0f && (chargeCurrent ?: 0f) != 0f) (v * (chargeCurrent ?: 0f)) / 1e9 else null
        val chargePower = if (v > 0f && chargeCurrent != null && chargeCurrent != 0f) (v * chargeCurrent) / 1e9 else null

        return PowerSnapshot(
            timestamp = System.currentTimeMillis(),
            level = level,
            plugged = plugged,
            status = status,
            present = present,
            tech = tech,
            temperature = tempC,
            voltage = v,
            vinVoltage = vinVoltage?.takeIf { it > 0f },
            maxVinVoltage = maxVinVoltage?.takeIf { it > 0f },
            minVinVoltage = minVinVoltage?.takeIf { it > 0f },
            chargeNow = chargeNow,
            chargeCounter = chargeCounter,
            inputCurrent = inputCurrent,
            outputCurrent = outputCurrent,
            chargeCurrent = chargeCurrent,
            maxChargeCurrent = maxChargeCurrent,
            maxInputCurrent = maxInputCurrent,
            maxDischargeCurrent = maxDischargeCurrent,
            power = power,
            chargePower = chargePower,
            health = health,
            cycleCount = cycleCount,
            capacity = capacity,
            pluggedTypeText = pluggedText(plugged),
            statusText = statusText(status),
            isCharging = isCharging,
            isDischarging = isDischarging,
            isUnpluggedFull = isUnpluggedFull,
        )
    }

    private fun safeInt(bm: BatteryManager?, prop: Int): Int? = try {
        bm?.getIntProperty(prop)
    } catch (t: Throwable) { null }

    private fun intentInt(intent: Intent, key: String): Int? = try {
        val e = intent.extras ?: return null
        when (val raw = e.get(key)) {
            is Int -> raw
            is Number -> raw.toInt()
            is String -> raw.toIntOrNull()
            else -> null
        }
    } catch (t: Throwable) { null }

    /**
     * Best-effort read of the adapter *input* voltage. Stock Android does not expose VIN as a
     * public extra, but several OEM ROMs do. If none are present we return null and the UI
     * falls back to cell voltage. Values normalized to millivolts.
     */
    private fun readOemVin(intent: Intent) = intentMv(intent, arrayOf(
        "vin_voltage", "vin_mv", "input_voltage", "charger_voltage", "battery_input_voltage",
    ))
    private fun readOemVinMax(intent: Intent) = intentMv(intent, arrayOf("vin_voltage_max", "max_vin_voltage"))
    private fun readOemVinMin(intent: Intent) = intentMv(intent, arrayOf("vin_voltage_min", "min_vin_voltage"))

    private fun intentMv(intent: Intent, keys: Array<String>): Float? {
        for (k in keys) {
            val v = try {
                when (val raw = intent.extras?.get(k)) {
                    is Number -> raw.toFloat()
                    is String -> raw.toFloatOrNull()
                    else -> null
                }
            } catch (t: Throwable) { null }
            if (v != null && v > 0f && v < 30_000f) return v
        }
        return null
    }

    private fun pluggedText(plugged: Int): String = when (plugged) {
        BatteryManager.BATTERY_PLUGGED_AC -> "AC · Adaptor"
        BatteryManager.BATTERY_PLUGGED_USB -> "USB"
        BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Nirkabel"
        else -> "Tidak tersambung"
    }

    private fun statusText(status: Int): String = when (status) {
        BatteryManager.BATTERY_STATUS_CHARGING -> "Sedang mengisi"
        BatteryManager.BATTERY_STATUS_FULL -> "Baterai penuh"
        BatteryManager.BATTERY_STATUS_DISCHARGING -> "Sedang terpakai"
        BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Tidak mengisi"
        else -> "Tidak diketahui"
    }
}

/** Read the current sticky battery state without needing a live receiver. */
fun Context.currentBatteryIntent(): Intent =
    registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        ?: android.content.Intent()
