package com.valid.motouring.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring

object MotouringMotion {
    fun <T> comfy(): SpringSpec<T> = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow)
    fun <T> press(): SpringSpec<T> = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
    const val staggerDelayMs = 70L
}
