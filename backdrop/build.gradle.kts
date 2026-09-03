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

    iosArm64("iosArm64")
    iosSimulatorArm64("iosSimulatorArm64")
    ohosArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.compose.foundation)
                implementation(libs.compose.ui)
                implementation(libs.compose.ui.graphics)
                implementation(project(":shapes"))
            }
        }

        val skikoMain by creating {
            dependsOn(commonMain)
        }

        if (enableExtraTargets) {
            val desktopMain by getting {
                dependsOn(skikoMain)
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
}

android {
    namespace = "com.kyant.backdrop"
    compileSdk = 37
    buildToolsVersion = "37.0.0"

    defaultConfig {
        minSdk = 21
    }
}
