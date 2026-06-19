package dev.azora.sdk.core.project.data.generator.website

import dev.azora.sdk.core.project.domain.CodeGenerator.GenScope
import dev.azora.sdk.core.project.domain.website.*

/**
 * Emits one `pages/<Name>.kt` per [WebsitePage]: a `@Page @Composable` function that declares the
 * state/context it needs, wraps the page's component tree in the shared `SiteLayout`, and renders
 * the [WebComponent] tree to Kobweb/Silk composables. Button click logic is produced by
 * [WebLogicTranslator] so codegen stays the single source of truth for logic.
 */
class PageEmitter(
    private val translator: WebLogicTranslator = WebLogicTranslatorImpl()
) : KobwebEmitter {

    override fun emit(ctx: KobwebGenContext): List<GeneratedSource> =
        ctx.model.pages.map { page -> emitPage(ctx, page) }

    private fun emitPage(ctx: KobwebGenContext, page: WebsitePage): GeneratedSource {
        val pascal = page.name.identifier().replaceFirstChar { it.uppercaseChar() }
        val fileName = if (page.isHome) "Index.kt" else "$pascal.kt"
        val fnName = "${pascal}Page"

        val handlers = page.logic.handlers
        val needsNav = handlers.any { h -> h.actions.any { it is NavigateAction } }
        val needsApi = handlers.any { h -> h.actions.any { it is CallApiAction } }
        val logicCtx = WebLogicEmitContext(model = ctx.model)

        val code = buildSource {
            packageLine("${ctx.pkg}.pages")
            imports(
                *buildList {
                    add("androidx.compose.runtime.*")
                    add("com.varabyte.kobweb.compose.css.*")
                    add("com.varabyte.kobweb.compose.foundation.layout.*")
                    add("com.varabyte.kobweb.compose.ui.*")
                    add("com.varabyte.kobweb.compose.ui.modifiers.*")
                    add("com.varabyte.kobweb.core.*")
                    add("com.varabyte.kobweb.silk.components.forms.Button")
                    add("com.varabyte.kobweb.silk.components.forms.TextInput")
                    add("com.varabyte.kobweb.silk.components.graphics.Image")
                    add("com.varabyte.kobweb.silk.components.navigation.Link")
                    add("com.varabyte.kobweb.silk.components.text.SpanText")
                    add("org.jetbrains.compose.web.css.*")
                    add("${ctx.pkg}.components.SiteLayout")
                    if (needsApi) {
                        add("${ctx.pkg}.api.ApiClient")
                        add("kotlinx.coroutines.launch")
                    }
                }.sorted().toTypedArray()
            )

            if (page.isHome) write("@Page") else write("@Page(${page.route.quote()})")
            write("@Composable")
            write("fun $fnName() {")
            gen {
                if (needsNav) write("val pageCtx = rememberPageContext()")
                if (needsApi) {
                    write("val scope = rememberCoroutineScope()")
                    write("val api = remember { ApiClient() }")
                }
                page.stateKeys.forEach { (key, initial) ->
                    write("var ${key.identifier()} by remember { mutableStateOf(${initial.quote()}) }")
                }
                write("SiteLayout {")
                gen { emitComponent(this, page, page.root, logicCtx) }
                write("}")
            }
            write("}")
        }

        return GeneratedSource("pages/$fileName", code)
    }

    private fun emitComponent(scope: GenScope, page: WebsitePage, component: WebComponent, logicCtx: WebLogicEmitContext) {
        val mod = KobwebStyle.modifier(component.modifier)
        when (component) {
            is WebColumn -> with(scope) {
                write("Column(")
                gen {
                    write("modifier = $mod,")
                    write("verticalArrangement = ${KobwebStyle.columnArrangement(component.modifier, component.arrangement)},")
                    write("horizontalAlignment = ${KobwebStyle.columnAlignment(component.arrangement)}")
                }
                write(") {")
                gen { component.children.forEach { emitComponent(this, page, it, logicCtx) } }
                write("}")
            }

            is WebRow -> with(scope) {
                write("Row(")
                gen {
                    write("modifier = $mod,")
                    write("horizontalArrangement = ${KobwebStyle.rowArrangement(component.modifier, component.arrangement)},")
                    write("verticalAlignment = ${KobwebStyle.rowAlignment(component.arrangement)}")
                }
                write(") {")
                gen { component.children.forEach { emitComponent(this, page, it, logicCtx) } }
                write("}")
            }

            is WebBox -> with(scope) {
                write("Box(modifier = $mod) {")
                gen { component.children.forEach { emitComponent(this, page, it, logicCtx) } }
                write("}")
            }

            is WebText -> scope.write("SpanText(${component.text.quote()}, $mod)")

            is WebButton -> with(scope) {
                val handler = page.logic.handler(component.onClickHandlerId)
                write("Button(")
                gen {
                    write("onClick = {")
                    if (handler != null) {
                        gen { translator.emitHandlerBody(this, handler, logicCtx) }
                    }
                    write("},")
                    write("modifier = $mod")
                }
                write(") {")
                gen { write("SpanText(${component.label.quote()})") }
                write("}")
            }

            is WebImage ->
                scope.write("Image(src = ${component.src.quote()}, modifier = $mod)")

            is WebLink ->
                scope.write("Link(path = ${component.href.quote()}, text = ${component.text.quote()}, modifier = $mod)")

            is WebInput -> with(scope) {
                val stateVar = component.stateKey?.identifier()
                write("TextInput(")
                gen {
                    write("text = ${stateVar ?: "\"\""},")
                    write("onTextChange = {${if (stateVar != null) " $stateVar = it " else ""}},")
                    write("placeholder = ${component.placeholder.quote()},")
                    write("modifier = $mod")
                }
                write(")")
            }

            is WebSpacer -> scope.write("Spacer()")
        }
    }
}
