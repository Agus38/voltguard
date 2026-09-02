package com.voltguard.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voltguard.app.data.PowerFormatters
import com.voltguard.app.data.prefs.MonitorSettings
import com.voltguard.app.ui.components.SectionTitle
import com.voltguard.app.ui.components.VgCard
import com.voltguard.app.ui.theme.Cyan
import com.voltguard.app.ui.theme.TextMuted
import com.voltguard.app.ui.theme.TextPrimary
import kotlinx.coroutines.launch

@Composable
fun Settings(
    settings: MonitorSettings,
    onSettings: (MonitorSettings) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(bottom = 28.dp),
    ) {
        Spacer(Modifier.height(4.dp))
        SectionTitle("Pengaturan monitoring", icon = Icons.Default.Tune)

        VgCard(modifier = Modifier.fillMaxWidth()) {
            SwitchRow(
                title = "Aktifkan alert",
                desc = "Tampilkan banner & notifikasi saat tegangan/suhu di luar ambang.",
                checked = settings.alertEnabled,
                onChecked = { onSettings(settings.copy(alertEnabled = it)) },
            )
            Spacer(Modifier.height(6.dp))
            SwitchRow(
                title = "Getar saat alert",
                desc = "Getar singkat ketika ada peringatan (hanya untuk level WASPADA).",
                checked = settings.soundEnabled,
                onChecked = { onSettings(settings.copy(soundEnabled = it)) },
            )
        }

        Spacer(Modifier.height(18.dp))
        SectionTitle("Ambang tegangan input (mV)", icon = Icons.Default.Notes)
        VgCard(modifier = Modifier.fillMaxWidth()) {
            SliderRow("Normal min", "%.0f mV", settings.lowVinMv, 3_500f, 5_600f) {
                onSettings(settings.copy(lowVinMv = it))
            }
            Spacer(Modifier.height(14.dp))
            SliderRow("Normal maks", "%.0f mV", settings.highVinMv, 3_500f, 5_600f) {
                onSettings(settings.copy(highVinMv = it))
            }
            Spacer(Modifier.height(18.dp))
            Text(
                "Di bawah “min” atau di atas “maks” => peringatan (kuning). " +
                        "Sangat di bawah/atas => bahaya (merah). Nilai default cocok untuk adapter 5 V.",
                color = TextMuted, fontSize = 11.5.sp,
            )
        }

        Spacer(Modifier.height(18.dp))
        SectionTitle("Ambang suhu (°C)")
        VgCard(modifier = Modifier.fillMaxWidth()) {
            SliderRow("Peringatan", "%.0f °C", settings.highTempWarnC, 28f, 50f) {
                onSettings(settings.copy(highTempWarnC = it))
            }
            Spacer(Modifier.height(14.dp))
            SliderRow("Bahaya", "%.0f °C", settings.highTempAlertC, 30f, 55f) {
                onSettings(settings.copy(highTempAlertC = it))
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(
            "Tentang",
            color = TextMuted, fontSize = 12.sp,
        )
        VgCard(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
            Text(
                "VoltGuard 1.0.0\n" +
                        "Membaca tegangan, arus, daya, suhu, dan status pengisian dari sistem baterai Android " +
                        "(BatteryManager). Tegangan input adapter (VIN) hanya tampil bila ROM perangkat " +
                        "menyediakan datanya; jika tidak, app menampilkan tegangan sel sebagai acuan. " +
                        "Aplikasi tidak mengubah apa pun pada perangkat — hanya memantau.",
                color = TextMuted, fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    desc: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = 14.5.sp)
            Text(desc, color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun SliderRow(
    label: String,
    unitFmt: String,
    value: Float,
    rangeStart: Float,
    rangeEnd: Float,
    onValue: (Float) -> Unit,
) {
    val range = (rangeEnd - rangeStart).coerceAtLeast(0.001f)
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = TextPrimary, fontSize = 13.5.sp, modifier = Modifier.weight(1f))
            Text(unitFmt.format(value), color = Cyan, fontSize = 13.5.sp)
        }
        androidx.compose.material3.Slider(
            value = ((value - rangeStart) / range).coerceIn(0f, 1f),
            onValueChange = { onValue(rangeStart + it * range) },
            valueRange = 0f..1f,
        )
    }
}
