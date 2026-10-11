---
name: compose-liquid-glass
description: Use this when integrating the Backdrop liquid-glass library or its Button, Toggle, Slider, Bottom Tabs, Dialog, and Mini Bar examples into Android, iOS, or HarmonyOS Compose projects.
---

# Compose Liquid Glass Integration

## Scope

This repository contains two different kinds of code:

- `backdrop` and `shapes` are reusable library modules.
- `app/.../catalog/components` and `app/.../catalog/destinations` are reference implementations. Copy them into the consuming project or extract them into a dedicated component module.

Do not assume that adding the `backdrop` dependency also provides `LiquidButton`, `LiquidToggle`, `LiquidSlider`, or `LiquidBottomTabs`.

## Required Backdrop Structure

Create one remembered backdrop for a visual region. The recorded background and every glass consumer must be siblings.

```kotlin
@Composable
fun GlassScreen() {
    val backdrop = rememberLayerBackdrop()

    Box(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(Res.drawable.wallpaper),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop),
            contentScale = ContentScale.Crop,
        )

        LiquidButton(
            onClick = {},
            backdrop = backdrop,
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Text("Continue")
        }
    }
}
```

Never record a parent that contains a glass node consuming the same backdrop:

```kotlin
// Wrong: content contains drawBackdrop(backdrop), so the layer can sample itself.
Box(Modifier.layerBackdrop(backdrop)) {
    ScreenContent(backdrop)
}
```

On HarmonyOS/Skia this can create recursive `RenderNode.onDraw -> drawDrawable -> RenderNode.onDraw` calls and terminate with a native stack overflow. Split the background/content recorder and glass overlays into siblings instead.

## Core APIs

### `rememberLayerBackdrop` and `layerBackdrop`

Use for sampling already composed UI. Apply `layerBackdrop(backdrop)` to the background source only. Keep the instance stable with `rememberLayerBackdrop()`.

### `rememberCanvasBackdrop`

Use when the background can be redrawn directly in a `DrawScope`, without recording another composable tree.

```kotlin
val backdrop = rememberCanvasBackdrop {
    drawRect(Color(0xFF4A90E2))
}
```

### `rememberCombinedBackdrop`

Use when a moving control must refract both the page and a local track or tab layer. Toggle, Slider, and Bottom Tabs use this pattern. Do not combine a layer with itself.

### `drawBackdrop`

Use this for normal glass surfaces. The important parameters are:

- `shape`: clipping and effect outline.
- `effects`: ordered material pipeline such as `vibrancy()`, `blur()`, and `lens()`.
- `highlight`: edge lighting.
- `shadow`: outer depth.
- `innerShadow`: pressed or inset depth.
- `layerBlock`: translation and scale applied to the glass layer.
- `onDrawSurface`: tint drawn over the sampled background.
- `onDrawBehind` and `onDrawFront`: custom decoration around the material.
- `exportedBackdrop`: exposes the rendered glass as another backdrop for composition effects.

Use `drawPlainBackdrop` only when highlight and shadow modifiers are intentionally managed elsewhere.

## Effects

### Blur

`blur(radius)` softens sampled pixels. Start around `2.dp` for small buttons, `8.dp` for navigation surfaces, and `8-16.dp` for dialogs. Large animated blur radii are expensive.

### Lens

`lens(refractionHeight, refractionAmount, chromaticAberration, depthEffect)` creates refraction.

- Small controls: about `12.dp, 24.dp`.
- Navigation containers: about `24.dp, 24.dp`.
- Dialogs: about `24.dp, 48.dp`, optionally with `depthEffect = true`.
- Enable chromatic aberration for a focused moving droplet, not every large surface.

If the first press stalls because the shader is compiled lazily, keep a tiny nonzero refraction input while visual alpha remains zero:

```kotlin
val shaderProgress = pressProgress.coerceAtLeast(0.001f)
lens(
    10.dp.toPx() * shaderProgress,
    14.dp.toPx() * shaderProgress,
    chromaticAberration = true,
)
```

### Vibrancy and Color Controls

Use `vibrancy()` before blur/lens for controls that must preserve background color. Use `colorControls()` for deliberate brightness or saturation changes, especially dialogs. Avoid stacking strong vibrancy and opaque surface tint because the result stops looking transparent.

## Components

### Liquid Button

Source: `app/src/commonMain/kotlin/com/kyant/backdrop/catalog/components/LiquidButton.kt`.

```kotlin
LiquidButton(
    onClick = onSave,
    backdrop = backdrop,
    tint = brandColor,
    isInteractive = true,
) {
    Icon(Icons.Default.Save, contentDescription = null)
    Text("Save")
}
```

Use `isInteractive = false` when the normal Compose indication is preferable. `tint` applies hue plus translucent tint; `surfaceColor` draws a direct surface color and can easily become opaque.

### Liquid Toggle

Source: `app/src/commonMain/kotlin/com/kyant/backdrop/catalog/components/LiquidToggle.kt`.

```kotlin
var enabled by remember { mutableStateOf(false) }
LiquidToggle(
    selected = { enabled },
    onSelect = { enabled = it },
    backdrop = backdrop,
)
```

Pass state through lambdas so drag animation reads the latest value. The track owns a local `LayerBackdrop`; the thumb consumes a combined page-and-track backdrop.

### Liquid Slider

Source: `app/src/commonMain/kotlin/com/kyant/backdrop/catalog/components/LiquidSlider.kt`.

```kotlin
var volume by remember { mutableFloatStateOf(0.5f) }
LiquidSlider(
    value = { volume },
    onValueChange = { volume = it },
    valueRange = 0f..1f,
    visibilityThreshold = 0.001f,
    backdrop = backdrop,
)
```

Use a threshold appropriate to the value range. The component supports track taps and thumb dragging. Preserve RTL handling if adapting its geometry.

### Liquid Bottom Tabs

Sources:

- `app/src/commonMain/kotlin/com/kyant/backdrop/catalog/components/LiquidBottomTabs.kt`
- `app/src/commonMain/kotlin/com/kyant/backdrop/catalog/components/LiquidBottomTab.kt`

```kotlin
var selected by remember { mutableIntStateOf(0) }
LiquidBottomTabs(
    selectedTabIndex = { selected },
    onTabSelected = { selected = it },
    backdrop = backdrop,
    tabsCount = destinations.size,
) {
    destinations.forEachIndexed { index, item ->
        LiquidBottomTab(onClick = { selected = index }) {
            Icon(item.icon, contentDescription = item.label)
            Text(item.label)
        }
    }
}
```

Rules:

- `tabsCount` must match the number of `LiquidBottomTab` children.
- Keep the hidden tinted tab recorder. The selected droplet combines it with the page backdrop to refract selected content.
- Do not add `clip(Capsule())` around the entire tabs container. The pressed droplet expands outside its idle bounds and will be cut off.
- Keep the light container near `Color(0xFFFAFAFA).copy(alpha = 0.4f)` unless the page has been visually retuned. A much lower alpha makes the pressed black surface look dirty in light mode.
- Account for the horizontal `4.dp + 4.dp` padding when calculating selected width: `(dockWidth - 8.dp) / tabsCount`.
- Keep the selected droplet's `scaleX`, `scaleY`, lens, highlight, shadow, and inner shadow aligned with the reference component. If the pressed edge becomes a hard dark band on HarmonyOS, verify that `InnerShadowModifier.configurePaint()` still calls `paint.blur(shadow.radius.toPx())`; removing that call changes the material, it is not a performance optimization.

```kotlin
scaleX = animatedScaleX
scaleY = animatedScaleY

lens(
    refractionHeight = 10.dp.toPx() * progress,
    refractionAmount = 14.dp.toPx() * progress,
    chromaticAberration = true,
)
```

### Compact Bottom Tabs

`compactProgress` is `0f` when expanded and `1f` when compact. When building a Mini Bar transition:

- Drive progress directly from stable scroll distance; do not add another chasing tween during a fling.
- Fade labels and nonselected tabs during the first half of the collapse.
- Detach actions such as Search explicitly when the design requires it; do not squeeze them gradually into zero width.
- Keep selected indicator geometry aligned with the same `dockWidth` used to calculate `tabWidth`.

### Dialog Glass

Reference: `app/src/commonMain/kotlin/com/kyant/backdrop/catalog/destinations/DialogContent.kt`.

Dialogs are a recipe rather than a library component. Dim the recorded background, then render the dialog as a sibling consuming that backdrop. Use stronger blur and surface alpha than small controls. Dialog buttons may use `LiquidButton` or ordinary filled controls depending on hierarchy.

### Mini Bar

Reference: `app/src/commonMain/kotlin/com/kyant/backdrop/catalog/destinations/MiniBarContent.kt`.

The list/background is the recorder. Bottom Tabs, Mini Player, and detached Search are sibling overlays. The Mini Player should continuously resize and move to its docked position; do not switch between two unrelated composables at a threshold.

## Shapes

- `Capsule()` for pills, tabs, toggles, sliders, and compact players.
- `RoundedRectangle(radius)` for regular cards and dialogs.
- `UnevenRoundedRectangle` for intentionally asymmetric containers.
- Shape lerp/copy helpers support continuous geometry animation.

The shape supplied to `drawBackdrop` should match the visible container shape. Avoid an extra parent clip unless clipping overflow is a deliberate requirement.

## HarmonyOS Rules

- The current Glass baseline is CPF 1.1.0: Kotlin `2.2.21-1.1.0`, Compose/HAR `1.9.2-1.1.0`, and OHOS Skiko `0.9.22.2-1.1.0`. The host imports the vendored official HAR as `@cpf-kmp-cmp/compose`.
- Use the Skia renderer for the currently verified liquid-glass path.
- Keep `compose.har`, Skiko, Compose, Kotlin, `libkn.so`, and `libkn_api.h` from matching toolchain versions.
- A successful build and install do not prove rendering correctness. Verify first frame, light and dark themes, press/drag/release, scrolling behind glass, and route return behavior on a physical device.
- Runtime shader support is platform dependent. Guard custom shaders with `isRuntimeShaderSupported()` and provide a nonshader fallback.
- Do not solve a native crash by wrapping the entire route in a self-consuming backdrop. Isolate the recorder and overlays structurally.

## Performance

- Reuse one route backdrop where practical instead of recording one full-screen layer per component.
- Use local track/tab backdrops only when the moving control must refract local content.
- Keep backdrop objects and animation controllers in `remember`.
- Avoid large blur, chromatic aberration, and depth effects on repeated list items.
- Prewarm only the expensive focused effect; a permanently visible nonzero effect can change idle appearance.
- Do not clip animated droplets at their idle bounds.

## Verification Checklist

1. Build Android and OHOS targets after shared component changes.
2. Confirm the configured renderer in Gradle output.
3. Test light and dark themes separately.
4. Hold, drag, release, and rapidly retap each interactive component.
5. Check 3, 4, and 5 tab layouts and compare press geometry with the reference Catalog component.
6. Scroll moving content behind glass and confirm the backdrop updates every frame.
7. Confirm no `RenderNode` recursion, native crash, first-press stall, clipping, or colored edge artifacts.
