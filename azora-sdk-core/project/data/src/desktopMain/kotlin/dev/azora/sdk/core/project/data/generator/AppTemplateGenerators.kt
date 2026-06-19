package dev.azora.sdk.core.project.data.generator

import dev.azora.sdk.core.io.FileSystem
import dev.azora.sdk.core.project.domain.AzoraProjectModel
import java.io.File

/**
 * Shared helpers for the builtin [EmptyTemplateGenerator].
 *
 * Non-Empty templates (Desktop/Web/Mobile/Multiplatform/Website) are now contributed by external
 * plugins (`azora-multiplatform-app-builder`, `azora-website-builder`), which ship their own
 * generators and a copy of the Gradle wrapper resources.
 */
internal object TemplateSupport {

    const val KOTLIN = "2.4.0"

    fun appName(project: AzoraProjectModel): String =
        project.name.lowercase().filter { it.isLetterOrDigit() }.ifBlank { "app" }

    fun gradleWrapperProperties(gradleVersion: String = "9.4.1") = """
        distributionBase=GRADLE_USER_HOME
        distributionPath=wrapper/dists
        distributionUrl=https\://services.gradle.org/distributions/gradle-$gradleVersion-bin.zip
        networkTimeout=10000
        validateDistributionUrl=true
        zipStoreBase=GRADLE_USER_HOME
        zipStorePath=wrapper/dists
    """.trimIndent() + "\n"

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
 *
 * This is the only builtin template — every other template is provided by an installed plugin.
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
