package com.voltguard.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voltguard.app.data.PowerFormatters
import com.voltguard.app.data.db.SampleEntity
import com.voltguard.app.ui.components.LineChart
import com.voltguard.app.ui.components.SectionTitle
import com.voltguard.app.ui.components.VgCard
import com.voltguard.app.ui.theme.Amber
import com.voltguard.app.ui.theme.Cyan
import com.voltguard.app.ui.theme.Green
import com.voltguard.app.ui.theme.Red
import com.voltguard.app.ui.theme.TextMuted
import com.voltguard.app.ui.theme.TextPrimary
import com.voltguard.app.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
private val dateFmt = SimpleDateFormat("dd MMM", Locale.getDefault())

@Composable
fun History(
    samples: List<SampleEntity>,
    onClear: () -> Unit,
) {
    val voltValues = samples.asReversed().mapNotNull { it.voltage.toFloat().takeIf { v -> v > 0f } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(bottom = 28.dp),
    ) {
        Spacer(Modifier.height(4.dp))
        SectionTitle("Riwayat tersimpan", icon = Icons.Default.Schedule)
        if (samples.isEmpty()) {
            VgCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Belum ada data tersimpan.\n" +
                            "Data direkam otomatis tiap ~10 detik saat aplikasi/layar aktif.",
                    color = TextMuted, fontSize = 13.5.sp,
                )
            }
        } else {
            VgCard(modifier = Modifier.fillMaxWidth()) {
                LineChart(values = voltValues, color = Cyan, lo = 3_400f, hi = 5_600f)
                Spacer(Modifier.height(8.dp))
                Text(
                    "${samples.size} sampel • ${dateFmt.format(Date(samples.first().ts))} s.d. " +
                            "${dateFmt.format(Date(samples.last().ts))}",
                    color = TextMuted, fontSize = 11.5.sp,
                )
            }
            Spacer(Modifier.height(16.dp))

            Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)) {
                samples.take(120).forEach { s ->
                    HistoryRow(s)
                }
            }

            Spacer(Modifier.height(18.dp))
            TextButton(onClick = onClear) {
                Icon(Icons.AutoMirrored.Filled.TrendingDown, null, tint = Red)
                Text(" Hapus semua data", color = Red, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun HistoryRow(s: SampleEntity) {
    val v = s.voltage.takeIf { it > 0f }
    val hc = colorForRow(s)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF16233A))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (s.isCharging) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
            null, tint = hc, modifier = Modifier.width(16.dp).height(16.dp),
        )
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text(timeFmt.format(Date(s.ts)), color = TextPrimary, fontSize = 13.5.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
            Text(
                "Battery ${s.level}% • ${PowerFormatters.temp(s.temperature.toDouble())}" +
                        (if (s.vinVoltage != null) " • VIN ${PowerFormatters.volt(s.vinVoltage)}" else ""),
                color = TextMuted, fontSize = 11.5.sp,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(PowerFormatters.volt(v), color = TextPrimary, fontSize = 15.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
            Text(PowerFormatters.power(s.power), color = TextMuted, fontSize = 11.5.sp)
        }
    }
}

@Composable
private fun colorForRow(s: SampleEntity): Color = when {
    s.vinVoltage != null && (s.vinVoltage < 4200f || s.vinVoltage > 5800f) -> Red
    s.temperature > 42f -> Red
    s.isCharging -> Green
    else -> Cyan
}
