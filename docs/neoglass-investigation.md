# NeoGlass Investigation

Date: 2026-10-11. Scope: research only, for the existing Glass library.
No runtime code, renderer selection, dependencies, build, or device installation
was changed or performed for this investigation.

## Identity And Evidence

- Package: [neoglass](https://ohpm.openharmony.cn/#/cn/detail/neoglass).
- Publisher: WhsApexCrew; registry author name is 铧Whs.
- Inspected release: **1.3.0**, published 2026-10-03 according to the registry.
- Registry minimum compatible SDK: **API 20**, HarmonyOS; arm64-v8a and x86_64.
- Declared license: Apache-2.0.
- Declared repository: <https://gitcode.com/WhsApexCrew/NeoGlass>.
- Registry metadata: <https://ohpm.openharmony.cn/ohpm/neoglass>.
- Inspected archive: <https://ohpm.openharmony.cn/ohpm/neoglass/-/neoglass-1.3.0.har>.

Archive SHA-512, base64, matches registry `dist.integrity`:

```text
1q/kCjumv9RmOTkWvD/melnQa7LGc753AAQr8BB+rBbvma5JjvPrh+mSyL0vpBHTkEAJMYizqmZn1xTBz/AYhg==
```

The public repository displayed no code, and anonymous `git clone` returned an
empty repository. The published HAR contains README, changelog, examples,
ArkTS declarations, compiled `ets/modules.abc`, and native shared libraries.
It does **not** contain the implementation `.ets` or C++ source files described
in its changelog. Therefore this is a package/documentation/binary inspection,
not a full source audit or a reproducible native build.

Local inspection artifacts are under
`/private/tmp/glass-neoglass-20261011/package`; the archive is
`/private/tmp/neoglass-1.3.0.har`. These temporary files are not project dependencies.

## Verified Rendering Route

```text
ArkUI background subtree identified by component ID
    -> component snapshot / PixelMap
    -> readPixelsToBuffer / ArrayBuffer
    -> native setBackground(id, buffer, width, height)
    -> OpenGL texture upload
    -> GLES fragment shaders and framebuffer passes
    -> EGL / NativeWindow / transparent page-level XComponent
ArkUI foreground content is placed above that canvas
```

Evidence beyond the README:

- `src/main/cpp/types/libneoglass/Index.d.ts` exposes `initXComponent`,
  `setBackground(ArrayBuffer, ...)`, `setElements`, and per-tag updates.
- `NeoGlassRuntime.d.ets` documents `componentSnapshot` via UIContext.
- Compiled ArkTS strings include `getComponentSnapshot`, `readPixelsToBuffer`,
  `ArrayBuffer`, `setBackground`, `g_snapshotBusy`, and `g_snapshotAgain`.
  Strings corroborate the documented route; they do not expose its full control flow.
- arm64 `libneoglass.so` imports `OH_NativeWindow_CreateNativeWindowFromSurfaceId`,
  `eglCreateContext`, `eglCreateWindowSurface`, `eglSwapBuffers`, `glShaderSource`,
  and `glTexImage2D`.
- ELF dependencies include `libnative_window.so`, `libEGL.so`, and `libGLESv3.so`.
- Native symbols include `UpdateBackgroundTexture`, `RenderLoop`, `DrawElement`,
  `CopySceneToBackdrop`, `PassRimHighlight`, and inner/outer-shadow passes.
- Embedded shader strings contain `#version 300 es`, `sampler2D`, refraction,
  seven dispersion samples, and `INTERIOR_SHRINK_MAX = 8.0`.

This is an independent GPU glass renderer, not a Fusion/native_drawing runtime
shader binding. No NPU path was found in the inspected package interfaces,
imports, or dependencies. This does not assert that every proprietary system
material implementation never uses an NPU internally.

## Live Background And Performance

- `backdropComponent` selects the subtree to capture; it must exclude the glass
  itself to avoid feedback. Without it, the documented automatic mode captures
  a single window snapshot, rather than continuously sampling a live source.
- `backdropFps` defaults to **12** in README and declarations; zero means static
  capture. The changelog describes 30/60 fps operation with a 16 ms minimum
  interval, but the README parameter table says 0-30. Treat maximum-rate behavior
  as unverified rather than resolving this discrepancy by assumption.
- The changelog describes snapshot scale **0.35**,
  `waitUntilRenderFinished: false`, and request coalescing to reduce lag.
- The README explicitly acknowledges **offscreen rendering + pixel readback +
  texture upload** costs. Thus native GLES output is not evidence of a zero-copy
  background input or an end-to-end performance win.
- Glass animation frequency and background-refresh frequency are different.
  A smoothly animated surface can still display older captured content.
- Publisher guidance recommends roughly 6-8 glass elements per screen. Its
  Scene sampling mode adds a scene-texture copy per relevant element.
- No comparable device traces, frame-time distribution, GPU timing, or measured
  Glass-versus-NeoGlass benchmark was established by this investigation.

## Layering And Fidelity Limits

- Version 1.3.0 requires a **page-level shared canvas**, placed between background
  content and foreground glass content. It is not a per-Compose-layer effect.
- The README warns against putting native glass inside scrolling containers:
  element rectangles are not automatically remeasured while scrolling. The
  suggested list substitute is system `backgroundEffect`, without native
  refraction/dispersion. That is not equivalent to our complete library.
- Alpha composition is device-dependent; the README documents possible black
  areas on some devices/previews. Its wallpaper fallback is not a live-background
  equivalent and would not meet the current Glass requirements.
- Its native API exposes one `cornerRadius`; Glass's original lens uses four
  corner radii and original source-layer offsets.
- Its documented/embedded lens includes interior shrink and separately tuned
  dispersion. The current Glass shader leaves pixels beyond the edge-refraction
  band unchanged and binds the previous filter image as child `content`.
  NeoGlass is therefore not a verified pixel-identical executor of our shaders.
- The shared canvas's element order does not by itself establish arbitrary
  interleaving with multiple Compose layers, clips, transforms, and foregrounds.

## Relevance To The Fusion Failure

NeoGlass shows a publicly accessible **alternative rendering route**: EGL/GLES
on an external surface. It does not supply the missing Drawing-compatible
RuntimeEffect compiler/builder, source-consuming runtime image filter,
filter-graph composition, or Fusion recording/replay semantics.

Moving Glass onto its renderer would be a backend/material integration project,
not a fix for the existing failing `RuntimeEffect.makeForShader` binding.
Calling the original unsupported path would still need to be eliminated or
properly implemented; adding another surface alone does not repair it.

Useful reference points are transparent-surface setup, coordinate alignment,
request coalescing, and lifecycle handling. Reduced snapshot resolution/rate
can reduce work, but sacrifices background fidelity/freshness. These are not
proof that the withdrawn hybrid implementation should be restored.

**Recommendation:** retain NeoGlass as a documented reference. Do not replace
the original material, mirror the Compose scene, or restore the withdrawn GPU
prototype based on this finding. Full source access and a scoped device test
would be needed before adopting any particular implementation detail.

## Related Glass Evidence

- [Fusion capability investigation](fusion-runtime-effects-system-capabilities.md)
- `backdrop/src/ohosMain/kotlin/com/kyant/backdrop/RuntimeShader.kt`
- `backdrop/src/commonMain/kotlin/com/kyant/backdrop/effects/Lens.kt`
- `backdrop/src/commonMain/kotlin/com/kyant/backdrop/internal/Shaders.kt`
- NeoGlass HAR: `README.md`, `CHANGELOG.md`, `NeoGlass.d.ets`,
  `NeoGlassRuntime.d.ets`, `libneoglass/Index.d.ts`, `example/BasicCard.ets`.
