package com.kyant.backdrop.catalog.destinations

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.catalog.BackdropDemoScaffold
import com.kyant.backdrop.catalog.components.progressiveBlur

@Composable
fun ProgressiveBlurContent() {
    val isLightTheme = !isSystemInDarkTheme()
    val contentColor = if (isLightTheme) Color.Black else Color.White
    val tintColor = if (isLightTheme) Color.White else Color(0xFF808080)

    BackdropDemoScaffold { backdrop ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16f.dp)
        ) {
            Box(
                Modifier
                    .progressiveBlur(
                        backdrop = backdrop,
                        tint = tintColor,
                    )
                    .height(128f.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                BasicText("alpha-masked progressive blur", style = TextStyle(contentColor, 16f.sp))
            }
        }
    }
}
