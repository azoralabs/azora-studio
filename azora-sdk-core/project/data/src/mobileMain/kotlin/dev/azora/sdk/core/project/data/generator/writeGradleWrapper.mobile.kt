package dev.azora.sdk.core.project.data.generator

import dev.azora.sdk.core.io.FileSystem

/**
 * Mobile (Android/iOS) implementation of [writeGradleWrapper].
 *
 * Writes only `gradle-wrapper.properties`. The binary Gradle wrapper (jar + scripts) is not
 * bundled on mobile because Gradle builds are not executed there.
 */
actual suspend fun writeGradleWrapper(fileSystem: FileSystem, projectPath: String, gradleVersion: String) {
    fileSystem.writeToFile(
        "$projectPath/gradle/wrapper/gradle-wrapper.properties",
        TemplateSupport.gradleWrapperProperties(gradleVersion)
    )
}
