# Fusion Material Probe

This standalone Gradle project does not change the normal Glass backend or
publish into `ohosApp`. It uses the same CPF 1.1.0 artifacts with Fusion selected.

## Gate 1: Existing Recorded Source

The real Compose list is recorded once into a `GraphicsLayer` and displayed.
`toImageBitmap()` replays that layer; no second UI scene is constructed. The
returned image is composited back into the same Fusion UI as a normal image.

The automatic test scrolls 90 times and validates an alternating color marker,
changing content hashes excluding the marker, opaque list content, black text,
and export/read timings. Logs begin with
`GlassMaterialProbe`. Native crashes are not caught by the Kotlin error handler.

## Gate 2: Isolated Original Shader

After gate 1 passes, the probe loads `libglass_material.so` with
`RTLD_NOW | RTLD_LOCAL`. The separate pure-Skiko executor compiles the original
`backdrop/.../internal/Shaders.kt` directly; it does not copy or translate the
shader source. It uses the original refraction and dispersion shader strings,
`RuntimeEffect`, `RuntimeShaderBuilder`, and `ImageFilter.makeRuntimeShader`.

Only `glass_material_render` and `glass_material_render_direct` are exported.
A version script and `-Bsymbolic`
keep Skia, Skiko and Kotlin/Native implementation symbols local. Only scalar
parameters and pixel buffers cross the C ABI, never renderer-owned C++ objects.
The library remains loaded because its Kotlin/Native GC workers cannot be
safely unloaded with `dlclose`.

For 12 changing source images, gate 2 requires exact identity output, at least
100 pixels changed by refraction, at least 100 pixels differing between normal
and deep/dispersion output, and changing output hashes on all 11 transitions.
Deep output is returned as an ordinary image in Fusion.

Gate 2 is a **CPU bitmap and Skia raster correctness experiment**, not a zero-copy
or GPU performance implementation. It does not invoke unsupported Fusion
RuntimeEffect functions or construct a mirrored UI. Complete component clipping,
transforms, effect chains and interactions are not integrated or validated.

## Gate 3: Isolated Original Shader On GPU

After the CPU gate passes, the same executor creates an app-owned GLES3 EGL
context and a 1x1 pbuffer, then a Skia Ganesh render target at the actual source
size. CPU and GPU modes call the same original material drawing code. No display
surface, XComponent, private system runtime-shader function or replacement
shader is introduced.

For 12 changing images, GPU identity must have no pixels exceeding a channel
error of 3. Normal refraction and deep/dispersion are compared with CPU references
at the same tolerance, allowing at most 1% outlier pixels. All 11 source and
effect hash transitions outside the marker must change. Output returns to Fusion
as an ordinary Image. A failed comparison is not a passing performance result.

Every call restores and verifies the previous EGL API, display, context and
draw/read surfaces. Cleanup closes the target and Ganesh context on their owning
thread, then destroys only the executor's EGL surface/context, not the display.

`GPU_STAGE` logs CPU wall-clock drawing, submit/wait and readback separately.
`GPU_RESULT` reports warm per-mode medians excluding the first sample. Those
whole-mode times include allocation, copies, upload, synchronization, readback
and diagnostic logging; they are not hardware GPU timestamps or frame times.

On 2026-10-11 the GPU gate passed on Mate 80, using its Maleoon 920 GPU. Identity
was exact in all 12 frames; normal refraction had a maximum channel error of 2.
Deep/dispersion had 8-17 outlier pixels per 529,200-pixel frame, within the
predeclared tolerance but not bit-identical to CPU. All 11 source and effect hash
transitions changed. All 37 EGL-state restoration checks and cleanup passed.

Warm whole-mode medians were 8.968 ms for refraction and 8.978 ms for deep output.
The paired GPU modes had a median of 18.242 ms versus 280.011 ms for the CPU pair
in the same gate. These include CPU transport and are not application frame
times. Source readPixels still had a 34.849 ms P95, and first GPU calls included
substantial initialization/compilation costs. The saved normal Skia HAP was
restored and its catalog checked after testing. This is a working GPU execution
boundary, not complete library integration or a proven whole-app speedup.

## Gate 4: Lower-Allocation Pixel Transfer

The direct C entry borrows input/output pixel memory synchronously. Its argument
order is input pointer, width, height, input row bytes, output row bytes, amount,
mode and output pointer. Modes 0/1/2 are identity/refraction/deep. Canonical pixels
are BGRA_8888, unpremultiplied, sRGB. No renderer-owned object crosses the ABI.

The Fusion host reads through a Pixmap into a reusable input array. The executor
reuses two thread-owned native Bitmaps, invalidates the source pixel generation
after each update, and preserves the old premultiplied GPU readback conversion.
It writes directly into a fresh Fusion-owned output Bitmap. Output images are
not pooled or overwritten after display; resize and mode-6 cleanup release the
executor buffers on their owning thread.

Nine synthetic checks cover three odd sizes, alpha values 0/32/96/192/255,
padded input/output strides and output tail guards. All nine must match the
legacy GPU output exactly and leave guards untouched. Twenty-four changing
Fusion list frames then require exact input/output parity, 23/23 adjacent source
and output changes and 11/11 changes independently for each material mode.
Execution order is balanced within each mode. The first two timing samples are
discarded; timing history stores numbers only, not pixel arrays or images.

On 2026-10-11 both Mate 80 runs passed all checks, including 103 EGL restoration
checks and cleanup per run. At 840x630 pixels, warm pipeline P50 improved from
21.357 to 8.245 ms in run 1 and from 29.169 to 11.678 ms in run 2. P95 improved
from 34.182 to 11.939 ms and from 47.042 to 15.324 ms respectively. These are
source pixel read, one GPU material call and return-image construction, not layer
export, validation, display upload, frame presentation or application frame time.
Native-buffer import and zero-copy transport remain unimplemented.

Reproduce the report from saved process-specific logs:

```sh
node experiments/fusion-material-probe/summarize-transfer.mjs <hilog-file>
```

The helper rejects missing/failed gates, non-exact transfer frames or missing
GPU cleanup. It reports P50/P95/max by mode and execution order. Detailed evidence
and the original component integration boundary are in
`docs/fusion-material-probe-validation.md`.

## Device Result

On 2026-10-11, Mate 80 (API 26) first passed the two CPU gates in the same Fusion process:

- Source marker/content/text checks: `90/90`; changed content: `89/89`.
- Original shader checks: `12/12`; changing output: `11/11`.
- Source size: `840x630` physical pixels.
- Export P50/P95: `1.831/2.579 ms`; readPixels P95: `33.160 ms`.
- Three raster modes combined P50: `218.243 ms`, not a single-effect timing.

Those CPU-run timings include allocations/conversion and possible GC, not an
isolated GPU cost or a frame-rate benchmark. Neither probe route is ready to
replace the normal demo.
The saved Skia HAP was reinstalled and started after testing. Details and local
evidence paths are in `docs/fusion-material-probe-validation.md`.

## Build

Build from the repository root:

```sh
./gradlew -p experiments/fusion-material-probe linkDebugSharedOhosArm64
```

Build a signed HAP using a scratch copy of the existing host and matching HAR:

```sh
sh experiments/fusion-material-probe/build-host.sh
```

The normal host's dependencies must already be installed. This script builds
both standalone libraries and stages matching `libkn.so`/`libkn_api.h`,
`libglass_material.so` and the two files under `host/` into a new temporary
directory. It does not install anything or run normal publish tasks.

The existing signing profile authorizes only the normal Glass bundle. Device
replacement requires explicit confirmation; build success alone is not a
successful source-export test. Restore the saved normal Skia HAP after testing.

## Link Requirements

- `--entry=0` and section GC remove the executable `_start`/`main` startup
  dependency emitted by this OHOS toolchain's default shared-library link.
- Skiko's OHOS ICU archive contains real unsuffixed `uloc_getDefault` and
  `uloc_toLanguageTag`. Linker aliases bind the `_skiko` references to these
  bundled implementations; no placeholder functions or private system API is
  used. The aliases and their targets must have matching local addresses.
- Strict loading stays enabled. No lazy-loading workaround or fake JPEG encoder
  is supplied. The raster gate uses Bitmap/Canvas and the GPU gate adds Surface/
  DirectContext; unused encoding code is removed by section GC. The executor
  retains the original runtime image filter.
