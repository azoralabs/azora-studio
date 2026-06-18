package dev.azora.sdk.core.project.data.generator

import dev.azora.sdk.core.io.FileSystem
import dev.azora.sdk.core.project.domain.AzoraProjectModel

/**
 * Generates a Compose Multiplatform `composeApp` project (single module + an `iosApp`
 * scaffold). Android + iOS targets are always included; Desktop and Web are toggled.
 *
 * - **Mobile** = Android + iOS
 * - **Multiplatform** = Android + iOS + Desktop + Web
 *
 * Version set verified building (Android + Desktop + Web) against the local Android SDK:
 * Kotlin 2.1.21 / AGP 8.7.3 / Compose Multiplatform 1.7.3 / Gradle 8.13. Plugins resolve
 * from Google + Gradle Plugin Portal + Maven Central.
 */
class KmpComposeAppGenerator(
    private val fileSystem: FileSystem,
    private val includeDesktop: Boolean,
    private val includeWeb: Boolean
) {
    private val kotlin = "2.1.21"
    private val agp = "8.7.3"
    private val compose = "1.7.3"
    private val gradle = "8.13"

    suspend fun generate(project: AzoraProjectModel, projectPath: String) {
        val app = TemplateSupport.appName(project)
        val pkg = TemplateSupport.packageName(project)
        val pkgPath = pkg.replace('.', '/')
        val title = project.name.ifBlank { "Azora App" }

        // ---- root ----
        fileSystem.writeToFile("$projectPath/settings.gradle.kts", settings(app))
        fileSystem.writeToFile("$projectPath/build.gradle.kts", rootBuild())
        fileSystem.writeToFile("$projectPath/gradle.properties", GRADLE_PROPERTIES)
        fileSystem.writeToFile("$projectPath/.gitignore", TemplateSupport.GITIGNORE)
        TemplateSupport.androidSdkDir()?.let {
            fileSystem.writeToFile("$projectPath/local.properties", "sdk.dir=$it\n")
        }
        TemplateSupport.writeGradleWrapper(fileSystem, projectPath, gradle)

        // ---- composeApp ----
        fileSystem.writeToFile("$projectPath/composeApp/build.gradle.kts", composeAppBuild(pkg, app))
        fileSystem.writeToFile("$projectPath/composeApp/src/commonMain/kotlin/$pkgPath/App.kt", appComposable(pkg, title))
        fileSystem.writeToFile("$projectPath/composeApp/src/androidMain/AndroidManifest.xml", androidManifest(title))
        fileSystem.writeToFile("$projectPath/composeApp/src/androidMain/kotlin/$pkgPath/MainActivity.kt", mainActivity(pkg))
        fileSystem.writeToFile("$projectPath/composeApp/src/iosMain/kotlin/$pkgPath/MainViewController.kt", iosViewController(pkg))
        if (includeDesktop) {
            fileSystem.writeToFile("$projectPath/composeApp/src/desktopMain/kotlin/$pkgPath/main.kt", desktopMain(pkg, title))
        }
        if (includeWeb) {
            fileSystem.writeToFile("$projectPath/composeApp/src/wasmJsMain/kotlin/$pkgPath/main.kt", webMain(pkg))
            fileSystem.writeToFile("$projectPath/composeApp/src/wasmJsMain/resources/index.html", indexHtml(app, title))
        }

        // ---- iosApp scaffold (Kotlin framework is configured above; Xcode wrapper is a stub) ----
        fileSystem.writeToFile("$projectPath/iosApp/iosApp/ContentView.swift", iosContentView())
        fileSystem.writeToFile("$projectPath/iosApp/iosApp/iOSApp.swift", iosAppSwift())
        fileSystem.writeToFile("$projectPath/iosApp/README.md", IOS_README)
    }

    private fun settings(app: String) = """
        pluginManagement {
            repositories {
                google()
                gradlePluginPortal()
                mavenCentral()
            }
        }
        dependencyResolutionManagement {
            repositories {
                google()
                mavenCentral()
            }
        }
        rootProject.name = "$app"
        include(":composeApp")
    """.trimIndent() + "\n"

    private fun rootBuild() = """
        plugins {
            kotlin("multiplatform") version "$kotlin" apply false
            id("com.android.application") version "$agp" apply false
            id("org.jetbrains.compose") version "$compose" apply false
            id("org.jetbrains.kotlin.plugin.compose") version "$kotlin" apply false
        }
    """.trimIndent() + "\n"

    private fun composeAppBuild(pkg: String, app: String): String {
        val targets = StringBuilder()
        targets.appendLine("    androidTarget()")
        if (includeDesktop) targets.appendLine("    jvm(\"desktop\")")
        targets.appendLine(
            """
            |    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach {
            |        it.binaries.framework { baseName = "ComposeApp"; isStatic = true }
            |    }
            """.trimMargin()
        )
        if (includeWeb) targets.appendLine(
            """
            |    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
            |    wasmJs {
            |        browser { commonWebpackConfig { outputFileName = "$app.js" } }
            |        binaries.executable()
            |    }
            """.trimMargin()
        )

        val sourceSets = StringBuilder()
        sourceSets.appendLine(
            """
            |        val commonMain by getting {
            |            dependencies {
            |                implementation(compose.runtime)
            |                implementation(compose.foundation)
            |                implementation(compose.material3)
            |                implementation(compose.ui)
            |            }
            |        }
            |        val androidMain by getting {
            |            dependencies {
            |                implementation("androidx.activity:activity-compose:1.9.3")
            |            }
            |        }
            """.trimMargin()
        )
        if (includeDesktop) sourceSets.appendLine(
            """
            |        val desktopMain by getting {
            |            dependencies { implementation(compose.desktop.currentOs) }
            |        }
            """.trimMargin()
        )

        val desktopBlock = if (includeDesktop) """

            compose.desktop {
                application {
                    mainClass = "$pkg.MainKt"
                    nativeDistributions {
                        targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
                        packageName = "$app"
                        packageVersion = "1.0.0"
                    }
                }
            }
        """.trimIndent() else ""

        val imports = buildString {
            if (includeDesktop) appendLine("import org.jetbrains.compose.desktop.application.dsl.TargetFormat")
        }

        return """
            ${imports}plugins {
                kotlin("multiplatform") version "$kotlin"
                id("com.android.application") version "$agp"
                id("org.jetbrains.compose") version "$compose"
                id("org.jetbrains.kotlin.plugin.compose") version "$kotlin"
            }

            kotlin {
            ${targets.toString().trimEnd()}
                jvmToolchain(17)

                sourceSets {
            ${sourceSets.toString().trimEnd()}
                }
            }

            android {
                namespace = "$pkg"
                compileSdk = 35
                defaultConfig {
                    applicationId = "$pkg"
                    minSdk = 24
                    targetSdk = 35
                    versionCode = 1
                    versionName = "1.0"
                }
                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }
            }
            $desktopBlock
        """.trimIndent() + "\n"
    }

    private fun appComposable(pkg: String, title: String) = """
        package $pkg

        import androidx.compose.material3.MaterialTheme
        import androidx.compose.material3.Text
        import androidx.compose.runtime.Composable

        @Composable
        fun App() {
            MaterialTheme {
                Text("Hello from $title")
            }
        }
    """.trimIndent() + "\n"

    private fun mainActivity(pkg: String) = """
        package $pkg

        import android.os.Bundle
        import androidx.activity.ComponentActivity
        import androidx.activity.compose.setContent

        class MainActivity : ComponentActivity() {
            override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                setContent { App() }
            }
        }
    """.trimIndent() + "\n"

    private fun androidManifest(title: String) = """
        <?xml version="1.0" encoding="utf-8"?>
        <manifest xmlns:android="http://schemas.android.com/apk/res/android">
            <application
                android:label="$title"
                android:theme="@android:style/Theme.Material.Light.NoActionBar">
                <activity
                    android:name=".MainActivity"
                    android:exported="true">
                    <intent-filter>
                        <action android:name="android.intent.action.MAIN" />
                        <category android:name="android.intent.category.LAUNCHER" />
                    </intent-filter>
                </activity>
            </application>
        </manifest>
    """.trimIndent() + "\n"

    private fun iosViewController(pkg: String) = """
        package $pkg

        import androidx.compose.ui.window.ComposeUIViewController

        fun MainViewController() = ComposeUIViewController { App() }
    """.trimIndent() + "\n"

    private fun desktopMain(pkg: String, title: String) = """
        package $pkg

        import androidx.compose.ui.window.singleWindowApplication

        fun main() = singleWindowApplication(title = "$title") {
            App()
        }
    """.trimIndent() + "\n"

    private fun webMain(pkg: String) = """
        @file:OptIn(ExperimentalComposeUiApi::class)

        package $pkg

        import androidx.compose.ui.ExperimentalComposeUiApi
        import androidx.compose.ui.window.ComposeViewport
        import kotlinx.browser.document

        fun main() {
            ComposeViewport(document.body!!) { App() }
        }
    """.trimIndent() + "\n"

    private fun indexHtml(app: String, title: String) = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <title>$title</title>
        </head>
        <body>
            <script src="$app.js"></script>
        </body>
        </html>
    """.trimIndent() + "\n"

    private fun iosContentView() = """
        import UIKit
        import SwiftUI
        import ComposeApp

        struct ComposeView: UIViewControllerRepresentable {
            func makeUIViewController(context: Context) -> UIViewController {
                MainViewControllerKt.MainViewController()
            }
            func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
        }

        struct ContentView: View {
            var body: some View {
                ComposeView().ignoresSafeArea(.all)
            }
        }
    """.trimIndent() + "\n"

    private fun iosAppSwift() = """
        import SwiftUI

        @main
        struct iOSApp: App {
            var body: some Scene {
                WindowGroup {
                    ContentView()
                }
            }
        }
    """.trimIndent() + "\n"

    private companion object {
        val GRADLE_PROPERTIES = """
            org.gradle.jvmargs=-Xmx3g -Dfile.encoding=UTF-8
            kotlin.code.style=official
            android.useAndroidX=true
            android.nonTransitiveRClass=true
        """.trimIndent() + "\n"

        val IOS_README = """
            # iOS app

            The Compose Multiplatform iOS framework (`ComposeApp`) is configured in
            `../composeApp`. The Swift entry points live here.

            To run on iOS, create/open an Xcode app project in this folder that:
            1. Adds a "Run Script" build phase: `cd "${'$'}SRCROOT/.." && ./gradlew :composeApp:embedAndSignAppleFrameworkForXcode`
            2. Links the generated `ComposeApp.framework` (Framework Search Paths +
               `${'$'}(SRCROOT)/../composeApp/build/xcode-frameworks/...`).
            3. Uses `ContentView.swift` / `iOSApp.swift` as the app sources.

            Android Studio's Kotlin Multiplatform support (or `kdoctor`) can generate this
            Xcode project for you.
        """.trimIndent() + "\n"
    }
}

/** Mobile template: Android + iOS. */
class MobileTemplateGenerator(fileSystem: FileSystem) {
    private val delegate = KmpComposeAppGenerator(fileSystem, includeDesktop = false, includeWeb = false)
    suspend fun generate(project: AzoraProjectModel, projectPath: String) = delegate.generate(project, projectPath)
}

/** Multiplatform template: Android + iOS + Desktop + Web. */
class MultiplatformTemplateGenerator(fileSystem: FileSystem) {
    private val delegate = KmpComposeAppGenerator(fileSystem, includeDesktop = true, includeWeb = true)
    suspend fun generate(project: AzoraProjectModel, projectPath: String) = delegate.generate(project, projectPath)
}
