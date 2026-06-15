@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

package dev.azora.convention

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.*

/**
 * Custom source set hierarchy template for Kotlin Multiplatform.
 *
 * This template defines intermediate source sets that allow sharing code between
 * specific platform combinations. The hierarchy is:
 *
 * ```
 * common
 * ├── nonWeb (Android + iOS + Desktop)
 * │   ├── mobile (Android + iOS)
 * │   │   ├── androidMain
 * │   │   └── ios
 * │   │       ├── iosArm64
 * │   │       ├── iosArm64
 * │   │       └── iosSimulatorArm64
 * │   ├── jvmCommon (Android + Desktop JVM)
 * │   │   ├── androidMain
 * │   │   └── desktopMain (jvm)
 * │   └── native/apple/ios
 * └── native
 *     └── apple
 *         ├── ios (iosArm64, iosSimulatorArm64)
 *         └── macos
 * ```
 *
 * ## Use Cases
 * - **nonWebMain**: Historic name kept from the WASM era; spans all current targets
 * - **mobileMain**: Share mobile-specific code (permissions, sensors, etc.)
 * - **jvmCommon**: Share JVM code between Android and Desktop
 * - **native/apple**: Share native Apple-specific code
 *
 * @see applyHierarchyTemplate Extension to apply this template
 */
private val hierarchyTemplate = KotlinHierarchyTemplate {
    withSourceSetTree(
        KotlinSourceSetTree.main,
        KotlinSourceSetTree.test,
    )

    common {
        withCompilations { true }

        group("nonWeb") {
            withAndroid()
            withJvm()
            group("ios") {
                withIos()
            }
        }

        group("mobile") {
            withAndroid()
            group("ios") {
                withIos()
            }
        }

        group("jvmCommon") {
            withAndroid()
            withJvm()
        }

        group("native") {
            withNative()

            group("apple") {
                withApple()

                group("ios") {
                    withIos()
                }

                group("macos") {
                    withMacos()
                }
            }
        }
    }
}

/**
 * Matches the Android target regardless of which plugin created it.
 *
 * `withAndroidTarget()` only matches the legacy KGP-owned `androidTarget()`. With the
 * AGP-owned `com.android.kotlin.multiplatform.library` target (required since AGP 9),
 * matching has to go through the compilation's platform type instead.
 */
private fun KotlinHierarchyBuilder.withAndroid() {
    withCompilations { it.target.platformType == KotlinPlatformType.androidJvm }
}

/**
 * Applies the custom hierarchy template to this multiplatform extension.
 *
 * Call this in your KMP extension configuration to enable the custom source set hierarchy.
 *
 * @see hierarchyTemplate For the hierarchy structure
 */
fun KotlinMultiplatformExtension.applyHierarchyTemplate() {
    applyHierarchyTemplate(hierarchyTemplate)
}
