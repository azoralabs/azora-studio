package dev.azora.sdk.core.project.data.generator

import dev.azora.sdk.core.io.FileSystem
import dev.azora.sdk.core.project.domain.AzoraProjectModel
import java.io.File

/**
 * Scaffolds a runnable [Kobweb](https://kobweb.varabyte.com) website into a freshly
 * created project directory.
 *
 * Produces a standard single-module Kobweb app (root + `:site`) using a version set
 * verified to build: Kobweb 0.24.0 / Kotlin 2.3.10 / Compose HTML 1.10.x. Kobweb
 * artifacts resolve from Maven Central and the Gradle Plugin Portal, so no custom
 * repository is required.
 *
 * After generation the project can be opened in any IDE, built with
 * `./gradlew :site:compileKotlinJs`, and served with the Kobweb CLI (`kobweb run`).
 */
class WebsiteTemplateGenerator(private val fileSystem: FileSystem) {

    suspend fun generate(project: AzoraProjectModel, projectPath: String) {
        val appName = sanitizeAppName(project.name)
        val pkg = project.packageName.ifBlank { "com.example.$appName" }
        val pkgPath = pkg.replace('.', '/')
        val title = project.name.ifBlank { "Azora Site" }

        // ----- Root project -----
        fileSystem.writeToFile("$projectPath/settings.gradle.kts", settingsGradle(appName))
        fileSystem.writeToFile("$projectPath/build.gradle.kts", ROOT_BUILD_GRADLE)
        fileSystem.writeToFile("$projectPath/gradle.properties", GRADLE_PROPERTIES)
        fileSystem.writeToFile("$projectPath/gradle/libs.versions.toml", LIBS_VERSIONS)
        fileSystem.writeToFile("$projectPath/.gitignore", GITIGNORE)
        fileSystem.writeToFile(
            "$projectPath/gradle/wrapper/gradle-wrapper.properties",
            GRADLE_WRAPPER_PROPERTIES
        )

        // ----- :site module -----
        fileSystem.writeToFile("$projectPath/site/build.gradle.kts", siteBuildGradle(pkg, appName))
        fileSystem.writeToFile("$projectPath/site/.kobweb/conf.yaml", confYaml(appName, title))
        fileSystem.writeToFile(
            "$projectPath/site/src/jsMain/kotlin/$pkgPath/AppEntry.kt",
            appEntry(pkg)
        )
        fileSystem.writeToFile(
            "$projectPath/site/src/jsMain/kotlin/$pkgPath/pages/Index.kt",
            indexPage(pkg, title)
        )
        fileSystem.writeToFile("$projectPath/site/src/jsMain/resources/public/.gitkeep", "")

        // ----- Gradle wrapper (best effort) -----
        copyGradleWrapper(projectPath)
    }

    /**
     * Copies the runnable Gradle wrapper (jar + scripts) from the host installation so
     * the generated project works out of the box. Best effort: if the source wrapper
     * cannot be located, the project still has `gradle-wrapper.properties` and can be
     * bootstrapped with a system Gradle (`gradle wrapper`) or opened in an IDE.
     */
    private suspend fun copyGradleWrapper(projectPath: String) {
        val sourceRoot = File(System.getProperty("user.dir") ?: return)
        val wrapperJar = File(sourceRoot, "gradle/wrapper/gradle-wrapper.jar")
        if (!wrapperJar.exists()) return
        runCatching {
            fileSystem.copyFile(wrapperJar.absolutePath, "$projectPath/gradle/wrapper/gradle-wrapper.jar")
            listOf("gradlew", "gradlew.bat").forEach { script ->
                val src = File(sourceRoot, script)
                if (src.exists()) {
                    fileSystem.copyFile(src.absolutePath, "$projectPath/$script")
                }
            }
            fileSystem.setExecutable("$projectPath/gradlew")
        }
    }

    private fun sanitizeAppName(name: String): String =
        name.lowercase().filter { it.isLetterOrDigit() }.ifBlank { "site" }

    private fun settingsGradle(appName: String) = """
        pluginManagement {
            repositories {
                gradlePluginPortal()
            }
        }

        dependencyResolutionManagement {
            repositories {
                mavenCentral()
                google()
            }
        }

        rootProject.name = "$appName"

        include(":site")
    """.trimIndent() + "\n"

    private fun siteBuildGradle(pkg: String, appName: String) = """
        import com.varabyte.kobweb.gradle.application.util.configAsKobwebApplication

        plugins {
            alias(libs.plugins.kotlin.multiplatform)
            alias(libs.plugins.compose.compiler)
            alias(libs.plugins.kobweb.application)
        }

        group = "$pkg"
        version = "1.0-SNAPSHOT"

        kobweb {
            app {
                index {
                    description.set("Powered by Kobweb")
                }
            }
        }

        kotlin {
            configAsKobwebApplication("$appName")
            sourceSets {
                jsMain.dependencies {
                    implementation(libs.compose.runtime)
                    implementation(libs.compose.html.core)
                    implementation(libs.kobweb.core)
                    implementation(libs.kobweb.silk)
                    implementation(libs.silk.icons.fa)
                }
            }
        }
    """.trimIndent() + "\n"

    private fun confYaml(appName: String, title: String) = """
        site:
          title: "$title"

        server:
          files:
            dev:
              contentRoot: "build/processedResources/js/main/public"
              script: "build/kotlin-webpack/js/developmentExecutable/$appName.js"
            prod:
              script: "build/kotlin-webpack/js/productionExecutable/$appName.js"
              siteRoot: ".kobweb/site"
          port: 8080
    """.trimIndent() + "\n"

    private fun appEntry(pkg: String) = """
        package $pkg

        import androidx.compose.runtime.*
        import com.varabyte.kobweb.compose.ui.Modifier
        import com.varabyte.kobweb.compose.ui.modifiers.*
        import com.varabyte.kobweb.core.App
        import com.varabyte.kobweb.silk.SilkApp
        import com.varabyte.kobweb.silk.components.layout.Surface
        import com.varabyte.kobweb.silk.init.InitSilk
        import com.varabyte.kobweb.silk.init.InitSilkContext
        import com.varabyte.kobweb.silk.init.registerStyleBase
        import com.varabyte.kobweb.silk.style.common.SmoothColorStyle
        import com.varabyte.kobweb.silk.style.toModifier

        @InitSilk
        fun initStyles(ctx: InitSilkContext) {
            ctx.stylesheet.registerStyleBase("html, body") { Modifier.fillMaxHeight() }
        }

        @App
        @Composable
        fun AppEntry(content: @Composable () -> Unit) {
            SilkApp {
                Surface(SmoothColorStyle.toModifier().fillMaxHeight()) {
                    content()
                }
            }
        }
    """.trimIndent() + "\n"

    private fun indexPage(pkg: String, title: String) = """
        package $pkg.pages

        import androidx.compose.runtime.*
        import com.varabyte.kobweb.compose.foundation.layout.Box
        import com.varabyte.kobweb.compose.ui.Alignment
        import com.varabyte.kobweb.compose.ui.Modifier
        import com.varabyte.kobweb.compose.ui.modifiers.fillMaxSize
        import com.varabyte.kobweb.core.Page
        import org.jetbrains.compose.web.dom.Text

        @Page
        @Composable
        fun HomePage() {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Welcome to $title")
            }
        }
    """.trimIndent() + "\n"

    private companion object {
        val ROOT_BUILD_GRADLE = """
            plugins {
                alias(libs.plugins.kotlin.multiplatform) apply false
            }
        """.trimIndent() + "\n"

        val GRADLE_PROPERTIES = """
            kotlin.code.style=official
            org.gradle.caching=true
            org.gradle.configuration-cache=true
        """.trimIndent() + "\n"

        val GRADLE_WRAPPER_PROPERTIES = """
            distributionBase=GRADLE_USER_HOME
            distributionPath=wrapper/dists
            distributionUrl=https\://services.gradle.org/distributions/gradle-9.4.1-bin.zip
            networkTimeout=10000
            validateDistributionUrl=true
            zipStoreBase=GRADLE_USER_HOME
            zipStorePath=wrapper/dists
        """.trimIndent() + "\n"

        val LIBS_VERSIONS = """
            [versions]
            compose-html = "1.10.0"
            compose-runtime = "1.10.2"
            kobweb = "0.24.0"
            kotlin = "2.3.10"

            [libraries]
            compose-html-core = { module = "org.jetbrains.compose.html:html-core", version.ref = "compose-html" }
            compose-runtime = { module = "androidx.compose.runtime:runtime", version.ref = "compose-runtime" }
            kobweb-core = { module = "com.varabyte.kobweb:kobweb-core", version.ref = "kobweb" }
            kobweb-silk = { module = "com.varabyte.kobweb:kobweb-silk", version.ref = "kobweb" }
            silk-icons-fa = { module = "com.varabyte.kobwebx:silk-icons-fa", version.ref = "kobweb" }

            [plugins]
            compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
            kobweb-application = { id = "com.varabyte.kobweb.application", version.ref = "kobweb" }
            kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
        """.trimIndent() + "\n"

        val GITIGNORE = """
            .gradle/
            build/
            .kobweb/.server/
            .idea/
            *.log
        """.trimIndent() + "\n"
    }
}
