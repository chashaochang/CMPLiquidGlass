# Fusion component probe

This is a separate experiment. The normal Glass app remains on Skia.

## Boundary

- Fusion owns the real background list and the final image composition.
- A symbol-isolated Skia library compiles the original `LiquidBottomTabs`,
  `LiquidBottomTab`, animations, backdrop effects, highlights and shadows directly
  from this checkout. It owns a small `CanvasLayersComposeScene` and Ganesh target.
- The complete glass component subtree is isolated, not just its material shader.
- Only borrowed BGRA pixels, scalar input events and measured semantics bounds cross
  the render ABI. The public `napi_env` initializes the isolated platform runtime.
- No full-screen XComponent, duplicated background UI, rewritten shader or private
  system render API is used. This is CPU transport, not zero-copy.

## Predeclared gates

These thresholds are recorded before the first component device test:

1. The Fusion crop must use the measured background and component-shell coordinates.
   It must contain real changing list content, not a stand-in texture.
2. Compare the entire original component from independent CPU/GPU scenes at the
   same frame timestamp, density, theme and input sequence. Compare alpha and the
   visible SrcOver RGB on the actual background. Ignore undefined transparent RGB.
3. Channel tolerance is 3/255. At most 1% of the union of nontransparent component
   pixels may exceed it; empty output fails. Do not loosen after a failed test.
4. Test 24 changing background samples, 12 per theme. Both source and component
   output must change on all 23 transitions. At least 1,000 component pixels must
   be nontransparent; pixels outside the component must remain transparent.
5. Query the original semantics tree before synthetic clicks/presses/drags. Test
   tap selection, held press growth, drag selection and cancellation/release.
6. Submit displayed images into Fusion, check its draw acknowledgements, inspect
   the device UI tree/screenshot, verify PID survival and explicit scene/GPU cleanup.

Whole-app frame rate, display presentation latency, accessibility completeness,
multiple-component ordering, zero-copy and production integration remain separate
questions. CPU reference tests are correctness tests, not FPS measurements.

## Build

```sh
sh experiments/fusion-component-probe/prepare-skiko.sh original-skiko.klib skia-m132-source libjpeg-turbo-m132.tar.gz /tmp/component-skiko.klib
GLASS_PROBE_SKIKO_KLIB=/tmp/component-skiko.klib sh experiments/fusion-component-probe/build-host.sh
```

The build script stages a separate OHOS host in a temporary directory; it never
installs anything or alters the normal app's backend/build output.

Device tests require an already-backed-up signed normal HAP. Keep the probe installed
across incremental attempts in one test batch. Restore once when the batch ends:

```sh
sh experiments/fusion-component-probe/run-device.sh probe-signed.hap normal-skia-signed.hap evidence-prefix
sh experiments/fusion-component-probe/run-device.sh --restore normal-skia-signed.hap evidence-prefix
```

The runner restores on interruption. For a single complete, self-restoring batch,
set `GLASS_PROBE_RESTORE_AT_EXIT=true` for that invocation. Neither operation reverts
source code or Git commits.

## Initial result: not passed

On 2026-10-11 the executor and Fusion host linked, and the scratch signed HAP built.
The first Mate 80 attempt (PID 52798) failed at `dlopen(RTLD_NOW | RTLD_LOCAL)`:
`OH_ResourceManager_GetMediaData` was unresolved. No component frame, input or
performance gate ran. This is not a successful Bottom Tabs integration.

The SDK identifies `libohresmgr.so` as the public provider. An additional offline
SDK/host symbol audit flagged `glEGLImageTargetTexture2DOES` and a Skia JPEG encoder
overload. SDK export availability and host dependency graphs alone are not proof
of runtime namespace resolution. These dependencies still need a proper fix;
do not substitute fake functions or lazy loading to label the probe passed.

The saved normal HAP was restored (PID 53643), and the UI tree contains the normal
catalog. Its SHA-256 is `052a14d1f9b357d2886ff69c72c60f22d77777bc1ccc616bec69adac5fedf908`.
Normal Glass source/backend are unchanged. New component-level viability remains
unproven; the earlier material-only GPU result is a separate, narrower result.

## Native dependency repair

The 2026-10-11 repair explicitly links the public `libohresmgr.so` and
`libGLESv2.so`. The published Skiko KLIB contains an Image bridge referring to
the Skia JPEG encoder, but its native Skia archive omits that encoder. Recompiling
Image.cc with function sections did not remove the reference; that attempt did
not pass the offline audit and was not installed.

`prepare-skiko.sh` instead compiles the unmodified upstream `SkJpegEncoderImpl.cpp`
and `SkJPEGWriteUtility.cpp` into a local copy of the original KLIB. It keeps the
original Image bridge and other native archives. There are no fake symbols or
lazy-loader workarounds. Normal Glass and shared dependency caches are unchanged.
The executor checks an 8x8 JPEG encode/decode round trip before scene creation.

Source inputs:

- Skiko 0.9.22.2-1.1.0 KLIB SHA-256:
  `fd128575a6917b04e0bcd6635499c664f97edba41dde830b4df587472ab57f96`.
- Skia revision `a00c390e98` from `https://github.com/JetBrains/skia`, matching the
  CPF Skiko `m132-a00c390e98-1` configuration. Source tarball SHA-256:
  `1d51cd7557b07ab71a329312726da1db2d143fa85ac7e28e3ac6cb14aafbbec7`.
- libjpeg-turbo revision `ccfbe1c82a3b6dbe8647ceb36a3f9ee711fba3cf` from
  `https://chromium.googlesource.com/chromium/deps/libjpeg_turbo`, as pinned by
  the Skia DEPS file. Archive SHA-256:
  `cc740beedd21de4f95229f5af0314a911402199a425ca912707652502c89a9f7`.

The corrected library passes the offline export/dependency audit (five exports,
32 inspected libraries, no missing strong symbols). Runtime component validation
is a separate gate, not implied by this audit or HAP compilation.
