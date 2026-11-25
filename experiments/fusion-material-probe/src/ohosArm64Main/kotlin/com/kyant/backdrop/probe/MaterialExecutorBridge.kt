@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.kyant.backdrop.probe

import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import kotlinx.cinterop.CFunction
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.invoke
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import platform.posix.RTLD_LOCAL
import platform.posix.RTLD_NOW
import platform.posix.dlerror
import platform.posix.dlopen
import platform.posix.dlsym
import platform.posix.memcpy
import kotlin.time.measureTime

internal typealias MaterialFunction = CFunction<(
    CPointer<IntVar>?, Int, Int, Float, Int, CPointer<IntVar>?
) -> Int>

internal suspend fun verifyMaterialExecutor(
    layer: GraphicsLayer,
    selectFrame: suspend (Int) -> Unit,
    show: (ImageBitmap) -> Unit,
    status: (String) -> Unit,
) {
    val library = dlopen("libglass_material.so", RTLD_NOW or RTLD_LOCAL)
    checkNotNull(library) { "Executor load failed: ${dlerror()?.toKString()}" }
    val render = checkNotNull(dlsym(library, "glass_material_render")) {
        "Executor symbol missing: ${dlerror()?.toKString()}"
    }.reinterpret<MaterialFunction>()
    val renderDirect = checkNotNull(dlsym(library, "glass_material_render_direct")) {
        "Direct executor symbol missing: ${dlerror()?.toKString()}"
    }.reinterpret<DirectMaterialFunction>()
    // Kotlin/Native runtimes cannot be unloaded while their GC workers still exist.
    val times = mutableListOf<Long>()
    var previousNormalHash: Int? = null
    var changedFrames = 0
    var validFrames = 0
    repeat(12) { index ->
        selectFrame(index + 90)
        withFrameNanos { }
        withFrameNanos { }
        val source = layer.toImageBitmap()
        val input = IntArray(source.width * source.height)
        source.readPixels(input)
        val outputs = Array(3) { IntArray(input.size) }
        val elapsed = measureTime {
            input.usePinned { sourceBuffer ->
                outputs.forEachIndexed { mode, output ->
                    output.usePinned { resultBuffer ->
                        check(render(sourceBuffer.addressOf(0), source.width, source.height, 24f, mode,
                            resultBuffer.addressOf(0)) == 0) { "Executor failed in mode $mode" }
                    }
                }
            }
        }
        val identityDifference = input.indices.count { input[it] != outputs[0][it] }
        val normalDifference = input.indices.count { input[it] != outputs[1][it] }
        val deepDifference = outputs[1].indices.count { outputs[1][it] != outputs[2][it] }
        val normalHash = outputs[1].contentHashCode()
        if (previousNormalHash != null && previousNormalHash != normalHash) changedFrames++
        previousNormalHash = normalHash
        if (identityDifference == 0 && normalDifference > 100 && deepDifference > 100) validFrames++
        times += elapsed.inWholeMicroseconds
        show(outputs[2].toFusionImage(source.width, source.height))
        status("Original shader frame $index")
        println(
            "GlassMaterialProbe MATERIAL index=$index identityDiff=$identityDifference " +
                "normalDiff=$normalDifference deepVsNormalDiff=$deepDifference hash=$normalHash " +
                "threeModesUs=${elapsed.inWholeMicroseconds}"
        )
    }
    val passed = validFrames == 12 && changedFrames == 11
    status(if (passed) "Original shader gate passed" else "Original shader gate failed")
    println(
        "GlassMaterialProbe MATERIAL_RESULT passed=$passed valid=$validFrames/12 changed=$changedFrames/11 " +
            "threeModesP50Us=${times.sorted()[5]} transport=cpu-bitmap execution=isolated-skia-raster " +
            "shader=original-refraction-and-dispersion gpu=false"
    )
    if (passed) verifyGpuExecutor(render, renderDirect, layer, selectFrame, show, status)
}

private suspend fun verifyGpuExecutor(
    render: CPointer<MaterialFunction>,
    renderDirect: CPointer<DirectMaterialFunction>,
    layer: GraphicsLayer,
    selectFrame: suspend (Int) -> Unit,
    show: (ImageBitmap) -> Unit,
    status: (String) -> Unit,
) {
    val modeTimes = Array(3) { mutableListOf<Long>() }
    val cpuTimes = mutableListOf<Long>()
    var valid = 0
    var previousSource: Int? = null
    var previousNormal: Int? = null
    var previousDeep: Int? = null
    var sourceChanges = 0
    var normalChanges = 0
    var deepChanges = 0
    try {
        repeat(12) { index ->
            selectFrame(index + 120)
            withFrameNanos { }
            withFrameNanos { }
            val source = layer.toImageBitmap()
            val input = IntArray(source.width * source.height)
            source.readPixels(input)
            val cpu = Array(2) { IntArray(input.size) }
            val outputs = Array(3) { IntArray(input.size) }
            var cpuUs = 0L
            input.usePinned { sourceBuffer ->
                val elapsed = measureTime {
                    cpu.forEachIndexed { mode, output ->
                        output.usePinned {
                            check(render(sourceBuffer.addressOf(0), source.width, source.height, 24f, mode + 1,
                                it.addressOf(0)) == 0) { "CPU reference failed" }
                        }
                    }
                }
                cpuUs = elapsed.inWholeMicroseconds
                outputs.forEachIndexed { mode, output ->
                    val gpuElapsed = measureTime {
                        output.usePinned {
                            check(render(sourceBuffer.addressOf(0), source.width, source.height, 24f, mode + 3,
                                it.addressOf(0)) == 0) { "GPU executor failed in mode $mode" }
                        }
                    }
                    modeTimes[mode] += gpuElapsed.inWholeMicroseconds
                }
            }
            cpuTimes += cpuUs
            val identity = pixelError(input, outputs[0])
            val normal = pixelError(cpu[0], outputs[1])
            val deep = pixelError(cpu[1], outputs[2])
            val normalDiff = input.indices.count { input[it] != outputs[1][it] }
            val deepDiff = outputs[1].indices.count { outputs[1][it] != outputs[2][it] }
            val sourceHash = input.contentHashOutsideMarker(source.width, source.height)
            val normalHash = outputs[1].contentHashOutsideMarker(source.width, source.height)
            val deepHash = outputs[2].contentHashOutsideMarker(source.width, source.height)
            if (previousSource != null && previousSource != sourceHash) sourceChanges++
            if (previousNormal != null && previousNormal != normalHash) normalChanges++
            if (previousDeep != null && previousDeep != deepHash) deepChanges++
            previousSource = sourceHash
            previousNormal = normalHash
            previousDeep = deepHash
            if (identity.outliers == 0 && normal.outliers <= input.size / 100 &&
                deep.outliers <= input.size / 100 && normalDiff > 100 && deepDiff > 100) valid++
            show(outputs[2].toFusionImage(source.width, source.height))
            status("GPU original shader frame $index")
            println(
                "GlassMaterialProbe GPU index=$index identityOutliers=${identity.outliers} identityMax=${identity.max} " +
                    "normalOutliers=${normal.outliers} normalMax=${normal.max} deepOutliers=${deep.outliers} deepMax=${deep.max} " +
                    "normalDiff=$normalDiff deepVsNormalDiff=$deepDiff sourceHash=$sourceHash normalHash=$normalHash deepHash=$deepHash " +
                    "cpuTwoModesUs=$cpuUs gpuIdentityUs=${modeTimes[0].last()} " +
                    "gpuRefractionUs=${modeTimes[1].last()} gpuDeepUs=${modeTimes[2].last()}"
            )
        }
        val passed = valid == 12 && sourceChanges == 11 && normalChanges == 11 && deepChanges == 11
        status(if (passed) "Original shader GPU gate passed" else "Original shader GPU gate failed")
        println(
            "GlassMaterialProbe GPU_RESULT passed=$passed valid=$valid/12 sourceChanges=$sourceChanges/11 " +
                "normalChanges=$normalChanges/11 deepChanges=$deepChanges/11 tolerance=3 maxOutlierRatio=0.01 " +
                "cpuTwoModesWarmP50Us=${cpuTimes.drop(1).median()} " +
                "gpuIdentityWarmP50Us=${modeTimes[0].drop(1).median()} " +
                "gpuRefractionWarmP50Us=${modeTimes[1].drop(1).median()} " +
                "gpuDeepWarmP50Us=${modeTimes[2].drop(1).median()} " +
                "transport=cpu-upload-readback execution=isolated-skia-ganesh gpu=true"
        )
        if (passed) verifyDirectPixelTransfer(render, renderDirect, layer, selectFrame, show, status)
    } finally {
        val result = render(null, 0, 0, 0f, 6, null)
        println("GlassMaterialProbe GPU_CLEANUP result=$result")
        check(result == 0) { "GPU executor cleanup failed" }
    }
}

private data class PixelError(val outliers: Int, val max: Int)

private fun pixelError(reference: IntArray, output: IntArray): PixelError {
    var outliers = 0
    var max = 0
    reference.indices.forEach { index ->
        var pixelMax = 0
        for (shift in 0..24 step 8) {
            val error = kotlin.math.abs(((reference[index] ushr shift) and 255) - ((output[index] ushr shift) and 255))
            pixelMax = maxOf(pixelMax, error)
        }
        if (pixelMax > 3) outliers++
        max = maxOf(max, pixelMax)
    }
    return PixelError(outliers, max)
}

internal fun IntArray.contentHashOutsideMarker(width: Int, height: Int): Int {
    var hash = 1
    for (y in 32 until height) {
        for (x in 32 until width) hash = 31 * hash + this[y * width + x]
    }
    return hash
}

private fun List<Long>.median(): Long = sorted()[size / 2]

internal fun IntArray.toFusionImage(width: Int, height: Int): ImageBitmap {
    val bytes = ByteArray(size * 4)
    usePinned { pixels ->
        bytes.usePinned { buffer -> memcpy(buffer.addressOf(0), pixels.addressOf(0), bytes.size.toULong()) }
    }
    val bitmap = Bitmap()
    val info = ImageInfo(width, height, ColorType.BGRA_8888, ColorAlphaType.UNPREMUL, ColorSpace.sRGB)
    check(bitmap.installPixels(info, bytes, width * 4))
    return bitmap.asComposeImageBitmap()
}
