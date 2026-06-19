package dev.azora.sdk.core.project.domain.website

import dev.azora.sdk.core.project.domain.CodeGenerator.GenScope

/**
 * Names of the local variables a [WebLogicTranslator] may reference when emitting a handler body.
 * The page emitter is responsible for declaring these in the surrounding composable.
 */
data class WebLogicEmitContext(
    val model: WebsiteModel,
    val pageContextVar: String = "pageCtx",
    val apiVar: String = "api",
    val scopeVar: String = "scope"
)

/**
 * Translates a page's [WebLogicGraph] handlers into Kotlin statements, written through a
 * [GenScope] so the output participates in the shared [CodeGenerator][dev.azora.sdk.core.project.domain.CodeGenerator]
 * indentation — no string interpolation of code blocks.
 *
 * This is the seam the AzoraNodes (`.azn`) integration plugs into: Phase 2 maps an authored graph
 * to a [WebLogicGraph]; the translator stays the single place that turns logic into code.
 */
interface WebLogicTranslator {
    /** Whether [handler]'s body must run inside a coroutine (i.e. performs async work). */
    fun requiresCoroutineScope(handler: WebEventHandler): Boolean

    /** Emits the statements of [handler] at the current [scope] indentation. */
    fun emitHandlerBody(scope: GenScope, handler: WebEventHandler, ctx: WebLogicEmitContext)
}

/** Default [WebLogicTranslator] targeting the generated Kobweb `ApiClient` + Kobweb router. */
class WebLogicTranslatorImpl : WebLogicTranslator {

    override fun requiresCoroutineScope(handler: WebEventHandler): Boolean =
        handler.actions.any { it is CallApiAction }

    override fun emitHandlerBody(scope: GenScope, handler: WebEventHandler, ctx: WebLogicEmitContext) {
        if (handler.actions.isEmpty()) {
            scope.write("// No actions defined for ${handler.name}")
            return
        }
        if (requiresCoroutineScope(handler)) {
            scope.write("${ctx.scopeVar}.launch {")
            scope.gen { handler.actions.forEach { emitAction(this, it, ctx) } }
            scope.write("}")
        } else {
            handler.actions.forEach { emitAction(scope, it, ctx) }
        }
    }

    private fun emitAction(scope: GenScope, action: WebAction, ctx: WebLogicEmitContext) {
        when (action) {
            is LogAction ->
                scope.write("console.log(${action.message.quote()})")

            is NavigateAction ->
                scope.write("${ctx.pageContextVar}.router.navigateTo(${action.route.quote()})")

            is SetStateAction ->
                scope.write("${action.stateKey.identifier()} = ${action.value.quote()}")

            is CallApiAction -> {
                val endpoint = ctx.model.endpoint(action.endpointId)
                if (endpoint == null) {
                    scope.write("// Unknown endpoint: ${action.endpointId}")
                    return
                }
                val call = "${ctx.apiVar}.${endpoint.name.identifier()}()"
                val resultKey = action.resultStateKey
                if (resultKey != null) {
                    scope.write("${resultKey.identifier()} = $call.toString()")
                } else {
                    scope.write("console.log($call.toString())")
                }
            }
        }
    }
}

/** Quotes [this] as a Kotlin string literal, escaping the characters that would break the literal. */
fun String.quote(): String {
    val escaped = this
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("$", "\\$")
        .replace("\n", "\\n")
        .replace("\t", "\\t")
    return "\"$escaped\""
}

/** Sanitizes [this] into a safe lower-camel Kotlin identifier. */
fun String.identifier(): String {
    val cleaned = buildString {
        var upperNext = false
        this@identifier.forEach { ch ->
            when {
                ch.isLetterOrDigit() -> {
                    append(if (upperNext) ch.uppercaseChar() else ch)
                    upperNext = false
                }
                else -> upperNext = isNotEmpty()
            }
        }
    }
    val safe = cleaned.ifBlank { "value" }
    return if (safe.first().isDigit()) "_$safe" else safe
}
