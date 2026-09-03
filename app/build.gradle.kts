@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val enableExtraTargets = providers.gradleProperty("enableExtraTargets")
    .map(String::toBoolean)
    .getOrElse(false)

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.jetbrains.compose)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    if (enableExtraTargets) {
        jvm("desktop")

        js(IR) {
            browser()
        }
        wasmJs {
            browser()
        }

        macosArm64()
    }

    listOf(
        iosArm64("iosArm64"),
        iosSimulatorArm64("iosSimulatorArm64"),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    ohosArm64 {
        binaries {
            sharedLib {
                baseName = "kn"
                export(libs.compose.multiplatform.export)
                linkerOpts(
                    "-lz",
                    "-lnative_drawing",
                    "--no-as-needed",
                    "-lrawfile.z",
                    "--as-needed",
                    "-limage_source",
                    "-lpixelmap",
                    "-lpixelmap_ndk.z",
                    "-lnative_window",
                    "-lace_napi.z",
                    "-lhilog_ndk.z",
                    "-lhitrace_ndk.z",
                    "-lffrt.z",
                    "-luv",
                    "-lunwind",
                    "-licu",
                )
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.compose.foundation)
                implementation(libs.compose.ui)
                implementation(libs.compose.ui.graphics)
                implementation(libs.compose.resources)
                implementation(libs.compose.material.ripple)
                implementation(project(":shapes"))
                implementation(project(":backdrop"))
            }
        }

        val androidMain by getting {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.androidx.activity.compose)
            }
        }

        val skikoMain by creating {
            dependsOn(commonMain)
        }

        if (enableExtraTargets) {
            val desktopMain by getting {
                dependsOn(skikoMain)
                dependencies {
                    implementation(compose.desktop.currentOs)
                }
            }

            val macosArm64Main by getting {
                dependsOn(skikoMain)
            }
        }

        val iosMain by creating {
            dependsOn(skikoMain)
        }

        val iosArm64Main by getting {
            dependsOn(iosMain)
        }

        val iosSimulatorArm64Main by getting {
            dependsOn(iosMain)
        }

        val ohosMain by creating {
            dependsOn(commonMain)
            dependencies {
                api(libs.compose.multiplatform.export)
            }
        }

        val ohosArm64Main by getting {
            dependsOn(ohosMain)
        }

        if (enableExtraTargets) {
            val jsMain by getting {
                dependsOn(skikoMain)
            }

            val wasmJsMain by getting {
                dependsOn(skikoMain)
            }
        }

        all {
            languageSettings.enableLanguageFeature("ContextParameters")
        }
    }

    compilerOptions {
        freeCompilerArgs.addAll("-Xlambdas=class")
    }
}

android {
    namespace = "com.kyant.backdrop.catalog.common"
    compileSdk = 37
    buildToolsVersion = "37.0.0"

    defaultConfig {
        minSdk = 21
    }
}

if (enableExtraTargets) {
    compose.desktop {
        application {
            mainClass = "com.kyant.backdrop.catalog.MainKt"

            nativeDistributions {
                targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
                packageName = "com.kyant.backdrop.catalog"
                packageVersion = "1.0.0"
            }
        }
    }
}

arrayOf("debug", "release").forEach { buildType ->
    val capitalizedType = buildType.replaceFirstChar { it.uppercase() }
    val ohosEntryDir = rootProject.layout.projectDirectory.dir("ohosApp/entry")
    val binaryOutputDir = layout.buildDirectory.dir("bin/ohosArm64/${buildType}Shared")
    val binaryOutputDirFile = layout.buildDirectory.dir("bin/ohosArm64/${buildType}Shared").get().asFile
    val composeResourcesDir = ohosEntryDir.dir("src/main/resources/rawfile/composeResources").asFile
    val composeResourcePackage = "${rootProject.name.lowercase()}.${name.lowercase()}.generated.resources"

    tasks.register<Copy>("publish${capitalizedType}BinariesToOhosApp") {
        group = "harmony"
        description = "Publish OHOS Kotlin/Native binaries to ohosApp."
        doNotTrackState("The OHOS staging directory is managed by the deployment task and may contain native outputs from DevEco.")
        dependsOn("link${capitalizedType}SharedOhosArm64")
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        into(ohosEntryDir)

        from(binaryOutputDir.map { it.file("libkn_api.h") }) {
            into("src/main/cpp/include/arm64-v8a")
        }
        from(binaryOutputDir.map { it.file("libkn.so") }) {
            into("libs/arm64-v8a")
        }

        doFirst {
            composeResourcesDir.deleteRecursively()
            if (!binaryOutputDirFile.exists()) {
                throw GradleException("Missing OHOS binary output: ${binaryOutputDirFile.absolutePath}")
            }
        }
        from(layout.buildDirectory.dir("generated/compose/resourceGenerator/preparedResources/commonMain/composeResources")) {
            into("src/main/resources/rawfile/composeResources/$composeResourcePackage")
        }
    }

    tasks.register("publish${capitalizedType}BinariesToHarmonyApp") {
        group = "harmony"
        description = "Compatibility alias for the OHOS app publish task."
        dependsOn("publish${capitalizedType}BinariesToOhosApp")
    }
}
