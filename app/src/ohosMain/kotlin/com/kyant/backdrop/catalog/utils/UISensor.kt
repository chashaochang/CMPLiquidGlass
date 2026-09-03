package com.kyant.backdrop.catalog.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sqrt

@Composable
actual fun rememberUISensor(): UISensor {
    val uiSensor = remember { OhosUISensorState }

    DisposableEffect(Unit) {
        uiSensor.start()
        onDispose { uiSensor.stop() }
    }

    return uiSensor
}

private object OhosUISensorState : UISensor {

    override var gravityAngle: Float by mutableFloatStateOf(45f)
        private set

    override var gravity: Offset by mutableStateOf(Offset.Zero)
        private set

    override fun start() = Unit

    override fun stop() = Unit

    fun updateGravity(x: Float, y: Float) {
        val norm = sqrt(x * x + y * y + 9.81f * 9.81f)
        val alpha = 0.5f
        gravityAngle = gravityAngle * (1f - alpha) + atan2(y, x) * (180f / PI).toFloat() * alpha
        gravity = gravity * (1f - alpha) + Offset(x / norm, y / norm) * alpha
    }
}

fun SetOhosGravity(x: Float, y: Float) {
    OhosUISensorState.updateGravity(x, y)
}
