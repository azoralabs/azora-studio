plugins {
    alias(libs.plugins.convention.cmp.library)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlin.stdlib)
            implementation(libs.kotlinx.serialization.json)

            implementation(projects.azoraSdk.canvas.domain)
        }
    }
}

/**
 * Refreshes the vendored azora-lang frontend (lexer / parser / AST) from a sibling
 * `azora-lang` checkout. The frontend is dependency-free Kotlin, vendored instead of
 * depended upon so Studio builds stay self-contained (the two repos pin different
 * Kotlin/AGP versions, which rules out a composite build).
 *
 * Run after the azora-lang frontend changes:
 *   ./gradlew :azora-sdk:nodes:domain:syncAzoraLangFrontend
 */
tasks.register<Copy>("syncAzoraLangFrontend") {
    val azoraLang = rootProject.projectDir.resolve("../azora-lang")
    from(azoraLang.resolve("compiler/src/commonMain/kotlin/org/azora/lang/frontend")) {
        include("Ast.kt", "Lexer.kt", "Parser.kt", "Token.kt")
    }
    into(projectDir.resolve("src/commonMain/kotlin/org/azora/lang/frontend"))
}
