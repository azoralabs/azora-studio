package dev.azora.sdk.core.project.data.generator

import dev.azora.sdk.core.io.FileSystem
import java.io.File

/**
 * Desktop implementation of [writeGradleWrapper].
 *
 * Writes `gradle-wrapper.properties` and copies the bundled wrapper jar/scripts (packaged as
 * resources under `/wrapper/...`) so the generated project builds out of the box regardless of
 * where Studio is launched from.
 */
actual suspend fun writeGradleWrapper(fileSystem: FileSystem, projectPath: String, gradleVersion: String) {
    fileSystem.writeToFile(
        "$projectPath/gradle/wrapper/gradle-wrapper.properties",
        TemplateSupport.gradleWrapperProperties(gradleVersion)
    )
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
