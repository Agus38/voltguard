package com.voltguard.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voltguard.app.ui.theme.Bg
import com.voltguard.app.ui.theme.TextMuted
import com.voltguard.app.ui.theme.TextPrimary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val LO = 3_200f
private const val HI = 5_600f
private const val START_DEG = 150f
private const val SWEEP_DEG = 240f
private const val ARC_W = 18f
private const val PAD = 24f

/**
 * 240° arc gauge for input/cell voltage, mapped over 3.2 V … 5.6 V.
 * Colored by the caller's health color.
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
            val center = Offset(size.width / 2f, size.height * 0.56f)
            val radius = minOf(size.width / 2f - PAD, size.height * 0.5f - PAD)
            val box = Offset(center.x - radius, center.y - radius)
            val dim = Size(radius * 2f, radius * 2f)

            drawArc(Brush.solid(Color(0x26FFFFFF)), START_DEG, SWEEP_DEG, false, box, dim,
                style = androidx.compose.ui.graphics.drawscope.Stroke(ARC_W, StrokeCap.Round))

            if (bandMin != null && bandMax != null) {
                val b0 = ((bandMin - LO) / (HI - LO)).coerceIn(0f, 1f)
                val b1 = ((bandMax - LO) / (HI - LO)).coerceIn(0f, 1f)
                drawArc(Brush.solid(color.copy(alpha = 0.30f)), START_DEG + SWEEP_DEG * b0,
                    (SWEEP_DEG * (b1 - b0)).coerceAtLeast(0f), false, box, dim,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(ARC_W, StrokeCap.Round))
            }

            if (animated > 0.001f) {
                drawArc(Brush.solid(color), START_DEG, SWEEP_DEG * animated, false, box, dim,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(ARC_W, StrokeCap.Round))
            }

            val a = (START_DEG + SWEEP_DEG * animated) * PI / 180f
            val dot = Offset(center.x + cos(a) * radius, center.y + sin(a) * radius)
            drawCircle(color, 9f, dot)
            drawCircle(Bg, 4f, dot)
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
