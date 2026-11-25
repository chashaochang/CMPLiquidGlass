# Fusion Material-Level Fallback: Device Validation

Date: 2026-10-11. Scope: current Glass project only.

## Experiment

`experiments/fusion-material-probe` is a standalone build selecting Fusion with
the same CPF 1.1.0 versions as normal Glass. Normal `gradle.properties` remains
`rendererBackend=skia`; original library/component/shader files are unchanged.

Gate 1 exports an existing `GraphicsLayer` containing a real Compose list via
`toImageBitmap()` and returns it to Fusion as an ordinary Image. It does not
capture the screen or build a second/mirrored UI scene. Gate 1 alone does not
invoke RuntimeEffect.

The automatic test runs 90 scroll updates. Success requires all 90 exact
color-marker matches, all 90 frames containing opaque list content and black
text, and at least 80 changing content hashes outside the marker. Export/read
CPU timings are recorded separately. A marker-only or frozen result must fail.

Gate 2 loads a separate pure-Skiko library with a strict local C ABI. It compiles
the original Glass shader source directly and executes the original refraction
and dispersion shaders through RuntimeEffect, RuntimeShaderBuilder and a runtime
image filter. No Skia objects, OHRender objects or private system handles cross
the boundary. The resulting pixels return to the same Fusion UI as an image.

## Build And Device Evidence

- Both standalone Kotlin/Native links passed; configuration cache reuse passed.
- The matching Fusion library/header and isolated executor were staged into a
  scratch copy of the existing OHOS host. CMake, ArkTS, HAP packaging and signing
  passed through `build-host.sh`, including all 34 scratch-host Hvigor tasks.
- With explicit user permission, the probe replaced the original bundle briefly
  on Mate 80, API 26. No automated UI click was performed; the test runs itself.
- Successful gate-2 HAP:
  `/private/var/folders/z0/4jhg_6jd21d3kmp8sqq3tgcr0000gn/T/glass-fusion-material-probe.Z5ERh2/entry/build/default/outputs/default/entry-default-signed.hap`
- Successful process: PID `13476`. Both gates reported PASS, and its screenshot
  showed the source list and the returned deep/dispersion output in Fusion.
- The saved Skia HAP was reinstalled and started after testing (PID `14791`).
  The process remained alive and a fresh screenshot showed the normal Backdrop
  Catalog. Normal source, backend selection and host staging were not replaced.

Verified CPU-only run:

| Check | Result |
| --- | --- |
| Source size | 840x630 physical pixels |
| Marker matches | 90/90 |
| Opaque content and black text | 90/90 each |
| Changed content hashes outside marker | 89/89 |
| Export P50 / P95 | 1.831 / 2.579 ms |
| readPixels P95 | 33.160 ms |
| Shader identity mismatch | 0 pixels in all 12 frames |
| Original refraction vs source | 7,829-8,422 differing pixels per frame |
| Deep/dispersion vs normal refraction | 10,298-10,744 differing pixels per frame |
| Valid shader frames / changed outputs | 12/12 / 11/11 |
| Three raster modes combined P50 | 218.243 ms |

Local evidence (temporary files):

- `/private/tmp/glass-fusion-material-probe-gate2-passed-20261011.log`
- `/private/tmp/glass-fusion-material-probe-gate2-passed-ui-20261011.json`
- `/private/tmp/glass-fusion-material-probe-gate2-passed-20261011.jpeg`
- Restored catalog: `/private/tmp/glass-skia-restored-after-fusion-probe-20261011.jpeg`
- Restore log: `/private/tmp/glass-skia-restored-after-fusion-probe-20261011.log`
- Saved normal HAP: `/private/tmp/glass-cpf-1.1.0-skia-baseline-signed.hap`

## GPU Gate: Passed On Device

The executor now has an app-owned GLES3 EGL context, a 1x1 pbuffer, a real Skia
Ganesh `DirectContext.makeGL()` and a reusable render target at the source size.
CPU and GPU paths use the same original shader strings and drawing function.
The GLES context is independent of Fusion; no renderer-owned C++ objects cross
the existing scalar/pixel-buffer ABI. This does not add a display surface or
XComponent and does not alter the normal Glass backend.

The automatic GPU gate compares 12 changing source images against CPU references:

- Identity: no pixel with channel error greater than 3.
- Refraction and deep/dispersion: at most 1% pixels above channel error 3.
- Refraction must differ from the source by more than 100 pixels; deep output
  must differ from normal by more than 100 pixels.
- Source, normal and deep content hashes exclude the marker area and must change
  on all 11 transitions.
- Every executor call verifies restoration of the calling thread's EGL API,
  display, context and draw/read surfaces. Cleanup also runs under that check.

Per-mode warm medians exclude the first frame and include copies, allocation,
upload, synchronization, readback and logging. `GPU_STAGE` logs drawing,
submit/wait and readback CPU wall-clock intervals separately. Neither measurement
is a hardware GPU timestamp or proof of 60/120 fps application performance.

Both standalone links and scratch-host packaging/signing passed again with the
EGL assertions and stage timing. Configuration cache reuse passed. The current
executor now exports only `glass_material_render` and
`glass_material_render_direct`; its dynamic imports contain
no unresolved Skia/Skiko/Kotlin implementation symbols. GPU imports are public
EGL/GLES APIs.

Successful signed GPU-probe HAP:
`/private/var/folders/z0/4jhg_6jd21d3kmp8sqq3tgcr0000gn/T/glass-fusion-material-probe.FvTnXb/entry/build/default/outputs/default/entry-default-signed.hap`

An earlier attempt was rejected with `10106102` while the phone was locked and
did not run the GPU gate. After the user unlocked Mate 80, the current probe was
installed and started successfully (PID `36652`, 17:04-17:05 local time). The
GPU identified itself as `HUAWEI / Maleoon 920 / OpenGL ES 3.2`, not a software
or raster renderer.

| Check | Result |
| --- | --- |
| Source marker/content/text checks | 90/90 each; content changed 89/89 |
| Original CPU shader gate | 12/12 valid; outputs changed 11/11 |
| GPU identity | Exact output in all 12 frames |
| GPU refraction vs CPU | 0 pixels above channel error 3; maximum channel error 2 |
| GPU deep/dispersion vs CPU | 8-17 outlier pixels per frame; maximum channel error 85 |
| GPU valid frames | 12/12 under the predeclared tolerance |
| Source / refraction / deep hash transitions | 11/11 each, excluding the marker |
| Warm GPU identity median | 10.573 ms |
| Warm GPU refraction median | 8.968 ms |
| Warm GPU deep/dispersion median | 8.978 ms |
| Warm paired GPU modes median | 18.242 ms |
| Warm paired CPU modes median in the same gate | 280.011 ms |
| Source export P50 / P95 | 1.878 / 2.653 ms |
| Source readPixels P50 / P95 / max | 2.757 / 34.849 / 58.624 ms |
| Verified EGL-state restorations | 37, including cleanup |
| GPU cleanup | Result 0; context and surface closed |

Each frame contains 529,200 pixels. Deep outliers therefore occupy at most
0.0033% of a frame, but deep output is not bit-identical to CPU. The largest
channel differences are not characterized by the current log; exact GPU-versus-
CPU parity, full Fusion translucent-widget scenes and other color spaces remain
additional tests.
No comparison threshold was relaxed after observing the results.

Warm CPU wall-clock stage medians:

| Mode | Draw/setup | Submit/wait | Readback |
| --- | --- | --- | --- |
| Refraction | 0.885 ms | 1.704 ms | 2.748 ms |
| Deep/dispersion | 0.813 ms | 1.616 ms | 2.692 ms |

These stage medians must not be added and called a hardware GPU duration. The
first whole-mode calls took 89.079 ms (identity, including context setup),
57.346 ms (refraction) and 42.545 ms (deep), so cold initialization/compilation
still needs a prewarm/lifecycle strategy before interactive integration.

The screenshot and UI tree showed the original Fusion list and returned GPU
deep output together, with the automatic passing status. No UI click was used.
The process remained alive through capture. Afterward the saved normal Skia HAP
was reinstalled and started (PID `38031`); a fresh screenshot showed Backdrop
Catalog. The normal source/backend/host staging remained unchanged.

Local evidence:

- `/private/tmp/glass-fusion-material-probe-gpu-20261011.log`
- `/private/tmp/glass-fusion-material-probe-gpu-ui-20261011.json`
- `/private/tmp/glass-fusion-material-probe-gpu-20261011.jpeg`
- `/private/tmp/glass-skia-restored-after-gpu-probe-20261011.log`
- `/private/tmp/glass-skia-restored-after-gpu-probe-ui-20261011.json`
- `/private/tmp/glass-skia-restored-after-gpu-probe-20261011.jpeg`

## Direct Pixel Transfer: Passed Twice On Device

The added `glass_material_render_direct` entry keeps the original shaders and
GPU drawing path. It changes pixel transport/allocation only. Arguments are
borrowed input pointer, width, height, input row bytes, output row bytes, amount,
mode and borrowed output pointer; modes 0/1/2 select identity/refraction/deep.
Both sides use BGRA_8888, unpremultiplied, sRGB pixels. No native renderer object
or ownership handle crosses the boundary.

The host reads a Fusion Bitmap's Pixmap directly into a reusable pinned input
array. The executor copies it into a pooled native source Bitmap and calls
`notifyPixelsChanged`, then uses the existing Ganesh target and original drawing
function. A second pooled Bitmap preserves the old N32-premultiplied readback;
Pixmap conversion writes directly into the caller's fresh output Bitmap. Output
images retain the normal managed lifetime and are deliberately not reused while
Fusion can still display them. Native buffers are thread-owned and are released
on size changes and mode-6 cleanup. The timing history retains scalars only.

Gate 4 first checks nine synthetic native-buffer cases: 67x49, 73x55 and 79x61,
each in identity/refraction/deep mode. Each source includes alpha values
0/32/96/192/255. Input/output row padding and output tail guards must remain
unchanged, and every output pixel must exactly match the old GPU path. These
are native-buffer alpha/conversion tests, not full Fusion translucent-widget or
wide-gamut validation.

It then compares 24 changing real Fusion-layer frames using old and direct
transport. Both source and output must be pixel-exact, with 23/23 adjacent
source/output changes outside the marker and 11/11 changing outputs independently
for normal and deep mode. Order alternates in two-frame groups so both material
modes exercise both orders. This is a paired exploratory benchmark, not isolated
single-route or application frame timing. No tolerance was relaxed.

The current signed HAP was built with configuration-cache reuse and all 34
scratch-host Hvigor tasks passing:
`/private/var/folders/z0/4jhg_6jd21d3kmp8sqq3tgcr0000gn/T/glass-fusion-material-probe.6FzZuL/entry/build/default/outputs/default/entry-default-signed.hap`

Two runs on Mate 80, PID 13548 and PID 23005, passed all five gates:

| Check | Run 1 | Run 2 |
| --- | --- | --- |
| Fusion source marker/content/text checks | 90/90; 89/89 changes | 90/90; 89/89 changes |
| Original CPU shader gate | 12/12; 11/11 changes | 12/12; 11/11 changes |
| Original GPU shader gate | 12/12; 11/11 changes per source/mode | 12/12; 11/11 changes per source/mode |
| Native alpha/stride checks | 9/9 exact; zero guard errors | 9/9 exact; zero guard errors |
| Real-frame old/direct parity | 24/24 exact input and output | 24/24 exact input and output |
| Source/output adjacent changes | 23/23 each | 23/23 each |
| Normal/deep output changes | 11/11 each | 11/11 each |
| EGL state restoration | 103 checks; cleanup result 0 | 103 checks; cleanup result 0 |

Each real frame is 840x630 physical pixels. Warm stats discard the first two
frames and use the remaining 22. P95 uses sorted index `floor((n - 1) * 0.95)`;
small-sample tail values are not sustained-performance evidence.

| CPU wall-clock measurement | Run 1 old / direct | Run 2 old / direct |
| --- | --- | --- |
| Source pixel read P50 | 3.212 / 1.758 ms | 6.066 / 3.206 ms |
| Source pixel read P95 | 18.109 / 2.972 ms | 32.799 / 5.070 ms |
| Material call P50, including transport/upload/readback | 11.187 / 6.367 ms | 14.043 / 8.502 ms |
| Return-image construction P50 | 1.397 / 0.105 ms | 1.607 / 0.123 ms |
| Total pipeline P50 | 21.357 / 8.245 ms | 29.169 / 11.678 ms |
| Total pipeline P95 | 34.182 / 11.939 ms | 47.042 / 15.324 ms |
| Total pipeline max | 42.884 / 14.695 ms | 53.897 / 16.328 ms |
| Normal total P50 | 21.357 / 8.271 ms | 29.814 / 11.292 ms |
| Deep total P50 | 21.474 / 8.245 ms | 23.152 / 12.963 ms |

Both execution-order groups also improved. The two full-pipeline median
comparisons show about 60% lower time; absolute timings vary between runs and
the cause of that variation was not profiled. Stage medians must not be added
and called a measured whole-frame duration. Shared layer export P50 was 1.531 ms
and 2.629 ms, measured separately. Total pipeline timing excludes layer export,
pixel validation/hashing, display upload and presentation. It does not measure
multiple concurrent materials or promise 60/120 fps.

An independent helper validates the log gates and exact transfer samples before
reporting statistics by mode and order:

```sh
node experiments/fusion-material-probe/summarize-transfer.mjs \
  /private/tmp/glass-fusion-material-probe-transfer-20261011.log \
  /private/tmp/glass-fusion-material-probe-transfer-repeat-20261011.log
```

The first attempt to save probe UI evidence through the IDE tools failed because
they could not resolve a Harmony project. Run 1 therefore has process logs but
no passing-probe screenshot. HDC's public layout/screenshot commands captured
run 2 successfully: its tree says `Direct pixel transfer gate passed`, and its
screenshot shows the Fusion source list and returned original GPU deep output.
The passing process stayed alive through capture.

The saved normal Skia HAP was restored after each run, PID 19945 and PID 25939
respectively. The first restoration encountered a USB system dialog; its
confirmation button was read from a fresh UI tree before clicking its center,
without changing the selected charging mode. Final restored screenshots show
the normal Backdrop Catalog without that dialog. Normal backend, original
component/shader source and host staging remain unchanged.

Local evidence:

- `/private/tmp/glass-fusion-material-probe-transfer-20261011.log`
- `/private/tmp/glass-fusion-material-probe-transfer-repeat-20261011.log`
- `/private/tmp/glass-fusion-transfer-repeat-ui-20261011.json`
- `/private/tmp/glass-fusion-material-probe-transfer-repeat-20261011.jpeg`
- `/private/tmp/glass-skia-restored-after-transfer-ui-20261011.json`
- `/private/tmp/glass-skia-restored-after-transfer-20261011.jpeg`
- `/private/tmp/glass-skia-restored-after-transfer-20261011.log`
- `/private/tmp/glass-skia-restored-after-transfer-repeat-ui-20261011.json`
- `/private/tmp/glass-skia-restored-after-transfer-repeat-20261011.jpeg`
- `/private/tmp/glass-skia-restored-after-transfer-repeat-20261011.log`
- `/private/tmp/glass-fusion-material-transfer-report-20261011.json`
- `/private/tmp/glass-material-transfer-symbols-20261011.txt`

## Lower-Copy Transport Audit

The installed SDK exposes `OH_PixelmapNative_CreatePixelmapFromNativeBuffer`
since API 22 and `OH_PixelmapNative_GetNativeBuffer` since API 12. These are a
candidate public transport path, not evidence that the whole route is zero-copy.

CPF Skiko 1.1.0's `OhosImageInteropSession` contract explicitly states that Fusion
returns `Unsupported` for SurfaceBuffer import. Its legacy Fusion import entry
also throws `UnsupportedOperationException`. The shared `OhosImageDecoder`
accepts encoded/rawfile/media requests, not an existing native PixelMap.
`Image.toComposeImageBitmapNoCopy` removes a bitmap copy for an already valid
renderer-owned Image; it does not convert an executor's Skia Image into a Fusion
Image or establish cross-context synchronization.

The inspected OHRender source has a PixelMap-backed Image constructor and
Bitmap/Pixmap pixel access, which are leads for a renderer-scoped bridge. That
source observation is not an exposed CPF Kotlin API or a verified binary ABI.
Do not pass independent Skia handles to those constructors. A native-buffer
bridge must establish the actual Fusion Image entry, pixel format/alpha/color
handling, lifetime, generation invalidation and producer/consumer fences on the
device before replacing CPU readback. No such bridge was added in this step.

## Isolation And Load Repairs

The executor's ELF dynamic symbol table defines only `glass_material_render`
and `glass_material_render_direct`.
`-Bsymbolic`, `--exclude-libs=ALL` and a version script keep implementation symbols
local, including its separately linked Kotlin/Native runtime. `RTLD_NOW` remains
enabled, and the loaded library is not unloaded while its GC workers exist.

Earlier strict-load failures happened before shader execution, not as SIGSEGV:

1. An unrelated JPEG encoder reference was pulled in by Image/Surface wrappers.
   The raster executor now uses Bitmap/Canvas and bitmap shaders, still applying
   the original runtime image filter. No fake encoder was introduced.
2. The shared library retained executable `_start` code referencing `main`.
   `--entry=0` and section GC remove that startup root. The final ELF entry is 0,
   and `main` is no longer imported.
3. Skiko's native language API expects `_skiko` ICU names, but its bundled OHOS
   static ICU uses unsuffixed names. Linker aliases bind both references to the
   real bundled implementations. Symbol inspection confirms matching local
   addresses for each alias/target and no unresolved `_skiko` ICU imports.

Disabling C-interface generation also disabled the explicit entry; the attempted
`moduleIncludeOnly` setting did not change C export filtering in this build.
Neither ineffective setting remains. These packaging repairs do not establish
the root cause of historical crashes in the normal Fusion shader path.

## Limits

The experiment establishes a working dynamic-pixel transport and original-shader
CPU/GPU execution boundary in one Fusion application process. It is **not a
complete mixed-rendering integration or a proven whole-application performance
improvement**. The isolated GPU material segment is faster than its measured CPU
reference; source readback, CPU allocations/copies and return-image construction
still remain outside that comparison.

The earlier three-mode raster timing includes identity, normal and deep renders
with allocation/copy costs; it is not one shader's timing. GPU mode timings also
include CPU transport. Export/read measurements include allocation/conversion
and possible GC, not an isolated GPU cost. List jumps plus pixel validation are
not a 60/120 fps scrolling benchmark. The current path must not replace normal
Glass.

Component clipping, overflow, transforms, source-coordinate mapping, all effect
chains, full Fusion semitransparent/wide-gamut cases, press animations, multiple materials,
long-duration lifecycle stability and sustained frame time remain unverified.

## Subsequent Gates

CPU pixel-transfer/allocation reduction has passed the direct-transfer gate.
NativeBuffer transport remains a separate investigation, blocked on finding a
public supported Fusion Image import path with explicit frame synchronization.
Do not claim zero-copy based on Pixmap access or substitute private constructors.

The next component gate is the original `LiquidBottomTabs`, not a separately
drawn imitation. Source inspection identifies these integration requirements:

- `DrawBackdropNode.recordBackdropBlock` already records the component's actual
  source using its coordinates, padding, `onDrawBackdrop` callback and inverse
  layer transform. The adapter must export this component-sized recording,
  not render a mirrored page or independently guess its crop.
- Replace only unsupported material evaluation at `updateEffects`/
  `drawBackdropLayer`. Preserve the existing layout, capsule clipping, layer
  transform, highlight, shadow, inner shadow, surface and content/front callbacks.
  No host RenderEffect/ImageFilter pointer may enter the isolated executor.
- The base tabs use the original vibrancy -> blur -> lens chain. Their hidden
  accent layer feeds the selected droplet through `CombinedBackdrop`; the
  selected droplet also uses dispersion, pressure scale and velocity distortion.
  Current ABI exposes only amount/mode and hard-coded probe shape uniforms: it
  cannot represent this complete chain and must not be installed as its adapter.
- An explicit value-only effect plan must retain the original DSL's effect
  order, color matrix, blur/tile mode, size, radii, padding, refraction height,
  depth/dispersion uniforms and current generation. Shared original source must
  remain authoritative, rather than duplicating shader formulas or component UI.
- Reusing output images requires proven consumer lifetime/generation handling;
  no such pooling is enabled. Background changes and press/drag state must drive
  updates independently, without stale snapshots or unbounded frame queues.
- Compare the complete chain against normal Skia at rest, on press, during drag,
  with changing list content, in light/dark themes and with multiple materials.
  Then measure actual frame times against the current normal demo. These checks
  are not implemented or passed by the material probe.

Do not introduce a mirrored UI, approximate replacement shader or full-screen
XComponent renderer. The normal Glass backend stays Skia until the real-component
and sustained-performance gates pass.
