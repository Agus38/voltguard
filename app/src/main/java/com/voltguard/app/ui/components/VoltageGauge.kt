package com.voltguard.app.ui.components

import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voltguard.app.ui.theme.Bg
import com.voltguard.app.ui.theme.TextMuted
import com.voltguard.app.ui.theme.TextPrimary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private const val LO = 3_200f
private const val HI = 5_600f
private const val START_DEG = 150f
private const val SWEEP_DEG = 240f
private const val ARC_W = 18f
private const val PAD = 24f

/** Build an AARRGGBB int from a Compose [Color] without relying on ui-graphics extensions. */
private fun Color.argbInt(): Int {
    val a = (alpha * 255f).roundToInt() and 0xFF
    val r = (red * 255f).roundToInt() and 0xFF
    val g = (green * 255f).roundToInt() and 0xFF
    val b = (blue * 255f).roundToInt() and 0xFF
    return (a shl 24) or (r shl 16) or (g shl 8) or b
}

/**
 * 240° arc gauge for input/cell voltage, mapped over 3.2 V … 5.6 V.
 * Colored by the caller's health color. Drawn with the platform Android Canvas
 * (android.graphics) for maximum build compatibility.
 */
@Composable
fun VoltageGauge(
    voltage: Float?,
    minVin: Float?,
    maxVin: Float?,
    color: Color,
    modifier: Modifier = Modifier,
    label: String = "TEGANGAN",
) {
    val displayMv = voltage?.takeIf { it > 0f } ?: 0f
    val pos = ((displayMv - LO) / (HI - LO)).coerceIn(0f, 1f)
    val animated by animateFloatAsState(targetValue = pos, animationSpec = tween(900))

    val bandMin = if (minVin != null && maxVin != null && minVin > 0f && maxVin > 0f) minVin else null
    val bandMax = if (bandMin != null) maxVin else null

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxWidth().height(220.dp)) {
            val c: android.graphics.Canvas = drawContext.nativeCanvas
            val cx = size.width / 2f
            val cy = size.height * 0.56f
            val r = minOf(size.width / 2f - PAD, size.height * 0.5f - PAD)
            val o = r - ARC_W / 2f
            val rect = RectF(cx - o, cy - o, cx + o, cy + o)

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = ARC_W
                strokeCap = Paint.Cap.ROUND
            }

            // Track
            paint.color = 0x26FFFFFF.toInt()
            c.drawArc(rect, START_DEG, SWEEP_DEG, false, paint)

            // Designed-range band
            if (bandMin != null && bandMax != null) {
                val b0 = ((bandMin - LO) / (HI - LO)).coerceIn(0f, 1f)
                val b1 = ((bandMax - LO) / (HI - LO)).coerceIn(0f, 1f)
                paint.color = color.copy(alpha = 0.30f).argbInt()
                c.drawArc(rect, START_DEG + SWEEP_DEG * b0, (SWEEP_DEG * (b1 - b0)).coerceAtLeast(0f), false, paint)
            }

            // Value fill
            if (animated > 0.001f) {
                paint.color = color.argbInt()
                c.drawArc(rect, START_DEG, SWEEP_DEG * animated, false, paint)
            }

            // Needle dot
            val ang = (START_DEG + SWEEP_DEG * animated) * PI / 180f
            val px = cx + cos(ang).toFloat() * r
            val py = cy + sin(ang).toFloat() * r
            paint.style = Paint.Style.FILL
            paint.color = color.argbInt()
            c.drawCircle(px, py, 9f, paint)
            paint.color = Bg.argbInt()
            c.drawCircle(px, py, 4f, paint)
        }

        Column(
            modifier = Modifier.offset(y = (-52).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(label, color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.6.sp)
            Text(
                if (displayMv > 0f) "%.1f V".format(displayMv / 1000f) else "—",
                color = TextPrimary, fontSize = 40.sp, fontWeight = FontWeight.Bold,
            )
            Text(
                if (displayMv > 0f) "%.0f mV".format(displayMv) else "menunggu data…",
                color = TextMuted, fontSize = 13.sp,
            )
        }
    }
}
