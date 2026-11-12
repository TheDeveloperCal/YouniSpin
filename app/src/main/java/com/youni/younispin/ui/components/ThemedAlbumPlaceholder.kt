package com.youni.younispin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.youni.younispin.ui.theme.YouniTheme

@Composable
fun ThemedAlbumPlaceholder(
    modifier: Modifier = Modifier
) {
    val colors = YouniTheme.colors
    Box(
        modifier = modifier.background(
            Brush.linearGradient(
                listOf(colors.surfaceElevated, colors.previewBackground, colors.displayGlass)
            )
        ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "YOUNI",
                    color = colors.textPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "SPIN",
                    color = colors.accent,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp
                )
            }
            Spacer(Modifier.height(7.dp))
            Text(
                "ALBUM ART UNAVAILABLE",
                color = colors.textSecondary,
                fontSize = 8.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp
            )
        }
    }
}
