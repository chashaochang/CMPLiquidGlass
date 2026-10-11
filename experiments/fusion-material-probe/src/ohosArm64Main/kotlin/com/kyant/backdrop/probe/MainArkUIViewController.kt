package com.kyant.backdrop.probe

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.napi.JsEnv
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.ComposeArkUIViewController
import cnames.structs.napi_env__
import cnames.structs.napi_value__
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.initMainHandler
import kotlin.time.measureTime

@OptIn(ExperimentalForeignApi::class)
fun MainArkUIViewController(env: CPointer<napi_env__>): CPointer<napi_value__> {
    initMainHandler(env)
    JsEnv.init(env)
    println("GlassMaterialProbe START backend=fusion-renderer gate=recorded-layer-export")
    return ComposeArkUIViewController(env) { RecordedLayerProbe() }
}

@Composable
private fun RecordedLayerProbe() {
    val layer = rememberGraphicsLayer()
    val listState = rememberLazyListState()
    var phase by remember { mutableIntStateOf(0) }
    val recordedPhase = remember { intArrayOf(-1) }
    var preview by remember { mutableStateOf<ImageBitmap?>(null) }
    var status by remember { mutableStateOf("Warming up") }

    LaunchedEffect(layer) {
        repeat(60) { withFrameNanos { } }
        val exportTimes = mutableListOf<Long>()
        val readTimes = mutableListOf<Long>()
        var previousHash: Int? = null
        var changed = 0
        var matched = 0
        var nontransparent = 0
        var textFrames = 0
        try {
            repeat(90) { index ->
                phase = index
                listState.scrollToItem(index % 10)
                // Wait for the updated content to be drawn, not just recomposed.
                withFrameNanos { }
                withFrameNanos { }
                check(layer.size.width > 32 && layer.size.height > 32) { "Layer not recorded" }
                val frame = recordedPhase[0]
                var snapshot: ImageBitmap? = null
                val exportTime = measureTime { snapshot = layer.toImageBitmap() }
                val image = checkNotNull(snapshot)
                val pixels = IntArray(image.width * image.height)
                val readTime = measureTime { image.readPixels(pixels) }
                var hash = 1
                var opaqueContentPixels = 0
                var darkContentPixels = 0
                for (y in 32 until image.height) {
                    for (x in 32 until image.width) {
                        val pixel = pixels[y * image.width + x]
                        hash = 31 * hash + pixel
                        if ((pixel ushr 24) == 255) {
                            opaqueContentPixels++
                            if ((pixel and 0x00FFFFFF) == 0) darkContentPixels++
                        }
                    }
                }
                if (previousHash != null && previousHash != hash) changed++
                previousHash = hash
                val marker = pixels[8 * image.width + 8]
                val expected = if (frame % 2 == 0) 0xFFFF3344.toInt() else 0xFF00CCAA.toInt()
                if (marker == expected) matched++
                if (opaqueContentPixels > 1000) nontransparent++
                if (darkContentPixels > 20) textFrames++
                exportTimes += exportTime.inWholeMicroseconds
                readTimes += readTime.inWholeMicroseconds
                preview = image
                status = "Frame $frame, marker ${marker.toUInt().toString(16)}"
                println(
                    "GlassMaterialProbe SAMPLE index=$index recorded=$frame " +
                        "size=${image.width}x${image.height} marker=${marker.toUInt().toString(16)} " +
                        "expected=${expected.toUInt().toString(16)} hash=$hash " +
                        "opaqueContent=$opaqueContentPixels darkContent=$darkContentPixels " +
                        "exportUs=${exportTime.inWholeMicroseconds} readUs=${readTime.inWholeMicroseconds}"
                )
            }
            val passed = matched == 90 && changed >= 80 && nontransparent == 90 && textFrames == 90
            status = if (passed) "Source export passed" else "Source export failed"
            println(
                "GlassMaterialProbe RESULT passed=$passed matched=$matched/90 changed=$changed/89 " +
                    "nontransparent=$nontransparent/90 textFrames=$textFrames/90 " +
                    "exportP50Us=${exportTimes.percentile(0.5)} " +
                    "exportP95Us=${exportTimes.percentile(0.95)} readP95Us=${readTimes.percentile(0.95)} " +
                    "transport=cpu-bitmap shader=not-invoked"
            )
            if (passed) {
                verifyMaterialExecutor(
                    layer = layer,
                    selectFrame = { index ->
                        phase = index
                        listState.scrollToItem(index % 10)
                    },
                    show = { preview = it },
                    status = { status = it },
                )
            }
        } catch (error: Throwable) {
            status = "Source export error: ${error.message}"
            println("GlassMaterialProbe ERROR ${error::class.simpleName}: ${error.message}")
        }
    }

    Column(Modifier.fillMaxSize().background(Color(0xFFF5F6F8)).padding(24.dp, 56.dp)) {
        BasicText(status, style = TextStyle(color = Color.Black, fontSize = 16.sp))
        Spacer(Modifier.height(12.dp))
        LazyColumn(
            state = listState,
            modifier = Modifier.size(240.dp, 180.dp).drawWithContent {
                val currentPhase = phase
                layer.record {
                    this@drawWithContent.drawContent()
                    drawRect(
                        color = if (currentPhase % 2 == 0) Color(0xFFFF3344) else Color(0xFF00CCAA),
                        topLeft = Offset.Zero,
                        size = Size(24f, 24f),
                    )
                }
                recordedPhase[0] = currentPhase
                drawLayer(layer)
            },
        ) {
            items((0..30).toList()) { index ->
                BasicText(
                    "Fusion row $index",
                    modifier = Modifier.background(
                        if (index % 2 == 0) Color(0xFFDCF0FA) else Color.White
                    ).padding(24.dp),
                    style = TextStyle(color = Color.Black, fontSize = 18.sp),
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        preview?.let { Image(it, contentDescription = "Recorded source", modifier = Modifier.size(240.dp, 180.dp)) }
    }
}

private fun List<Long>.percentile(fraction: Double): Long =
    sorted()[((size - 1) * fraction).toInt()]
