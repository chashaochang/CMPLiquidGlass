plugins {
    kotlin("multiplatform") version "2.2.21-1.1.0"
}

kotlin {
    ohosArm64 {
        binaries.sharedLib {
            baseName = "glass_material"
            linkerOpts(
                "-lz", "-luv", "-lunwind", "-lrawfile.z", "-lhilog_ndk.z", "-lEGL", "-lGLESv3",
                "-lace_napi.z", "-lnative_window", "-limage_source", "-lpixelmap",
                "-Bsymbolic", "--exclude-libs=ALL", "--version-script=${file("exports.map").absolutePath}",
                "--entry=0", "--gc-sections",
                // The bundled OHOS ICU archive uses unsuffixed names; alias the real implementations.
                "--undefined=uloc_getDefault", "--undefined=uloc_toLanguageTag",
                "--defsym=uloc_getDefault_skiko=uloc_getDefault",
                "--defsym=uloc_toLanguageTag_skiko=uloc_toLanguageTag",
            )
        }
    }
    sourceSets {
        ohosArm64Main {
            kotlin.srcDir("../../../backdrop/src/commonMain/kotlin")
            kotlin.include(
                "**/FilterExecutor.kt",
                "**/MaterialGpu.kt",
                "com/kyant/backdrop/internal/Shaders.kt",
                "org/intellij/lang/annotations/Language.kt",
            )
            dependencies {
                implementation("org.jetbrains.skiko:skiko-ohosarm64:0.9.22.2-1.1.0")
            }
        }
    }
}
