package com.valid.motouring.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.valid.motouring.ui.theme.Charcoal600
import com.valid.motouring.ui.theme.Charcoal700
import com.valid.motouring.ui.theme.Charcoal900

private val cardShape = RoundedCornerShape(18.dp)

@Composable
fun MotouringCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    var base = modifier
        .clip(cardShape)
        .background(Brush.verticalGradient(listOf(Charcoal700, Charcoal900)))
        .border(1.dp, Charcoal600, cardShape)

    if (onClick != null) {
        base = base
            .pressScale(interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    }

    Column(modifier = base, content = content)
}
