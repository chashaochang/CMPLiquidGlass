# Liquid Glass (Backdrop)

![frontPhoto](artworks/banner.jpg)

A customizable Liquid Glass effect library for Compose Multiplatform.

## Docs

[![Maven Central](https://img.shields.io/maven-central/v/io.github.kyant0/backdrop)](https://central.sonatype.com/artifact/io.github.kyant0/backdrop)

[Documentation](https://kyant.gitbook.io/backdrop)

## Components

The library does not include any high-level components; you will need to create your own.
Below are some example components:

- [LiquidButton](/app/src/commonMain/kotlin/com/kyant/backdrop/catalog/components/LiquidButton.kt)
- [LiquidToggle](/app/src/commonMain/kotlin/com/kyant/backdrop/catalog/components/LiquidToggle.kt)
- [LiquidSlider](/app/src/commonMain/kotlin/com/kyant/backdrop/catalog/components/LiquidSlider.kt)
- [LiquidBottomTabs](/app/src/commonMain/kotlin/com/kyant/backdrop/catalog/components/LiquidBottomTabs.kt)

## HarmonyOS

The Glass catalog uses the CPF 1.1.0 toolchain with the Skia renderer:

| Component | Version |
| --- | --- |
| Kotlin | `2.2.21-1.1.0` |
| Compose Multiplatform | `1.9.2-1.1.0` |
| OHOS Skiko | `0.9.22.2-1.1.0` |
| `@cpf-kmp-cmp/compose` HAR | `1.9.2-1.1.0` |

The host vendors the [official OHPM HAR](https://repo.harmonyos.com/ohpm/@cpf-kmp-cmp/compose/-/compose-1.9.2-1.1.0.har) at `ohosApp/entry/libs/compose.har`.
Keep this HAR and the Gradle versions aligned. Always publish `libkn.so` and `libkn_api.h` from the same native build.

```sh
./gradlew :androidApp:assembleDebug
./gradlew harmonyApp
```

`harmonyApp` builds, installs, and launches the catalog on a connected device using Debug native binaries.
For performance checks, run `:app:publishReleaseBinariesToOhosApp` and build the HAP with Hvigor directly; rerunning `harmonyApp` replaces the staged native binaries with Debug ones.

## Demo

- [Backdrop Catalog](./androidApp/release/androidApp-release.apk)

![Screenshots of Backdrop Catalog](artworks/catalog_app.jpg)
