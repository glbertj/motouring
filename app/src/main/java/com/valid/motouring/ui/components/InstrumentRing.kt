package com.valid.motouring.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.valid.motouring.ui.theme.MotouringColors
import com.valid.motouring.ui.theme.MotouringMotion
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun InstrumentRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    strokeWidth: Dp = 4.dp,
    showTicks: Boolean = size >= 48.dp,
    showGlow: Boolean = size >= 56.dp,
    content: @Composable () -> Unit = {},
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = MotouringMotion.comfy(),
        label = "ringProgress",
    )

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = strokeWidth.toPx()
            val radius = (size.toPx() - stroke) / 2f
            val center = Offset(size.toPx() / 2f, size.toPx() / 2f)

            if (showGlow) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(MotouringColors.ringGlow.copy(alpha = 0.5f), MotouringColors.ringGlow.copy(alpha = 0f)),
                        center = center,
                        radius = radius * 1.3f,
                    ),
                    radius = radius * 1.3f,
                    center = center,
                )
            }

            drawCircle(
                color = MotouringColors.ringTrack,
                radius = radius,
                center = center,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )

            drawArc(
                color = MotouringColors.ringProgress,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )

            if (showTicks) {
                val tickLength = stroke * 1.5f
                for (angleDeg in listOf(0f, 90f, 180f, 270f)) {
                    val angleRad = Math.toRadians(angleDeg.toDouble())
                    val outer = Offset(
                        center.x + (radius + stroke) * cos(angleRad).toFloat(),
                        center.y + (radius + stroke) * sin(angleRad).toFloat(),
                    )
                    val inner = Offset(
                        center.x + (radius + stroke - tickLength) * cos(angleRad).toFloat(),
                        center.y + (radius + stroke - tickLength) * sin(angleRad).toFloat(),
                    )
                    drawLine(color = MotouringColors.ringTick, start = inner, end = outer, strokeWidth = 1.5.dp.toPx())
                }
            }
        }
        content()
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun InstrumentRingPreview() {
    com.valid.motouring.ui.theme.MotouringTheme {
        InstrumentRing(progress = 0.62f, size = 64.dp) {
            androidx.compose.material3.Text(
                text = "62%",
                style = com.valid.motouring.ui.theme.MotouringTextStyles.statValue,
            )
        }
    }
}
