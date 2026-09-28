package com.voltguard.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
 * 240° arc gauge dengan glow effect untuk input/cell voltage, mapped over 3.2 V … 5.6 V.
 * Enhanced visual: glow shadow, smooth animation
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
    val animated by animateFloatAsState(
        targetValue = pos, 
        animationSpec = tween(1200, easing = androidx.compose.animation.core.FastOutSlowInEasing)
    )

    val bandMin = if (minVin != null && maxVin != null && minVin > 0f && maxVin > 0f) minVin else null
    val bandMax = if (bandMin != null) maxVin else null

    // Pulse animation untuk glow effect
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha: Float by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxWidth().height(220.dp)) {
            val center = Offset(size.width / 2f, size.height * 0.56f)
            val radius = minOf(size.width / 2f - PAD, size.height * 0.5f - PAD)
            val topLeft = Offset(center.x - radius, center.y - radius)
            val dim = Size(radius * 2f, radius * 2f)
            val style = Stroke(width = ARC_W, cap = StrokeCap.Round)

            // Track
            drawArc(Color(0x26FFFFFF), START_DEG, SWEEP_DEG, false, topLeft, dim, style = style)

            // Designed-range band
            if (bandMin != null && bandMax != null) {
                val b0 = ((bandMin - LO) / (HI - LO)).coerceIn(0f, 1f)
                val b1 = ((bandMax - LO) / (HI - LO)).coerceIn(0f, 1f)
                drawArc(
                    color.copy(alpha = 0.30f),
                    START_DEG + SWEEP_DEG * b0,
                    (SWEEP_DEG * (b1 - b0)).coerceAtLeast(0f),
                    false, topLeft, dim, style = style,
                )
            }

            // Glow effect (outer shadow)
            if (animated > 0.001f) {
                drawArc(
                    color.copy(alpha = glowAlpha * 0.4f),
                    START_DEG,
                    SWEEP_DEG * animated,
                    false,
                    topLeft,
                    dim,
                    style = Stroke(width = ARC_W + 8f, cap = StrokeCap.Round)
                )
            }

            // Value fill dengan gradient
            if (animated > 0.001f) {
                val gradient = Brush.sweepGradient(
                    colors = listOf(
                        color.copy(alpha = 0.6f),
                        color,
                        color.copy(alpha = 0.8f)
                    ),
                    center = center
                )
                drawArc(
                    brush = gradient,
                    startAngle = START_DEG,
                    sweepAngle = SWEEP_DEG * animated,
                    useCenter = false,
                    topLeft = topLeft,
                    size = dim,
                    style = style
                )
            }

            // Needle dot dengan glow
            val ang = (START_DEG + SWEEP_DEG * animated) * PI / 180f
            val px = center.x + cos(ang).toFloat() * radius
            val py = center.y + sin(ang).toFloat() * radius
            val dot = Offset(px, py)
            
            // Outer glow
            drawCircle(color.copy(alpha = glowAlpha * 0.5f), radius = 14f, center = dot)
            // Main dot
            drawCircle(color, radius = 9f, center = dot)
            // Center
            drawCircle(Bg, radius = 4f, center = dot)
        }

        Column(
            modifier = Modifier.offset(y = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(label, color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.6.sp)
            Text(
                if (displayMv > 0f) "%.1f V".format(displayMv / 1000f) else "—",
                color = TextPrimary, fontSize = 38.sp, fontWeight = FontWeight.Bold,
            )
            Text(
                if (displayMv > 0f) "%.0f mV".format(displayMv) else "menunggu data…",
                color = TextMuted, fontSize = 13.sp,
            )
        }
    }
}
