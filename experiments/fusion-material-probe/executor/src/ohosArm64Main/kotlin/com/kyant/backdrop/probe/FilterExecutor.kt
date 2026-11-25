@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class, kotlinx.cinterop.ExperimentalForeignApi::class)

package com.kyant.backdrop.probe

import com.kyant.backdrop.internal.RoundedRectRefractionShaderString
import com.kyant.backdrop.internal.RoundedRectRefractionWithDispersionShaderString
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.interpretCPointer
import kotlinx.cinterop.plus
import kotlinx.cinterop.rawValue
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Pixmap
import org.jetbrains.skia.Rect
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder
import org.jetbrains.skia.impl.use
import platform.posix.memcpy
import platform.posix.pthread_self
import kotlin.native.CName

private val refraction by lazy { RuntimeEffect.makeForShader(RoundedRectRefractionShaderString) }
private val dispersion by lazy { RuntimeEffect.makeForShader(RoundedRectRefractionWithDispersionShaderString) }
private var gpu: MaterialGpu? = null
private var transferBuffers: TransferBuffers? = null

// Only scalar values and pixel buffers cross this ABI; no renderer-owned C++ object does.
@CName("glass_material_render")
fun renderMaterial(
    input: CPointer<IntVar>?,
    width: Int,
    height: Int,
    amount: Float,
    mode: Int,
    output: CPointer<IntVar>?,
): Int {
    if (mode == 6) {
        return try {
            gpu?.close()
            gpu = null
            transferBuffers?.close()
            transferBuffers = null
            0
        } catch (error: Throwable) {
            println("GlassMaterialExecutor GPU_CLOSE_ERROR ${error.message}")
            -2
        }
    }
    if (input == null || output == null || width !in 1..2048 || height !in 1..2048 || mode !in 0..5) return -1
    return try {
        val byteCount = width * height * 4
        val bytes = ByteArray(byteCount)
        bytes.usePinned { memcpy(it.addressOf(0), input, byteCount.toULong()) }
        val info = ImageInfo(width, height, ColorType.BGRA_8888, ColorAlphaType.UNPREMUL, ColorSpace.sRGB)
        Bitmap().use { bitmap ->
            check(bitmap.installPixels(info, bytes, width * 4))
            Bitmap().use { result ->
                check(result.allocN32Pixels(width, height))
                if (mode >= 3) {
                    val executor = gpu ?: MaterialGpu.create().also { gpu = it }
                    executor.render(width, height, result, mode - 3) { canvas ->
                        drawMaterial(canvas, bitmap, width, height, amount, mode - 3)
                    }
                } else {
                    Canvas(result).use { canvas -> drawMaterial(canvas, bitmap, width, height, amount, mode) }
                }
                val rendered = checkNotNull(result.readPixels(info, width * 4, 0, 0))
                rendered.usePinned { memcpy(output, it.addressOf(0), byteCount.toULong()) }
            }
        }
        0
    } catch (error: Throwable) {
        println("GlassMaterialExecutor ERROR ${error::class.simpleName}: ${error.message}")
        -2
    }
}

// Buffers are borrowed only for this synchronous call; neither renderer's objects cross the ABI.
@CName("glass_material_render_direct")
fun renderMaterialDirect(
    input: CPointer<IntVar>?,
    width: Int,
    height: Int,
    inputRowBytes: Int,
    outputRowBytes: Int,
    amount: Float,
    mode: Int,
    output: CPointer<IntVar>?,
): Int {
    if (input == null || output == null || width !in 1..2048 || height !in 1..2048 || mode !in 0..2 ||
        inputRowBytes !in width * 4..32768 || outputRowBytes !in width * 4..32768 || !amount.isFinite()) return -1
    return try {
        var buffers = transferBuffers
        buffers?.checkThread()
        if (buffers == null || buffers.width != width || buffers.height != height) {
            buffers?.close()
            transferBuffers = null
            buffers = TransferBuffers(width, height)
            transferBuffers = buffers
        }
        val source = input.reinterpret<ByteVar>()
        val target = checkNotNull(interpretCPointer<ByteVar>(buffers.source.pixels.addr))
        val targetRowBytes = buffers.source.pixels.rowBytes
        if (inputRowBytes == width * 4 && targetRowBytes == inputRowBytes) {
            memcpy(target, source, (width * height * 4).toULong())
        } else {
            for (row in 0 until height) {
                memcpy(target + row * targetRowBytes, source + row * inputRowBytes, (width * 4).toULong())
            }
        }
        buffers.source.bitmap.notifyPixelsChanged()
        val executor = gpu ?: MaterialGpu.create().also { gpu = it }
        executor.render(width, height, buffers.result.bitmap, mode) { canvas ->
            drawMaterial(canvas, buffers.source.bitmap, width, height, amount, mode)
        }
        check(buffers.result.pixels.readPixels(buffers.info, output.rawValue, outputRowBytes)) {
            "Direct output conversion failed"
        }
        0
    } catch (error: Throwable) {
        println("GlassMaterialExecutor DIRECT_ERROR ${error::class.simpleName}: ${error.message}")
        -2
    }
}

private class TransferBitmap(info: ImageInfo) {
    val bitmap = Bitmap()
    val pixels: Pixmap

    init {
        try {
            check(bitmap.allocPixels(info)) { "Transfer bitmap allocation failed" }
            pixels = checkNotNull(bitmap.peekPixels()) { "Transfer bitmap pixels unavailable" }
        } catch (error: Throwable) {
            bitmap.close()
            throw error
        }
    }

    fun close() {
        pixels.close()
        bitmap.close()
    }
}

private class TransferBuffers(val width: Int, val height: Int) {
    private val thread = pthread_self()
    val info = ImageInfo(width, height, ColorType.BGRA_8888, ColorAlphaType.UNPREMUL, ColorSpace.sRGB)
    val source = TransferBitmap(info)
    val result = try {
        // Preserve the old premultiplied readback and CPU conversion, including transparent pixels.
        TransferBitmap(ImageInfo.makeN32Premul(width, height))
    } catch (error: Throwable) {
        source.close()
        throw error
    }

    init {
        println("GlassMaterialExecutor TRANSFER_BUFFERS size=${width}x$height reusedNativeBitmaps=2")
    }

    fun checkThread() {
        check(pthread_self() == thread) { "Transfer buffers used on another thread" }
    }

    fun close() {
        checkThread()
        result.close()
        source.close()
        println("GlassMaterialExecutor TRANSFER_BUFFERS_CLOSED")
    }
}

private fun drawMaterial(canvas: Canvas, bitmap: Bitmap, width: Int, height: Int, amount: Float, mode: Int) {
    canvas.clear(0)
    bitmap.makeShader().use { source ->
        Paint().use { paint ->
            paint.shader = source
            if (mode == 0) {
                canvas.drawRect(Rect.makeWH(width.toFloat(), height.toFloat()), paint)
            } else {
                RuntimeShaderBuilder(if (mode == 2) dispersion else refraction).use { builder ->
                    val radius = minOf(width, height) * 0.2f
                    builder.uniform("size", width.toFloat(), height.toFloat())
                    builder.uniform("offset", 0f, 0f)
                    builder.uniform("cornerRadii", radius, radius, radius, radius)
                    builder.uniform("refractionHeight", minOf(width, height) * 0.15f)
                    builder.uniform("refractionAmount", -amount)
                    builder.uniform("depthEffect", if (mode == 2) 1f else 0f)
                    if (mode == 2) builder.uniform("chromaticAberration", 1f)
                    ImageFilter.makeRuntimeShader(builder, "content", null).use { filter ->
                        paint.imageFilter = filter
                        canvas.drawRect(Rect.makeWH(width.toFloat(), height.toFloat()), paint)
                    }
                }
            }
        }
    }
}
