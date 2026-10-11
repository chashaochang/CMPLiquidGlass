package com.kyant.backdrop.catalog.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawPlainBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.runtimeShaderEffect

fun Modifier.progressiveBlur(
    backdrop: Backdrop,
    tint: Color,
    blurRadius: Dp = 4.dp,
    tintIntensity: Float = 0.8f,
    gradientStartFraction: Float = 0.5f,
    exportedBackdrop: LayerBackdrop? = null,
): Modifier = drawPlainBackdrop(
    backdrop = backdrop,
    shape = { RectangleShape },
    effects = {
        blur(blurRadius.toPx())
        runtimeShaderEffect(
            key = "VerticalProgressiveBlur",
            shaderString = """
                uniform shader content;

                uniform float2 size;
                layout(color) uniform half4 tint;
                uniform float tintIntensity;
                uniform float gradientStartFraction;

                half4 main(float2 coord) {
                    float alpha = smoothstep(
                        size.y,
                        size.y * gradientStartFraction,
                        coord.y
                    );
                    return mix(content.eval(coord) * alpha, tint * alpha, tintIntensity);
                }
            """.trimIndent(),
            uniformShaderName = "content",
        ) {
            setFloatUniform("size", size.width, size.height)
            setColorUniform("tint", tint)
            setFloatUniform("tintIntensity", tintIntensity)
            setFloatUniform("gradientStartFraction", gradientStartFraction)
        }
    },
    exportedBackdrop = exportedBackdrop,
)
