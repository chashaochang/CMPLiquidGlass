package com.kyant.backdrop.catalog.components

import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import kotlinx.coroutines.launch

@Composable
fun LiquidFloatingTabs(
    labels: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
) {
    val isLightTheme = !isSystemInDarkTheme()
    val listState = rememberLazyListState()
    val animationScope = rememberCoroutineScope()

    LazyRow(
        modifier = modifier,
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        itemsIndexed(labels) { index, label ->
            val selected = index == selectedIndex
            val contentColor = if (isLightTheme) {
                Color.Black.copy(alpha = if (selected) 1f else .78f)
            } else {
                Color.White.copy(alpha = if (selected) 1f else .86f)
            }
            val selectedSurfaceColor = if (selected) {
                if (isLightTheme) Color.Black.copy(alpha = .12f)
                else Color.White.copy(alpha = .12f)
            } else {
                Color.Unspecified
            }
            LiquidButton(
                onClick = {
                    onSelected(index)
                    animationScope.launch {
                        var item = listState.layoutInfo.visibleItemsInfo
                            .firstOrNull { it.index == index }
                        if (item == null) {
                            listState.animateScrollToItem(index)
                            item = listState.layoutInfo.visibleItemsInfo
                                .firstOrNull { it.index == index }
                        }
                        item?.let {
                            val layoutInfo = listState.layoutInfo
                            val viewportWidth =
                                layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
                            val distanceToCenter =
                                it.offset - (viewportWidth - it.size) / 2
                            listState.animateScrollBy(distanceToCenter.toFloat())
                        }
                    }
                },
                backdrop = backdrop,
                surfaceColor = selectedSurfaceColor,
            ) {
                BasicText(
                    label,
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 16.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    ),
                )
            }
        }
    }
}
