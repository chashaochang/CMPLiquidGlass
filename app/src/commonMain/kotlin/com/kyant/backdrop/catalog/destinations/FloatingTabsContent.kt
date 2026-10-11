package com.kyant.backdrop.catalog.destinations

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.catalog.components.LiquidFloatingTabs

private val floatingTabLabels = listOf("Recently added", "Variety", "K-Drama", "Movies", "Animation")
private const val floatingTabsAnchorKey = "floating-tabs-anchor"

private val floatingTabColors = listOf(
    Color(0xFFEF6A5B) to Color(0xFF7C274C),
    Color(0xFF30A8A0) to Color(0xFF174F6B),
    Color(0xFFF0B45A) to Color(0xFF76512D),
    Color(0xFF6F8FE8) to Color(0xFF3B356F),
    Color(0xFFC06FD0) to Color(0xFF5B315F),
    Color(0xFF67A85F) to Color(0xFF244F42),
)

@Composable
fun FloatingTabsContent() {
    val isLightTheme = !isSystemInDarkTheme()
    val backdrop = rememberLayerBackdrop()
    val listState = rememberLazyListState()
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    val backgroundColor = if (isLightTheme) Color(0xFFF1F2F5) else Color(0xFF111216)
    val primaryText = if (isLightTheme) Color(0xFF17181B) else Color.White
    val density = LocalDensity.current
    val tabsInsetPx = with(density) { 12.dp.roundToPx() }
    val stickyTopPx = WindowInsets.systemBars.getTop(density) + tabsInsetPx
    val tabsTopPx by remember(listState, stickyTopPx, tabsInsetPx) {
        derivedStateOf {
            val anchor = listState.layoutInfo.visibleItemsInfo
                .firstOrNull { it.key == floatingTabsAnchorKey }
            when {
                anchor != null -> maxOf(stickyTopPx, anchor.offset + tabsInsetPx)
                listState.firstVisibleItemIndex > 1 -> stickyTopPx
                else -> stickyTopPx
            }
        }
    }

    Box(Modifier.fillMaxSize().background(backgroundColor)) {
        Box(
            Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor)
                    .layerBackdrop(backdrop),
                contentPadding = PaddingValues(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    Spacer(Modifier.windowInsetsTopHeight(WindowInsets.systemBars))
                    Spacer(Modifier.height(18.dp))
                    BasicText(
                        "Movie",
                        style = TextStyle(primaryText, 30.sp, FontWeight.SemiBold),
                    )
                }
                item(key = floatingTabsAnchorKey) {
                    Spacer(Modifier.fillMaxWidth().height(72.dp))
                }
                itemsIndexed(floatingTabColors + floatingTabColors) { index, colors ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(112.dp)
                            .background(
                                Brush.horizontalGradient(listOf(colors.first, colors.second)),
                                RoundedCornerShape(18.dp),
                            )
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Box(
                            Modifier
                                .size(72.dp)
                                .background(Color.White.copy(alpha = .18f), RoundedCornerShape(14.dp)),
                        )
                        androidx.compose.foundation.layout.Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            BasicText(
                                "Collection ${index + 1}",
                                style = TextStyle(Color.White, 18.sp, FontWeight.SemiBold),
                            )
                            BasicText(
                                floatingTabLabels[index % floatingTabLabels.size],
                                style = TextStyle(Color.White.copy(alpha = .72f), 14.sp),
                            )
                        }
                    }
                }
                item {
                    Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.systemBars))
                    Spacer(Modifier.height(24.dp))
                }
            }
        }

        LiquidFloatingTabs(
            labels = floatingTabLabels,
            selectedIndex = selectedIndex,
            onSelected = { selectedIndex = it },
            backdrop = backdrop,
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .displayCutoutPadding()
                .offset { IntOffset(0, tabsTopPx) }
                .padding(horizontal = 18.dp),
        )
    }
}
