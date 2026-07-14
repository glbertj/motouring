package com.valid.motouring.ui.theme

import androidx.compose.ui.graphics.Color

val Charcoal950 = Color(0xFF100E0C)
val Charcoal900 = Color(0xFF15130F)
val Charcoal800 = Color(0xFF1A1714)
val Charcoal700 = Color(0xFF241E19)
val Charcoal600 = Color(0xFF2A2522)
val Charcoal500 = Color(0xFF3D3632)
val AccentPrimary = Color(0xFFFF5A36)
val OffWhite = Color(0xFFF5F1EC)
val Muted = Color(0xFFA89F97)
val MutedDim = Color(0xFF7A8087)

// Category accents (used by POI pins, group bar, tags). A restrained multi-hue set on top of the orange.
val PoiFuel = Color(0xFF4ADE80)     // green
val PoiRepair = AccentPrimary       // brand orange
val PoiRest = Color(0xFFF5C34B)     // amber
val RiderBlue = Color(0xFF7CB8FF)   // self / rider
val RiderPurple = Color(0xFF8B7BE8)
val RiderCoral = Color(0xFFFF8A65)
val SosRed = Color(0xFFFF3B30)      // danger — SOS / crash / in-trouble
val SpeakingGreen = Color(0xFF4ADE80)

object MotouringColors {
    val ringTrack = Charcoal600
    val ringProgress = AccentPrimary
    val ringTick = Charcoal500
    val ringGlow = Charcoal700
    val badgeLocked = Charcoal600
    val badgeEarned = AccentPrimary
    val liked = AccentPrimary
    val poiFuel = PoiFuel
    val poiRepair = PoiRepair
    val poiRest = PoiRest
    val rider = RiderBlue
    val riderPurple = RiderPurple
    val riderCoral = RiderCoral
    val speaking = SpeakingGreen
    val goal = AccentPrimary
    val sos = SosRed
}
