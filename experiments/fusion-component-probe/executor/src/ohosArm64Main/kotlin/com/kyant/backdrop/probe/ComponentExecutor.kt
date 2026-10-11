@file:OptIn(
    kotlinx.cinterop.ExperimentalForeignApi::class,
    kotlin.experimental.ExperimentalNativeApi::class,
    androidx.compose.ui.InternalComposeUiApi::class,
)

package com.kyant.backdrop.probe

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.LocalSystemTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.SystemTheme
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.napi.JsEnv
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.scene.ComposeScene
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.catalog.FlightIcon
import com.kyant.backdrop.catalog.components.LiquidBottomTab
import com.kyant.backdrop.catalog.components.LiquidBottomTabs
import com.kyant.backdrop.internal.InverseLayerScope
import cnames.structs.napi_env__
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.FloatVar
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.interpretCPointer
import kotlinx.cinterop.rawValue
import kotlinx.cinterop.set
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.initMainHandler
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Pixmap
import org.jetbrains.skia.impl.use
import platform.posix.memcpy
import platform.posix.pthread_self
import kotlin.native.CName

private val sessions = arrayOfNulls<ComponentSession>(2)
private var componentGpu: MaterialGpu? = null
private var initialized = false
private var ownerThread = pthread_self()

@CName("glass_component_init")
fun initializeComponent(env: CPointer<napi_env__>?): Int {
    if (env == null) return -1
    return try {
        if (initialized) {
            check(pthread_self() == ownerThread)
        } else {
            ownerThread = pthread_self()
            initMainHandler(env)
            JsEnv.init(env)
            initialized = true
            println("GlassComponentExecutor INIT mainHandler=true jsEnv=true backend=skia")
        }
        0
    } catch (error: Throwable) {
        println("GlassComponentExecutor INIT_ERROR ${error.message}")
        -2
    }
}

// The isolated library owns its complete scene. Only scalar values and borrowed pixels leave it.
@CName("glass_component_render")
fun renderComponent(
    input: CPointer<IntVar>?, width: Int, height: Int, outputRowBytes: Int,
    density: Float, frameTime: Long, dark: Int, mode: Int, output: CPointer<IntVar>?,
): Int {
    if (input == null || output == null || width !in 64..2048 || height !in 64..1024 ||
        outputRowBytes !in width * 4..32768 || !density.isFinite() || density !in 0.5f..6f ||
        dark !in 0..1 || mode !in 0..1 || frameTime < 0) return -1
    return try {
        check(initialized && pthread_self() == ownerThread) { "Component runtime not initialized on this thread" }
        val previous = sessions[mode]
        previous?.checkThread()
        if (previous != null && (previous.width != width || previous.height != height || previous.density != density)) {
            val gpu = componentGpu
            if (gpu != null) gpu.withGpuContext { previous.close() } else previous.close()
            sessions[mode] = null
        }
        val session = sessions[mode] ?: ComponentSession(width, height, density).also { sessions[mode] = it }
        session.update(input, dark != 0)
        if (mode == 1) {
            val gpu = componentGpu ?: MaterialGpu.create().also { componentGpu = it }
            gpu.render(width, height, session.result, 8, trace = session.frames < 3) { canvas ->
                canvas.clear(0)
                session.scene.render(canvas.asComposeCanvas(), frameTime)
            }
        } else {
            Canvas(session.result).use { canvas ->
                canvas.clear(0)
                session.scene.render(canvas.asComposeCanvas(), frameTime)
            }
        }
        session.frames++
        check(session.resultPixels.readPixels(session.info, output.rawValue, outputRowBytes))
        session.selected.intValue
    } catch (error: Throwable) {
        println("GlassComponentExecutor ERROR ${error::class.simpleName}: ${error.message}")
        -2
    }
}

@CName("glass_component_bounds")
fun componentBounds(mode: Int, index: Int, output: CPointer<FloatVar>?): Int {
    if (mode !in 0..1 || index !in 0..3 || output == null) return -1
    return try {
        val session = checkNotNull(sessions[mode])
        session.checkThread()
        val node = session.tabNode(index) ?: return -3
        val bounds = node.boundsInRoot
        check(bounds.width > 0f && bounds.height > 0f)
        output[0] = bounds.left
        output[1] = bounds.top
        output[2] = bounds.right
        output[3] = bounds.bottom
        0
    } catch (error: Throwable) {
        println("GlassComponentExecutor BOUNDS_ERROR ${error.message}")
        -2
    }
}

@CName("glass_component_input")
fun componentInput(mode: Int, event: Int, x: Float, y: Float, time: Long): Int {
    if (mode !in 0..1 || event !in 0..3 || !x.isFinite() || !y.isFinite() || time < 0) return -1
    return try {
        val session = checkNotNull(sessions[mode])
        session.checkThread()
        if (event == 3) {
            session.scene.cancelPointerInput()
        } else {
            session.scene.sendPointerEvent(
                eventType = when (event) {
                    0 -> PointerEventType.Press
                    1 -> PointerEventType.Move
                    else -> PointerEventType.Release
                },
                position = Offset(x, y),
                timeMillis = time / 1_000_000,
                type = PointerType.Touch,
            )
        }
        0
    } catch (error: Throwable) {
        println("GlassComponentExecutor INPUT_ERROR ${error.message}")
        -2
    }
}

@CName("glass_component_close")
fun closeComponent(): Int = try {
    val closeScenes = {
        sessions.forEachIndexed { index, session ->
            session?.close()
            sessions[index] = null
        }
    }
    val gpu = componentGpu
    if (gpu != null) gpu.withGpuContext(closeScenes) else closeScenes()
    gpu?.close()
    componentGpu = null
    println("GlassComponentExecutor CLOSED scenes=0")
    0
} catch (error: Throwable) {
    println("GlassComponentExecutor CLOSE_ERROR ${error.message}")
    -2
}

private class ComponentSession(val width: Int, val height: Int, val density: Float) {
    private val thread = pthread_self()
    val info = ImageInfo(width, height, ColorType.BGRA_8888, ColorAlphaType.UNPREMUL, ColorSpace.sRGB)
    private val source = Bitmap().apply { check(allocPixels(info)) }
    private val sourcePixels: Pixmap = checkNotNull(source.peekPixels())
    val result = Bitmap().apply { check(allocN32Pixels(width, height)) }
    val resultPixels: Pixmap = checkNotNull(result.peekPixels())
    val selected = mutableIntStateOf(0)
    private val sourceGeneration = mutableIntStateOf(0)
    private val dark = mutableIntStateOf(0)
    private val owners = mutableListOf<SemanticsOwner>()
    var frames = 0

    private val backdrop = object : Backdrop {
        override val isCoordinatesDependent = true
        private val inverse = InverseLayerScope()
        private val image = source.asComposeImageBitmap()

        override fun DrawScope.drawBackdrop(
            density: Density, coordinates: LayoutCoordinates?, layerBlock: (GraphicsLayerScope.() -> Unit)?
        ) {
            sourceGeneration.intValue
            val position = coordinates?.positionInRoot() ?: return
            withTransform({
                if (layerBlock != null) {
                    inverse.reset()
                    with(inverse) { inverseTransform(density, layerBlock) }
                }
                translate(-position.x, -position.y)
            }) { drawImage(image) }
        }
    }

    private val platform = object : PlatformContext by PlatformContext.Empty {
        override val semanticsOwnerListener = object : PlatformContext.SemanticsOwnerListener {
            override fun onSemanticsOwnerAppended(semanticsOwner: SemanticsOwner) { owners += semanticsOwner }
            override fun onSemanticsOwnerRemoved(semanticsOwner: SemanticsOwner) { owners -= semanticsOwner }
            override fun onSemanticsChange(semanticsOwner: SemanticsOwner) = Unit
            override fun onLayoutChange(semanticsOwner: SemanticsOwner, semanticsNodeId: Int) = Unit
        }
    }

    val scene: ComposeScene = CanvasLayersComposeScene(
        density = Density(density), size = IntSize(width, height),
        coroutineContext = Dispatchers.Unconfined, platformContext = platform,
    ).also { scene ->
        scene.setContent {
            CompositionLocalProvider(LocalSystemTheme provides if (dark.intValue == 1) SystemTheme.Dark else SystemTheme.Light) {
                val contentColor = if (dark.intValue == 1) Color.White else Color.Black
                val icon = rememberVectorPainter(FlightIcon)
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LiquidBottomTabs(
                        selectedTabIndex = { selected.intValue }, onTabSelected = { selected.intValue = it },
                        backdrop = backdrop, tabsCount = 4,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 36.dp),
                    ) {
                        repeat(4) { index ->
                            LiquidBottomTab(
                                onClick = { selected.intValue = index },
                                modifier = Modifier.testTag("component-tab-$index"),
                            ) {
                                Box(Modifier.size(28.dp).paint(icon, colorFilter = ColorFilter.tint(contentColor)))
                                BasicText("Tab ${index + 1}", style = TextStyle(contentColor, 12.sp))
                            }
                        }
                    }
                }
            }
        }
    }

    init { println("GlassComponentExecutor SCENE size=${width}x$height density=$density originalComponents=true") }

    fun update(input: CPointer<IntVar>, isDark: Boolean) {
        checkThread()
        check(sourcePixels.rowBytes == width * 4)
        val pixels = checkNotNull(interpretCPointer<IntVar>(sourcePixels.addr))
        memcpy(pixels, input, (width * height * 4).toULong())
        source.notifyPixelsChanged()
        sourceGeneration.intValue++
        dark.intValue = if (isDark) 1 else 0
    }

    fun tabNode(index: Int): SemanticsNode? {
        fun find(node: SemanticsNode): SemanticsNode? {
            if (node.config.getOrNull(SemanticsProperties.TestTag) == "component-tab-$index") return node
            for (child in node.children) find(child)?.let { return it }
            return null
        }
        return owners.firstNotNullOfOrNull { find(it.unmergedRootSemanticsNode) }
    }

    fun checkThread() { check(pthread_self() == thread) { "Component scene used on another thread" } }

    fun close() {
        checkThread()
        scene.close()
        resultPixels.close()
        result.close()
        sourcePixels.close()
        source.close()
        owners.clear()
    }
}
