plugins {
    kotlin("multiplatform") version "2.2.21-1.1.0"
    kotlin("plugin.compose") version "2.2.21-1.1.0"
    id("org.jetbrains.compose") version "1.9.2-1.1.0"
}

kotlin {
    ohosArm64 {
        binaries.sharedLib {
            baseName = "kn"
            export("org.jetbrains.compose.export:export:1.9.2-1.1.0")
            linkerOpts(
                "-lz", "-lnative_drawing", "--no-as-needed", "-lrawfile.z", "--as-needed",
                "-limage_source", "-lpixelmap", "-lpixelmap_ndk.z", "-lnative_window",
                "-lace_napi.z", "-lhilog_ndk.z", "-lhitrace_ndk.z", "-lffrt.z", "-luv",
                "-lunwind", "-licu",
            )
        }
    }
    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.compose.foundation:foundation:1.9.2-1.1.0")
            implementation("org.jetbrains.compose.ui:ui:1.9.2-1.1.0")
            implementation("org.jetbrains.compose.ui:ui-graphics:1.9.2-1.1.0")
        }
        ohosArm64Main.dependencies {
            api("org.jetbrains.compose.export:export:1.9.2-1.1.0")
        }
    }
}
