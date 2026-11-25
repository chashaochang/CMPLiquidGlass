# CPF 1.1.0 Fusion Renderer Validation

Date: 2026-10-11. Scope: the current Glass project only.

## Saved Baseline

Commit `e5e3c88` (`Save CPF 1.1.0 Skia catalog baseline`) on `kmp`
preserves the working Skia configuration and current catalog/components.
The commit is local; it has not been pushed.

| Component | Version |
| --- | --- |
| Kotlin | `2.2.21-1.1.0` |
| Compose Multiplatform | `1.9.2-1.1.0` |
| OHOS Skiko | `0.9.22.2-1.1.0` |
| Official `@cpf-kmp-cmp/compose` HAR | `1.9.2-1.1.0` |

Skia runtime logs report `getRendererTypeId id=0` and
`getRenderBackendId id=0`. Catalog startup, Bottom Tabs interaction/long press,
and returning to the catalog were verified on the phone. This is a targeted
smoke test, not exhaustive component or performance acceptance.

## Experiment

Changed `rendererBackend=skia` to `rendererBackend=fusion-renderer` in
`gradle.properties`. The existing native linker options cover the system
libraries required by the official unified-rendering guide. The same official
HAR is used for both backends.

No glass effects or crash protections were changed for this comparison.
The OHOS RenderEffect, RuntimeShader, and GraphicsLayer effect support flags
remain `true`.

Performed a clean native build without build/configuration caches:

```sh
./gradlew clean :app:publishReleaseBinariesToOhosApp \
  --no-build-cache --no-configuration-cache --console=plain
```

Native publication and Hvigor `clean assembleHap` succeeded. Installed the
new signed HAP. Both build and device logs confirm Fusion, not Skia:

```text
ComposePlugin: rendererBackend=fusion-renderer, skikoVersion=0.9.22.2-1.1.0
ComposePlugin: applied fusion-renderer linker options to ohosArm64
getRendererTypeId id=1
getRenderBackendId id=1 fnMode=1
```

Device: HUAWEI Mate 80, `VYG-AL00 7.0.0.109(SP6C00E105R3P4)`.
Bundle: `com.kyant.backdrop.catalog`.

## Reproduction And Result

The catalog homepage renders. Opening either Bottom Tabs or Buttons terminates
the process with `SIGSEGV(SEGV_ACCERR)` at `0x0000000000c65780`.
Each automated tap used the target's freshly read UI-tree bounds.

| Entry | Crash time | PID | Result |
| --- | --- | --- | --- |
| Bottom Tabs | 2026-10-11 12:32:15.559 | 32239 | Native crash |
| Buttons, after restart | 2026-10-11 12:37:11.588 | 33669 | Same native crash path |

Both reports identify the same `libkn.so` Build ID as the local Fusion binary:
`5db4e05ac6efc38a237b9146f211a14d4329cf2a`.
The staged library also matches the symbolized library's SHA-256.

Symbolized common frames:

```text
0xb07798 org_jetbrains_skia_RuntimeEffect__1nMakeForShader
0xafa47c Kotlin_K2NStub
0x9ba624 com.kyant.backdrop.RuntimeShader(String)
0xa84c5c com.kyant.backdrop.catalog.utils.InteractiveHighlight.<init>
```

The next component-specific frame is `LiquidBottomTabs` or `LiquidButton`.
`InteractiveHighlight` creates the shader through
`backdrop/src/ohosMain/kotlin/com/kyant/backdrop/RuntimeShader.kt`, which calls
`RuntimeEffect.makeForShader(shaderString)`.

Disassembly shows the native wrapper calling `SkRuntimeEffect::MakeForShader`
through the PLT. The crash register `x17` contains the invalid branch target
`0xc65780`. The dynamic symbol table lists `SkRuntimeEffect::MakeForShader`
and `SkRuntimeEffectBuilder::makeShader` as undefined imports. An undefined
import alone is normal for a shared library; combined with this call site and
invalid target, the evidence points to a native binding/linkage problem in
this RuntimeEffect path on the Fusion build.

This does not establish that every RuntimeShader or RenderEffect operation is
unsupported by Fusion. It does establish that Glass's current direct Skia
RuntimeEffect path is not usable in this tested configuration. Full glass
appearance, refraction, long-press behavior, and performance cannot be accepted
because component entry already crashes.

## Native Capability Follow-Up

Inspection of the matching Skiko tag `v0.9.22.2-1.1.0` found that its Fusion
build excludes `RuntimeEffect.cc` and `RuntimeShaderBuilder.cc`. Its Fusion
`ImageFilter.cc` also returns null for runtime-shader filters and filter
composition. An in-app dynamic-symbol probe did not find the tested
RuntimeEffect APIs; that temporary probe has since been removed.

This narrows the original crash diagnosis: the existing direct Skia shader
bindings cannot simply be reused with this Fusion build. These findings are
about the tested bindings, not every possible native OHOS rendering API.

## Support Boundary And Public Planning

Follow-up research used these pinned upstream sources:

- Skiko tag `v0.9.22.2-1.1.0`, the matching version used by this project.
- Fusion `v1.0.0`, commit `0cbef2a82436f4dba593595251ecacb0655ce17f`.
- Fusion `develop`, commit `58548a74b28ae34d073e467a44a5d0493296197c`.
  This is not asserted to be the exact source used to publish CPF 1.1.0.
- Installed HarmonyOS SDK API 26, native package `26.0.0.105`.

The relevant RuntimeEffect header, Compose filter implementation, and native
Drawing API adapter are unchanged between the two inspected Fusion refs.
The upstream `OHRender/API-todo/upgradeTodo.txt` states that custom shaders
are currently unsupported. `OHRender/API-todo/skimagefilter.txt` lists both
`Compose` and `RuntimeShader` as `no-support`.

The public SDK native_drawing headers, ArkTS drawing declarations, and arm64
SDK link-stub exports expose built-in shaders and several built-in image
filters, but no runtime-shader compiler/builder entry point was found. This
is a statement about this SDK's public API, not a claim that the OS internally
has no runtime shader engine or that future SDKs cannot add one.

Fusion shader and image-filter wrappers hand `OH_Drawing_ShaderEffect*` and
`OH_Drawing_ImageFilter*` to system drawing. A normal Skia shader pointer is
not a documented substitute for either handle. Consequently, restoring an
excluded bridge or linking a SkSL compiler alone would not establish support:
the original shader also needs a compatible execution path, child-image
binding, and composition with the system's layer/filter pipeline.

The CPF 1.1.0 release notes describe RenderNode, frame scheduling, overlay
linkage, and lifecycle work, not RuntimeShader enablement. Public Fusion issue
search for `shader` across open/closed issues returned four unrelated issues.
Issue #57 has a maintainer acknowledgment about documenting rendering modes
and known problems; it is not a RuntimeShader implementation commitment.
No explicit RuntimeShader delivery date was found in the inspected material.
This does not establish the maintainers' private plans or their full reasons
for omitting the feature.

### Implications For A Library-Backend Repair

- Preserve common components, original shader source, effect order, parameters,
  and GraphicsLayer background flow; do not substitute approximate materials.
- First establish how the original shader can be compiled and executed by the
  actual Fusion drawing backend. A missing native entry point is only part of
  the failure, not proof that adding a bridge makes the effect work.
- Then validate child-layer input, filter composition, coordinate transforms,
  alpha/color handling, clipping, resource lifetime, and frame synchronization.
- Shipping a separate Skia executor or custom GPU pipeline would be a hybrid
  backend with new ownership and performance costs, not pure Fusion support.
  That route has not been accepted as a replacement for this library repair.

No full backend-support patch has been implemented or verified. Claims that
the feature can definitely be added, or will be faster, are not justified by
the available evidence. Implementation is paused pending this feasibility work.

The subsequent [system-capability research](fusion-runtime-effects-system-capabilities.md)
confirms implemented internal OpenHarmony RuntimeEffect/RuntimeShaderBuilder
APIs. Their absence from the inspected public SDK is not absence of an internal
engine. Offline inspection of previously obtained binaries from the same phone
also confirms private runtime compiler, input-filter and serialization exports.
The system uses `std::__h`, while the tested Fusion binary imports some runtime
operations using `std::__n1`; existing bindings are not a drop-in ABI match.
The source-consuming filter exists in underlying system Skia, but its supported
Drawing/application bridge and full filter-graph/replay integration remain
unestablished. An exact-name shell probe was denied execution; application
resolution was not tested after the user chose offline-only research.

References:

- https://gitcode.com/CPF-KMP-CMP/fusion-render/blob/v1.0.0/OHRender/API-todo/upgradeTodo.txt
- https://gitcode.com/CPF-KMP-CMP/fusion-render/blob/v1.0.0/OHRender/API-todo/skimagefilter.txt
- https://gitcode.com/CPF-KMP-CMP/fusion-render/issues/57

## Withdrawn Local GPU Prototype

The former experimental route `glass.route=gpu` opened `pages/GpuGlass`.
Its page, controllers, native library, dependencies, and registration have now
been removed at the user's request. The observations below are historical;
the prototype did not fix the original catalog's Fusion shader bindings.

| Path | Observed result |
| --- | --- |
| Cropped or full `componentSnapshot.get` PixelMap | Native-buffer access failed with `IMAGE_DMA_NOT_EXIST` (`7600173`) |
| `BuilderNode` texture export with a custom component root | Exported into an `OH_NativeImage` source successfully |
| Two local `XComponentType.TEXTURE` outputs | Rendered sampled background on Mate 80 |

The prototype has a visible Fusion Compose scene and a second exported Fusion
Compose scene. Kotlin scroll state synchronizes the exported mirror with the
visible list. The GL output consists of two local glass regions, not one
full-screen XComponent. No CPU image-copy fallback was added.

### Appearance And Integration Limits

The prototype is not visually equivalent to the saved Skia implementation:

- Its GLSL uses a simplified rounded lens, five-tap blur, approximate rim and
  press highlight. It does not reproduce the original complete effect chain,
  color dispersion, shadow, highlight behavior, or configurable parameters.
- Its sampled source is a separately rendered mirror, not the visible scene's
  exact current frame. Scroll synchronization can lag; arbitrary animation,
  component state, and overlapping glass layers are not covered.
- Rendering the scene twice and the current conservative `glFinish()` calls
  are unresolved performance costs. A GPU import alone proves no speedup.
- Lifecycle, safe areas, rotation, stacking order, and the complete component
  set have not passed acceptance.

The reported abnormal appearance is consistent with these material differences;
the specific visible artifacts have not been individually isolated. Ordinary
shared liquid-glass component/shader sources were not replaced by this probe.

### Verification Level

The pre-batch-fix package logged Fusion backend IDs and GPU `Maleoon 920`.
Its background and both local glass regions rendered nonblank. Scrolling
changed the sampled content; a tap, a long press, and several swipes left the
process alive. This was a limited prototype smoke test, not visual parity or
end-to-end performance acceptance.

The latest package adds `beginFrame()` to update the source once for both glass
regions in a batch, latest-frame buffer dropping, and native trace markers.
Native publication and Hvigor packaging succeeded, and installation succeeded.
That latest package has not yet been launched or device-tested. A trace from
the preceding package was collected but has not been analyzed. Native present
timings are not whole-app frame times or an FPS result.

## Current State

The independent GPU experiment has been withdrawn from application sources.
Default `rendererBackend` is restored to `skia`; original effects remain enabled.
OHOS native publication and Android debug assembly succeeded after removal.
The staged native library and generated header again match the saved baseline.
No backend capability patch or effect-disablement workaround has been added.
Hvigor `clean assembleHap` also succeeded. Inspection of the signed HAP found
the original `libentry.so` and `libkn.so`, and no `libglassgpu.so`.

The installed phone package has not yet been replaced in this withdrawal step.
The saved baseline remains at `e5e3c88`; its signed backup is
`/private/tmp/glass-cpf-1.1.0-skia-baseline-signed.hap`.

The original Fusion RuntimeEffect failure remains unresolved. The next work
must establish a real original-library backend repair, not restore the withdrawn
prototype or promise feature completeness before native execution is validated.

Reference: the CPF documentation tag `KMP-v2.2.21-1.1.0`,
`zh-cn/UI开发/CMP统一渲染用户手册.md`, especially the backend verification and
self-rendering-to-unified-rendering sections.
