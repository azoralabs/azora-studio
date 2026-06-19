package dev.azora.sdk.core.project.data.generator.website

/**
 * Emits `AppEntry.kt`: the Silk `@App` wrapper plus an `@InitSilk` block that applies the model's
 * [theme][dev.azora.sdk.core.project.domain.website.WebTheme] as base `html, body` styles.
 */
class AppEntryEmitter : KobwebEmitter {

    override fun emit(ctx: KobwebGenContext): List<GeneratedSource> {
        val theme = ctx.model.theme
        val bg = KobwebStyle.colorLiteral(theme.background)
        val fg = KobwebStyle.colorLiteral(theme.onBackground)
        val fontFamilies = theme.fontFamily.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val code = buildSource {
            packageLine(ctx.pkg)
            imports(
                "androidx.compose.runtime.*",
                "com.varabyte.kobweb.compose.ui.Modifier",
                "com.varabyte.kobweb.compose.ui.modifiers.*",
                "com.varabyte.kobweb.core.App",
                "com.varabyte.kobweb.silk.SilkApp",
                "com.varabyte.kobweb.silk.components.layout.Surface",
                "com.varabyte.kobweb.silk.init.InitSilk",
                "com.varabyte.kobweb.silk.init.InitSilkContext",
                "com.varabyte.kobweb.silk.init.registerStyleBase",
                "com.varabyte.kobweb.silk.style.common.SmoothColorStyle",
                "com.varabyte.kobweb.silk.style.toModifier",
                "org.jetbrains.compose.web.css.*"
            )

            write("@InitSilk")
            write("fun initTheme(ctx: InitSilkContext) {")
            gen {
                write("ctx.stylesheet.registerStyleBase(\"html, body\") {")
                gen {
                    write("Modifier")
                    gen {
                        write(".fillMaxHeight()")
                        bg?.let { write(".backgroundColor($it)") }
                        fg?.let { write(".color($it)") }
                        if (fontFamilies.isNotEmpty()) {
                            val args = fontFamilies.joinToString(", ") { "\"$it\"" }
                            write(".fontFamily($args)")
                        }
                    }
                }
                write("}")
            }
            write("}")
            blank()

            write("@App")
            write("@Composable")
            write("fun AppEntry(content: @Composable () -> Unit) {")
            gen {
                write("SilkApp {")
                gen {
                    write("Surface(SmoothColorStyle.toModifier().fillMaxHeight()) {")
                    gen { write("content()") }
                    write("}")
                }
                write("}")
            }
            write("}")
        }

        return listOf(GeneratedSource("AppEntry.kt", code))
    }
}
