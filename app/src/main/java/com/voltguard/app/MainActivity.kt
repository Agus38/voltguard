package com.voltguard.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.voltguard.app.ui.MainViewModel
import com.voltguard.app.ui.screens.Dashboard
import com.voltguard.app.ui.screens.History
import com.voltguard.app.ui.screens.Settings
import com.voltguard.app.ui.theme.Bg
import com.voltguard.app.ui.theme.Cyan
import com.voltguard.app.ui.theme.TextMuted
import com.voltguard.app.ui.theme.TextPrimary
import com.voltguard.app.ui.theme.VoltGuardTheme
import com.voltguard.app.service.CollectorService
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        viewModel = MainViewModel(application as VoltGuardApp)
        requestNotificationPermission()
        startService()

        setContent {
            VoltGuardTheme {
                VoltGuardRoot(viewModel)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        startService()
    }

    private fun startService() {
        val i = Intent(this, CollectorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(i)
        else startService(i)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }
    }
}

@Composable
fun VoltGuardRoot(viewModel: MainViewModel) {
    val ui by viewModel.ui.collectAsState()
    val history by viewModel.history.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val alert by viewModel.lastAlert.collectAsState()
    var tab by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    // In-session voltage samples for the dashboard sparkline (most recent last).
    val session = remember { mutableStateOf(java.util.LinkedList<Float>()) }
    val lastTs = remember { longArrayOf(0L) }
    LaunchedEffect(Unit) {
        viewModel.ui.collect { u ->
            val s = u.snap
            if (s.timestamp > 0 && s.timestamp != lastTs[0] && s.voltage > 0f) {
                lastTs[0] = s.timestamp
                val list = session.value
                list.addLast(s.voltage)
                if (list.size > 120) list.removeFirst()
            }
        }
    }

    Scaffold(
        containerColor = Bg,
        contentColor = TextPrimary,
        bottomBar = { BottomBar(selected = tab) { tab = it } },
    ) { innerPad ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPad)) {
            when (tab) {
                0 -> Dashboard(
                    ui = ui,
                    voltageHistory = session.value.toList(),
                    alert = alert,
                    onRefresh = {
                        viewModel.collectNow()
                        scope.launch { viewModel.refreshHistory() }
                    },
                    onOpenSettings = { tab = 2 },
                )
                1 -> History(
                    samples = history,
                    onClear = { scope.launch { viewModel.clearHistory() } },
                )
                2 -> Settings(
                    settings = settings,
                    onSettings = { viewModel.updateSettings { it } },
                )
            }
        }
    }
}

@Composable
private fun BottomBar(selected: Int, onSelect: (Int) -> Unit) {
    val items = listOf(
        Triple(0, "Ringkasan", Icons.Default.Home),
        Triple(1, "Riwayat", Icons.Default.Schedule),
        Triple(2, "Pengaturan", Icons.Default.Settings),
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Bg)
            .padding(horizontal = 20.dp)
            .padding(bottom = 14.dp, top = 8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { (idx, label, icon) ->
                val active = idx == selected
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (active) Cyan.copy(alpha = 0.14f) else Color.Transparent)
                        .clickable(onClick = { onSelect(idx) })
                        .padding(horizontal = 6.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (active) Cyan else TextMuted,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        label,
                        color = if (active) Cyan else TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
            }
        }
    }
}
