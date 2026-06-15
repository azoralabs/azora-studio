package org.azora.studio.highlight

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import org.azora.lang.token.TokenType

class SyntaxHighlighter(
    private val usePastel: Boolean = true,
    private val boldKeywords: Boolean = true,
    private val italicPreprocessor: Boolean = true,
    private val underlineVariables: Boolean = false
) {
    // ── Azora Website Pastel palette ──
    private val pastelPink = Color(0xFFD16B8E)
    private val pastelTeal = Color(0xFF5FA89F)
    private val pastelOrange = Color(0xFFD4A574)
    private val pastelGreen = Color(0xFF7DBF8A)
    private val pastelYellow = Color(0xFFE6C96B)
    private val pastelPurple = Color(0xFFB06FA8)
    private val pastelWhite = Color(0xFFD9D9D9)
    private val pastelLightGray = Color(0xFFB2B3B3)
    private val gray60 = Color(0xFF676767)
    private val gray40 = Color(0xFFB2B3B3)
    private val azRed = Color(0xFFE63946)

    // ── Accent palette ──
    private val accentPink = Color(0xFFE8568E)
    private val accentTeal = Color(0xFF4EC9B0)
    private val accentOrange = Color(0xFFE8944A)
    private val accentGreen = Color(0xFF5CC96B)
    private val accentYellow = Color(0xFFF0D060)
    private val accentPurple = Color(0xFFC06FB8)
    private val accentWhite = Color(0xFFE8E8E8)
    private val accentLightGray = Color(0xFFC0C0C0)
    private val accentGrayComment = Color(0xFF808080)
    private val accentGrayOp = Color(0xFFC0C0C0)
    private val accentRed = Color(0xFFE63946)

    private val keyword get() = if (usePastel) pastelPink else accentPink
    private val typeColor get() = if (usePastel) pastelTeal else accentTeal
    private val function get() = if (usePastel) pastelOrange else accentOrange
    private val string get() = if (usePastel) pastelGreen else accentGreen
    private val number get() = if (usePastel) pastelWhite else accentWhite
    private val commentColor get() = if (usePastel) gray60 else accentGrayComment
    private val operatorColor get() = if (usePastel) gray40 else accentGrayOp
    private val annotation get() = if (usePastel) pastelYellow else accentYellow
    private val boolColor get() = if (usePastel) pastelPink else accentPink
    private val white get() = if (usePastel) pastelWhite else accentWhite
    private val paramColor get() = if (usePastel) pastelLightGray else accentLightGray
    private val unusedColor get() = if (usePastel) gray60 else accentGrayComment
    private val error get() = if (usePastel) azRed else accentRed

    fun highlight(source: String): AnnotatedString = highlight(emptySet(), source)

    fun highlight(externalTypeNames: Set<String> = emptySet(), source: String): AnnotatedString {
        return try {
            val realTokens = org.azora.lang.lexer.Lexer(source).tokenize()
            val builder = AnnotatedString.Builder(source)

            val usedPackages = collectUsedPackages(realTokens)

            var prevType: TokenType? = null
            var insideParamList = false
            var parenDepth = 0
            var braceDepth = 0
            var paramListStartDepth = 0
            var afterPackageKeyword = false
            var afterUseKeyword = false
            var currentUsePath = ""
            var currentParamNames = mutableSetOf<String>()
            var funcBodyBraceDepth = -1  // brace depth when entering a function body
            var expectParamList = false   // true after seeing func/flow/task + name
            var inFuncDecl = false        // true between declaration name and opening {

            for (token in realTokens) {
                val start = token.startOffset
                val end = (start + token.lexeme.length).coerceAtMost(source.length)
                if (start >= end || start < 0) {
                    if (token.type != TokenType.NEWLINE) prevType = token.type
                    continue
                }

                val tt = token.type

                if (tt == TokenType.L_PAREN) {
                    parenDepth++
                    // Only enter param list if this ( follows a function declaration name
                    if (expectParamList && parenDepth == 1) {
                        insideParamList = true
                        paramListStartDepth = parenDepth
                    }
                    expectParamList = false
                } else if (tt == TokenType.R_PAREN) {
                    if (insideParamList && parenDepth == paramListStartDepth) insideParamList = false
                    parenDepth--
                } else if (tt == TokenType.L_BRACE) {
                    braceDepth++
                    // Entering a function body — track depth to know when we leave
                    if (inFuncDecl) {
                        funcBodyBraceDepth = braceDepth
                        inFuncDecl = false
                    }
                } else if (tt == TokenType.R_BRACE) {
                    if (braceDepth == funcBodyBraceDepth) {
                        currentParamNames.clear()
                        funcBodyBraceDepth = -1
                    }
                    braceDepth--
                }

                if (tt == TokenType.PACKAGE) afterPackageKeyword = true
                else if (tt == TokenType.USE) { afterUseKeyword = true; currentUsePath = "" }

                val isCallableName = prevType == TokenType.FN || prevType == TokenType.TASK ||
                    prevType == TokenType.FLOW || prevType == TokenType.HOOK || prevType == TokenType.TEST

                // When we hit a new function/flow/task declaration name, reset params and expect ( next
                if (isCallableName && tt == TokenType.IDENTIFIER) {
                    currentParamNames.clear()
                    funcBodyBraceDepth = -1
                    expectParamList = true
                    inFuncDecl = true
                }

                val style = when {
                    // Package path → white, italic if enabled
                    afterPackageKeyword && (tt == TokenType.IDENTIFIER || tt == TokenType.DOT) ->
                        SpanStyle(color = white, fontStyle = if (italicPreprocessor) FontStyle.Italic else null)

                    // Use path → white if used (grayed if unused), italic if enabled
                    afterUseKeyword && (tt == TokenType.IDENTIFIER || tt == TokenType.DOT) -> {
                        currentUsePath += token.lexeme
                        val pathColor = if (currentUsePath in usedPackages || usedPackages.any { it.startsWith(currentUsePath) }) white else unusedColor
                        SpanStyle(color = pathColor, fontStyle = if (italicPreprocessor) FontStyle.Italic else null)
                    }

                    // fn/task/flow/hook/test name → function color
                    tt == TokenType.IDENTIFIER && isCallableName -> SpanStyle(color = function)

                    // Param list: uppercase → type, lowercase → param name (collect it)
                    tt == TokenType.IDENTIFIER && insideParamList -> {
                        val ch = token.lexeme.firstOrNull()
                        if ((ch != null && ch.isUpperCase()) || token.lexeme in externalTypeNames) {
                            SpanStyle(color = typeColor)
                        } else {
                            // Collect param name (identifier before `:` in param list)
                            currentParamNames.add(token.lexeme)
                            SpanStyle(color = paramColor)
                        }
                    }

                    // Regular identifiers → white (variables/names) or type/function
                    tt == TokenType.IDENTIFIER -> {
                        val ch = token.lexeme.firstOrNull()
                        when {
                            ch != null && ch.isUpperCase() -> SpanStyle(color = typeColor)
                            token.lexeme in externalTypeNames -> SpanStyle(color = typeColor)
                            token.lexeme in builtinFunctions -> SpanStyle(color = function)
                            token.lexeme in currentParamNames -> SpanStyle(color = paramColor)
                            else -> SpanStyle(
                                color = white,
                                textDecoration = if (underlineVariables) TextDecoration.Underline else null
                            )
                        }
                    }

                    else -> styleForToken(tt)
                }

                if (style != null) builder.addStyle(style, start, end)

                if (tt == TokenType.NEWLINE) {
                    afterPackageKeyword = false
                    if (afterUseKeyword) { afterUseKeyword = false; currentUsePath = "" }
                } else {
                    if (afterPackageKeyword && tt != TokenType.IDENTIFIER && tt != TokenType.DOT) afterPackageKeyword = false
                    if (afterUseKeyword && tt != TokenType.IDENTIFIER && tt != TokenType.DOT && tt != TokenType.USE) {
                        afterUseKeyword = false; currentUsePath = ""
                    }
                    prevType = tt
                }
            }

            builder.toAnnotatedString()
        } catch (_: Throwable) {
            AnnotatedString(source)
        }
    }

    private fun collectUsedPackages(tokens: List<org.azora.lang.token.Token>): Set<String> {
        val declared = mutableSetOf<String>()
        var i = 0
        while (i < tokens.size) {
            if (tokens[i].type == TokenType.USE) {
                i++
                if (i < tokens.size && tokens[i].type == TokenType.SCOPE) { declared.add("scope"); i++ }
                val path = StringBuilder()
                while (i < tokens.size && (tokens[i].type == TokenType.IDENTIFIER || tokens[i].type == TokenType.DOT)) {
                    path.append(tokens[i].lexeme); i++
                }
                if (path.isNotEmpty()) declared.add(path.toString())
            } else i++
        }
        return declared
    }

    private fun styleForToken(type: TokenType): SpanStyle? = when (type) {
        // Keywords
        TokenType.VAR, TokenType.FIN, TokenType.FN, TokenType.IF, TokenType.ELSE,
        TokenType.FOR, TokenType.LOOP, TokenType.WHILE, TokenType.RETURN,
        TokenType.BREAK, TokenType.CONTINUE, TokenType.IN, TokenType.AS, TokenType.IS,
        TokenType.WHEN, TokenType.NULL, TokenType.EXPOSE, TokenType.CONFINE,
        TokenType.ASSERT, TokenType.TRACE, TokenType.LAUNCH, TokenType.ASYNC,
        TokenType.AWAIT, TokenType.INLINE, TokenType.SCOPE, TokenType.USE,
        TokenType.FLIP, TokenType.FLOP, TokenType.BY, TokenType.WITH,
        TokenType.WHERE, TokenType.EACH, TokenType.LET, TokenType.TASK,
        TokenType.SUSPEND, TokenType.FLOW, TokenType.YIELD, TokenType.FAIL,
        TokenType.TRY, TokenType.CATCH, TokenType.DEFER, TokenType.RESCUE,
        TokenType.GUARD, TokenType.DYN, TokenType.PROTECT, TokenType.BASE,
        TokenType.LEAF, TokenType.THIS, TokenType.THROW, TokenType.SELF,
        TokenType.IT, TokenType.REF, TokenType.MUT, TokenType.ALLOC, TokenType.DROP,
        TokenType.UNSAFE, TokenType.INJECT, TokenType.WRAP, TokenType.BIND,
        TokenType.LAZY, TokenType.OUT, TokenType.BRIDGE, TokenType.OPER,
        TokenType.CTOR, TokenType.DTOR, TokenType.PROP, TokenType.REPL,
        TokenType.SOLO, TokenType.ISOLATED, TokenType.THREADLOCAL,
        TokenType.REGION, TokenType.EFFECT, TokenType.VIEW, TokenType.REM,
        TokenType.HOOK, TokenType.TEST, TokenType.PACKAGE ->
            SpanStyle(color = keyword, fontWeight = if (boldKeywords) FontWeight.Bold else null)

        // Type keywords
        TokenType.ENUM, TokenType.SLOT, TokenType.PACK, TokenType.IMPL,
        TokenType.INFX, TokenType.NODE, TokenType.SPEC, TokenType.TYPEALIAS,
        TokenType.TYPE ->
            SpanStyle(color = typeColor, fontWeight = if (boldKeywords) FontWeight.Bold else null)

        // Preprocessor
        TokenType.DEEPINLINE, TokenType.NOINLINE, TokenType.PLATFORM ->
            SpanStyle(color = if (usePastel) pastelPurple else accentPurple, fontStyle = if (italicPreprocessor) FontStyle.Italic else null)

        // Decorators
        TokenType.DECO, TokenType.DECORATOR -> SpanStyle(color = annotation)

        // Strings
        TokenType.STRING_LITERAL, TokenType.RAW_STRING_LITERAL, TokenType.CHAR_LITERAL,
        TokenType.STRING_TEMPLATE_START, TokenType.STRING_TEMPLATE_MID, TokenType.STRING_TEMPLATE_END ->
            SpanStyle(color = string)

        TokenType.DOLLAR -> SpanStyle(color = annotation)

        // Numbers
        TokenType.INT_LITERAL, TokenType.REAL_LITERAL -> SpanStyle(color = number)

        // Booleans
        TokenType.BOOL_LITERAL -> SpanStyle(color = boolColor, fontWeight = if (boldKeywords) FontWeight.Bold else null)

        // Operators & punctuation
        TokenType.PLUS, TokenType.MINUS, TokenType.STAR, TokenType.SLASH, TokenType.PERCENT,
        TokenType.EQUAL, TokenType.EQUAL_EQUAL, TokenType.BANG_EQUAL,
        TokenType.LESS, TokenType.LESS_EQUAL, TokenType.GREATER, TokenType.GREATER_EQUAL,
        TokenType.AND_AND, TokenType.OR_OR, TokenType.BANG, TokenType.ARROW,
        TokenType.DOT_DOT, TokenType.DOT_DOT_LESS, TokenType.ELLIPSIS,
        TokenType.QUESTION, TokenType.QUESTION_QUESTION, TokenType.QUESTION_DOT,
        TokenType.PLUS_EQUAL, TokenType.MINUS_EQUAL, TokenType.STAR_EQUAL,
        TokenType.SLASH_EQUAL, TokenType.PERCENT_EQUAL,
        TokenType.PLUS_PLUS, TokenType.MINUS_MINUS,
        TokenType.AMPERSAND, TokenType.PIPE, TokenType.TILDE, TokenType.CARET,
        TokenType.SHIFT_LEFT, TokenType.SHIFT_RIGHT,
        TokenType.L_PAREN, TokenType.R_PAREN, TokenType.L_BRACE, TokenType.R_BRACE,
        TokenType.L_BRACKET, TokenType.R_BRACKET,
        TokenType.COMMA, TokenType.COLON, TokenType.DOT, TokenType.SEMICOLON,
        TokenType.DOUBLE_COLON ->
            SpanStyle(color = operatorColor)

        TokenType.TOKEN_ERROR -> SpanStyle(color = error)

        else -> null
    }

    companion object {
        private val builtinFunctions = setOf(
            "print", "println", "readLine", "sizeof", "typeof", "len",
            "push", "pop", "map", "filter", "reduce", "forEach", "find",
            "contains", "sort", "reverse", "join", "split", "trim",
            "abs", "min", "max", "sqrt", "pow", "floor", "ceil", "round",
            "sin", "cos", "tan", "asin", "acos", "atan", "atan2",
            "log", "log2", "log10", "exp",
            "random", "randomInt", "randomReal",
            "sleep", "time", "panic"
        )

        fun create(
            usePastel: Boolean = true,
            boldKeywords: Boolean = true,
            italicPreprocessor: Boolean = true,
            underlineVariables: Boolean = false
        ): SyntaxHighlighter = SyntaxHighlighter(usePastel, boldKeywords, italicPreprocessor, underlineVariables)
    }
}
