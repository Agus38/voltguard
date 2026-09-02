package com.voltguard.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Dehaze
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.voltguard.app.data.PowerFormatters
import com.voltguard.app.data.PowerFormatters.HealthState
import com.voltguard.app.data.AlertEvent
import com.voltguard.app.data.AlertLevel
import com.voltguard.app.ui.UiPower
import com.voltguard.app.ui.components.LineChart
import com.voltguard.app.ui.components.SectionTitle
import com.voltguard.app.ui.components.StatTile
import com.voltguard.app.ui.components.VgCard
import com.voltguard.app.ui.components.VoltageGauge
import com.voltguard.app.ui.theme.Amber
import com.voltguard.app.ui.theme.Cyan
import com.voltguard.app.ui.theme.Green
import com.voltguard.app.ui.theme.Red
import com.voltguard.app.ui.theme.TextMuted
import com.voltguard.app.ui.theme.TextPrimary
import com.voltguard.app.ui.theme.TextSecondary

@Composable
fun healthColor(h: HealthState): Color = when (h) {
    HealthState.GOOD -> Green
    HealthState.WARN -> Amber
    HealthState.ALERT -> Red
    HealthState.UNKNOWN -> Cyan
}

@Composable
fun Dashboard(
    ui: UiPower,
    voltageHistory: List<Float>,
    alert: AlertEvent?,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val s = ui.snap
    val h = ui.health
    val hc = healthColor(h)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(bottom = 28.dp),
    ) {
        Spacer(Modifier.height(4.dp))

        // Header row
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "VoltGuard",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Pantau tegangan & pengisian",
                    color = TextMuted,
                    fontSize = 12.5.sp,
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(hc.copy(alpha = 0.16f))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(8.dp).height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(hc),
                    )
                    Text(
                        h.label,
                        color = hc,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 7.dp),
                    )
                }
            }
        }

        // Hero gauge card
        VgCard(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(hc.copy(alpha = 0.04f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                VoltageGauge(
                    voltage = ui.displayVinMv ?: ui.displayMv,
                    minVin = s.minVinVoltage,
                    maxVin = s.maxVinVoltage,
                    color = hc,
                    label = if (ui.hasOemVin) "TEGANGAN INPUT" else "TEGANGAN SEL",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(10.dp))
            // Source + status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val charging = s.isCharging
                Icon(
                    imageVector = if (charging) Icons.Default.Bolt else Icons.Default.BatteryChargingFull,
                    contentDescription = null,
                    tint = if (charging) Amber else TextMuted,
                    modifier = Modifier.width(20.dp).height(20.dp),
                )
                Column(Modifier.padding(start = 10.dp).weight(1f)) {
                    Text(s.statusText, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        s.pluggedTypeText + " • " + s.tech +
                                (if (ui.estimatedMs >= 0) " • est. " + PowerFormatters.formatDuration(ui.estimatedMs) else ""),
                        color = TextMuted, fontSize = 12.5.sp,
                    )
                }
                TextButton(onClick = onRefresh) {
                    Icon(Icons.Default.Sync, contentDescription = null, tint = Cyan)
                    Text(" Segarkan", color = Cyan, fontSize = 12.sp)
                }
            }
        }

        if (alert != null) {
            AlertBanner(alert.title, alert.message, alert.level == AlertLevel.ALERT)
        }

        Spacer(Modifier.height(20.dp))
        SectionTitle("Riwayat tegangan", icon = Icons.Default.TrendingUp) {
            Text(
                if (voltageHistory.size > 1)
                    PowerFormatters.volt(voltageHistory.last().toFloat()) else "—",
                color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium,
            )
        }
        VgCard(modifier = Modifier.fillMaxWidth()) {
            if (voltageHistory.size > 1) {
                LineChart(
                    values = voltageHistory,
                    color = hc,
                    lo = 3_400f,
                    hi = 5_600f,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Perubahan tegangan sel dalam sesi ini  •  ${voltageHistory.size} titik",
                    color = TextMuted, fontSize = 11.5.sp,
                )
            } else {
                Text(
                    "Mengumpulkan data… buka aplikasi saat terhubung ke charger.",
                    color = TextMuted, fontSize = 13.sp,
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        SectionTitle("Detail daya")

        // Stat grid
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                VgCard(modifier = Modifier.weight(1f)) {
                    StatTile("Tegangan input",
                        PowerFormatters.volt(s.vinVoltage), Icons.Default.Bolt,
                        valueColor = if (s.vinVoltage != null) hc else TextMuted,
                        sub = if (ui.hasOemVin) "VIN adapter" else "tidak tersedia (lihat sel)")
                }
                VgCard(modifier = Modifier.weight(1f)) {
                    StatTile("Tegangan sel",
                        PowerFormatters.volt(s.voltage), Icons.Default.BatteryChargingFull,
                        sub = PowerFormatters.milliVolt(s.voltage))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                VgCard(modifier = Modifier.weight(1f)) {
                    StatTile("Arus input",
                        PowerFormatters.currentA(s.inputCurrent ?: s.chargeCurrent),
                        if (s.isCharging) Icons.Default.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                        sub = "maks. " + PowerFormatters.currentA(s.maxInputCurrent))
                }
                VgCard(modifier = Modifier.weight(1f)) {
                    StatTile("Daya",
                        PowerFormatters.power(s.power ?: s.chargePower),
                        Icons.Default.Bolt,
                        valueColor = if (s.isCharging) Amber else TextPrimary,
                        sub = if (s.isCharging) "menyala" else "—")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                VgCard(modifier = Modifier.weight(1f)) {
                    StatTile("Suhu",
                        PowerFormatters.temp(s.temperature), Icons.Default.Thermostat,
                        valueColor = PowerFormatters.classifyTemp(s.temperature).let {
                            if (it == HealthState.ALERT) Red else if (it == HealthState.WARN) Amber else TextPrimary
                        },
                        sub = "baterai")
                }
                VgCard(modifier = Modifier.weight(1f)) {
                    StatTile("Level",
                        PowerFormatters.percent(s.level), Icons.Default.BatteryChargingFull,
                        valueColor = Cyan, sub = if (s.isCharging) "meningkat" else "menurun")
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        SectionTitle("Info perangkat & pengisian")

        VgCard(modifier = Modifier.fillMaxWidth()) {
            InfoRow("Teknologi baterai", s.tech)
            InfoDivider()
            InfoRow("Kesehatan", healthLabel(s.health))
            InfoDivider()
            InfoRow("Status", s.statusText)
            InfoDivider()
            InfoRow("Sumber", s.pluggedTypeText)
            InfoDivider()
            InfoRow("Daya isi (maks.)", PowerFormatters.currentA(s.maxChargeCurrent))
            InfoDivider()
            InfoRow("Muatan saat ini", PowerFormatters.currentA(s.chargeNow.takeIf { it != 0f }))
        }

        Spacer(Modifier.height(20.dp))
        VgCard(modifier = Modifier.fillMaxWidth(), onClick = onOpenSettings) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Dehaze, null, tint = Cyan, modifier = Modifier.width(18.dp).height(18.dp))
                Text("Atur ambang alert & notifikasi", color = TextPrimary,
                    fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp).weight(1f))
            }
        }
    }
}

@Composable
fun AlertBanner(title: String, msg: String, isAlert: Boolean) {
    val bg = if (isAlert) Red else Amber
    VgCard(modifier = Modifier.fillMaxWidth().padding(top = 14.dp), color = bg.copy(alpha = 0.12f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, null, tint = bg, modifier = Modifier.width(20.dp).height(20.dp))
            Column(Modifier.padding(start = 10.dp)) {
                Text(title, color = bg, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
                Text(msg, color = TextSecondary, fontSize = 12.5.sp)
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, color = TextSecondary, fontSize = 13.5.sp, modifier = Modifier.weight(1f))
        Text(value, color = TextPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun InfoDivider() =
    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x12FFFFFF)))

private fun healthLabel(health: Int): String = when (health) {
    1 -> "Baik (Google: BATTERY_HEALTH_GOOD)"
    2 -> "Cold"
    3 -> "Overheat"
    4 -> "Over voltage"
    5 -> "Non-recoverable"
    else -> "Tidak diketahui"
}
