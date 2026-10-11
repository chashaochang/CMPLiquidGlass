package com.kyant.backdrop.catalog.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

@Composable
fun LiquidActionBar(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    navigation: (@Composable BoxScope.(buttonBackdrop: Backdrop) -> Unit)? = null,
    title: @Composable BoxScope.() -> Unit = {},
    actions: @Composable RowScope.(buttonBackdrop: Backdrop) -> Unit = {},
) {
    val isLightTheme = !isSystemInDarkTheme()
    val tintColor = if (isLightTheme) Color.White else Color(0xFF202124)
    val density = LocalDensity.current
    val topInset = with(density) { WindowInsets.systemBars.getTop(density).toDp() }
    val progressiveBackdrop = rememberLayerBackdrop()
    val buttonBackdrop = rememberCombinedBackdrop(backdrop, progressiveBackdrop)

    Box(
        modifier
            .fillMaxWidth()
            .height(topInset + 56.dp)
            .progressiveBlur(
                backdrop = backdrop,
                tint = tintColor,
                tintIntensity = if (isLightTheme) 0.72f else 0.68f,
                gradientStartFraction = 1f / 3f,
                exportedBackdrop = progressiveBackdrop,
            ),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .displayCutoutPadding()
                .padding(start = 16.dp, top = 6.dp, end = 16.dp),
        ) {
            if (navigation != null) {
                Box(
                    modifier = Modifier.align(Alignment.TopStart),
                ) {
                    navigation(buttonBackdrop)
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(44.dp)
                    .padding(horizontal = 64.dp),
                contentAlignment = Alignment.Center,
                content = title,
            )

            Row(
                modifier = Modifier.align(Alignment.TopEnd),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                actions(buttonBackdrop)
            }
        }
    }
}

@Composable
fun LiquidActionBarButton(
    onClick: () -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    LiquidButton(
        onClick = onClick,
        backdrop = backdrop,
        modifier = modifier.size(44.dp),
        surfaceColor = if (isSystemInDarkTheme()) {
            Color.Black.copy(alpha = 0.12f)
        } else {
            Color.White.copy(alpha = 0.12f)
        },
        height = 44.dp,
        contentPadding = PaddingValues(0.dp),
        contentSpacing = 0.dp,
        content = content,
    )
}
