package com.voltguard.app.data

import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import com.voltguard.app.data.PowerFormatters.HealthState

/**
 * Turns the system battery broadcast + BatteryManager properties into a normalized
 * [PowerSnapshot]. Pure-ish, stateless, and defensive: every read is guarded so a
 * missing/unsupported field degrades to null instead of crashing.
 */
object PowerCollector {

    fun collect(context: Context, intent: Intent): PowerSnapshot {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager

        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1).coerceIn(0, 100)
        val levelPct = if (intent.hasExtra(BatteryManager.EXTRA_LEVEL)) level
        else intent.getIntExtra(BatteryManager.EXTRA_RAW_CAPACITY, -1).let {
            if (it in 0..100) it else level
        }

        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        val present = intent.getBooleanExtra(BatteryManager.EXTRA_PRESENT, true)
        val tech = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "—"
        val tempC = (intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10.0)
        val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0).toFloat()
        val health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)

        val chargeCounter = intent.getIntExtra(BatteryManager.EXTRA_CHARGE_COUNTER, 0).toFloat()
        val chargeNow = safeInt(bm, BatteryManager.BATTERY_PROPERTY_CURRENT_NOW).toFloat()
        val inputCurrent = safeInt(bm, BatteryManager.BATTERY_PROPERTY_INPUT_CURRENT)?.let { it.toFloat() }
            ?: intent.getExtraOrNull(BatteryManager.EXTRA_INPUT_CURRENT)?.toFloat()
        val outputCurrent = intent.getExtraOrNull(BatteryManager.EXTRA_OUTPUT_CURRENT)?.toFloat()
        val chargeCurrent = intent.getExtraOrNull(BatteryManager.EXTRA_CHARGE_CURRENT)?.toFloat()
            ?: chargeNow.takeIf { it != 0f }
        val maxChargeCurrent = safeInt(bm, BatteryManager.BATTERY_PROPERTY_MAX_CHARGING_CURRENT)?.toFloat()
            ?: intent.getExtraOrNull(BatteryManager.EXTRA_MAX_CHARGING_CURRENT)?.toFloat()
        val maxDischargeCurrent = safeInt(bm, BatteryManager.BATTERY_PROPERTY_MAX_DISCHARGING_CURRENT)?.toFloat()
        val maxInputCurrent = safeInt(bm, BatteryManager.BATTERY_PROPERTY_MAX_INPUT_CURRENT)?.toFloat()
            ?: intent.getExtraOrNull(BatteryManager.EXTRA_MAX_CHARGING_CURRENT)?.toFloat()

        val capacity = intent.getIntExtra(BatteryManager.EXTRA_CAPACITY, -1).takeIf { it in 0..100 }
        val cycleCount = safeInt(bm, BatteryManager.BATTERY_PROPERTY_CHARGE_CYCLE)

        val vinVoltage = readOemVin(intent, bm)
        val maxVinVoltage = readOemVinMax(intent, bm)
        val minVinVoltage = readOemVinMin(intent, bm)

        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING
        val isDischarging = status == BatteryManager.BATTERY_STATUS_DISCHARGING
        val isUnpluggedFull = plugged == 0 && status == BatteryManager.BATTERY_STATUS_FULL

        val v = voltage
        val power = when {
            isCharging && v > 0f && (chargeCurrent ?: 0f) != 0f ->
                (v * (chargeCurrent ?: 0f)) / 1e9   // mV * uA / 1e9 = W
            else -> null
        }
        val chargePower = when {
            v > 0f && chargeCurrent != null && chargeCurrent != 0f -> (v * chargeCurrent) / 1e9
            else -> null
        }

        return PowerSnapshot(
            timestamp = System.currentTimeMillis(),
            level = levelPct,
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
    } catch (t: Throwable) {
        null
    }

    /**
     * Best-effort read of the *adapter input* voltage. Stock Android does not expose
     * VIN as a public extra, but several OEM ROMs do. We probe a handful of documented
     * hidden keys; if none are present we return null and the UI falls back to cell voltage.
     * Values are normalized to millivolts.
     */
    private fun readOemVin(intent: Intent, bm: BatteryManager?): Float? =
        firstPlausibleMv(intent, arrayOf(
            "vin_voltage", "vin_mv", "input_voltage", "charger_voltage",
            "battery_plugged_voltage", "ac_voltage", "battery_input_voltage",
        ))
    private fun readOemVinMax(intent: Intent, bm: BatteryManager?): Float? =
        firstPlausibleMv(intent, arrayOf("vin_voltage_max", "max_vin_voltage"))
    private fun readOemVinMin(intent: Intent, bm: BatteryManager?): Float? =
        firstPlausibleMv(intent, arrayOf("vin_voltage_min", "min_vin_voltage"))

    private fun firstPlausibleMv(intent: Intent, keys: Array<String>): Float? {
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

    private fun Intent.getExtraOrNull(key: String): Int? = try {
        if (extras != null && extras!![key] is Int) (extras!![key] as? Int) else null
    } catch (t: Throwable) { null }

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
