package dev.azora.nodes.domain

import org.azora.lang.frontend.Expr
import org.azora.lang.frontend.NumericSuffix
import org.azora.lang.frontend.ReactiveKind
import org.azora.lang.frontend.Stmt
import org.azora.lang.frontend.TokenType
import org.azora.lang.frontend.TypeAnnotation
import org.azora.lang.frontend.TypeRef

/**
 * Prints azora-lang frontend AST subtrees back to azora source text.
 *
 * Used by the .az ↔ .azn converters:
 * - [AzToNodesConverter] prints statements/expressions it cannot represent as
 *   structured nodes into `AZ_CODE` / `AZ_EXPR` fallback nodes.
 * - Round-trip tests print whole reparsed programs to verify fidelity.
 *
 * The output is *semantically* equivalent, normalized source — original
 * whitespace and comments are not preserved. Every [Stmt] and [Expr] variant
 * is covered so the converters can always fall back to raw source.
 */
object AzSourcePrinter {

    private const val INDENT = "    "

    // -------------------------------------------------------------------
    // Statements
    // -------------------------------------------------------------------

    /** Prints [stmts] as source lines at the given [indent] level. */
    fun printBlock(stmts: List<Stmt>, indent: Int = 0): String =
        stmts.joinToString("\n") { printStmt(it, indent) }

    /** Prints a single statement (possibly multi-line) at [indent]. */
    fun printStmt(stmt: Stmt, indent: Int = 0): String {
        val pad = INDENT.repeat(indent)
        return when (stmt) {
            is Stmt.VarDecl -> "$pad${decl("var", stmt.name, stmt.type, stmt.initializer)}"
            is Stmt.LetDecl -> "$pad${decl("let", stmt.name, stmt.type, stmt.initializer)}"
            is Stmt.FinDecl -> "$pad${decl("fin", stmt.name, stmt.type, stmt.initializer)}"
            is Stmt.InlineVar -> "${pad}inline ${decl("var", stmt.name, stmt.type, stmt.initializer)}"
            is Stmt.InlineLet -> "${pad}inline ${decl("let", stmt.name, stmt.type, stmt.initializer)}"
            is Stmt.InlineFin -> "${pad}inline ${decl("fin", stmt.name, stmt.type, stmt.initializer)}"
            is Stmt.RemDecl -> "${pad}${reactiveKeyword(stmt.kind)} ${decl("var", stmt.name, stmt.type, stmt.initializer)}"
            is Stmt.Assignment -> "$pad${stmt.name} = ${printExpr(stmt.value)}"
            is Stmt.InlineAssignment -> "${pad}inline ${stmt.name} = ${printExpr(stmt.value)}"
            is Stmt.IndexAssign -> "$pad${printExpr(stmt.target)}[${printExpr(stmt.index)}] = ${printExpr(stmt.value)}"
            is Stmt.MemberAssign -> "$pad${printExpr(stmt.target)}.${stmt.name} = ${printExpr(stmt.value)}"
            is Stmt.DerefAssign -> "${pad}deref ${printExpr(stmt.target)} = ${printExpr(stmt.value)}"
            is Stmt.Return -> if (stmt.value != null) "${pad}return ${printExpr(stmt.value)}" else "${pad}return"
            is Stmt.ExprStmt -> "$pad${printExpr(stmt.expr)}"
            is Stmt.Throw -> "${pad}throw ${printExpr(stmt.value)}"
            is Stmt.Yield -> "${pad}yield ${printExpr(stmt.value)}"
            is Stmt.Break -> pad + "break" + (stmt.label?.let { " @$it" } ?: "")
            is Stmt.Continue -> pad + "continue" + (stmt.label?.let { " @$it" } ?: "")
            is Stmt.Assert -> "${pad}assert ${printExpr(stmt.condition)} { ${printExpr(stmt.message)} }"
            is Stmt.InlineAssert -> "${pad}inline assert ${printExpr(stmt.condition)} { ${printExpr(stmt.message)} }"
            is Stmt.Trace -> "${pad}trace { ${printExpr(stmt.message)} }"
            is Stmt.InlineTrace -> "${pad}inline trace { ${printExpr(stmt.message)} }"

            is Stmt.If -> printIf("if", stmt.condition, stmt.thenBranch, stmt.elseBranch, indent)
            is Stmt.InlineIf -> printIf("inline if", stmt.condition, stmt.thenBranch, stmt.elseBranch, indent)
            is Stmt.DeepInlineIf -> printIf("deepinline if", stmt.condition, stmt.thenBranch, stmt.elseBranch, indent)

            is Stmt.While -> buildString {
                append(pad)
                stmt.label?.let { append("@$it ") }
                append("while ${printExpr(stmt.condition)} {\n")
                appendBody(stmt.body, indent)
                append("$pad}")
            }
            is Stmt.For -> buildString {
                append(pad)
                stmt.label?.let { append("@$it ") }
                append("for ")
                if (stmt.reverse) append("reverse ")
                append("${stmt.name} in ${printExpr(stmt.iterable)}")
                stmt.step?.let { append(" by ${printExpr(it)}") }
                append(" {\n")
                appendBody(stmt.body, indent)
                append("$pad}")
            }
            is Stmt.InlineFor -> buildString {
                append("${pad}inline for ${stmt.name} in ${printExpr(stmt.iterable)} {\n")
                appendBody(stmt.body, indent)
                append("$pad}")
            }
            is Stmt.Loop -> buildString {
                append(pad)
                stmt.label?.let { append("@$it ") }
                append("loop")
                stmt.iterable?.let { append(" ${printExpr(it)}") }
                append(" {\n")
                appendBody(stmt.body, indent)
                append("$pad}")
            }
            is Stmt.When -> buildString {
                append("${pad}when ${printExpr(stmt.scrutinee)} {\n")
                val inner = INDENT.repeat(indent + 1)
                for (branch in stmt.branches) {
                    append("$inner${branch.patterns.joinToString(", ") { printExpr(it) }} -> {\n")
                    appendBody(branch.body, indent + 1)
                    append("$inner}\n")
                }
                if (stmt.elseBranch != null) {
                    append("${inner}else -> {\n")
                    appendBody(stmt.elseBranch!!, indent + 1)
                    append("$inner}\n")
                }
                append("$pad}")
            }
            is Stmt.Try -> buildString {
                append("${pad}try {\n")
                appendBody(stmt.body, indent)
                if (stmt.catchBody != null) {
                    append("$pad} catch {")
                    if (stmt.catchName != null) append(" ${stmt.catchName} ->")
                    append("\n")
                    appendBody(stmt.catchBody!!, indent)
                }
                append("$pad}")
            }
            is Stmt.Defer -> buildString {
                append(pad)
                if (stmt.onFail) append("fail ")
                append("defer {\n")
                appendBody(stmt.body, indent)
                append("$pad}")
            }
            is Stmt.Zone -> printBlockKeyword(if (stmt.unsafe) "unsafe" else if (stmt.alloc) "zone alloc" else "zone", stmt.body, indent)
            is Stmt.FriendZone -> printBlockKeyword(if (stmt.alloc) "friend zone alloc" else "friend zone", stmt.body, indent)
            is Stmt.InlineBlock -> printBlockKeyword("inline", stmt.body, indent)
            is Stmt.DeepInlineBlock -> printBlockKeyword("deepinline", stmt.body, indent)
            is Stmt.Effect -> printBlockKeyword("effect", stmt.body, indent)
            is Stmt.Panic -> "${pad}${if (stmt.inlinePanic) "inline " else ""}panic { ${printExpr(stmt.message)} }"
            is Stmt.NoInline -> "${pad}noinline ${printStmt(stmt.stmt).trimStart()}"
        }
    }

    private fun printIf(keyword: String, condition: Expr, thenBranch: List<Stmt>, elseBranch: List<Stmt>?, indent: Int): String {
        val pad = INDENT.repeat(indent)
        return buildString {
            append("$pad$keyword ${printExpr(condition)} {\n")
            appendBody(thenBranch, indent)
            if (elseBranch != null) {
                // Collapse `else { if ... }` back into `else if` chains.
                val single = elseBranch.singleOrNull()
                if (single is Stmt.If) {
                    append("$pad} else ${printStmt(single, indent).trimStart()}")
                    return@buildString
                }
                append("$pad} else {\n")
                appendBody(elseBranch, indent)
            }
            append("$pad}")
        }
    }

    private fun printBlockKeyword(keyword: String, body: List<Stmt>, indent: Int): String {
        val pad = INDENT.repeat(indent)
        return buildString {
            append("$pad$keyword {\n")
            appendBody(body, indent)
            append("$pad}")
        }
    }

    private fun StringBuilder.appendBody(body: List<Stmt>, indent: Int) {
        for (s in body) append(printStmt(s, indent + 1)).append("\n")
    }

    private fun reactiveKeyword(kind: ReactiveKind): String = when (kind) {
        ReactiveKind.MEM -> "mem"
        ReactiveKind.REM -> "rem"
        ReactiveKind.RET -> "ret"
    }

    private fun decl(keyword: String, name: String, type: TypeAnnotation, initializer: Expr): String {
        val typePart = when (type) {
            is TypeAnnotation.Explicit -> ": ${printType(type.ref)}"
            TypeAnnotation.Inferred -> ""
        }
        return "$keyword $name$typePart = ${printExpr(initializer)}"
    }

    // -------------------------------------------------------------------
    // Expressions
    // -------------------------------------------------------------------

    /** Prints a single expression as azora source. */
    fun printExpr(expr: Expr): String = when (expr) {
        is Expr.IntLiteral -> "${expr.value}${suffixText(expr.suffix)}"
        is Expr.RealLiteral -> {
            val text = expr.value.toString()
            val normalized = if ('.' in text || 'e' in text || 'E' in text || "Infinity" in text || "NaN" in text) text else "$text.0"
            "$normalized${suffixText(expr.suffix)}"
        }
        is Expr.StringLiteral -> "\"${escapeString(expr.value)}\""
        is Expr.CharLiteral -> "'${escapeChar(expr.value)}'"
        is Expr.BoolLiteral -> "${expr.value}"
        is Expr.NullLiteral -> "null"
        is Expr.Identifier -> expr.name
        is Expr.Grouping -> "(${printExpr(expr.expr)})"
        is Expr.Binary -> "${printExpr(expr.left)} ${operatorText(expr.op)} ${printExpr(expr.right)}"
        is Expr.Unary -> "${operatorText(expr.op)}${printExpr(expr.operand)}"
        is Expr.Call -> "${expr.callee}(${expr.args.joinToString(", ") { printExpr(it) }})"
        is Expr.MethodCall -> "${printExpr(expr.target)}.${expr.name}(${expr.args.joinToString(", ") { printExpr(it) }})"
        is Expr.Member -> "${printExpr(expr.target)}.${expr.name}"
        is Expr.SafeMember -> "${printExpr(expr.target)}?.${expr.name}"
        is Expr.Index -> "${printExpr(expr.target)}[${printExpr(expr.index)}]"
        is Expr.Range -> "${printExpr(expr.from)}${if (expr.inclusive) ".." else "..<"}${printExpr(expr.to)}"
        is Expr.ArrayLiteral -> "[${expr.elements.joinToString(", ") { printExpr(it) }}]"
        is Expr.SetLiteral -> "setOf(${expr.elements.joinToString(", ") { printExpr(it) }})"
        is Expr.MapLit ->
            if (expr.entries.isEmpty()) "[:]"
            else "[${expr.entries.joinToString(", ") { (k, v) -> "${printExpr(k)}: ${printExpr(v)}" }}]"
        is Expr.TupleLit -> "(${expr.elements.joinToString(", ") { printExpr(it) }})"
        is Expr.VariantLit -> "Var(${expr.elements.joinToString(", ") { printExpr(it) }})"
        is Expr.TupleAccess -> "${printExpr(expr.target)}.${expr.index}"
        is Expr.StringTemplate -> printTemplate(expr)
        is Expr.Lambda -> {
            val params = expr.params.joinToString(", ") { p -> "${p.name}: ${printType(p.type)}" }
            val arrow = if (expr.params.isEmpty()) "" else "$params -> "
            val single = expr.body.singleOrNull()
            if (single is Stmt.Return && single.value != null) {
                "{ $arrow${printExpr(single.value!!)} }"
            } else if (single is Stmt.ExprStmt) {
                "{ $arrow${printExpr(single.expr)} }"
            } else {
                buildString {
                    append("{ $arrow\n")
                    for (s in expr.body) append(printStmt(s, 1)).append("\n")
                    append("}")
                }
            }
        }
        is Expr.NamedArg -> "${expr.name}: ${printExpr(expr.value)}"
        is Expr.CatchExpr -> "${printExpr(expr.expr)} catch ${printExpr(expr.fallback)}"
        is Expr.IfExpr -> "if ${printExpr(expr.condition)} { ${printExpr(expr.thenExpr)} } else { ${printExpr(expr.elseExpr)} }"
        is Expr.NullCoalesce -> "${printExpr(expr.left)} ?? ${printExpr(expr.right)}"
        is Expr.Cast -> "${printExpr(expr.expr)} as ${printType(expr.targetType)}"
        is Expr.IsCheck -> "${printExpr(expr.expr)} is ${expr.typeName}"
        is Expr.UpperScopeAccess -> "${"^".repeat(expr.depth)}${expr.name}"
        is Expr.Alloc -> "alloc ${printExpr(expr.value)}"
        is Expr.AllocBuffer -> "alloc ${expr.typeName}[${printExpr(expr.count)}]"
        is Expr.Deref -> "deref ${printExpr(expr.target)}"
        is Expr.Isolated -> "isolated ${printExpr(expr.value)}"
        is Expr.Await -> "await ${printExpr(expr.value)}"
        is Expr.Inject -> "inject ${expr.typeName}"
        is Expr.Spread -> "...${printExpr(expr.array)}"
    }

    private fun printTemplate(expr: Expr.StringTemplate): String = buildString {
        append('"')
        for (part in expr.parts) {
            when (part) {
                is Expr.StringTemplatePart.Literal -> append(escapeString(part.text))
                is Expr.StringTemplatePart.Expr -> {
                    val inner = part.expr
                    if (inner is Expr.Identifier) append("$").append(inner.name)
                    else append("\${").append(printExpr(inner)).append("}")
                }
            }
        }
        append('"')
    }

    // -------------------------------------------------------------------
    // Types & tokens
    // -------------------------------------------------------------------

    /** Prints a type reference as written in azora source. */
    fun printType(ref: TypeRef): String = when (ref) {
        is TypeRef.Named -> {
            val base = if (ref.args.isEmpty()) ref.name else "${ref.name}<${ref.args.joinToString(", ") { printType(it) }}>"
            if (ref.variadic) "...$base" else base
        }
        is TypeRef.Array -> "Array<${printType(ref.element)}>"
        is TypeRef.Map -> "Map<${printType(ref.key)}, ${printType(ref.value)}>"
        is TypeRef.Set -> "Set<${printType(ref.element)}>"
        is TypeRef.Function -> "(${ref.params.joinToString(", ") { printType(it) }}) -> ${printType(ref.ret)}"
        is TypeRef.Tuple -> "(${ref.elements.joinToString(", ") { printType(it) }})"
        is TypeRef.Nullable -> "${printType(ref.inner)}?"
        is TypeRef.Failable -> "${printType(ref.ok)}!${ref.errSet}"
        is TypeRef.Pointer -> "${printType(ref.inner)}*"
        is TypeRef.Reference -> "${ref.kind.spelling} ${printType(ref.inner)}"
    }

    /** The source text of an operator token. */
    fun operatorText(op: TokenType): String = when (op) {
        TokenType.PLUS -> "+"
        TokenType.MINUS -> "-"
        TokenType.STAR -> "*"
        TokenType.SLASH -> "/"
        TokenType.PERCENT -> "%"
        TokenType.EQUAL_EQUAL -> "=="
        TokenType.BANG_EQUAL -> "!="
        TokenType.LESS -> "<"
        TokenType.LESS_EQUAL -> "<="
        TokenType.GREATER -> ">"
        TokenType.GREATER_EQUAL -> ">="
        TokenType.AND_AND -> "&&"
        TokenType.OR_OR -> "||"
        TokenType.BANG -> "!"
        TokenType.AMP -> "&"
        TokenType.PIPE -> "|"
        TokenType.CARET -> "^"
        TokenType.TILDE -> "~"
        TokenType.SHIFT_LEFT -> "<<"
        TokenType.SHIFT_RIGHT -> ">>"
        TokenType.QMARK_QMARK -> "??"
        else -> op.name.lowercase()
    }

    private fun suffixText(suffix: NumericSuffix): String = when (suffix) {
        NumericSuffix.NONE -> ""
        NumericSuffix.BYTE -> "b"
        NumericSuffix.UBYTE -> "ub"
        NumericSuffix.SHORT -> "s"
        NumericSuffix.USHORT -> "us"
        NumericSuffix.UINT -> "u"
        NumericSuffix.LONG -> "L"
        NumericSuffix.ULONG -> "uL"
        NumericSuffix.CENT -> "c"
        NumericSuffix.UCENT -> "uc"
        NumericSuffix.FLOAT -> "f"
        NumericSuffix.DECIMAL -> "D"
    }

    /** Escapes a string literal body for azora source. */
    fun escapeString(s: String): String = buildString {
        for (c in s) {
            when (c) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\t' -> append("\\t")
                '\r' -> append("\\r")
                '$' -> append("\\$")
                else -> append(c)
            }
        }
    }

    private fun escapeChar(c: Char): String = when (c) {
        '\\' -> "\\\\"
        '\'' -> "\\'"
        '\n' -> "\\n"
        '\t' -> "\\t"
        '\r' -> "\\r"
        else -> c.toString()
    }
}
