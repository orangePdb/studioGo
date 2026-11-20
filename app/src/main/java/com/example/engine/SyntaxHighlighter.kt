package com.example.engine

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.example.data.model.IdeFileType
import com.example.ui.theme.EditorSyntaxColors

class IdeSyntaxVisualTransformation(
    private val fileType: IdeFileType,
    private val colors: EditorSyntaxColors,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val highlighted = SyntaxHighlighter.highlight(text.text, fileType, colors)
        return TransformedText(highlighted, OffsetMapping.Identity)
    }
}

object SyntaxHighlighter {

    private val kotlinKeywords = setOf(
        "package", "import", "class", "interface", "object", "data", "sealed", "enum",
        "fun", "val", "var", "const", "override", "private", "public", "internal", "protected",
        "return", "if", "else", "when", "for", "while", "do", "in", "is", "as", "by",
        "true", "false", "null", "this", "super", "try", "catch", "finally", "suspend",
        "plugins", "android", "defaultConfig", "buildFeatures", "dependencies", "implementation",
        "id", "platform", "alias",
    )

    private val composeTypes = setOf(
        "Column", "Row", "Box", "Card", "Surface", "Scaffold", "Text", "Button",
        "OutlinedButton", "IconButton", "Icon", "Spacer", "Switch", "Slider", "Divider",
        "HorizontalDivider", "Modifier", "Color", "Alignment", "Arrangement", "FontWeight",
        "Icons", "Bundle", "ComponentActivity", "MaterialTheme", "String", "Int", "Boolean",
        "Float", "Unit", "Remember", "remember", "mutableStateOf", "setContent",
    )

    fun highlight(
        code: String,
        fileType: IdeFileType,
        colors: EditorSyntaxColors,
    ): AnnotatedString {
        if (code.isEmpty()) return AnnotatedString("")

        val builder = AnnotatedString.Builder(code)
        builder.addStyle(SpanStyle(color = colors.plainText), 0, code.length)

        when (fileType) {
            IdeFileType.XML -> highlightXml(code, builder, colors)
            IdeFileType.KOTLIN, IdeFileType.GRADLE, IdeFileType.JSON, IdeFileType.PROPERTIES -> {
                highlightKotlinLike(code, builder, colors)
            }
        }

        return builder.toAnnotatedString()
    }

    private fun highlightKotlinLike(
        code: String,
        builder: AnnotatedString.Builder,
        colors: EditorSyntaxColors,
    ) {
        val len = code.length
        var i = 0
        while (i < len) {
            val c = code[i]

            // Line comment
            if (c == '/' && i + 1 < len && code[i + 1] == '/') {
                val end = code.indexOf('\n', i).let { if (it == -1) len else it }
                builder.addStyle(
                    SpanStyle(color = colors.comment, fontStyle = FontStyle.Italic),
                    i,
                    end,
                )
                i = end
                continue
            }

            // Block comment
            if (c == '/' && i + 1 < len && code[i + 1] == '*') {
                val close = code.indexOf("*/", i + 2)
                val end = if (close == -1) len else close + 2
                builder.addStyle(
                    SpanStyle(color = colors.comment, fontStyle = FontStyle.Italic),
                    i,
                    end,
                )
                i = end
                continue
            }

            // String literal
            if (c == '"') {
                var end = i + 1
                while (end < len) {
                    if (code[end] == '\\' && end + 1 < len) {
                        end += 2
                    } else if (code[end] == '"') {
                        end++
                        break
                    } else if (code[end] == '\n') {
                        break
                    } else {
                        end++
                    }
                }
                builder.addStyle(SpanStyle(color = colors.stringLiteral), i, end)
                i = end
                continue
            }

            // Annotation
            if (c == '@') {
                var end = i + 1
                while (end < len && (code[end].isLetterOrDigit() || code[end] == '_')) {
                    end++
                }
                if (end > i + 1) {
                    builder.addStyle(
                        SpanStyle(color = colors.annotation, fontWeight = FontWeight.Medium),
                        i,
                        end,
                    )
                }
                i = end
                continue
            }

            // Number / Hex literal
            if (c.isDigit()) {
                var end = i + 1
                while (
                    end < len &&
                    (code[end].isLetterOrDigit() || code[end] == '.' || code[end] == '_')
                ) {
                    end++
                }
                builder.addStyle(SpanStyle(color = colors.numberLiteral), i, end)
                i = end
                continue
            }

            // Identifier / keyword / type
            if (c.isLetter() || c == '_') {
                var end = i + 1
                while (end < len && (code[end].isLetterOrDigit() || code[end] == '_')) {
                    end++
                }
                val token = code.substring(i, end)
                when {
                    token in kotlinKeywords -> {
                        builder.addStyle(
                            SpanStyle(color = colors.keyword, fontWeight = FontWeight.SemiBold),
                            i,
                            end,
                        )
                    }
                    token in composeTypes || token.first().isUpperCase() -> {
                        builder.addStyle(SpanStyle(color = colors.type), i, end)
                    }
                    end < len && code[end] == '(' -> {
                        builder.addStyle(SpanStyle(color = colors.functionCall), i, end)
                    }
                }
                i = end
                continue
            }

            i++
        }
    }

    private fun highlightXml(
        code: String,
        builder: AnnotatedString.Builder,
        colors: EditorSyntaxColors,
    ) {
        val len = code.length
        var i = 0
        while (i < len) {
            if (code.startsWith("<!--", i)) {
                val close = code.indexOf("-->", i + 4)
                val end = if (close == -1) len else close + 3
                builder.addStyle(
                    SpanStyle(color = colors.comment, fontStyle = FontStyle.Italic),
                    i,
                    end,
                )
                i = end
                continue
            }
            if (code[i] == '"') {
                var end = i + 1
                while (end < len && code[end] != '"') {
                    end++
                }
                if (end < len) end++
                builder.addStyle(SpanStyle(color = colors.stringLiteral), i, end)
                i = end
                continue
            }
            if (code[i] == '<') {
                var end = i + 1
                if (end < len && (code[end] == '/' || code[end] == '?')) end++
                while (end < len && !code[end].isWhitespace() && code[end] != '>' && code[end] != '/') {
                    end++
                }
                builder.addStyle(
                    SpanStyle(color = colors.xmlTag, fontWeight = FontWeight.SemiBold),
                    i,
                    end,
                )
                i = end
                continue
            }
            if (code[i].isLetter() && i > 0) {
                var end = i + 1
                while (end < len && (code[end].isLetterOrDigit() || code[end] == ':' || code[end] == '_')) {
                    end++
                }
                if (end < len && code[end] == '=') {
                    builder.addStyle(SpanStyle(color = colors.xmlAttribute), i, end)
                    i = end
                    continue
                }
            }
            i++
        }
    }
}
