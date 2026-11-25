# Fusion Runtime Effects: Required System Capabilities

Date: 2026-10-11. Scope: feasibility research for the original Glass library.
No rendering implementation, backend switch, device installation, or performance
test was performed for this research.
The latest follow-up is offline only, using previously obtained device binaries
and local source checkouts. No application-process probe or further device
experiment was performed after the user chose offline research.

## Conclusion

The missing capability is not generic GPU rendering. The original effects need
programmable shaders and source-consuming image filters inside the Drawing
pipeline used by Fusion, with compatible handles and recorded-layer semantics.

OpenHarmony's graphics engine contains implemented internal C++ `RuntimeEffect`
and `RuntimeShaderBuilder` APIs. This was checked in both a release tag and a
current source snapshot, including implementations that call Skia RuntimeEffect.
Previously obtained Mate 80 system binaries also export these APIs and the
underlying Skia source-consuming RuntimeShader image filter. Thus, neither
"the system has no runtime shader engine" nor "the phone lacks the underlying
runtime input filter" describes the available evidence correctly.

However, the inspected public HarmonyOS SDK does not expose corresponding
runtime shader compiler/builder APIs through Drawing. Device ELF exports prove
binary presence, not application loader access, ABI-safe invocation, or working
Fusion integration. The actual system C++ namespace differs from both the
public SDK and the tested Kotlin/Native Fusion binary. A supported bridge from
an application into the actual native renderer remains unestablished.

Restoring CPF bindings alone would not establish full support. Runtime image
input binding, filter composition, recording/replay, and frame updates must also
work. A pure-Fusion, public-API-only repair is not proven feasible at present.

## Evidence Scope

| Source | Inspected version |
| --- | --- |
| Project | Local Skia baseline `e5e3c88`, original effects still enabled |
| CPF Skiko | Tag `v0.9.22.2-1.1.0` |
| CPF Fusion | `v1.0.0`, `0cbef2a82436f4dba593595251ecacb0655ce17f`; `develop`, `58548a74b28ae34d073e467a44a5d0493296197c` |
| HarmonyOS SDK | API 26, native package `26.0.0.105`, public headers, ArkTS declarations, and arm64 link-stub exports |
| Previously acquired device binaries | Mate 80 / `VYG-AL00 7.0.0.109(SP6C00E105R3P4)`, base system `OpenHarmony-7.0.0.105`, API 26 |
| Previously built Fusion binary | `libkn.so`, Build ID `5db4e05ac6efc38a237b9146f211a14d4329cf2a`, matching the documented crash |
| OpenHarmony graphics | `OpenHarmony-v6.0-Release`, `ab8ad47aee9eaaf22df79e93a645eda7a892f33e` |
| OpenHarmony graphics | Source snapshot `ae94ce5359339c62ab1ea380f749e1a2e4abc0d8`, commit date 2026-10-10 |

Neither inspected Fusion ref is asserted to be the exact source used to publish
CPF 1.1.0. Neither OpenHarmony ref is asserted to match the phone's Huawei OS.
SDK link stubs describe the application link surface, not device execution.
The HarmonyOS documentation-search service failed to connect; the conclusions
below use inspected SDK declarations and upstream source, not that service.

## Actual Device Binary Evidence

These files were acquired before the offline-only decision. This follow-up
only reads their local copies; it does not retrieve additional device files.

| Device library | Local copy under `/private/tmp/` | ELF Build ID |
| --- | --- | --- |
| `/system/lib64/libnative_drawing.so` | `glass-device-native-drawing-20261011.so` | `5c4a63ddc6f3056b2c99d20e962f3797` |
| `/system/lib64/lib2d_graphics.z.so` | `glass-device-2d-graphics-20261011.so` | `0f70e31a7e1861ac68c4b67a13098317` |
| `/system/lib64/libskia_canvaskit.z.so` | `glass-device-skia-canvaskit-20261011.so` | `93c9d8068132f2cf68f2994c3c0932db` |

The actual native Drawing library's SONAME is `libnative_drawing_ndk.z.so`.
Its dependencies include both `lib2d_graphics.z.so` and
`libskia_canvaskit.z.so`. This is a static dependency graph, not evidence that
the application's loader namespace permits direct access to private libraries.

Confirmed defined dynamic C++ exports include:

- Drawing `RuntimeEffect::CreateForShader`, `RuntimeShaderBuilder::MakeShader`,
  float/array uniform setters, `SetUniformVec4`, `SetChild`, and `MakeImage`.
- Drawing `ImageFilter::CreateComposeImageFilter`, ShaderEffect/ImageFilter
  serialization, and their Skia adapters.
- System Skia `SkRuntimeEffect::MakeForShader`,
  `SkRuntimeEffectBuilder::makeShader`, `SkImageFilters::RuntimeShader`, and
  `SkImageFilters::Compose`.
- System Skia `SkRuntimeShader::CreateProc`,
  `SkRuntimeImageFilter::CreateProc`, its `flatten` and `onFilterImage` methods,
  and `SkRegisterRuntimeImageFilterFlattenable`.

The system Drawing binary also contains DDGR runtime implementations. Export
presence does not establish which engine the current Fusion context selects;
a bridge hardwired to raw Skia objects would not cover that distinction.

No runtime compiler/builder or source-consuming runtime-filter C factory was
found in the actual native Drawing library. Its
`OH_Drawing_ShaderEffectCreateCompose` and
`OH_Drawing_ImageFilterCreateFromShaderEffect` exports are different operations,
as explained below.

| Verification gate | Current evidence |
| --- | --- |
| Engine source has runtime compilation/building | Confirmed in inspected OpenHarmony refs |
| Actual phone contains the required private engine symbols | Confirmed by offline dynamic-symbol inspection |
| Public API 26 exposes equivalent runtime constructors | Not found in inspected headers, declarations, or link stubs |
| Application can resolve the exact private symbols | Not established |
| Invocation is ABI-safe and returns compatible Drawing handles | Not established |
| Original effects survive Fusion recording/replay and stay live | Not tested |
| Performance is better than the current Skia baseline | Not measured |

### C++ ABI Boundary

The binary evidence identifies three different C++ ABI namespaces:

| Boundary | Observed namespace |
| --- | --- |
| Device system Drawing/Skia dynamic exports | `std::__h` |
| Tested Fusion `libkn.so` RuntimeEffect uniform/child imports | `std::__n1` |
| Installed public native SDK `__config_site` | `_LIBCPP_ABI_NAMESPACE __1`, ABI version 1 |

For example, the Fusion binary imports
`SkRuntimeEffect::findUniform(std::__n1::basic_string_view<...>)`, while the
system library exports the `std::__h` variant. These are different mangled
symbols, so the existing binding is not a drop-in match for the system binary.

Some names, including `MakeForShader(SkString, Options const&)`, contain no
visible STL type and are identical. That still does not establish compatible
class layouts, return objects, builder containers, reference counting, allocators,
or destruction. Changing namespace macros, importing internal headers, or
fabricating a `NativeHandle` is not an ABI-safe repair.

The saved Fusion binary's wrapper at `0xb07798` calls the compiler PLT entry;
the recorded invalid target `0xc65780` equals that ELF's `.plt` start address.
Together with its undefined runtime imports, this reinforces the binding/
resolution diagnosis. It does not prove why the loader left that target or
that directly linking a private library would repair the full shader path.

### Probe Limitation

Before the offline-only decision, a C-only shell probe was compiled to resolve
exact exported names with `dlopen`/`dlsym`, never to invoke private functions.
Device execution was rejected with `Permission denied`; no symbol-resolution
results were produced. The temporary device executable was removed. The HDC
command's exit status did not establish execution success.

An application-process follow-up was proposed but not authorized. Earlier
negative probes of tested API names do not negate the private C++ exports now
confirmed in these binaries. No loader restriction has been bypassed.

## Original Library Requirements

- `RuntimeShader.kt` creates a RuntimeEffect and a RuntimeShaderBuilder. It
  exposes float/vector uniforms, integer uniforms, color uniforms, and a shader
  for Paint/ShaderBrush use. Integer-array uniforms currently throw; this
  research did not establish a current effect requiring them.
- `InteractiveHighlight.kt` uses a procedural shader without a background child,
  through `ShaderBrush` and additive blending. This is a paint-shader path.
- `Lens.kt` binds the name `content` through `ImageFilter.makeRuntimeShader`.
  It requires the prior layer/filter image as a child shader, not just uniform
  values. Original refraction/dispersion samples `content.eval(...)` at displaced
  coordinates. The dispersion variant performs seven input samples.
- `ProgressiveBlur.kt` first blurs, then samples that result in a runtime shader
  with an alpha/tint gradient. A procedural paint shader alone is insufficient.
- OHOS `internal/RenderEffect.kt` calls `ImageFilter.makeCompose` to preserve
  `outer(inner(source))` effect order.
- `LayerBackdrop.kt` replays the existing `GraphicsLayer`, with relative
  coordinates and inverse transforms. It does not require a screen screenshot
  or a second Compose list. `DrawBackdropModifier.kt` records padded regions
  so blur/refraction and pressed overflow are not clipped to the nominal box.

## Capability Matrix

"Public" below means declared in the inspected SDK, not device-tested here.

| Required capability | Public system surface | Internal engine/source evidence | Responsibility and remaining gap |
| --- | --- | --- | --- |
| Compile the original SkSL-compatible source and bind typed uniforms/children | No Drawing runtime compiler/builder found in API 26 headers, ArkTS drawing declarations, or Drawing link-stub exports | `RuntimeEffect::CreateForShader`, `RuntimeShaderBuilder::SetUniform`, `SetChild`, and Skia adapter implementations exist; corresponding private device exports are confirmed | A supported application-facing native entry point is needed before CPF can bridge it. Original source compatibility, validation/errors, integer/color semantics, and uniform lifetime still need verification |
| Execute the resulting shader as a Paint/ShaderBrush | Public Brush accepts an `OH_Drawing_ShaderEffect*`; public constructors provide built-in colors, gradients, image shaders, and blends | Internal RuntimeShaderBuilder produces a Drawing ShaderEffect | Need a supported constructor/handle bridge for the runtime shader. An app's ordinary `SkShader*` is not a documented substitute |
| Execute a runtime image filter with automatic binding of the current source or previous filter result to `content` | `OH_Drawing_ImageFilterCreateFromShaderEffect` exists, but does not provide a child-input name or image-filter input | ShaderImageFilter is a source-generating leaf filter. The device's raw Skia RuntimeShader input-filter factory exists, but no counterpart was found in the inspected Drawing ImageFilter facade | Need a Drawing-compatible bridge to that contract, input image binding, bounds/offset mapping, and original layer replay. A runtime paint shader plus a leaf filter does not supply this |
| Compose filters in the original order | Blur, color-filter, and offset constructors accept an input filter; no generic Drawing image-filter Compose constructor found | `ImageFilter::CreateComposeImageFilter` and its Skia adapter call to `SkImageFilters::Compose` exist | Built-in chains may be adapted within CPF, but arbitrary original Compose needs a supported path. The current source rejects Lazy inputs in the direct Compose factory |
| Record/replay the effects within Fusion's native rendering lifecycle | Canvas SaveLayer, transforms, command recording, and replay are public | Drawing serialization/lazy materialization exist; device Skia has runtime flattening and reconstruction exports | A constructor succeeding is not sufficient: runtime effects must survive the actual recording/replay path, preserve resource ownership, and update when either source content or uniforms change |
| GPU execution, alpha/color correctness, and synchronization | Drawing GPU contexts/surfaces, NativeImage/NativeBuffer, and image-processing APIs exist | Skia adapters exist; the device also has DDGR runtime implementations. GPU execution is not the missing API by itself | Preserve premultiplied alpha, sRGB/color-uniform meaning, sampling, clipping, blending, thread/context ownership, and release timing. Zero-copy and performance are unverified. An external executor would additionally require bidirectional image interop and fences |

For the original in-renderer image-filter route, the source is already part of
the recorded layer. It should be bound by the renderer; a screen capture,
mirrored scene, or public export of the system's raw framebuffer is not an
intrinsic requirement. NativeBuffer/EGL interop becomes an additional problem
only if execution is moved out of that pipeline. That alternative is not the
accepted repair direction.

## Important Non-Equivalences

### ShaderEffect Compose Is Not ImageFilter Compose

`OH_Drawing_ShaderEffectCreateCompose(dst, src, mode)` blends two shaders.
It does not implement the original sequential filter operation
`outer(inner(source))`, and does not compile arbitrary shader source.

### Shader-Based ImageFilter Is Not RuntimeShader ImageFilter

The inspected native implementation of
`OH_Drawing_ImageFilterCreateFromShaderEffect` creates
`ImageFilterLazy::CreateShader`. Its eventual engine implementation calls
`SkImageFilters::Shader`, not `SkImageFilters::RuntimeShader`.

The internal ImageFilter documentation explicitly calls this a leaf filter
that introduces shader-generated input. It does not automatically bind the
previous image as the original shader's `content` child. This distinction
matters even after a runtime shader compiler becomes available.

### Internal Headers Are Not An Application ABI

The system implementation uses native handles around Drawing-owned objects.
It also has serialization, lazy materialization, and engine-specific adapters.
Copying internal headers or constructing a lookalike handle in the app would
not establish ABI compatibility, supported ownership, or replay safety.

In the inspected current source, direct `CreateComposeImageFilter` explicitly
rejects Lazy inputs, while public NDK filter constructors create Lazy objects.
This is a concrete reason why calling a discovered internal factory is not
automatically a valid replacement for the public native path. It is not proof
of why CPF maintainers originally chose to omit support.

### Four Float Arguments Are Not Necessarily A Float4 Uniform

In the inspected current OpenHarmony Skia adapter,
`SetUniform(name, x, y, width, height)` builds five normalized sampling offsets.
It is not the original Glass operation that writes four arbitrary floats.
`SetUniformVec4` is a separate operation. Integer uniform overloads were not
found in the inspected Drawing builder interface either.

A bridge must preserve typed uniforms and the existing color/premultiplication
contract, not match overloads by parameter count. This is source-contract
evidence, not a claim that the phone's exact function body has been tested.

### EffectKit And ImageEffect Do Not Supply A Drop-In Drawing Bridge

EffectKit's `OH_Filter_SetColorMatrix` is a color transformation, not arbitrary
spatial refraction. Its inspected PixelMap processing API states that it does
not support multi-threaded calls.

ImageKit's separate ImageEffect API provides public custom filter delegates
(`OH_EffectFilter_Register`, API 12), NativeBuffer input/output, and texture-ID
processing (`OH_EffectFilter_RenderWithTextureId`, API 20). Texture processing
requires valid GL_TEXTURE_2D objects and different input/output texture IDs.

This is a genuine custom-processing mechanism, not evidence of a runtime SkSL
compiler or a Drawing-compatible runtime filter. It would still need original
shader execution, correct input-layer access, a return path into Fusion's
Drawing pipeline, threading/context compatibility, and synchronization. None
of that integration was implemented or accepted by this research.

Relevant public SysCaps include:

- `SystemCapability.Graphic.Graphic2D.NativeDrawing`
- `SystemCapability.Graphic.Graphic2D.NativeImage`
- `SystemCapability.Graphic.Graphic2D.NativeBuffer`
- `SystemCapability.Multimedia.ImageEffect.Core`

These capability names do not imply runtime-shader support or guaranteed GPU
interop; exact APIs, OS versions, and device support must be checked separately.

## Recording And Replay Integration

The inspected `CmdListHelper` has two materially different paths:

1. Non-Lazy ShaderEffect/ImageFilter objects serialize the underlying engine
   object into the command list and deserialize it during replay. The Skia
   adapters use `SkBinaryWriteBuffer` and `SkReadBuffer`. The device's runtime
   flatten/reconstruction exports provide groundwork for this route, but do
   not prove factory registration, source-acceptance policy, or actual replay.
2. Lazy objects record a Drawing Object subtype. Replay recreates that subtype
   and calls `Materialize()` to obtain the engine object. `ImageFilterLazy`
   explicitly rejects direct `Serialize`/`Deserialize`; these paths must not
   be mixed.

`ShaderImageFilterObj` stores only a ShaderEffect and crop rectangle. Its
materialization calls `CreateShaderImageFilter`, not a source-consuming
RuntimeShader filter. Reusing that subtype cannot provide the missing
`content` input binding.

If the platform chooses to add declarative Lazy runtime/Compose objects, the
objects need source/builder-state, named input-filter references, coordinate/
sampling metadata, marshalling, and subtype registration in every replay
process that receives them. Registering a new subtype only in the HAP does
not update a separate system process. The existing `ObjectHelper` registry
returns no constructor for an unrecognized type/subtype.

Conversely, if a supported platform bridge produces compatible non-Lazy
objects and the existing runtime flattening route accepts them, a new Lazy
subtype is not automatically necessary. Offline evidence cannot decide that
route on the phone. It would be wrong to label the whole serialization engine
missing or to prescribe an IPC-format change before checking this.

The Skia ShaderEffect/ImageFilter deserializers in the inspected source return
`true` after a non-null data input without explicitly checking the reconstructed
engine object. A successful boolean result alone is therefore not an acceptance
criterion; the restored shader/filter and its output must be verified.

Finally, the input binding must remain a reference to the renderer's current
layer/filter result. A captured bitmap or materialization cache alone does not
provide live background updates. Uniform/source changes must rebuild or
invalidate the recorded effect appropriately without per-frame recompilation
or uncontrolled GPU resource allocation.

## What Can Be Added Where

1. **CPF/Skiko:** implement missing wrappers, preserve filter graphs, correct
   capability reporting, avoid unresolved native calls, and handle supported
   built-in effects. These changes cannot create a missing system execution
   hook by themselves.
2. **Platform Drawing layer:** expose a supported runtime compiler/builder,
   runtime shader handles, source-consuming runtime image filter, and generic
   filter composition. Verify their recording/replay and lifetime contracts.
   Internal engine code provides useful groundwork, not a finished public API.
3. **OpenHarmony system build:** source changes could be developed in a system
   fork, but deploying that fork is a platform task. Modifying Glass's HAP does
   not update the graphics service on a stock Mate 80. The shipped Huawei OS's
   matching engine/ABI and release path are not established by this research.

This research does not establish a private CPF/vendor integration route, an
official delivery schedule, or why all of these APIs have not been exposed.

### Scoped Platform Patch Map

This is an offline design map, not an implemented or deployable patch. It uses
the original source and filter graph, not a separate Glass renderer.

| Layer | Candidate patch surface | Required work |
| --- | --- | --- |
| Native application entry | Drawing NDK compiler/builder/filter constructors and handle helpers | A supported C boundary with opaque handles, compiler diagnostics, typed uniforms, ownership and destruction; no cross-boundary STL or raw app Skia pointers |
| Drawing facade and engine adapters | `effect/image_filter.h`, `image_filter_impl.h`, runtime builder access, Skia/DDGR adapters | Add the source-consuming RuntimeShader image-filter contract with named input, coordinates and sample bounds; establish active-engine support instead of forcing one private adapter |
| Composition | `image_filter.cpp`, engine Compose implementation, optionally Lazy objects | Preserve outer/inner order and safely materialize or represent Lazy inputs; direct Compose currently rejects them |
| Recording/replay | `cmd_list_helper.cpp`, shader/filter serialization; optional Object subtypes and their build/registration | First test existing runtime flattening; add new wire types only if needed, with source validation, resource lifetime and system replay support |
| CPF Skiko/Fusion bridge | RuntimeEffect/Builder native bindings, `ImageFilter.cc`, native build exclusions | Implement against the supported platform entry, retain original common-code semantics, and avoid unresolved imports or falsely reporting full support |

The clean platform route therefore does not require inventing a shader
compiler: the engine groundwork exists. The unresolved part is exposing and
integrating it safely. An OpenHarmony source patch can be designed offline,
but a Glass HAP cannot by itself replace the stock Huawei Drawing/Render
Service implementation. A vendor-supported entry or confirmed private ABI
route would be separate evidence; neither is established here.

## Minimum Contract And Verification Gates

The following is a proposed capability contract, not a list of existing public
API names:

1. Compile original source; return a valid effect or an explicit diagnostic;
   bind uniforms/children safely; expose a Drawing-compatible shader handle.
2. Apply that shader as a native image filter while binding the current input
   image by name, with documented local coordinates, offsets, and sample bounds.
3. Compose that filter with blur/color filters in the original order and carry
   it through the same recorded-layer execution and update lifecycle.

Only after that supported entry path is established should implementation
verification proceed from original procedural highlight to original lens,
blur-plus-lens/progressive blur, then moving backgrounds and multiple layers.
Check press overflow, per-corner radii, light/dark alpha, return/disposal,
rotation, and resource release. Compilation alone is not visual acceptance.
Measure whole-app device frame times before claiming a Fusion performance win.

## Sources

Original-library files are in this repository:

- `backdrop/src/ohosMain/kotlin/com/kyant/backdrop/RuntimeShader.kt`
- `backdrop/src/ohosMain/kotlin/com/kyant/backdrop/internal/RenderEffect.kt`
- `backdrop/src/commonMain/kotlin/com/kyant/backdrop/effects/Lens.kt`
- `backdrop/src/commonMain/kotlin/com/kyant/backdrop/internal/Shaders.kt`
- `backdrop/src/commonMain/kotlin/com/kyant/backdrop/backdrops/LayerBackdrop.kt`
- `backdrop/src/commonMain/kotlin/com/kyant/backdrop/DrawBackdropModifier.kt`
- `app/src/commonMain/kotlin/com/kyant/backdrop/catalog/utils/InteractiveHighlight.kt`
- `app/src/commonMain/kotlin/com/kyant/backdrop/catalog/components/ProgressiveBlur.kt`

Pinned upstream evidence:

- [OpenHarmony release: RuntimeShaderBuilder interface](https://gitcode.com/openharmony/graphic_graphic_2d/blob/ab8ad47aee9eaaf22df79e93a645eda7a892f33e/rosen/modules/2d_graphics/include/effect/runtime_shader_builder.h)
- [OpenHarmony release: RuntimeEffect implementation](https://gitcode.com/openharmony/graphic_graphic_2d/blob/ab8ad47aee9eaaf22df79e93a645eda7a892f33e/rosen/modules/2d_graphics/src/drawing/engine_adapter/skia_adapter/skia_runtime_effect.cpp)
- [OpenHarmony release: native ImageFilter constructors](https://gitcode.com/openharmony/graphic_graphic_2d/blob/ab8ad47aee9eaaf22df79e93a645eda7a892f33e/rosen/modules/2d_graphics/drawing_ndk/src/drawing_image_filter.cpp)
- [Current snapshot: ImageFilter contracts](https://gitcode.com/openharmony/graphic_graphic_2d/blob/ae94ce5359339c62ab1ea380f749e1a2e4abc0d8/rosen/modules/2d_graphics/include/effect/image_filter.h)
- [Current snapshot: Compose/Lazy input boundary](https://gitcode.com/openharmony/graphic_graphic_2d/blob/ae94ce5359339c62ab1ea380f749e1a2e4abc0d8/rosen/modules/2d_graphics/src/drawing/effect/image_filter.cpp)
- [Current snapshot: image-filter engine adapter](https://gitcode.com/openharmony/graphic_graphic_2d/blob/ae94ce5359339c62ab1ea380f749e1a2e4abc0d8/rosen/modules/2d_graphics/src/drawing/engine_adapter/skia_adapter/skia_image_filter.cpp)
- [Current snapshot: four-float uniform versus Vec4](https://gitcode.com/openharmony/graphic_graphic_2d/blob/ae94ce5359339c62ab1ea380f749e1a2e4abc0d8/rosen/modules/2d_graphics/src/drawing/engine_adapter/skia_adapter/skia_runtime_shader_builder.cpp)
- [Current snapshot: recorded shader/filter paths](https://gitcode.com/openharmony/graphic_graphic_2d/blob/ae94ce5359339c62ab1ea380f749e1a2e4abc0d8/rosen/modules/2d_graphics/src/drawing/recording/cmd_list_helper.cpp)
- [Current snapshot: Lazy image-filter materialization](https://gitcode.com/openharmony/graphic_graphic_2d/blob/ae94ce5359339c62ab1ea380f749e1a2e4abc0d8/rosen/modules/2d_graphics/src/drawing/effect/image_filter_lazy.cpp)
- [Current snapshot: leaf ShaderImageFilter object](https://gitcode.com/openharmony/graphic_graphic_2d/blob/ae94ce5359339c62ab1ea380f749e1a2e4abc0d8/rosen/modules/2d_graphics/src/drawing/effect/shader_image_filter_obj.cpp)
- [Current snapshot: Drawing native-handle ownership](https://gitcode.com/openharmony/graphic_graphic_2d/blob/ae94ce5359339c62ab1ea380f749e1a2e4abc0d8/rosen/modules/2d_graphics/drawing_ndk/drawing_utils/drawing_helper.h)
- [Current snapshot: Skia reconstruction helper](https://gitcode.com/openharmony/graphic_graphic_2d/blob/ae94ce5359339c62ab1ea380f749e1a2e4abc0d8/rosen/modules/2d_graphics/src/drawing/engine_adapter/skia_adapter/skia_helper.h)
- [CPF Skiko: Fusion build exclusions](https://gitcode.com/CPF-KMP-CMP/skiko/blob/v0.9.22.2-1.1.0/skiko/buildSrc/src/main/kotlin/tasks/configuration/NativeTasksConfiguration.kt)
- [CPF Skiko: image-filter bindings](https://gitcode.com/CPF-KMP-CMP/skiko/blob/v0.9.22.2-1.1.0/skiko/src/nativeJsMain/cpp/ImageFilter.cc)
- [CPF Fusion: documented shader limitation](https://gitcode.com/CPF-KMP-CMP/fusion-render/blob/v1.0.0/OHRender/API-todo/upgradeTodo.txt)

Local public API evidence is under:

```text
/Applications/DevEco-Studio.app/Contents/sdk/default/openharmony/
  native/sysroot/usr/include/native_drawing/drawing_shader_effect.h
  native/sysroot/usr/include/native_drawing/drawing_image_filter.h
  native/sysroot/usr/include/native_drawing/drawing_canvas.h
  native/sysroot/usr/include/native_drawing/drawing_record_cmd.h
  native/sysroot/usr/include/native_drawing/drawing_gpu_context.h
  native/sysroot/usr/include/native_drawing/drawing_surface.h
  native/sysroot/usr/include/native_effect/effect_filter.h
  native/sysroot/usr/include/multimedia/image_effect/image_effect.h
  native/sysroot/usr/include/multimedia/image_effect/image_effect_filter.h
  native/sysroot/usr/lib/aarch64-linux-ohos/libnative_drawing.so
  ets/api/@ohos.graphics.drawing.d.ts
```

The historical crash and withdrawn prototype are documented separately in
[CPF 1.1.0 Fusion validation](cpf-1.1.0-fusion-validation.md).

Offline binary inspection used the installed SDK's `llvm-nm -D --defined-only
--demangle`, `llvm-readobj --notes --dynamic-table`, `llvm-readelf --relocations`,
and `llvm-objdump`. SHA-256 values for the three device artifacts and saved
Fusion binary, respectively:

```text
352c04b7c70402079a6481b838c43ec27e37bb7a2576baa6b7eaf8fb57b8b1f3
083686b4f96fc41bd62c4c85c4669da942cee8e07f3bdc1c1f032d957f4eceb6
3ee390c67cf89b93a1f65b64a5714e02105abaaa8f71d75868c36843ce2f4822
58f5f7336446efbaa5f0277955bfbd2a11224b5015bd11e8aafcd51e9792cf00
```
