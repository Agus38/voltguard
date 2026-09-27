package com.voltguard.app.ui.components

/**
 * UpdateDialog - Alert for new version available
 * Author: Agus Dev
 */

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voltguard.app.data.UpdateInfo
import com.voltguard.app.ui.theme.Cyan
import com.voltguard.app.ui.theme.TextMuted
import com.voltguard.app.ui.theme.TextPrimary

@Composable
fun UpdateDialog(
    updateInfo: UpdateInfo,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Update Tersedia",
                color = Cyan,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Versi ${updateInfo.latestVersion} sudah tersedia",
                    color = TextPrimary,
                    fontSize = 14.sp,
                )
                Text(
                    "Versi kamu saat ini: ${updateInfo.currentVersion}",
                    color = TextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
                
                if (updateInfo.releaseNotes.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Perubahan:",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        updateInfo.releaseNotes,
                        color = TextMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (updateInfo.downloadUrl.isNotBlank()) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateInfo.downloadUrl))
                        context.startActivity(intent)
                    } else {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Agus38/voltguard/releases/latest"))
                        context.startActivity(intent)
                    }
                    onDismiss()
                }
            ) {
                Text("Download", color = Cyan, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Nanti", color = TextMuted, fontSize = 14.sp)
            }
        }
    )
}
