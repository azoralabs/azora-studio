package dev.azora.sdk.core.project.data.generator.website

import dev.azora.sdk.core.project.domain.CodeGenerator
import dev.azora.sdk.core.project.domain.CodeGeneratorImpl
import dev.azora.sdk.core.project.domain.website.WebsiteModel

/**
 * Inputs shared by every Kobweb emitter for a single generation pass.
 *
 * @property model The website being generated.
 * @property pkg Root Kotlin package of the generated `:site` module (e.g. `com.example.mysite`).
 * @property appName Sanitized application name used for webpack output / config.
 * @property title Human-readable site title.
 */
data class KobwebGenContext(
    val model: WebsiteModel,
    val pkg: String,
    val appName: String,
    val title: String
)

/**
 * One generated Kotlin source file.
 *
 * @property relativePath Path relative to the package source root
 *   (`site/src/jsMain/kotlin/<pkgPath>/`), e.g. `pages/Index.kt`.
 * @property code The fully-formed file contents.
 */
data class GeneratedSource(
    val relativePath: String,
    val code: String
)

/**
 * Produces Kotlin source for one concern of the site (app entry, pages, navigation, API client, …).
 *
 * Every emitter writes its Kotlin through [CodeGenerator] (`gen { … }` / `build()`) — never via raw
 * string interpolation of code blocks — so output indentation is consistent and the concerns stay
 * decoupled and independently testable.
 */
interface KobwebEmitter {
    fun emit(ctx: KobwebGenContext): List<GeneratedSource>
}

/** Builds a source string by running [block] against a fresh [CodeGeneratorImpl]. */
internal fun buildSource(block: CodeGenerator.GenScope.() -> Unit): String =
    CodeGeneratorImpl().gen(block).build()

/** Writes `package <pkg>` followed by a blank line. */
internal fun CodeGenerator.GenScope.packageLine(pkg: String) {
    write("package $pkg")
    blank()
}

/** Writes each import (already sorted by the caller) followed by a trailing blank line. */
internal fun CodeGenerator.GenScope.imports(vararg lines: String) {
    lines.filter { it.isNotBlank() }.forEach { write("import $it") }
    blank()
}
