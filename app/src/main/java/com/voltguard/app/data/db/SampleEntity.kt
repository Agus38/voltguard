package com.voltguard.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A persisted sample, taken every ~10s by the background service so history
 * survives app restarts. Kept small on purpose (battery/charger telemetry).
 */
@Entity(tableName = "samples")
data class SampleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ts: Long,
    val level: Int,
    val status: Int,
    val plugged: Int,
    val temperature: Float,
    val voltage: Float,
    val vinVoltage: Float?,
    val inputCurrent: Float?,
    val chargeCurrent: Float?,
    val power: Double?,
    val isCharging: Boolean,
)
