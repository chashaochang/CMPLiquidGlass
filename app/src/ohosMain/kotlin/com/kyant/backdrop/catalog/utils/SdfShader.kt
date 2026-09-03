package com.kyant.backdrop.catalog.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.asOhosRuntimeShader
import com.kyant.backdrop.effects.runtimeShaderEffect
import com.kyant.backdrop.isRuntimeShaderSupported
import org.jetbrains.skia.Shader
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.imageResource

@Composable
actual fun rememberSdfShader(drawableResource: DrawableResource): SdfShader {
    val imageResource = imageResource(drawableResource)
    return remember(imageResource) { OhosSdfShader(imageResource) }
}

actual fun SdfShader(imageBitmap: ImageBitmap): SdfShader {
    return OhosSdfShader(imageBitmap)
}

@Immutable
private class OhosSdfShader(
    private val sdfBitmap: ImageBitmap,
) : SdfShader {

    private val sdfTexture: Shader = sdfBitmap.asSkiaBitmap().makeShader()

    override val width: Int
        get() = sdfBitmap.width

    override val height: Int
        get() = sdfBitmap.height

    override fun BackdropEffectScope.apply(
        refractionHeight: Float,
        lightAngle: Float,
    ) {
        if (!isRuntimeShaderSupported()) return

        runtimeShaderEffect("SdfShader", SdfShaderString, "content") {
            asOhosRuntimeShader().child("sdfTex", sdfTexture)
            setFloatUniform("size", size.width, size.height)
            setFloatUniform("sdfTexSize", sdfBitmap.width.toFloat(), sdfBitmap.height.toFloat())
            setFloatUniform("refractionHeight", refractionHeight)
            setFloatUniform("lightAngle", lightAngle)
        }
    }
}
