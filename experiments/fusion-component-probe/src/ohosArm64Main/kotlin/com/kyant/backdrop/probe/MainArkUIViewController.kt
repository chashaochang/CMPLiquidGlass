@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.kyant.backdrop.probe

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.napi.JsEnv
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.ComposeArkUIViewController
import cnames.structs.napi_env__
import cnames.structs.napi_value__
import kotlinx.cinterop.CPointer
import kotlinx.coroutines.initMainHandler
import kotlin.math.roundToInt
import kotlin.time.measureTime

fun MainArkUIViewController(env: CPointer<napi_env__>): CPointer<napi_value__> {
    initMainHandler(env)
    JsEnv.init(env)
    println("GlassComponentProbe START backend=fusion-renderer componentBackend=isolated-skia-ganesh")
    return ComposeArkUIViewController(env) { OriginalComponentProbe(env) }
}

private class CropGeometry {
    var sourcePosition = Offset.Unspecified
    var shellPosition = Offset.Unspecified
    var shellSize = IntSize.Zero
    var recordedPhase = -1
    var submittedFrame = -1
    var drawnFrame = -1
}

@Composable
private fun OriginalComponentProbe(env: CPointer<napi_env__>) {
    val background = rememberGraphicsLayer()
    val crop = rememberGraphicsLayer()
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val geometry = remember { CropGeometry() }
    val list = rememberLazyListState()
    var phase by remember { mutableIntStateOf(0) }
    var dark by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Bottom Tabs") }
    var image by remember { mutableStateOf<ImageBitmap?>(null) }
    var tabBounds by remember { mutableStateOf<List<Rect>>(emptyList()) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var live by remember { mutableStateOf(false) }
    var executor by remember { mutableStateOf<ComponentExecutor?>(null) }

    LaunchedEffect(background, crop) {
        var bridge: ComponentExecutor? = null
        var sourcePixels = IntArray(0)
        var displayed = 0
        try {
            repeat(45) { withFrameNanos { } }
            val active = ComponentExecutor(env)
            bridge = active
            executor = active
            suspend fun capture(): CapturedComponentSource {
                val size = geometry.shellSize
                check(size.width > 64 && size.height > 64 && geometry.recordedPhase >= 0)
                check(geometry.sourcePosition != Offset.Unspecified && geometry.shellPosition != Offset.Unspecified)
                val position = geometry.shellPosition - geometry.sourcePosition
                check(position.x >= 0f && position.y >= 0f && position.x + size.width <= background.size.width + 1f &&
                    position.y + size.height <= background.size.height + 1f) { "Measured crop outside source" }
                var snapshot: ImageBitmap? = null
                val exportUs = measureTime {
                    crop.record(density, direction, size) {
                        withTransform({ translate(-position.x, -position.y) }) { drawLayer(background) }
                    }
                    snapshot = crop.toImageBitmap()
                }.inWholeMicroseconds
                val bitmap = checkNotNull(snapshot).asSkiaBitmap()
                var readUs = 0L
                try {
                    val count = size.width * size.height
                    if (sourcePixels.size != count) sourcePixels = IntArray(count)
                    readUs = measureTime {
                        readComponentPixels(bitmap, componentInfo(size.width, size.height), sourcePixels)
                    }.inWholeMicroseconds
                } finally {
                    bitmap.close()
                }
                return CapturedComponentSource(
                    ComponentSource(size.width, size.height, density.density, sourcePixels),
                    geometry.recordedPhase, position, exportUs, readUs,
                )
            }
            suspend fun show(frame: ComponentFrame) {
                image = frame.image
                selectedTab = frame.selected
                tabBounds = (0..3).map { active.tabBounds(it) }
                geometry.submittedFrame = displayed++
                withFrameNanos { }
                withFrameNanos { }
                check(geometry.drawnFrame == geometry.submittedFrame) { "Fusion image draw not acknowledged" }
            }
            val passed = verifyOriginalComponent(
                active, capture = { capture() },
                selectSource = { index, isDark ->
                    dark = isDark
                    phase = index
                    list.scrollToItem(index % 10)
                    withFrameNanos { }
                    withFrameNanos { }
                    check(geometry.recordedPhase == index) { "Fusion source is stale" }
                },
                show = { show(it) },
                status = { status = it },
            )
            if (passed) {
                live = true
                status = "GPU"
                val pipelineTimes = mutableListOf<Long>()
                repeat(60) { index ->
                    phase = index + 300
                    list.scrollToItem(index % 10)
                    withFrameNanos { }
                    withFrameNanos { }
                    val source = capture()
                    val time = withFrameNanos { it }
                    val frame = active.frame(source.source, time, dark, compare = false)
                    pipelineTimes += source.exportUs + source.readUs + frame.gpuUs + frame.imageUs
                    show(frame)
                    println("GlassComponentProbe LIVE index=$index exportUs=${source.exportUs} readUs=${source.readUs} " +
                        "gpuUs=${frame.gpuUs} imageUs=${frame.imageUs} drawn=${geometry.drawnFrame} selected=${frame.selected}")
                }
                println("GlassComponentProbe LIVE_RESULT samples=60 warmPipelineP50Us=${pipelineTimes.drop(2).median()} " +
                    "excludes=display-presentation frameRateMeasured=false zeroCopy=false")
            }
            status = if (passed) "Passed" else "Comparison failed"
            println("GlassComponentProbe RESULT passed=$passed displayed=$displayed originalComponents=true " +
                "fullComponentIsolated=true normalAppChanged=false")
        } catch (error: Throwable) {
            status = "Failed: ${error.message}"
            println("GlassComponentProbe ERROR ${error::class.simpleName}: ${error.message}")
        } finally {
            live = false
            executor = null
            bridge?.close()
        }
    }

    val textColor = if (dark) Color.White else Color.Black
    Box(Modifier.fillMaxSize().background(if (dark) Color(0xFF17191D) else Color(0xFFF5F6F8)).padding(top = 52.dp, bottom = 28.dp)) {
        LazyColumn(
            state = list,
            contentPadding = PaddingValues(top = 80.dp, bottom = 160.dp),
            modifier = Modifier.fillMaxSize()
                .onGloballyPositioned { geometry.sourcePosition = it.positionInRoot() }
                .drawWithContent {
                    val currentPhase = phase
                    background.record {
                        drawRect(if (dark) Color(0xFF17191D) else Color(0xFFF5F6F8))
                        this@drawWithContent.drawContent()
                    }
                    geometry.recordedPhase = currentPhase
                    drawLayer(background)
                },
        ) {
            items((0..40).toList()) { index ->
                val colors = if (dark) listOf(0xFF254558, 0xFF365846, 0xFF4B344B, 0xFF503D34)
                    else listOf(0xFFCCE5F4, 0xFFCDEAD9, 0xFFE9D6E9, 0xFFF1DFD1)
                Column(Modifier.fillMaxWidth().height(88.dp).background(Color(colors[(index + phase) % 4])).padding(24.dp, 14.dp)) {
                    BasicText("Fusion row $index", style = TextStyle(textColor, 18.sp))
                    BasicText("${index * 17 + phase}", style = TextStyle(textColor.copy(alpha = 0.65f), 14.sp))
                }
            }
        }
        BasicText(status, style = TextStyle(textColor, 18.sp),
            modifier = Modifier.align(Alignment.TopStart).padding(24.dp, 16.dp).testTag("component-status"))
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(128.dp)
            .onGloballyPositioned {
                geometry.shellPosition = it.positionInRoot()
                geometry.shellSize = it.size
            }
            .testTag("component-shell")
            .pointerInput(executor, live) {
                val active = executor
                if (active != null && live) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val id = down.id
                        active.pointer(0, down.position, down.uptimeMillis * 1_000_000, compare = false)
                        down.consume()
                        var released = false
                        try {
                            while (!released) {
                                val change = awaitPointerEvent().changes.firstOrNull { it.id == id } ?: break
                                active.pointer(if (change.pressed) 1 else 2, change.position,
                                    change.uptimeMillis * 1_000_000, compare = false)
                                change.consume()
                                released = !change.pressed
                            }
                        } finally {
                            if (!released) active.pointer(3, down.position, down.uptimeMillis * 1_000_000, compare = false)
                        }
                    }
                }
            }) {
            image?.let {
                Image(it, contentDescription = null, contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize().drawWithContent {
                        drawContent()
                        geometry.drawnFrame = geometry.submittedFrame
                    })
            }
            tabBounds.forEachIndexed { index, bounds ->
                Box(Modifier.offset { IntOffset(bounds.left.roundToInt(), bounds.top.roundToInt()) }
                    .size(with(density) { bounds.width.toDp() }, with(density) { bounds.height.toDp() })
                    .testTag("fusion-component-tab-$index")
                    .semantics {
                        role = Role.Tab
                        selected = index == selectedTab
                        contentDescription = "Tab ${index + 1}"
                    })
            }
        }
    }
}

private fun List<Long>.median() = sorted()[size / 2]
