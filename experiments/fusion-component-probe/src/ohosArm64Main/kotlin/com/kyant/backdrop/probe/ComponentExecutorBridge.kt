@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.kyant.backdrop.probe

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import cnames.structs.napi_env__
import kotlinx.cinterop.CFunction
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.FloatVar
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.interpretCPointer
import kotlinx.cinterop.invoke
import kotlinx.cinterop.rawValue
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.impl.use
import platform.posix.RTLD_LOCAL
import platform.posix.RTLD_NOW
import platform.posix.dlerror
import platform.posix.dlopen
import platform.posix.dlsym
import kotlin.math.abs
import kotlin.time.measureTime

private typealias InitComponent = CFunction<(CPointer<napi_env__>?) -> Int>
private typealias RenderComponent = CFunction<(
    CPointer<IntVar>?, Int, Int, Int, Float, Long, Int, Int, CPointer<IntVar>?
) -> Int>
private typealias InputComponent = CFunction<(Int, Int, Float, Float, Long) -> Int>
private typealias BoundsComponent = CFunction<(Int, Int, CPointer<FloatVar>?) -> Int>
private typealias CloseComponent = CFunction<() -> Int>

internal data class ComponentSource(val width: Int, val height: Int, val density: Float, val pixels: IntArray)
internal data class ComponentFrame(
    val image: ImageBitmap,
    val selected: Int,
    val pixels: IntArray,
    val error: ComponentError?,
    val cpuUs: Long,
    val gpuUs: Long,
    val imageUs: Long,
)

internal class ComponentExecutor(env: CPointer<napi_env__>) {
    // Keep the isolated Kotlin runtime loaded until process exit, even after its scenes close.
    private val library = checkNotNull(dlopen("libglass_component.so", RTLD_NOW or RTLD_LOCAL)) {
        "Component library load failed: ${dlerror()?.toKString()}"
    }
    private val initialize = symbol("glass_component_init").reinterpret<InitComponent>()
    private val render = symbol("glass_component_render").reinterpret<RenderComponent>()
    private val input = symbol("glass_component_input").reinterpret<InputComponent>()
    private val bounds = symbol("glass_component_bounds").reinterpret<BoundsComponent>()
    private val close = symbol("glass_component_close").reinterpret<CloseComponent>()
    private var cpuPixels = IntArray(0)
    private var gpuPixels = IntArray(0)
    private var closed = false

    init {
        check(initialize(env) == 0) { "Component initialization failed" }
        println("GlassComponentProbe ABI symbols=5 load=RTLD_NOW_LOCAL objectsCrossed=none")
    }

    private fun symbol(name: String) = checkNotNull(dlsym(library, name)) {
        "Component symbol $name missing: ${dlerror()?.toKString()}"
    }

    fun frame(source: ComponentSource, time: Long, dark: Boolean, compare: Boolean = true): ComponentFrame {
        check(!closed)
        val count = source.width * source.height
        if (cpuPixels.size != count) {
            cpuPixels = IntArray(count)
            gpuPixels = IntArray(count)
        }
        val info = componentInfo(source.width, source.height)
        val output = Bitmap()
        var cpuSelected = -1
        var gpuSelected = -1
        var cpuUs = 0L
        var gpuUs = 0L
        var imageUs = 0L
        try {
            imageUs = measureTime { check(output.allocPixels(info)) }.inWholeMicroseconds
            source.pixels.usePinned { pixels ->
                if (compare) {
                    cpuUs = measureTime {
                        cpuPixels.usePinned {
                            cpuSelected = render(pixels.addressOf(0), source.width, source.height, source.width * 4,
                                source.density, time, if (dark) 1 else 0, 0, it.addressOf(0))
                        }
                    }.inWholeMicroseconds
                    check(cpuSelected in 0..3) { "CPU component render failed: $cpuSelected" }
                }
                gpuUs = measureTime {
                    checkNotNull(output.peekPixels()).use {
                        gpuSelected = render(pixels.addressOf(0), source.width, source.height, it.rowBytes,
                            source.density, time, if (dark) 1 else 0, 1,
                            checkNotNull(interpretCPointer<IntVar>(it.addr)))
                    }
                }.inWholeMicroseconds
            }
            check(gpuSelected in 0..3) { "GPU component render failed: $gpuSelected" }
            if (compare) {
                check(cpuSelected == gpuSelected) { "CPU/GPU selected tab mismatch: $cpuSelected/$gpuSelected" }
                readComponentPixels(output, info, gpuPixels)
            }
            lateinit var image: ImageBitmap
            imageUs += measureTime {
                output.notifyPixelsChanged()
                image = output.asComposeImageBitmap()
            }.inWholeMicroseconds
            return ComponentFrame(image, gpuSelected, if (compare) gpuPixels else IntArray(0),
                if (compare) componentError(cpuPixels, gpuPixels, source.pixels) else null,
                cpuUs, gpuUs, imageUs)
        } catch (error: Throwable) {
            output.close()
            throw error
        }
    }

    fun tabBounds(index: Int, mode: Int = 1): Rect {
        check(!closed)
        val values = FloatArray(4)
        values.usePinned { check(bounds(mode, index, it.addressOf(0)) == 0) { "Tab $index semantics bounds missing" } }
        return Rect(values[0], values[1], values[2], values[3]).also {
            check(it.width > 0f && it.height > 0f)
        }
    }

    fun tabCenter(index: Int): Offset {
        val gpuBounds = tabBounds(index)
        val cpuBounds = tabBounds(index, 0)
        check(abs(gpuBounds.left - cpuBounds.left) < 1f && abs(gpuBounds.top - cpuBounds.top) < 1f &&
            abs(gpuBounds.right - cpuBounds.right) < 1f && abs(gpuBounds.bottom - cpuBounds.bottom) < 1f)
        println("GlassComponentProbe INPUT_TREE tab=$index bounds=$gpuBounds point=${gpuBounds.center} source=original-semantics")
        return gpuBounds.center
    }

    fun pointer(event: Int, position: Offset, time: Long, compare: Boolean = true) {
        check(!closed)
        if (compare) check(input(0, event, position.x, position.y, time) == 0)
        check(input(1, event, position.x, position.y, time) == 0)
    }

    fun close() {
        if (closed) return
        val result = close.invoke()
        closed = true
        println("GlassComponentProbe CLEANUP result=$result")
        check(result == 0)
    }
}

internal fun componentInfo(width: Int, height: Int) =
    ImageInfo(width, height, ColorType.BGRA_8888, ColorAlphaType.UNPREMUL, ColorSpace.sRGB)

internal fun readComponentPixels(bitmap: Bitmap, info: ImageInfo, output: IntArray) {
    check(output.size == info.width * info.height)
    checkNotNull(bitmap.peekPixels()).use { pixels ->
        output.usePinned { check(pixels.readPixels(info, it.addressOf(0).rawValue, info.width * 4)) }
    }
}

internal data class ComponentError(val outliers: Int, val active: Int, val max: Int) {
    val passed: Boolean get() = active > 100 && outliers * 100 <= active
}

// Compare visible SrcOver colors on the actual source plus alpha, not undefined RGB at alpha zero.
private fun componentError(cpu: IntArray, gpu: IntArray, source: IntArray): ComponentError {
    var outliers = 0
    var active = 0
    var max = 0
    for (index in cpu.indices) {
        val a = cpu[index] ushr 24
        val b = gpu[index] ushr 24
        if (a == 0 && b == 0) continue
        active++
        var error = abs(a - b)
        for (shift in 0..16 step 8) {
            val background = (source[index] ushr shift) and 255
            val ca = (((cpu[index] ushr shift) and 255) * a + background * (255 - a) + 127) / 255
            val cb = (((gpu[index] ushr shift) and 255) * b + background * (255 - b) + 127) / 255
            error = maxOf(error, abs(ca - cb))
        }
        if (error > 3) outliers++
        max = maxOf(max, error)
    }
    return ComponentError(outliers, active, max)
}
