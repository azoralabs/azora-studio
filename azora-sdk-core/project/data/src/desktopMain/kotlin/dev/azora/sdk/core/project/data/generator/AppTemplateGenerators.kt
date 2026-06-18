package dev.azora.sdk.core.project.data.generator

import dev.azora.sdk.core.io.FileSystem
import dev.azora.sdk.core.project.domain.AzoraProjectModel
import java.io.File

/**
 * Shared helpers for the standalone project templates.
 *
 * All templates target the version set used by Azora Studio itself (verified building):
 * Kotlin 2.4.0 and Compose Multiplatform 1.11.1, resolving from Maven Central + Google +
 * the Gradle Plugin Portal.
 */
internal object TemplateSupport {

    const val KOTLIN = "2.4.0"
    const val COMPOSE = "1.11.1"

    fun appName(project: AzoraProjectModel): String =
        project.name.lowercase().filter { it.isLetterOrDigit() }.ifBlank { "app" }

    fun packageName(project: AzoraProjectModel): String =
        project.packageName.ifBlank { "com.example.${appName(project)}" }

    fun gradleWrapperProperties(gradleVersion: String = "9.4.1") = """
        distributionBase=GRADLE_USER_HOME
        distributionPath=wrapper/dists
        distributionUrl=https\://services.gradle.org/distributions/gradle-$gradleVersion-bin.zip
        networkTimeout=10000
        validateDistributionUrl=true
        zipStoreBase=GRADLE_USER_HOME
        zipStorePath=wrapper/dists
    """.trimIndent() + "\n"

    /** Best-effort Android SDK location for a generated project's `local.properties`. */
    fun androidSdkDir(): String? =
        System.getenv("ANDROID_HOME")
            ?: System.getenv("ANDROID_SDK_ROOT")
            ?: System.getProperty("user.home")?.let { home ->
                listOf("$home/Library/Android/sdk", "$home/Android/Sdk")
                    .firstOrNull { File(it).isDirectory }
            }

    val GITIGNORE = """
        .gradle/
        build/
        .idea/
        .kotlin/
        local.properties
        *.log
    """.trimIndent() + "\n"

    /**
     * Writes the Gradle wrapper (properties + jar + scripts) so the generated project is
     * runnable out of the box. The jar/scripts are bundled as resources in this module
     * (`/wrapper/...`) rather than copied from the host, so it works regardless of where
     * Studio is launched from.
     */
    suspend fun writeGradleWrapper(fileSystem: FileSystem, projectPath: String, gradleVersion: String = "9.4.1") {
        fileSystem.writeToFile("$projectPath/gradle/wrapper/gradle-wrapper.properties", gradleWrapperProperties(gradleVersion))
        runCatching {
            copyResource("/wrapper/gradle-wrapper.jar", fileSystem.getAbsolutePath("$projectPath/gradle/wrapper/gradle-wrapper.jar"))
            copyResource("/wrapper/gradlew", fileSystem.getAbsolutePath("$projectPath/gradlew"))
            copyResource("/wrapper/gradlew.bat", fileSystem.getAbsolutePath("$projectPath/gradlew.bat"))
            fileSystem.setExecutable("$projectPath/gradlew")
        }
    }

    private fun copyResource(resourcePath: String, destAbsolutePath: String) {
        val input = TemplateSupport::class.java.getResourceAsStream(resourcePath) ?: return
        val dest = File(destAbsolutePath)
        dest.parentFile?.mkdirs()
        input.use { stream -> dest.outputStream().use { out -> stream.copyTo(out) } }
    }
}

/**
 * Empty template: a minimal, runnable Kotlin/JVM console application (no Kotlin Multiplatform).
 */
class EmptyTemplateGenerator(private val fileSystem: FileSystem) {
    suspend fun generate(project: AzoraProjectModel, projectPath: String) {
        val app = TemplateSupport.appName(project)
        fileSystem.writeToFile("$projectPath/settings.gradle.kts", """
            pluginManagement { repositories { gradlePluginPortal(); mavenCentral() } }
            rootProject.name = "$app"
        """.trimIndent() + "\n")
        fileSystem.writeToFile("$projectPath/build.gradle.kts", """
            plugins {
                kotlin("jvm") version "${TemplateSupport.KOTLIN}"
                application
            }

            repositories { mavenCentral() }

            kotlin { jvmToolchain(17) }

            application { mainClass.set("MainKt") }
        """.trimIndent() + "\n")
        fileSystem.writeToFile("$projectPath/src/main/kotlin/Main.kt", """
            fun main() {
                println("Hello from ${project.name.ifBlank { "your Azora app" }}")
            }
        """.trimIndent() + "\n")
        fileSystem.writeToFile("$projectPath/.gitignore", TemplateSupport.GITIGNORE)
        TemplateSupport.writeGradleWrapper(fileSystem, projectPath)
    }
}

/**
 * Desktop template: a Compose Multiplatform desktop (JVM) application.
 */
class DesktopTemplateGenerator(private val fileSystem: FileSystem) {
    suspend fun generate(project: AzoraProjectModel, projectPath: String) {
        val app = TemplateSupport.appName(project)
        val title = project.name.ifBlank { "Azora Desktop" }
        fileSystem.writeToFile("$projectPath/settings.gradle.kts", """
            pluginManagement { repositories { gradlePluginPortal(); mavenCentral(); google() } }
            rootProject.name = "$app"
        """.trimIndent() + "\n")
        fileSystem.writeToFile("$projectPath/build.gradle.kts", """
            import org.jetbrains.compose.desktop.application.dsl.TargetFormat

            plugins {
                kotlin("jvm") version "${TemplateSupport.KOTLIN}"
                id("org.jetbrains.compose") version "${TemplateSupport.COMPOSE}"
                id("org.jetbrains.kotlin.plugin.compose") version "${TemplateSupport.KOTLIN}"
            }

            repositories { mavenCentral(); google() }

            kotlin { jvmToolchain(17) }

            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(compose.material3)
            }

            compose.desktop {
                application {
                    mainClass = "MainKt"
                    nativeDistributions {
                        targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
                        packageName = "$app"
                        packageVersion = "1.0.0"
                    }
                }
            }
        """.trimIndent() + "\n")
        fileSystem.writeToFile("$projectPath/src/main/kotlin/Main.kt", """
            import androidx.compose.material3.MaterialTheme
            import androidx.compose.material3.Text
            import androidx.compose.ui.window.singleWindowApplication

            fun main() = singleWindowApplication(title = "$title") {
                MaterialTheme {
                    Text("Hello from $title")
                }
            }
        """.trimIndent() + "\n")
        fileSystem.writeToFile("$projectPath/.gitignore", TemplateSupport.GITIGNORE)
        TemplateSupport.writeGradleWrapper(fileSystem, projectPath)
    }
}

/**
 * Web template: a Compose Multiplatform web (Wasm/JS) application that runs in the browser.
 */
class WebTemplateGenerator(private val fileSystem: FileSystem) {
    suspend fun generate(project: AzoraProjectModel, projectPath: String) {
        val app = TemplateSupport.appName(project)
        val title = project.name.ifBlank { "Azora Web" }
        fileSystem.writeToFile("$projectPath/settings.gradle.kts", """
            pluginManagement { repositories { gradlePluginPortal(); mavenCentral(); google() } }
            rootProject.name = "$app"
        """.trimIndent() + "\n")
        fileSystem.writeToFile("$projectPath/build.gradle.kts", """
            import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

            plugins {
                kotlin("multiplatform") version "${TemplateSupport.KOTLIN}"
                id("org.jetbrains.compose") version "${TemplateSupport.COMPOSE}"
                id("org.jetbrains.kotlin.plugin.compose") version "${TemplateSupport.KOTLIN}"
            }

            repositories { mavenCentral(); google() }

            kotlin {
                @OptIn(ExperimentalWasmDsl::class)
                wasmJs {
                    browser {
                        commonWebpackConfig { outputFileName = "$app.js" }
                    }
                    binaries.executable()
                }
                sourceSets {
                    val wasmJsMain by getting {
                        dependencies {
                            implementation(compose.runtime)
                            implementation(compose.foundation)
                            implementation(compose.material3)
                            implementation(compose.ui)
                        }
                    }
                }
            }
        """.trimIndent() + "\n")
        fileSystem.writeToFile("$projectPath/src/wasmJsMain/kotlin/Main.kt", """
            @file:OptIn(ExperimentalComposeUiApi::class)

            import androidx.compose.material3.MaterialTheme
            import androidx.compose.material3.Text
            import androidx.compose.ui.ExperimentalComposeUiApi
            import androidx.compose.ui.window.ComposeViewport
            import kotlinx.browser.document

            fun main() {
                ComposeViewport(document.body!!) {
                    MaterialTheme {
                        Text("Hello from $title")
                    }
                }
            }
        """.trimIndent() + "\n")
        fileSystem.writeToFile("$projectPath/src/wasmJsMain/resources/index.html", """
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
        """.trimIndent() + "\n")
        fileSystem.writeToFile("$projectPath/.gitignore", TemplateSupport.GITIGNORE)
        TemplateSupport.writeGradleWrapper(fileSystem, projectPath)
    }
}
