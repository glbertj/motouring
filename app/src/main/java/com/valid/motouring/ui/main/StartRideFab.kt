package com.valid.motouring.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.valid.motouring.ui.theme.AccentPrimary
import com.valid.motouring.ui.theme.Charcoal700
import com.valid.motouring.ui.theme.MotouringMotion
import com.valid.motouring.ui.theme.OffWhite

@Composable
fun StartRideFab(
    open: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rot by animateFloatAsState(if (open) 45f else 0f, MotouringMotion.press(), label = "fabRot")

    Box(modifier) {
        // The FAB button itself
        Surface(
            color = AccentPrimary,
            shape = RoundedCornerShape(20.dp),
            shadowElevation = 10.dp,
            modifier = Modifier.size(60.dp).clickable { onToggle() },
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    if (open) Icons.Filled.Close else Icons.Filled.PlayArrow,
                    contentDescription = "Start ride",
                    tint = Color(0xFF100E0C),
                    modifier = Modifier.rotate(if (open) 0f else rot),
                )
            }
        }
    }
}

// The menu (scrim + items) is hosted as a full-screen overlay so its scrim covers the whole
// screen; MainScaffold composes it as a sibling of Scaffold (not inside the bottomBar slot) so
// it doesn't affect Scaffold's measured bottom-bar height.
@Composable
fun RideActionMenu(
    onDismiss: () -> Unit,
    onStartSolo: () -> Unit,
    onStartGroup: () -> Unit,
    onPlanRoute: () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(Color(0xAA000000)).clickable(onClick = onDismiss)) {
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AnimatedVisibility(true) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    MenuItem("Plan a route", onPlanRoute)
                    MenuItem("Start group ride", onStartGroup)
                    MenuItem("Start solo ride", onStartSolo)
                }
            }
        }
    }
}

@Composable
private fun MenuItem(label: String, onClick: () -> Unit) {
    Surface(
        color = Charcoal700,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 6.dp,
        modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick),
    ) {
        Box(Modifier.padding(horizontal = 18.dp, vertical = 11.dp)) {
            Text(label, color = OffWhite)
        }
    }
}
