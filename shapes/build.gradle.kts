@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val ohosBuildRequested = gradle.startParameter.taskNames.isEmpty() ||
    gradle.startParameter.taskNames.any { it.contains("Ohos", ignoreCase = true) }
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
    iosArm64("iosArm64")
    iosSimulatorArm64("iosSimulatorArm64")

    ohosArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.ui)
        }

        all {
            languageSettings.enableLanguageFeature("ContextParameters")
        }
    }
}

android {
    namespace = "com.kyant.shapes"
    compileSdk = 37
    buildToolsVersion = "37.0.0"

    defaultConfig {
        minSdk = 21
    }
}
