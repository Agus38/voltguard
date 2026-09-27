package com.voltguard.app.data

/**
 * UpdateChecker - Check for new releases via GitHub API
 * Author: Agus Dev
 */

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val hasUpdate: Boolean,
    val latestVersion: String,
    val currentVersion: String,
    val downloadUrl: String,
    val releaseNotes: String,
)

object UpdateChecker {
    private const val GITHUB_API = "https://api.github.com/repos/Agus38/voltguard/releases/latest"

    suspend fun check(context: Context): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val current = getCurrentVersion(context)
            val url = URL(GITHUB_API)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.setRequestProperty("Accept", "application/json")

            if (conn.responseCode != 200) return@withContext null

            val response = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)

            val latestTag = json.optString("tag_name", "").removePrefix("v")
            val releaseNotes = json.optString("body", "")
            
            // Find APK asset
            val assets = json.optJSONArray("assets")
            var apkUrl = ""
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk")) {
                        apkUrl = asset.optString("browser_download_url", "")
                        break
                    }
                }
            }

            val hasUpdate = compareVersions(latestTag, current) > 0

            UpdateInfo(
                hasUpdate = hasUpdate,
                latestVersion = latestTag,
                currentVersion = current,
                downloadUrl = apkUrl,
                releaseNotes = releaseNotes.take(200),
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun getCurrentVersion(context: Context): String = try {
        val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        pInfo.versionName ?: "1.0.0"
    } catch (e: Exception) {
        "1.0.0"
    }

    private fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.split(".").mapNotNull { it.toIntOrNull() }
        val parts2 = v2.split(".").mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(parts1.size, parts2.size)

        for (i in 0 until maxLen) {
            val p1 = parts1.getOrNull(i) ?: 0
            val p2 = parts2.getOrNull(i) ?: 0
            if (p1 != p2) return p1.compareTo(p2)
        }
        return 0
    }
}
