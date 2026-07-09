package com.valid.motouring.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.valid.motouring.ui.theme.MotouringMotion
import kotlinx.coroutines.delay

@Composable
fun StaggeredEntrance(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var visible by remember(index) { mutableStateOf(false) }
    LaunchedEffect(index) {
        delay(index * MotouringMotion.staggerDelayMs)
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = MotouringMotion.comfy()) +
            slideInVertically(animationSpec = MotouringMotion.comfy(), initialOffsetY = { it / 6 }),
        modifier = modifier,
    ) {
        content()
    }
}
