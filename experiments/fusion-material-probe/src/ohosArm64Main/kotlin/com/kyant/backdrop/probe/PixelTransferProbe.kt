@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.kyant.backdrop.probe

import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import kotlinx.cinterop.CFunction
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.interpretCPointer
import kotlinx.cinterop.invoke
import kotlinx.cinterop.rawValue
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.impl.use
import platform.posix.memcmp
import kotlin.time.measureTime

internal typealias DirectMaterialFunction = CFunction<(
    CPointer<IntVar>?, Int, Int, Int, Int, Float, Int, CPointer<IntVar>?
) -> Int>

internal suspend fun verifyDirectPixelTransfer(
    render: CPointer<MaterialFunction>,
    renderDirect: CPointer<DirectMaterialFunction>,
    layer: GraphicsLayer,
    selectFrame: suspend (Int) -> Unit,
    show: (ImageBitmap) -> Unit,
    status: (String) -> Unit,
) {
    verifyPaddedAlphaBuffers(render, renderDirect)
    val oldSamples = mutableListOf<TransferTiming>()
    val newSamples = mutableListOf<TransferTiming>()
    val exportTimes = mutableListOf<Long>()
    var directInput = IntArray(0)
    var actualOutput = IntArray(0)
    var previousSource: Int? = null
    var previousOutput: Int? = null
    var sourceChanges = 0
    var outputChanges = 0
    val previousByMode = arrayOfNulls<Int>(2)
    val changesByMode = IntArray(2)
    var valid = 0
    repeat(24) { index ->
        selectFrame(index + 180)
        withFrameNanos { }
        withFrameNanos { }
        var source: ImageBitmap? = null
        val exportTime = measureTime { source = layer.toImageBitmap() }
        exportTimes += exportTime.inWholeMicroseconds
        val image = checkNotNull(source)
        val bitmap = image.asSkiaBitmap()
        val info = transferInfo(image.width, image.height)
        val count = image.width * image.height
        if (directInput.size != count) {
            directInput = IntArray(count)
            actualOutput = IntArray(count)
        }
        try {
            val mode = index % 2 + 1
            lateinit var old: TransferRun
            lateinit var direct: TransferRun
            // Balance both execution orders within each effect mode.
            if (index % 4 < 2) {
                old = runLegacyTransfer(render, image, mode)
                direct = runDirectTransfer(renderDirect, bitmap, info, directInput, mode)
            } else {
                direct = runDirectTransfer(renderDirect, bitmap, info, directInput, mode)
                old = runLegacyTransfer(render, image, mode)
            }
            try {
                readInto(direct.image.asSkiaBitmap(), info, actualOutput)
                val sourceEqual = old.input.samePixels(directInput)
                val outputEqual = checkNotNull(old.output).samePixels(actualOutput)
                val sourceHash = directInput.contentHashOutsideMarker(image.width, image.height)
                val outputHash = actualOutput.contentHashOutsideMarker(image.width, image.height)
                if (previousSource != null && previousSource != sourceHash) sourceChanges++
                if (previousOutput != null && previousOutput != outputHash) outputChanges++
                if (previousByMode[mode - 1] != null && previousByMode[mode - 1] != outputHash) changesByMode[mode - 1]++
                previousByMode[mode - 1] = outputHash
                previousSource = sourceHash
                previousOutput = outputHash
                if (sourceEqual && outputEqual) valid++
                oldSamples += old.timing()
                newSamples += direct.timing()
                show(direct.image)
                status("Direct pixel transfer frame $index")
                println(
                    "GlassMaterialProbe TRANSFER index=$index mode=$mode sourceExact=$sourceEqual outputExact=$outputEqual " +
                        "sourceHash=$sourceHash outputHash=$outputHash oldReadUs=${old.readUs} directReadUs=${direct.readUs} " +
                        "oldRenderUs=${old.renderUs} directRenderUs=${direct.renderUs} " +
                        "oldImageUs=${old.imageUs} directImageUs=${direct.imageUs} " +
                        "oldTotalUs=${old.totalUs} directTotalUs=${direct.totalUs} outputRowBytes=${direct.rowBytes}"
                )
            } finally {
                // The reference image is never displayed; the shown image keeps its normal managed lifetime.
                old.image.asSkiaBitmap().close()
            }
        } finally {
            bitmap.close()
        }
    }
    val oldWarm = oldSamples.drop(2)
    val directWarm = newSamples.drop(2)
    val passed = valid == 24 && sourceChanges == 23 && outputChanges == 23 && changesByMode.all { it == 11 }
    status(if (passed) "Direct pixel transfer gate passed" else "Direct pixel transfer gate failed")
    println(
        "GlassMaterialProbe TRANSFER_RESULT passed=$passed valid=$valid/24 sourceChanges=$sourceChanges/23 " +
            "outputChanges=$outputChanges/23 normalChanges=${changesByMode[0]}/11 deepChanges=${changesByMode[1]}/11 " +
            "exact=true samples=22 " +
            "oldReadP50Us=${oldWarm.map { it.readUs }.percentile(0.5)} " +
            "directReadP50Us=${directWarm.map { it.readUs }.percentile(0.5)} " +
            "oldTotalP50Us=${oldWarm.map { it.totalUs }.percentile(0.5)} " +
            "directTotalP50Us=${directWarm.map { it.totalUs }.percentile(0.5)} " +
            "oldTotalP95Us=${oldWarm.map { it.totalUs }.percentile(0.95)} " +
            "directTotalP95Us=${directWarm.map { it.totalUs }.percentile(0.95)} " +
            "exportP50Us=${exportTimes.drop(2).percentile(0.5)} " +
            "transport=direct-cpu-pixels pooledExecutorBitmaps=2 frameOutputReuse=false zeroCopy=false"
    )
}

private data class TransferRun(
    val input: IntArray,
    val output: IntArray?,
    val image: ImageBitmap,
    val readUs: Long,
    val renderUs: Long,
    val imageUs: Long,
    val totalUs: Long,
    val rowBytes: Int,
) {
    fun timing() = TransferTiming(readUs, renderUs, imageUs, totalUs)
}

private data class TransferTiming(val readUs: Long, val renderUs: Long, val imageUs: Long, val totalUs: Long)

private fun runLegacyTransfer(render: CPointer<MaterialFunction>, source: ImageBitmap, mode: Int): TransferRun {
    lateinit var input: IntArray
    lateinit var output: IntArray
    lateinit var image: ImageBitmap
    var readUs = 0L
    var renderUs = 0L
    var imageUs = 0L
    val total = measureTime {
        readUs = measureTime {
            input = IntArray(source.width * source.height)
            source.readPixels(input)
        }.inWholeMicroseconds
        renderUs = measureTime {
            output = IntArray(input.size)
            input.usePinned { pixels ->
                output.usePinned {
                    check(render(pixels.addressOf(0), source.width, source.height, 24f, mode + 3, it.addressOf(0)) == 0)
                }
            }
        }.inWholeMicroseconds
        imageUs = measureTime { image = output.toFusionImage(source.width, source.height) }.inWholeMicroseconds
    }
    return TransferRun(input, output, image, readUs, renderUs, imageUs, total.inWholeMicroseconds, source.width * 4)
}

private fun runDirectTransfer(
    render: CPointer<DirectMaterialFunction>,
    source: Bitmap,
    info: ImageInfo,
    input: IntArray,
    mode: Int,
): TransferRun {
    val output = Bitmap()
    try {
        lateinit var image: ImageBitmap
        var readUs = 0L
        var renderUs = 0L
        var imageUs = 0L
        var rowBytes = 0
        val total = measureTime {
            readUs = measureTime { readInto(source, info, input) }.inWholeMicroseconds
            imageUs = measureTime { check(output.allocPixels(info)) }.inWholeMicroseconds
            renderUs = measureTime {
                checkNotNull(output.peekPixels()).use { pixels ->
                    rowBytes = pixels.rowBytes
                    val destination = checkNotNull(interpretCPointer<IntVar>(pixels.addr))
                    input.usePinned {
                        check(render(it.addressOf(0), info.width, info.height, info.width * 4, rowBytes, 24f, mode,
                            destination) == 0) { "Direct pixel executor failed" }
                    }
                }
            }.inWholeMicroseconds
            imageUs += measureTime {
                output.notifyPixelsChanged()
                image = output.asComposeImageBitmap()
            }.inWholeMicroseconds
        }
        return TransferRun(input, null, image, readUs, renderUs, imageUs, total.inWholeMicroseconds, rowBytes)
    } catch (error: Throwable) {
        output.close()
        throw error
    }
}

private fun readInto(bitmap: Bitmap, info: ImageInfo, output: IntArray) {
    require(output.size == info.width * info.height)
    checkNotNull(bitmap.peekPixels()).use { pixels ->
        output.usePinned {
            check(pixels.readPixels(info, it.addressOf(0).rawValue, info.width * 4)) { "Direct pixel read failed" }
        }
    }
}

private fun verifyPaddedAlphaBuffers(render: CPointer<MaterialFunction>, direct: CPointer<DirectMaterialFunction>) {
    var valid = 0
    repeat(3) { sizeIndex ->
        val width = 67 + sizeIndex * 6
        val height = 49 + sizeIndex * 6
        val inputStride = width + 3
        val outputStride = width + 5
        val guard = 0x1337BEEF
        val input = IntArray(width * height) { index ->
            val x = index % width
            val y = index / width
            val alpha = when ((x + y) % 5) { 0 -> 0; 1 -> 32; 2 -> 96; 3 -> 192; else -> 255 }
            (alpha shl 24) or (((x * 17) % 256) shl 16) or (((y * 23) % 256) shl 8) or ((x + y * 3) % 256)
        }
        val paddedInput = IntArray(inputStride * height) { guard }
        for (y in 0 until height) input.copyInto(paddedInput, y * inputStride, y * width, (y + 1) * width)
        repeat(3) { mode ->
            val reference = IntArray(input.size)
            val paddedOutput = IntArray(outputStride * height + 8) { guard }
            input.usePinned { source ->
                reference.usePinned { check(render(source.addressOf(0), width, height, 24f, mode + 3, it.addressOf(0)) == 0) }
            }
            paddedInput.usePinned { source ->
                paddedOutput.usePinned {
                    check(direct(source.addressOf(0), width, height, inputStride * 4, outputStride * 4, 24f, mode,
                        it.addressOf(0)) == 0)
                }
            }
            val actual = IntArray(input.size)
            var guardErrors = 0
            for (y in 0 until height) {
                paddedOutput.copyInto(actual, y * width, y * outputStride, y * outputStride + width)
                for (x in width until outputStride) if (paddedOutput[y * outputStride + x] != guard) guardErrors++
                for (x in width until inputStride) if (paddedInput[y * inputStride + x] != guard) guardErrors++
            }
            for (index in outputStride * height until paddedOutput.size) if (paddedOutput[index] != guard) guardErrors++
            val exact = reference.samePixels(actual)
            if (exact && guardErrors == 0) valid++
            println(
                "GlassMaterialProbe TRANSFER_ALPHA size=${width}x$height mode=$mode exact=$exact guardErrors=$guardErrors " +
                    "inputRowBytes=${inputStride * 4} outputRowBytes=${outputStride * 4}"
            )
        }
    }
    println("GlassMaterialProbe TRANSFER_ALPHA_RESULT passed=${valid == 9} valid=$valid/9 exact=true")
    check(valid == 9) { "Direct alpha/stride parity failed" }
}

private fun transferInfo(width: Int, height: Int) =
    ImageInfo(width, height, ColorType.BGRA_8888, ColorAlphaType.UNPREMUL, ColorSpace.sRGB)

private fun IntArray.samePixels(other: IntArray): Boolean {
    if (size != other.size) return false
    return usePinned { left ->
        other.usePinned { right -> memcmp(left.addressOf(0), right.addressOf(0), (size * 4).toULong()) == 0 }
    }
}

private fun List<Long>.percentile(fraction: Double): Long = sorted()[((size - 1) * fraction).toInt()]
