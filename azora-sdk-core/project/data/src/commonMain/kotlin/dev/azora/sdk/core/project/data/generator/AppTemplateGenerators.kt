package dev.azora.sdk.core.project.data.generator

import dev.azora.sdk.core.io.FileSystem
import dev.azora.sdk.core.project.domain.AzoraProjectModel

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
}

/**
 * Writes the Gradle wrapper so the generated project is runnable.
 *
 * Platform behavior:
 * - Desktop (JVM): writes `gradle-wrapper.properties` and copies the bundled wrapper jar/scripts
 *   so the project builds out of the box.
 * - Mobile (Android/iOS): writes only `gradle-wrapper.properties`; the binary wrapper is not
 *   bundled on mobile since Gradle builds are not run there.
 */
expect suspend fun writeGradleWrapper(fileSystem: FileSystem, projectPath: String, gradleVersion: String = "9.4.1")

/**
 * Empty template: a TRULY empty project — only `project.azora` (the project
 * metadata, written by the repository) exists after creation. No build files,
 * no sources, no directories; structure comes from what the user adds or from
 * plugins/libraries they enable later.
 *
 * This is the only builtin template — every other template is provided by an
 * installed plugin or library.
 */
class EmptyTemplateGenerator(@Suppress("unused") private val fileSystem: FileSystem) {
    suspend fun generate(project: AzoraProjectModel, projectPath: String) {
        // Intentionally nothing.
    }
}
