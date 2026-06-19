package dev.azora.sdk.core.project.data.generator

import dev.azora.sdk.core.io.FileSystem
import dev.azora.sdk.core.project.data.generator.website.AppEntryEmitter
import dev.azora.sdk.core.project.data.generator.website.ApiClientEmitter
import dev.azora.sdk.core.project.data.generator.website.FeatureEmitter
import dev.azora.sdk.core.project.data.generator.website.KobwebEmitter
import dev.azora.sdk.core.project.data.generator.website.KobwebGenContext
import dev.azora.sdk.core.project.data.generator.website.KobwebProjectFiles
import dev.azora.sdk.core.project.data.generator.website.NavigationEmitter
import dev.azora.sdk.core.project.data.generator.website.PageEmitter
import dev.azora.sdk.core.project.domain.AzoraProjectModel
import dev.azora.sdk.core.project.domain.website
import dev.azora.sdk.core.project.domain.website.WebsiteModel

/**
 * Scaffolds a runnable [Kobweb](https://kobweb.varabyte.com) website from a project's
 * [WebsiteModel].
 *
 * This is the orchestrator of a decoupled, emitter-based pipeline: it loads the website model
 * (falling back to [WebsiteModel.default] for a freshly created project), then runs each
 * [KobwebEmitter] — every one of which produces Kotlin through the
 * [CodeGenerator][dev.azora.sdk.core.project.domain.CodeGenerator] DSL rather than raw string
 * interpolation — and writes the results into a standard single-module Kobweb app (`root` + `:site`).
 *
 * After generation the project can be opened in any IDE, built with
 * `./gradlew :site:compileKotlinJs`, and served with the Kobweb CLI (`kobweb run`).
 */
class WebsiteTemplateGenerator(private val fileSystem: FileSystem) {

    /** Kotlin emitters, one per site concern. Order is irrelevant — each produces its own files. */
    private val emitters: List<KobwebEmitter> = listOf(
        AppEntryEmitter(),
        NavigationEmitter(),
        ApiClientEmitter(),
        FeatureEmitter(),
        PageEmitter()
    )

    suspend fun generate(project: AzoraProjectModel, projectPath: String) {
        val appName = sanitizeAppName(project.name)
        val pkg = project.packageName.ifBlank { "com.example.$appName" }
        val pkgPath = pkg.replace('.', '/')
        val title = project.name.ifBlank { "Azora Site" }

        val model = project.settings.website ?: WebsiteModel.default(project.name)
        val ctx = KobwebGenContext(model = model, pkg = pkg, appName = appName, title = title)

        // ----- Root project (config / build files) -----
        fileSystem.writeToFile("$projectPath/settings.gradle.kts", KobwebProjectFiles.settingsGradle(appName))
        fileSystem.writeToFile("$projectPath/build.gradle.kts", KobwebProjectFiles.rootBuildGradle)
        fileSystem.writeToFile("$projectPath/gradle.properties", KobwebProjectFiles.gradleProperties)
        fileSystem.writeToFile("$projectPath/gradle/libs.versions.toml", KobwebProjectFiles.libsVersions)
        fileSystem.writeToFile("$projectPath/.gitignore", KobwebProjectFiles.gitignore)

        // ----- :site module (config) -----
        fileSystem.writeToFile("$projectPath/site/build.gradle.kts", KobwebProjectFiles.siteBuildGradle(pkg, appName))
        fileSystem.writeToFile("$projectPath/site/.kobweb/conf.yaml", KobwebProjectFiles.confYaml(appName, title))
        fileSystem.writeToFile("$projectPath/site/src/jsMain/resources/public/.gitkeep", "")

        // ----- :site module (generated Kotlin via CodeGenerator emitters) -----
        val sourceRoot = "$projectPath/site/src/jsMain/kotlin/$pkgPath"
        emitters.forEach { emitter ->
            emitter.emit(ctx).forEach { source ->
                fileSystem.writeToFile("$sourceRoot/${source.relativePath}", source.code)
            }
        }

        // ----- Gradle wrapper (properties + jar + scripts, runnable out of the box) -----
        TemplateSupport.writeGradleWrapper(fileSystem, projectPath)
    }

    private fun sanitizeAppName(name: String): String =
        name.lowercase().filter { it.isLetterOrDigit() }.ifBlank { "site" }
}
