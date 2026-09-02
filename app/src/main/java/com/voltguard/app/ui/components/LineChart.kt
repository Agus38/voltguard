package com.voltguard.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voltguard.app.ui.theme.Amber
import com.voltguard.app.ui.theme.Red
import com.voltguard.app.ui.theme.TextMuted
import kotlin.math.max

/**
 * A lightweight, dependency-free line chart drawn on a Canvas.
 * [values] are plotted left→right; y is auto-scaled to the data with padding.
 * [lo]/[hi] (optional) override the y-axis bounds — useful for voltage bands.
 */
@Composable
fun LineChart(
    values: List<Float>,
    color: Color = Amber,
    modifier: Modifier = Modifier,
    lo: Float? = null,
    hi: Float? = null,
    threshold: Float? = null,
) {
    Canvas(
        modifier = modifier.fillMaxWidth().height(170.dp).padding(vertical = 4.dp)
    ) {
        val w = size.width
        val h = size.height
        val padX = 4f
        val padTop = 10f
        val padBottom = 18f
        val plotW = w - padX * 2
        val plotH = h - padTop - padBottom

        if (values.size < 2) return@Canvas

        val dataLo = values.min()
        val dataHi = values.max()
        val yLo = minOf(lo ?: dataLo, dataLo)
        val yHi = maxOf(hi ?: dataHi, dataHi)
        val span = (yHi - yLo).takeIf { it > 0.001f } ?: 1f

        fun x(i: Int) = padX + plotW * (i / (values.size - 1).toFloat())
        fun y(v: Float) = padTop + plotH * (1f - (v - yLo) / span)

        // Threshold line
        if (threshold != null && threshold in yLo..yHi) {
            val ty = y(threshold)
            drawLine(
                color = Red.copy(alpha = 0.5f),
                start = Offset(padX, ty), end = Offset(w - padX, ty),
                strokeWidth = 2f,
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                    floatArrayOf(8f, 6f), 0f
                ),
            )
        }

        // Grid (3 horizontal lines)
        for (g in 0..3) {
            val gy = padTop + plotH * (g / 3f)
            drawLine(
                color = Color(0x1AFFFFFF), start = Offset(padX, gy),
                end = Offset(w - padX, gy), strokeWidth = 1f,
            )
        }

        // Area fill
        val fill = Path().apply {
            moveTo(x(0), padTop + plotH)
            values.forEachIndexed { i, v -> lineTo(x(i), y(v)) }
            lineTo(x(values.size - 1), padTop + plotH)
            close()
        }
        drawPath(fill, Brush.verticalGradient(
            colors = listOf(color.copy(alpha = 0.35f), color.copy(alpha = 0.0f)),
            startY = padTop,
            endY = padTop + plotH,
        ))

        // Line
        val line = Path().apply {
            values.forEachIndexed { i, v ->
                if (i == 0) moveTo(x(0), y(v)) else lineTo(x(i), y(v))
            }
        }
        drawPath(line, color, style = androidx.compose.ui.graphics.drawscope.Stroke(3f))

        // Last point
        val lx = x(values.size - 1)
        val ly = y(values.last())
        drawCircle(color, 5f, Offset(lx, ly))
    }
}
