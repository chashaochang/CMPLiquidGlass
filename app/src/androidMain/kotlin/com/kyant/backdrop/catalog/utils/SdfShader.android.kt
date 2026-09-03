package com.kyant.backdrop.catalog.utils

import android.graphics.BitmapShader
import android.graphics.Shader
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.asAndroidRuntimeShader
import com.kyant.backdrop.effects.runtimeShaderEffect
import com.kyant.backdrop.isRuntimeShaderSupported
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.imageResource

@Composable
actual fun rememberSdfShader(drawableResource: DrawableResource): SdfShader {
    val imageResource = imageResource(drawableResource)
    return remember(imageResource) { SdfShader(imageResource) }
}

actual fun SdfShader(imageBitmap: ImageBitmap): SdfShader {
    return SdfShaderImpl(imageBitmap)
}

@Immutable
private class SdfShaderImpl(val sdfBitmap: ImageBitmap) : SdfShader {

    private val sdfTexture =
        BitmapShader(sdfBitmap.asAndroidBitmap(), Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)

    override val width: Int
        get() = sdfBitmap.width

    override val height: Int
        get() = sdfBitmap.height

    override fun BackdropEffectScope.apply(
        refractionHeight: Float,
        lightAngle: Float,
    ) {
        if (isRuntimeShaderSupported()) {
            runtimeShaderEffect("SdfShader", SdfShaderString, "content") {
                asAndroidRuntimeShader().setInputBuffer("sdfTex", sdfTexture)
                setFloatUniform("size", size.width, size.height)
                setFloatUniform("sdfTexSize", sdfBitmap.width.toFloat(), sdfBitmap.height.toFloat())
                setFloatUniform("refractionHeight", refractionHeight)
                setFloatUniform("lightAngle", lightAngle)
            }
        }
    }
}
