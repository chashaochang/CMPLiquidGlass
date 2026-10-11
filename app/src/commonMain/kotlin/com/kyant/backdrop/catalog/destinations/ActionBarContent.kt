package com.kyant.backdrop.catalog.destinations

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.catalog.BackdropDemoScaffold
import com.kyant.backdrop.catalog.components.LiquidActionBar
import com.kyant.backdrop.catalog.components.LiquidActionBarBackIcon
import com.kyant.backdrop.catalog.components.LiquidActionBarButton
import com.kyant.backdrop.catalog.components.LiquidActionBarMoreIcon

private val actionBarDemoColors = listOf(
    Color(0xFFEF6A5B) to Color(0xFF8D315F),
    Color(0xFF38AAA2) to Color(0xFF205D76),
    Color(0xFFF1B34F) to Color(0xFF8A5732),
    Color(0xFF6E91EA) to Color(0xFF48407E),
    Color(0xFFC16DD1) to Color(0xFF71396C),
    Color(0xFF65AA62) to Color(0xFF2E6652),
)

@Composable
fun ActionBarContent(onBack: () -> Unit) {
    val isLightTheme = !isSystemInDarkTheme()
    val contentColor = if (isLightTheme) Color(0xFF17181B) else Color.White
    val iconColorFilter = ColorFilter.tint(contentColor)
    val backIcon = rememberVectorPainter(LiquidActionBarBackIcon)
    val moreIcon = rememberVectorPainter(LiquidActionBarMoreIcon)
    var actionCount by rememberSaveable { mutableIntStateOf(0) }

    BackdropDemoScaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0)) { backgroundBackdrop ->
        val listBackdrop = rememberLayerBackdrop()
        val actionBarBackdrop = rememberCombinedBackdrop(backgroundBackdrop, listBackdrop)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .displayCutoutPadding()
                .layerBackdrop(listBackdrop),
            contentPadding = PaddingValues(start = 18.dp, top = 116.dp, end = 18.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                BasicText(
                    text = "Library",
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
                    style = TextStyle(contentColor, 32.sp, FontWeight.SemiBold),
                )
            }
            itemsIndexed(actionBarDemoColors + actionBarDemoColors) { index, colors ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(132.dp)
                        .background(
                            brush = Brush.horizontalGradient(listOf(colors.first, colors.second)),
                            shape = RoundedCornerShape(18.dp),
                        )
                        .padding(20.dp),
                    contentAlignment = Alignment.BottomStart,
                ) {
                    BasicText(
                        text = "Collection ${index + 1}",
                        style = TextStyle(Color.White, 20.sp, FontWeight.SemiBold),
                    )
                }
            }
            item {
                Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.systemBars))
            }
        }

        LiquidActionBar(
            backdrop = actionBarBackdrop,
            modifier = Modifier.align(Alignment.TopCenter),
            navigation = { buttonBackdrop ->
                LiquidActionBarButton(onClick = onBack, backdrop = buttonBackdrop) {
                    Image(
                        painter = backIcon,
                        contentDescription = "Back",
                        modifier = Modifier.size(20.dp),
                        colorFilter = iconColorFilter,
                    )
                }
            },
            title = {
                BasicText(
                    text = if (actionCount == 0) "My Library" else "Updated $actionCount",
                    style = TextStyle(contentColor, 18.sp, FontWeight.SemiBold),
                )
            },
            actions = { buttonBackdrop ->
                LiquidActionBarButton(
                    onClick = { actionCount++ },
                    backdrop = buttonBackdrop,
                ) {
                    Image(
                        painter = moreIcon,
                        contentDescription = "More",
                        modifier = Modifier.size(20.dp),
                        colorFilter = iconColorFilter,
                    )
                }
            },
        )
    }
}
