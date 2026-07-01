package dev.azora.sdk.compiler.scaffold

import dev.azora.sdk.core.io.FileSystem
import dev.azora.sdk.core.project.domain.AzoraProjectModel
import java.io.File

/**
 * Target-agnostic helpers for scaffolding generated projects on disk: identifier sanitization,
 * `.gitignore`, the Gradle wrapper, and best-effort Android SDK discovery. Template plugins
 * (React site, Compose Multiplatform app, …) build on these; nothing here is specific to any
 * output framework.
 */
object ProjectScaffold {

    /** Lowercase alphanumeric app name derived from the project name (Gradle/npm safe). */
    fun appName(project: AzoraProjectModel): String =
        project.name.lowercase().filter { it.isLetterOrDigit() }.ifBlank { "app" }

    /** The project's package name, falling back to `com.example.<appName>`. */
    fun packageName(project: AzoraProjectModel): String =
        project.packageName.ifBlank { "com.example.${appName(project)}" }

    fun gradleWrapperProperties(gradleVersion: String = DEFAULT_GRADLE_VERSION) = """
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
    suspend fun writeGradleWrapper(fileSystem: FileSystem, projectPath: String, gradleVersion: String = DEFAULT_GRADLE_VERSION) {
        fileSystem.writeToFile("$projectPath/gradle/wrapper/gradle-wrapper.properties", gradleWrapperProperties(gradleVersion))
        runCatching {
            copyResource("/wrapper/gradle-wrapper.jar", fileSystem.getAbsolutePath("$projectPath/gradle/wrapper/gradle-wrapper.jar"))
            copyResource("/wrapper/gradlew", fileSystem.getAbsolutePath("$projectPath/gradlew"))
            copyResource("/wrapper/gradlew.bat", fileSystem.getAbsolutePath("$projectPath/gradlew.bat"))
            fileSystem.setExecutable("$projectPath/gradlew")
        }
    }

    private fun copyResource(resourcePath: String, destAbsolutePath: String) {
        val input = ProjectScaffold::class.java.getResourceAsStream(resourcePath) ?: return
        val dest = File(destAbsolutePath)
        dest.parentFile?.mkdirs()
        input.use { stream -> dest.outputStream().use { out -> stream.copyTo(out) } }
    }

    private const val DEFAULT_GRADLE_VERSION = "9.4.1"
}
