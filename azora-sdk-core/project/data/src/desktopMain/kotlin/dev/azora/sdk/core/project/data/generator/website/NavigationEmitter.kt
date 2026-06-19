package dev.azora.sdk.core.project.data.generator.website

import dev.azora.sdk.core.project.domain.website.quote

/**
 * Emits `components/SiteLayout.kt`: a shared `SiteNav` bar built from the model's
 * [navigation][dev.azora.sdk.core.project.domain.website.NavigationModel] and a `SiteLayout`
 * wrapper every page composes around its content.
 */
class NavigationEmitter : KobwebEmitter {

    override fun emit(ctx: KobwebGenContext): List<GeneratedSource> {
        val nav = ctx.model.navigation
        val brand = nav.brand.ifBlank { ctx.title }
        val surface = KobwebStyle.colorLiteral(ctx.model.theme.surface)

        val code = buildSource {
            packageLine("${ctx.pkg}.components")
            imports(
                "androidx.compose.runtime.*",
                "com.varabyte.kobweb.compose.css.*",
                "com.varabyte.kobweb.compose.foundation.layout.*",
                "com.varabyte.kobweb.compose.ui.*",
                "com.varabyte.kobweb.compose.ui.modifiers.*",
                "com.varabyte.kobweb.silk.components.navigation.Link",
                "com.varabyte.kobweb.silk.components.text.SpanText",
                "org.jetbrains.compose.web.css.*"
            )

            write("@Composable")
            write("fun SiteNav() {")
            gen {
                val navModifier = buildString {
                    append("Modifier.fillMaxWidth().padding(16.px)")
                    surface?.let { append(".backgroundColor($it)") }
                }
                write("Row(")
                gen {
                    write("modifier = $navModifier,")
                    write("horizontalArrangement = Arrangement.SpaceBetween,")
                    write("verticalAlignment = Alignment.CenterVertically")
                }
                write(") {")
                gen {
                    write("SpanText(${brand.quote()}, Modifier.fontSize(20.px).fontWeight(FontWeight.Bold))")
                    write("Row(horizontalArrangement = Arrangement.spacedBy(16.px)) {")
                    gen {
                        nav.items.forEach { item ->
                            write("Link(path = ${item.route.quote()}, text = ${item.label.quote()})")
                        }
                    }
                    write("}")
                }
                write("}")
            }
            write("}")
            blank()

            write("@Composable")
            write("fun SiteLayout(content: @Composable () -> Unit) {")
            gen {
                write("Column(Modifier.fillMaxWidth().fillMaxHeight()) {")
                gen {
                    write("SiteNav()")
                    write("content()")
                }
                write("}")
            }
            write("}")
        }

        return listOf(GeneratedSource("components/SiteLayout.kt", code))
    }
}
