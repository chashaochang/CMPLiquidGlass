package com.kyant.backdrop.catalog.destinations

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.catalog.BackdropDemoScaffold
import com.kyant.backdrop.catalog.components.LiquidBottomTab
import com.kyant.backdrop.catalog.components.LiquidBottomTabs
import com.kyant.backdrop.catalog.utils.BackHandler
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedRectangle

private data class MiniBarTrack(val title: String, val artist: String, val duration: String, val color: Color)

private val miniBarTracks = listOf(
    MiniBarTrack("Midnight City", "M83", "4:03", Color(0xFF6258D9)),
    MiniBarTrack("Intro", "The xx", "2:07", Color(0xFF0B9581)),
    MiniBarTrack("Sunset Lover", "Petit Biscuit", "3:58", Color(0xFFE17B56)),
    MiniBarTrack("Night Drive", "Chromatics", "4:21", Color(0xFFC45182)),
    MiniBarTrack("A Walk", "Tycho", "5:12", Color(0xFF2D77B8)),
    MiniBarTrack("Open Eye Signal", "Jon Hopkins", "7:17", Color(0xFF398A65)),
    MiniBarTrack("Awake", "Tycho", "4:44", Color(0xFFA56D31)),
    MiniBarTrack("Weightless", "Marconi Union", "8:00", Color(0xFF596A72))
)

@Composable
fun MiniBarContent() {
    val isLightTheme = !isSystemInDarkTheme()
    val contentColor = if (isLightTheme) Color.Black else Color.White
    val secondaryColor = contentColor.copy(alpha = 0.58f)
    val listState = rememberLazyListState()
    var selectedTrackIndex by remember { mutableIntStateOf(0) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(true) }
    var isNowPlaying by remember { mutableStateOf(false) }
    val selectedTrack = miniBarTracks[selectedTrackIndex]
    val density = LocalDensity.current
    val trackExtentPx = with(density) { 84.dp.toPx() }
    val collapseStartPx = with(density) { 24.dp.toPx() }
    val collapseRangePx = with(density) { 112.dp.toPx() }
    var headerExtentPx by remember(density) {
        mutableFloatStateOf(with(density) { 112.dp.toPx() })
    }

    LaunchedEffect(listState, density) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo
                .firstOrNull { it.index == 0 }
                ?.size
        }.collect { headerSize ->
            if (headerSize != null) {
                headerExtentPx = headerSize + with(density) { 12.dp.toPx() }
            }
        }
    }

    val scrollDistance by remember {
        derivedStateOf {
            val firstIndex = listState.firstVisibleItemIndex
            val firstOffset = listState.firstVisibleItemScrollOffset.toFloat()
            if (firstIndex == 0) {
                firstOffset
            } else {
                headerExtentPx + (firstIndex - 1) * trackExtentPx + firstOffset
            }
        }
    }
    val rawDockProgress by remember {
        derivedStateOf {
            ((scrollDistance - collapseStartPx) / collapseRangePx).coerceIn(0f, 1f)
        }
    }
    // The list is already the animation driver. An additional tween makes the
    // dock chase the scroll offset and causes visible jitter during a fling.
    val dockProgress = if (isNowPlaying) 0f else rawDockProgress

    BackHandler(isNowPlaying) { isNowPlaying = false }

    BackdropDemoScaffold { backdrop ->
        BoxWithConstraints(Modifier.fillMaxSize()) {
            if (isNowPlaying) {
                NowPlayingContent(track = selectedTrack, isPlaying = isPlaying, backdrop = backdrop, contentColor = contentColor, secondaryColor = secondaryColor, onBack = { isNowPlaying = false }, onTogglePlay = { isPlaying = !isPlaying })
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp, 38.dp, 16.dp, 182.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        BasicText("Mini Bar", style = TextStyle(contentColor, 30.sp, FontWeight.Medium))
                        BasicText("Scroll the queue to dock the player into the tab bar.", Modifier.padding(top = 8.dp, bottom = 20.dp), style = TextStyle(secondaryColor, 15.sp))
                        BasicText("Up next", style = TextStyle(contentColor, 20.sp, FontWeight.Medium))
                    }
                    items(miniBarTracks, key = { it.title }) { track ->
                        MiniBarQueueItem(track, track == selectedTrack, contentColor, secondaryColor) {
                            selectedTrackIndex = miniBarTracks.indexOf(track)
                            isPlaying = true
                        }
                    }
                }

                val availableWidth = maxWidth
                Box(Modifier.fillMaxSize()) {
                    // Search leaves the original tab row immediately. The
                    // detached button itself can still follow the scroll progress.
                    val isSearchDetached = dockProgress > 0f
                    val expandedPlayerWidth = availableWidth - 32.dp
                    val compactPlayerWidth = availableWidth - 164.dp
                    val playerWidth = expandedPlayerWidth + (compactPlayerWidth - expandedPlayerWidth) * dockProgress
                    val playerHeight = 58.dp
                    val playerStart = 16.dp + (82.dp - 16.dp) * dockProgress
                    val playerBottom = 88.dp + (16.dp - 88.dp) * dockProgress

                    LiquidBottomTabs(
                        selectedTabIndex = { selectedTabIndex },
                        onTabSelected = { selectedTabIndex = it },
                        backdrop = backdrop,
                        tabsCount = 5,
                        compactProgress = { dockProgress },
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    ) {
                        repeat(5) { index ->
                            LiquidBottomTab(
                                onClick = { selectedTabIndex = index },
                                index = index,
                                isSelected = index == selectedTabIndex,
                                forceCollapse = index == 4,
                                collapseProgress =
                                    if (index == 4 && isSearchDetached) 1f else dockProgress,
                                modifier = Modifier.graphicsLayer {
                                    alpha = if (index == 4) {
                                        if (isSearchDetached) 0f else 1f
                                    } else if (index == selectedTabIndex) {
                                        1f
                                    } else {
                                        1f - (dockProgress * 2f).coerceIn(0f, 1f)
                                    }
                                }
                            ) {
                                MiniBarTabIcon(index, contentColor)
                                if (dockProgress < 0.5f) {
                                    BasicText(
                                        listOf("Home", "Library", "Browse", "Radio", "Search")[index],
                                        Modifier.graphicsLayer {
                                            alpha = 1f - (dockProgress * 2f).coerceIn(0f, 1f)
                                        },
                                        style = TextStyle(contentColor, 11.sp)
                                    )
                                }
                            }
                        }
                    }
                    MiniPlayerSurface(
                        track = selectedTrack,
                        isPlaying = isPlaying,
                        progress = dockProgress,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = playerStart, bottom = playerBottom)
                            .width(playerWidth)
                            .height(playerHeight),
                        backdrop = backdrop,
                        contentColor = contentColor,
                        secondaryColor = secondaryColor,
                        onOpen = { isNowPlaying = true },
                        onTogglePlay = { isPlaying = !isPlaying }
                    )
                    SearchButton(
                        backdrop = backdrop,
                        contentColor = contentColor,
                        onClick = { selectedTabIndex = 4 },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 16.dp)
                            .graphicsLayer { alpha = dockProgress }
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniBarQueueItem(track: MiniBarTrack, isSelected: Boolean, contentColor: Color, secondaryColor: Color, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedRectangle(22.dp))
            .background(if (isSelected) track.color.copy(alpha = 0.24f) else Color.White.copy(alpha = 0.1f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AlbumArt(track.color, Modifier.size(48.dp))
        Column(Modifier.padding(start = 12.dp).weight(1f)) {
            BasicText(track.title, style = TextStyle(contentColor, 16.sp, FontWeight.Medium), maxLines = 1)
            BasicText(track.artist, style = TextStyle(secondaryColor, 13.sp), maxLines = 1)
        }
        BasicText(track.duration, style = TextStyle(secondaryColor, 13.sp))
    }
}

@Composable
private fun MiniPlayerSurface(track: MiniBarTrack, isPlaying: Boolean, progress: Float, modifier: Modifier, backdrop: Backdrop, contentColor: Color, secondaryColor: Color, onOpen: () -> Unit, onTogglePlay: () -> Unit) {
    val albumArtSize = 44.dp + (40.dp - 44.dp) * progress
    Box(
        modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { Capsule() },
            effects = { vibrancy(); blur(8.dp.toPx()); lens(16.dp.toPx(), 28.dp.toPx(), chromaticAberration = true) },
            shadow = { Shadow(alpha = 0.18f) },
            innerShadow = { InnerShadow(radius = 5.dp, alpha = 0.25f) },
            onDrawSurface = { drawRect(Color.White.copy(alpha = 0.2f)) }
        ).clip(Capsule()).clickable(onClick = onOpen).padding(6.dp)
    ) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            AlbumArt(track.color, Modifier.size(albumArtSize))
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                BasicText(track.title, style = TextStyle(contentColor, 14.sp, FontWeight.Medium), maxLines = 1)
                BasicText(track.artist, style = TextStyle(secondaryColor, 11.sp), maxLines = 1)
            }
            Box(Modifier.size(42.dp).clickable(onClick = onTogglePlay), contentAlignment = Alignment.Center) { PlayPauseIcon(isPlaying, contentColor) }
        }
    }
}

@Composable
private fun MiniBarTabDock(
    selectedTabIndex: Int,
    progress: Float,
    backdrop: Backdrop,
    contentColor: Color,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier
) {
    BoxWithConstraints(modifier) {
        val dockWidth = maxWidth + (58.dp - maxWidth) * progress
        Box(
            Modifier
                .width(dockWidth)
                .height(64.dp)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { Capsule() },
                    effects = { vibrancy(); blur(8.dp.toPx()); lens(24.dp.toPx(), 24.dp.toPx()) },
                    shadow = { Shadow(alpha = 0.18f) },
                    innerShadow = { InnerShadow(radius = 5.dp, alpha = 0.25f) },
                    onDrawSurface = { drawRect(Color.White.copy(alpha = 0.2f)) }
                )
                .clip(Capsule())
        ) {
            val tabWidth = (dockWidth - 8.dp) / 5f
            Box(
                Modifier
                    .padding(4.dp)
                    .width(tabWidth)
                    .height(56.dp)
                    .offset(x = tabWidth * selectedTabIndex)
                    .graphicsLayer { alpha = 1f - progress }
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { Capsule() },
                        effects = { vibrancy(); blur(4.dp.toPx()); lens(10.dp.toPx(), 14.dp.toPx()) },
                        highlight = { Highlight.Default },
                        onDrawSurface = { drawRect(Color.White.copy(alpha = 0.14f)) }
                    )
            )
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(4.dp)
                    .graphicsLayer { alpha = 1f - progress },
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(5) { index ->
                    LiquidBottomTab({ onTabSelected(index) }) {
                        MiniBarTabIcon(index, contentColor)
                        BasicText(
                            listOf("Home", "Library", "Browse", "Radio", "Search")[index],
                            style = TextStyle(contentColor, 11.sp)
                        )
                    }
                }
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = progress }
                    .clickable { onTabSelected(selectedTabIndex) },
                contentAlignment = Alignment.Center
            ) {
                MiniBarTabIcon(selectedTabIndex, contentColor)
            }
        }
    }
}

@Composable
private fun DockedMiniPlayer(track: MiniBarTrack, isPlaying: Boolean, alpha: Float, modifier: Modifier, backdrop: Backdrop, contentColor: Color, secondaryColor: Color, onOpen: () -> Unit, onTogglePlay: () -> Unit) {
    Box(
        modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { Capsule() },
            effects = { vibrancy(); blur(8.dp.toPx()); lens(16.dp.toPx(), 28.dp.toPx(), chromaticAberration = true) },
            shadow = { Shadow(alpha = 0.18f) },
            innerShadow = { InnerShadow(radius = 5.dp, alpha = 0.25f) },
            onDrawSurface = { drawRect(Color.White.copy(alpha = 0.2f)) }
        ).graphicsLayer { this.alpha = alpha }.clip(Capsule()).clickable(onClick = onOpen).padding(8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            AlbumArt(track.color, Modifier.size(42.dp))
            Column(Modifier.padding(start = 8.dp).weight(1f)) {
                BasicText(track.title, style = TextStyle(contentColor, 12.sp, FontWeight.Medium), maxLines = 1)
                BasicText(track.artist, style = TextStyle(secondaryColor, 10.sp), maxLines = 1)
            }
            Box(Modifier.size(32.dp).clickable(onClick = onTogglePlay), contentAlignment = Alignment.Center) {
                PlayPauseIcon(isPlaying, contentColor)
            }
        }
    }
}

@Composable
private fun CompactTabButton(selectedTabIndex: Int, backdrop: Backdrop, contentColor: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .size(58.dp)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { Capsule() },
                effects = { vibrancy(); blur(8.dp.toPx()); lens(16.dp.toPx(), 28.dp.toPx()) },
                shadow = { Shadow(alpha = 0.18f) },
                innerShadow = { InnerShadow(radius = 5.dp, alpha = 0.25f) },
                onDrawSurface = { drawRect(Color.White.copy(alpha = 0.2f)) }
            )
            .clip(Capsule())
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        MiniBarTabIcon(selectedTabIndex, contentColor)
    }
}

@Composable
private fun SearchButton(backdrop: Backdrop, contentColor: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(58.dp)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { Capsule() },
                effects = { vibrancy(); blur(8.dp.toPx()); lens(16.dp.toPx(), 28.dp.toPx()) },
                shadow = { Shadow(alpha = 0.18f) },
                innerShadow = { InnerShadow(radius = 5.dp, alpha = 0.25f) },
                onDrawSurface = { drawRect(Color.White.copy(alpha = 0.2f)) }
            )
            .clip(Capsule())
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        MiniBarTabIcon(4, contentColor)
    }
}

@Composable
private fun NowPlayingContent(track: MiniBarTrack, isPlaying: Boolean, backdrop: Backdrop, contentColor: Color, secondaryColor: Color, onBack: () -> Unit, onTogglePlay: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            BasicText("‹", Modifier.clickable(onClick = onBack), style = TextStyle(contentColor, 38.sp))
            Spacer(Modifier.weight(1f))
            BasicText("NOW PLAYING", style = TextStyle(secondaryColor, 12.sp, FontWeight.Medium))
            Spacer(Modifier.weight(1f))
            BasicText("•••", style = TextStyle(contentColor, 22.sp))
        }
        Spacer(Modifier.weight(1f))
        AlbumArt(track.color, Modifier.size(280.dp))
        BasicText(track.title, Modifier.padding(top = 28.dp), style = TextStyle(contentColor, 25.sp, FontWeight.Medium))
        BasicText(track.artist, Modifier.padding(top = 8.dp), style = TextStyle(secondaryColor, 16.sp))
        Spacer(Modifier.height(28.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            BasicText("1:12", style = TextStyle(secondaryColor, 11.sp))
            Spacer(Modifier.width(8.dp))
            Box(Modifier.weight(1f).height(4.dp).drawBackdrop(backdrop = backdrop, shape = { Capsule() }, effects = { blur(3.dp.toPx()) }, onDrawSurface = { drawRect(contentColor.copy(alpha = 0.35f)) }))
            Spacer(Modifier.width(8.dp))
            BasicText(track.duration, style = TextStyle(secondaryColor, 11.sp))
        }
        Row(Modifier.padding(top = 28.dp), verticalAlignment = Alignment.CenterVertically) {
            BasicText("↶", style = TextStyle(contentColor, 28.sp))
            Spacer(Modifier.width(34.dp))
            Box(Modifier.size(64.dp).clickable(onClick = onTogglePlay), contentAlignment = Alignment.Center) { PlayPauseIcon(isPlaying, contentColor) }
            Spacer(Modifier.width(34.dp))
            BasicText("↷", style = TextStyle(contentColor, 28.sp))
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun AlbumArt(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.clip(RoundedRectangle(14.dp))) {
        drawRect(color)
        drawCircle(Color.White.copy(alpha = 0.22f), radius = size.minDimension * 0.3f, center = Offset(size.width * 0.72f, size.height * 0.3f))
        drawLine(Color.White.copy(alpha = 0.7f), Offset(size.width * 0.18f, size.height * 0.78f), Offset(size.width * 0.78f, size.height * 0.18f), strokeWidth = size.minDimension * 0.08f, cap = StrokeCap.Round)
    }
}

@Composable
private fun PlayPauseIcon(isPlaying: Boolean, color: Color) {
    Canvas(Modifier.size(22.dp)) {
        if (isPlaying) {
            drawLine(color, Offset(size.width * 0.3f, size.height * 0.22f), Offset(size.width * 0.3f, size.height * 0.78f), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
            drawLine(color, Offset(size.width * 0.7f, size.height * 0.22f), Offset(size.width * 0.7f, size.height * 0.78f), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
        } else {
            val path = Path().apply { moveTo(size.width * 0.3f, size.height * 0.18f); lineTo(size.width * 0.76f, size.height * 0.5f); lineTo(size.width * 0.3f, size.height * 0.82f); close() }
            drawPath(path, color)
        }
    }
}

@Composable
private fun MiniBarTabIcon(index: Int, color: Color) {
    Canvas(Modifier.size(22.dp)) {
        when (index) {
            0 -> {
                val path = Path().apply {
                    moveTo(size.width * 0.16f, size.height * 0.48f)
                    lineTo(size.width * 0.5f, size.height * 0.18f)
                    lineTo(size.width * 0.84f, size.height * 0.48f)
                    lineTo(size.width * 0.78f, size.height * 0.48f)
                    lineTo(size.width * 0.78f, size.height * 0.84f)
                    lineTo(size.width * 0.22f, size.height * 0.84f)
                    lineTo(size.width * 0.22f, size.height * 0.48f)
                }
                drawPath(path, color, style = Stroke(2.2.dp.toPx(), cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
            }
            1 -> {
                drawRoundRect(color, topLeft = Offset(size.width * 0.18f, size.height * 0.18f), size = androidx.compose.ui.geometry.Size(size.width * 0.64f, size.height * 0.64f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()), style = Stroke(2.2.dp.toPx()))
                drawLine(color, Offset(size.width * 0.32f, size.height * 0.18f), Offset(size.width * 0.32f, size.height * 0.82f), strokeWidth = 2.dp.toPx())
                drawLine(color, Offset(size.width * 0.68f, size.height * 0.18f), Offset(size.width * 0.68f, size.height * 0.82f), strokeWidth = 2.dp.toPx())
            }
            2 -> {
                drawCircle(color, radius = size.minDimension * 0.34f, center = Offset(size.width / 2f, size.height / 2f), style = Stroke(2.2.dp.toPx()))
                drawLine(color, Offset(size.width * 0.5f, size.height * 0.18f), Offset(size.width * 0.5f, size.height * 0.5f), strokeWidth = 2.2.dp.toPx(), cap = StrokeCap.Round)
                drawLine(color, Offset(size.width * 0.5f, size.height * 0.5f), Offset(size.width * 0.72f, size.height * 0.68f), strokeWidth = 2.2.dp.toPx(), cap = StrokeCap.Round)
            }
            3 -> {
                drawArc(color, 205f, 130f, false, style = Stroke(2.2.dp.toPx(), cap = StrokeCap.Round))
                drawArc(color, 215f, 110f, false, style = Stroke(2.2.dp.toPx(), cap = StrokeCap.Round))
                drawCircle(color, size.minDimension * 0.1f, Offset(size.width / 2f, size.height * 0.72f))
            }
            else -> {
                drawCircle(color, radius = size.minDimension * 0.28f, center = Offset(size.width * 0.42f, size.height * 0.42f), style = Stroke(2.2.dp.toPx()))
                drawLine(color, Offset(size.width * 0.63f, size.height * 0.63f), Offset(size.width * 0.84f, size.height * 0.84f), strokeWidth = 2.2.dp.toPx(), cap = StrokeCap.Round)
            }
        }
    }
}
