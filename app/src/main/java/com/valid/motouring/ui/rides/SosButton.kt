package com.valid.motouring.ui.rides

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.valid.motouring.ui.theme.MotouringColors

private const val HOLD_MS = 2000

@Composable
fun SosButton(onFire: () -> Unit, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    var pressed by remember { mutableStateOf(false) }

    LaunchedEffect(pressed) {
        if (pressed) {
            progress.animateTo(1f, tween(HOLD_MS, easing = LinearEasing))
            if (progress.value >= 1f) onFire()
        } else {
            progress.snapTo(0f)
        }
    }

    Box(
        modifier = modifier
            .size(60.dp)
            .clip(CircleShape)
            .background(MotouringColors.sos)
            .drawBehind {
                if (progress.value > 0f) {
                    val stroke = 5.dp.toPx()
                    drawArc(
                        color = Color.White,
                        startAngle = -90f,
                        sweepAngle = 360f * progress.value,
                        useCenter = false,
                        topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2),
                        size = Size(size.width - stroke, size.height - stroke),
                        style = Stroke(width = stroke),
                    )
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    pressed = true
                    tryAwaitRelease()
                    pressed = false
                })
            },
        contentAlignment = Alignment.Center,
    ) {
        Text("SOS", color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun SosButtonPreview() {
    com.valid.motouring.ui.theme.MotouringTheme {
        SosButton(onFire = {})
    }
}
