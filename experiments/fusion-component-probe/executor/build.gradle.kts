plugins {
    kotlin("multiplatform") version "2.2.21-1.1.0"
    kotlin("plugin.compose") version "2.2.21-1.1.0"
    id("org.jetbrains.compose") version "1.9.2-1.1.0"
}

val localSkiko = providers.gradleProperty("probeSkikoKlib").orNull?.let(::file)
if (localSkiko != null) {
    configurations.configureEach { exclude(group = "org.jetbrains.skiko") }
}

kotlin {
    ohosArm64 {
        binaries.sharedLib {
            baseName = "glass_component"
            export("org.jetbrains.compose.export:export:1.9.2-1.1.0")
            linkerOpts(
                "-lz", "-luv", "-lunwind", "-lrawfile.z", "-lohresmgr", "-lhilog_ndk.z",
                "-lEGL", "-lGLESv2", "-lGLESv3",
                "-lace_napi.z", "-lnative_window", "-limage_source", "-lpixelmap", "-licu",
                "-L${file("../../../ohosApp/entry/oh_modules/@cpf-kmp-cmp/compose/libs/arm64-v8a").absolutePath}",
                "--no-as-needed", "-lskikobridge", "--as-needed",
                "-Bsymbolic", "--exclude-libs=ALL", "--version-script=${file("exports.map").absolutePath}",
                "--entry=0", "--gc-sections",
                "--undefined=uloc_getDefault", "--undefined=uloc_toLanguageTag",
                "--defsym=uloc_getDefault_skiko=uloc_getDefault",
                "--defsym=uloc_toLanguageTag_skiko=uloc_toLanguageTag",
            )
        }
    }
    sourceSets {
        commonMain {
            kotlin.srcDirs(
                "../../../backdrop/src/commonMain/kotlin",
                "../../../shapes/src/commonMain/kotlin",
                "../../../app/src/commonMain/kotlin",
            )
            kotlin.include(
                "com/kyant/backdrop/*.kt", "com/kyant/backdrop/backdrops/**",
                "com/kyant/backdrop/effects/**", "com/kyant/backdrop/internal/**",
                "com/kyant/backdrop/highlight/**", "com/kyant/backdrop/shadow/**",
                "com/kyant/shapes/**", "org/intellij/lang/annotations/**",
                "com/kyant/backdrop/catalog/components/LiquidBottomTab.kt",
                "com/kyant/backdrop/catalog/components/LiquidBottomTabs.kt",
                "com/kyant/backdrop/catalog/utils/DampedDragAnimation.kt",
                "com/kyant/backdrop/catalog/utils/DragGestureInspector.kt",
                "com/kyant/backdrop/catalog/utils/InteractiveHighlight.kt",
                "com/kyant/backdrop/catalog/utils/Coroutines.kt",
                "com/kyant/backdrop/catalog/FlightIcon.kt",
            )
            dependencies {
                api("org.jetbrains.compose.export:export:1.9.2-1.1.0")
                implementation("org.jetbrains.compose.foundation:foundation:1.9.2-1.1.0")
                implementation("org.jetbrains.compose.ui:ui:1.9.2-1.1.0")
                implementation("org.jetbrains.compose.ui:ui-graphics:1.9.2-1.1.0")
            }
        }
        ohosArm64Main {
            kotlin.srcDirs(
                "../../../backdrop/src/ohosMain/kotlin",
                "../../../app/src/ohosMain/kotlin",
                "../../fusion-material-probe/executor/src/ohosArm64Main/kotlin",
            )
            kotlin.include(
                "com/kyant/backdrop/Platform.kt", "com/kyant/backdrop/RuntimeShader.kt",
                "com/kyant/backdrop/internal/**",
                "com/kyant/backdrop/catalog/utils/Coroutines.kt",
                "com/kyant/backdrop/probe/ComponentExecutor.kt",
                "com/kyant/backdrop/probe/MaterialGpu.kt",
            )
            dependencies {
                if (localSkiko == null) {
                    implementation("org.jetbrains.skiko:skiko-ohosarm64:0.9.22.2-1.1.0")
                } else {
                    implementation(files(localSkiko))
                }
            }
        }
        all { languageSettings.enableLanguageFeature("ContextParameters") }
    }
}
