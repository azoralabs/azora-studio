package dev.azora.sdk.core.project.data.generator.website

/**
 * Static, non-Kotlin project artifacts for the generated Kobweb site: Gradle scripts, the version
 * catalog, Kobweb config, and ignore rules.
 *
 * These are configuration/build files (not the Kotlin "codegen" routed through `CodeGenerator`), so
 * they remain simple templated text. The version set matches the one verified to build:
 * Kobweb 0.24.0 / Kotlin 2.3.10 / Compose HTML 1.10.x, with kotlinx-serialization + coroutines
 * added for the generated API client.
 */
internal object KobwebProjectFiles {

    fun settingsGradle(appName: String) = """
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

    val rootBuildGradle = """
        plugins {
            alias(libs.plugins.kotlin.multiplatform) apply false
        }
    """.trimIndent() + "\n"

    val gradleProperties = """
        kotlin.code.style=official
        org.gradle.caching=true
        org.gradle.configuration-cache=true
    """.trimIndent() + "\n"

    val libsVersions = """
        [versions]
        compose-html = "1.10.0"
        compose-runtime = "1.10.2"
        kobweb = "0.24.0"
        kotlin = "2.3.10"
        kotlinx-coroutines = "1.9.0"
        kotlinx-serialization = "1.7.3"

        [libraries]
        compose-html-core = { module = "org.jetbrains.compose.html:html-core", version.ref = "compose-html" }
        compose-runtime = { module = "androidx.compose.runtime:runtime", version.ref = "compose-runtime" }
        kobweb-core = { module = "com.varabyte.kobweb:kobweb-core", version.ref = "kobweb" }
        kobweb-silk = { module = "com.varabyte.kobweb:kobweb-silk", version.ref = "kobweb" }
        silk-icons-fa = { module = "com.varabyte.kobwebx:silk-icons-fa", version.ref = "kobweb" }
        kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "kotlinx-coroutines" }
        kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinx-serialization" }

        [plugins]
        compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
        kobweb-application = { id = "com.varabyte.kobweb.application", version.ref = "kobweb" }
        kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
        kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
    """.trimIndent() + "\n"

    val gitignore = """
        .gradle/
        build/
        .kobweb/.server/
        .idea/
        *.log
    """.trimIndent() + "\n"

    fun siteBuildGradle(pkg: String, appName: String) = """
        import com.varabyte.kobweb.gradle.application.util.configAsKobwebApplication

        plugins {
            alias(libs.plugins.kotlin.multiplatform)
            alias(libs.plugins.kotlin.serialization)
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
                    implementation(libs.kotlinx.coroutines.core)
                    implementation(libs.kotlinx.serialization.json)
                }
            }
        }
    """.trimIndent() + "\n"

    fun confYaml(appName: String, title: String) = """
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
}
