package com.voltguard.app.data

import com.voltguard.app.data.PowerFormatters.HealthState

/** Level of a raised monitoring alert. */
enum class AlertLevel { WARN, ALERT }

/** A single raised condition, with a friendly message for the UI banner & notification. */
data class AlertEvent(
    val level: AlertLevel,
    val title: String,
    val message: String,
    val at: Long,
)
