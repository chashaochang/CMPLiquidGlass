package com.kyant.backdrop.probe

import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset

internal data class CapturedComponentSource(
    val source: ComponentSource,
    val phase: Int,
    val position: Offset,
    val exportUs: Long,
    val readUs: Long,
)

internal suspend fun verifyOriginalComponent(
    executor: ComponentExecutor,
    capture: suspend () -> CapturedComponentSource,
    selectSource: suspend (Int, Boolean) -> Unit,
    show: suspend (ComponentFrame) -> Unit,
    status: (String) -> Unit,
): Boolean {
    println("GlassComponentProbe GATES tolerance=3 maxOutlierRatio=0.01 denominator=active-component " +
        "compare=visible-srcOver-and-alpha samples=24 themes=2 thresholdsPredeclared=true")
    var valid = 0
    var nonblank = 0
    var sourceChanges = 0
    var outputChanges = 0
    var previousSource: Int? = null
    var previousOutput: Int? = null
    repeat(24) { index ->
        val dark = index >= 12
        selectSource(index, dark)
        val captured = capture()
        val source = captured.source
        val time = withFrameNanos { it }
        val frame = executor.frame(source, time, dark)
        val error = checkNotNull(frame.error)
        val shape = shapePixels(frame.pixels, source.width)
        val sourceHash = source.pixels.contentHashCode()
        val outputHash = frame.pixels.contentHashCode()
        if (previousSource != null && previousSource != sourceHash) sourceChanges++
        if (previousOutput != null && previousOutput != outputHash) outputChanges++
        previousSource = sourceHash
        previousOutput = outputHash
        val cornersTransparent = intArrayOf(0, source.width - 1, frame.pixels.size - source.width,
            frame.pixels.lastIndex).all { frame.pixels[it] ushr 24 == 0 }
        if (shape.count > 1000 && cornersTransparent) nonblank++
        if (error.passed && shape.count > 1000 && cornersTransparent) valid++
        show(frame)
        status(if (dark) "Dark $index" else "Light $index")
        println("GlassComponentProbe FRAME index=$index dark=$dark phase=${captured.phase} " +
            "size=${source.width}x${source.height} cropX=${captured.position.x} cropY=${captured.position.y} " +
            "sourceHash=$sourceHash outputHash=$outputHash outliers=${error.outliers} active=${error.active} " +
            "max=${error.max} nontransparent=${shape.count} cornersTransparent=$cornersTransparent " +
            "cpuUs=${frame.cpuUs} gpuUs=${frame.gpuUs} exportUs=${captured.exportUs} readUs=${captured.readUs}")
    }
    val pixelsPassed = valid == 24 && nonblank == 24 && sourceChanges == 23 && outputChanges == 23
    println("GlassComponentProbe PIXEL_RESULT passed=$pixelsPassed valid=$valid/24 nonblank=$nonblank/24 " +
        "sourceChanges=$sourceChanges/23 outputChanges=$outputChanges/23 tolerance=3 maxOutlierRatio=0.01")
    if (nonblank != 24) return false

    val interactionsPassed = (0..1).map { theme ->
        selectSource(100 + theme * 40, theme == 1)
        val captured = capture()
        // Freeze the real source during input tests so only original component animation can change it.
        val source = captured.source.copy(pixels = captured.source.pixels.copyOf())
        verifyComponentInput(executor, source, theme == 1, show, status)
    }.all { it }
    return pixelsPassed && interactionsPassed
}

private suspend fun verifyComponentInput(
    executor: ComponentExecutor,
    source: ComponentSource,
    dark: Boolean,
    show: suspend (ComponentFrame) -> Unit,
    status: (String) -> Unit,
): Boolean {
    var compared = 0
    var valid = 0
    var stepIndex = 0
    suspend fun step(stage: String): ComponentFrame {
        val time = withFrameNanos { it }
        val frame = executor.frame(source, time, dark)
        val error = checkNotNull(frame.error)
        compared++
        if (error.passed) valid++
        show(frame)
        status(stage)
        val shape = shapePixels(frame.pixels, source.width)
        println("GlassComponentProbe INPUT_FRAME dark=$dark index=${stepIndex++} stage=$stage selected=${frame.selected} " +
            "hash=${frame.pixels.contentHashCode()} height=${shape.height} count=${shape.count} " +
            "outliers=${error.outliers} active=${error.active} max=${error.max} cpuUs=${frame.cpuUs} gpuUs=${frame.gpuUs}")
        return frame
    }
    suspend fun pointer(event: Int, position: Offset) {
        executor.pointer(event, position, withFrameNanos { it })
    }

    repeat(4) { step("theme") }
    val tap = executor.tabCenter(1)
    pointer(0, tap)
    step("tap-down")
    pointer(2, tap)
    var idle: ComponentFrame? = null
    repeat(18) { idle = step("tap-settle") }
    val tapped = checkNotNull(idle)
    val tapPassed = tapped.selected == 1
    val idleHash = tapped.pixels.contentHashCode()
    val idleShape = shapePixels(tapped.pixels, source.width)

    val heldPoint = executor.tabCenter(1)
    pointer(0, heldPoint)
    var holdChanges = 0
    var previousHold: Int? = null
    var maxHeight = idleShape.height
    var maxCount = idleShape.count
    repeat(18) {
        val frame = step("hold")
        val hash = frame.pixels.contentHashCode()
        if (previousHold != null && previousHold != hash) holdChanges++
        previousHold = hash
        val shape = shapePixels(frame.pixels, source.width)
        maxHeight = maxOf(maxHeight, shape.height)
        maxCount = maxOf(maxCount, shape.count)
    }
    val holdPassed = previousHold != idleHash && holdChanges >= 2 && maxHeight > idleShape.height + source.density &&
        maxCount > idleShape.count
    pointer(2, heldPoint)
    repeat(18) { step("release") }

    val dragStart = executor.tabCenter(1)
    pointer(0, dragStart)
    repeat(3) { step("drag-down") }
    var dragEnd = dragStart
    repeat(12) { index ->
        val target = executor.tabCenter(3)
        val progress = (index + 1) / 12f
        dragEnd = Offset(dragStart.x + (target.x - dragStart.x) * progress,
            dragStart.y + (target.y - dragStart.y) * progress)
        pointer(1, dragEnd)
        step("drag")
    }
    pointer(2, dragEnd)
    var dragged: ComponentFrame? = null
    repeat(20) { dragged = step("drag-settle") }
    val dragPassed = checkNotNull(dragged).selected == 3

    val cancelStart = executor.tabCenter(3)
    pointer(0, cancelStart)
    step("cancel-down")
    val cancelEnd = executor.tabCenter(2)
    pointer(1, cancelEnd)
    step("cancel-move")
    pointer(3, cancelEnd)
    repeat(18) { step("cancel-settle") }
    val recovery = executor.tabCenter(0)
    pointer(0, recovery)
    step("recovery-down")
    pointer(2, recovery)
    var recovered: ComponentFrame? = null
    repeat(20) { recovered = step("recovery") }
    val cancelPassed = checkNotNull(recovered).selected == 0

    val passed = tapPassed && holdPassed && dragPassed && cancelPassed && valid == compared
    println("GlassComponentProbe INPUT_RESULT dark=$dark passed=$passed tap=$tapPassed hold=$holdPassed " +
        "drag=$dragPassed cancelRecovery=$cancelPassed parity=$valid/$compared holdChanges=$holdChanges " +
        "idleHeight=${idleShape.height} pressedHeight=$maxHeight idleCount=${idleShape.count} pressedCount=$maxCount " +
        "coordinates=original-semantics backgroundsFrozen=true")
    return passed
}

private data class ShapePixels(val count: Int, val height: Int)

private fun shapePixels(pixels: IntArray, width: Int): ShapePixels {
    var count = 0
    var top = Int.MAX_VALUE
    var bottom = -1
    for (index in pixels.indices) {
        if (pixels[index] ushr 24 > 32) {
            count++
            val y = index / width
            top = minOf(top, y)
            bottom = maxOf(bottom, y)
        }
    }
    return ShapePixels(count, if (bottom < 0) 0 else bottom - top + 1)
}
