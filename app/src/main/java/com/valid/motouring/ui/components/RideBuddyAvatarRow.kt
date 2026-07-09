package com.valid.motouring.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.valid.motouring.ui.theme.Charcoal600
import com.valid.motouring.ui.theme.Charcoal800
import com.valid.motouring.ui.theme.MotouringTextStyles

@Composable
fun RideBuddyAvatarRow(
    avatarResList: List<Int>,
    modifier: Modifier = Modifier,
    maxVisible: Int = 4,
) {
    Row(modifier = modifier) {
        avatarResList.take(maxVisible).forEach { res ->
            Image(
                painter = painterResource(id = res),
                contentDescription = null,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, Charcoal600, CircleShape)
                    .padding(2.dp),
            )
        }
        val overflow = avatarResList.size - maxVisible
        if (overflow > 0) {
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(Charcoal800)
                    .border(1.5.dp, Charcoal600, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "+$overflow", style = MotouringTextStyles.statLabel)
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun RideBuddyAvatarRowPreview() {
    com.valid.motouring.ui.theme.MotouringTheme {
        RideBuddyAvatarRow(
            avatarResList = com.valid.motouring.data.fake.FakeDataProvider.users.map { it.avatarRes },
        )
    }
}
