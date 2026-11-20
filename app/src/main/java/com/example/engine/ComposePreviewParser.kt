package com.example.engine

sealed interface PreviewNode {
    data class ColumnNode(
        val paddingDp: Int = 0,
        val centerHorizontally: Boolean = false,
        val children: List<PreviewNode>,
    ) : PreviewNode

    data class RowNode(
        val paddingDp: Int = 0,
        val centerVertically: Boolean = true,
        val children: List<PreviewNode>,
    ) : PreviewNode

    data class BoxNode(
        val paddingDp: Int = 0,
        val children: List<PreviewNode>,
    ) : PreviewNode

    data class CardNode(
        val paddingDp: Int = 0,
        val children: List<PreviewNode>,
    ) : PreviewNode

    data class TextNode(
        val templateText: String,
        val fontSizeSp: Int = 14,
        val isBold: Boolean = false,
        val colorHex: Long? = null,
    ) : PreviewNode

    data class ButtonNode(
        val label: String,
        val actionCode: String,
    ) : PreviewNode

    data class SwitchNode(
        val stateVarName: String,
        val label: String,
    ) : PreviewNode

    data class SliderNode(
        val stateVarName: String,
        val label: String,
    ) : PreviewNode

    data class IconNode(
        val iconName: String,
        val tintHex: Long? = null,
    ) : PreviewNode

    data class SpacerNode(
        val heightDp: Int = 8,
        val widthDp: Int = 8,
    ) : PreviewNode

    data object DividerNode : PreviewNode
}

data class ParsedPreviewScreen(
    val composableName: String,
    val initialStates: Map<String, Any>,
    val rootNodes: List<PreviewNode>,
)

object ComposePreviewParser {

    fun parse(kotlinCode: String): ParsedPreviewScreen {
        val states = extractInitialStates(kotlinCode)
        val previewEntry = findPreviewComposableBody(kotlinCode)
        val name = previewEntry.first
        val body = previewEntry.second
        val nodes = if (body.isNotBlank()) parseBlock(body) else emptyList()
        return ParsedPreviewScreen(
            composableName = name,
            initialStates = states,
            rootNodes = nodes,
        )
    }

    private fun extractInitialStates(code: String): Map<String, Any> {
        val states = linkedMapOf<String, Any>()
        val regex = Regex("""var\s+([a-zA-Z_][a-zA-Z0-9_]*)\s+by\s+remember\s*\{\s*mutableStateOf\(([^)]+)\)\s*\}""")
        for (match in regex.findAll(code)) {
            val varName = match.groupValues[1]
            val rawVal = match.groupValues[2].trim()
            val parsed: Any = when {
                rawVal == "true" -> true
                rawVal == "false" -> false
                rawVal.toIntOrNull() != null -> rawVal.toInt()
                rawVal.removeSuffix("f").toFloatOrNull() != null -> rawVal.removeSuffix("f").toFloat()
                rawVal.startsWith("\"") && rawVal.endsWith("\"") && rawVal.length >= 2 ->
                    rawVal.substring(1, rawVal.length - 1)
                else -> rawVal
            }
            states[varName] = parsed
        }
        return states
    }

    private fun findPreviewComposableBody(code: String): Pair<String, String> {
        val previewIdx = code.indexOf("@Preview")
        val searchFrom = if (previewIdx != -1) previewIdx else code.indexOf("@Composable")
        if (searchFrom == -1) return "Screen" to ""

        val funRegex = Regex("""fun\s+([A-Za-z0-9_]+)\s*\([^)]*\)\s*\{""")
        val match = funRegex.find(code, searchFrom) ?: return "Screen" to ""
        val fnName = match.groupValues[1]
        val openBrace = match.range.last
        val closeBrace = findMatchingBrace(code, openBrace)
        val body = if (closeBrace > openBrace) code.substring(openBrace + 1, closeBrace) else ""
        return fnName to body
    }

    private fun parseBlock(block: String): List<PreviewNode> {
        val nodes = mutableListOf<PreviewNode>()
        var i = 0
        val len = block.length

        while (i < len) {
            // Skip whitespace and comments
            if (block[i].isWhitespace()) {
                i++
                continue
            }
            if (block.startsWith("//", i)) {
                val nl = block.indexOf('\n', i)
                i = if (nl == -1) len else nl + 1
                continue
            }
            if (block.startsWith("/*", i)) {
                val close = block.indexOf("*/", i + 2)
                i = if (close == -1) len else close + 2
                continue
            }

            // Try matching a Composable call
            val callMatch = Regex("""^([A-Z][A-Za-z0-9_]*)\b""").find(block.substring(i))
            if (callMatch == null) {
                i++
                continue
            }

            val name = callMatch.groupValues[1]
            var cursor = i + name.length
            while (cursor < len && block[cursor].isWhitespace()) cursor++

            var args = ""
            if (cursor < len && block[cursor] == '(') {
                val closeParen = findMatchingDelimiter(block, cursor, '(', ')')
                if (closeParen > cursor) {
                    args = block.substring(cursor + 1, closeParen)
                    cursor = closeParen + 1
                }
            }

            while (cursor < len && block[cursor].isWhitespace()) cursor++

            var lambdaBody = ""
            if (cursor < len && block[cursor] == '{') {
                val closeBrace = findMatchingBrace(block, cursor)
                if (closeBrace > cursor) {
                    lambdaBody = block.substring(cursor + 1, closeBrace)
                    cursor = closeBrace + 1
                }
            }

            when (name) {
                "Column" -> nodes += PreviewNode.ColumnNode(
                    paddingDp = extractIntArg(args, "padding") ?: 0,
                    centerHorizontally = args.contains("CenterHorizontally"),
                    children = parseBlock(lambdaBody),
                )
                "Row" -> nodes += PreviewNode.RowNode(
                    paddingDp = extractIntArg(args, "padding") ?: 0,
                    centerVertically = !args.contains("Top") && !args.contains("Bottom"),
                    children = parseBlock(lambdaBody),
                )
                "Box" -> nodes += PreviewNode.BoxNode(
                    paddingDp = extractIntArg(args, "padding") ?: 0,
                    children = parseBlock(lambdaBody),
                )
                "Card", "Surface" -> nodes += PreviewNode.CardNode(
                    paddingDp = extractIntArg(args, "padding") ?: 4,
                    children = parseBlock(lambdaBody),
                )
                "Text" -> {
                    val txt = extractStringParam(args, "text")
                        ?: extractFirstQuotedString(args)
                        ?: "Text"
                    val size = extractIntArg(args, "fontSize") ?: 14
                    val bold = args.contains("Bold") || args.contains("SemiBold")
                    val color = extractColorHex(args)
                    nodes += PreviewNode.TextNode(
                        templateText = txt,
                        fontSizeSp = size,
                        isBold = bold,
                        colorHex = color,
                    )
                }
                "Button", "OutlinedButton" -> {
                    val action = extractOnClickAction(args)
                    val childNodes = parseBlock(lambdaBody)
                    val label = (childNodes.firstOrNull { it is PreviewNode.TextNode } as? PreviewNode.TextNode)
                        ?.templateText ?: "Button"
                    nodes += PreviewNode.ButtonNode(label = label, actionCode = action)
                }
                "Switch" -> {
                    val stateVar = Regex("""checked\s*=\s*([a-zA-Z_][a-zA-Z0-9_]*)""")
                        .find(args)?.groupValues?.getOrNull(1) ?: "toggle"
                    val label = extractStringParam(args, "label") ?: stateVar
                    nodes += PreviewNode.SwitchNode(stateVarName = stateVar, label = label)
                }
                "Slider" -> {
                    val stateVar = Regex("""value\s*=\s*([a-zA-Z_][a-zA-Z0-9_]*)""")
                        .find(args)?.groupValues?.getOrNull(1) ?: "sliderValue"
                    val label = extractStringParam(args, "label") ?: stateVar
                    nodes += PreviewNode.SliderNode(stateVarName = stateVar, label = label)
                }
                "Icon" -> {
                    val iconName = Regex("""Icons\.(?:Default|Filled|Outlined)\.(?:[A-Za-z]+\.)?([A-Za-z0-9_]+)""")
                        .find(args)?.groupValues?.getOrNull(1)
                        ?: Regex("""Icons\.[A-Za-z]+\.([A-Za-z0-9_]+)""")
                            .find(args)?.groupValues?.getOrNull(1)
                        ?: "Bolt"
                    nodes += PreviewNode.IconNode(
                        iconName = iconName,
                        tintHex = extractColorHex(args),
                    )
                }
                "Spacer" -> {
                    val h = extractIntArg(args, "height") ?: 8
                    val w = extractIntArg(args, "width") ?: 8
                    nodes += PreviewNode.SpacerNode(heightDp = h, widthDp = w)
                }
                "Divider", "HorizontalDivider" -> nodes += PreviewNode.DividerNode
            }

            i = cursor
        }

        return nodes
    }

    private fun extractStringParam(args: String, paramName: String): String? {
        val regex = Regex("""$paramName\s*=\s*"((?:[^"\\]|\\.)*)"""")
        return regex.find(args)?.groupValues?.getOrNull(1)
    }

    private fun extractFirstQuotedString(args: String): String? {
        val regex = Regex(""""((?:[^"\\]|\\.)*)"""")
        return regex.find(args)?.groupValues?.getOrNull(1)
    }

    private fun extractIntArg(args: String, key: String): Int? {
        val regex = Regex("""$key\s*(?:=|\()\s*(\d+)\s*(?:\.dp|\.sp)?""")
        return regex.find(args)?.groupValues?.getOrNull(1)?.toIntOrNull()
    }

    private fun extractColorHex(args: String): Long? {
        val hexRegex = Regex("""Color\(\s*0x([0-9A-Fa-f]{6,8})\s*\)""")
        val match = hexRegex.find(args)?.groupValues?.getOrNull(1) ?: return null
        val full = if (match.length == 6) "FF$match" else match
        return full.toLongOrNull(16)
    }

    private fun extractOnClickAction(args: String): String {
        val idx = args.indexOf("onClick")
        if (idx == -1) return ""
        val braceIdx = args.indexOf('{', idx)
        if (braceIdx == -1) return ""
        val closeIdx = findMatchingBrace(args, braceIdx)
        return if (closeIdx > braceIdx) args.substring(braceIdx + 1, closeIdx).trim() else ""
    }

    private fun findMatchingBrace(text: String, openIndex: Int): Int =
        findMatchingDelimiter(text, openIndex, '{', '}')

    private fun findMatchingDelimiter(text: String, openIndex: Int, open: Char, close: Char): Int {
        var depth = 0
        var inString = false
        var i = openIndex
        while (i < text.length) {
            val c = text[i]
            if (c == '"' && (i == 0 || text[i - 1] != '\\')) {
                inString = !inString
            } else if (!inString) {
                if (c == open) depth++
                else if (c == close) {
                    depth--
                    if (depth == 0) return i
                }
            }
            i++
        }
        return -1
    }

    fun resolveTemplateText(template: String, stateMap: Map<String, Any>): String {
        var result = template
        val interpRegex = Regex("""\$\{?([a-zA-Z_][a-zA-Z0-9_]*)\}?""")
        result = interpRegex.replace(result) { match ->
            val key = match.groupValues[1]
            stateMap[key]?.toString() ?: match.value
        }
        return result
    }

    fun applyClickAction(actionCode: String, current: Map<String, Any>): Pair<Map<String, Any>, String?> {
        if (actionCode.isBlank()) return current to null
        val updated = current.toMutableMap()
        val logs = mutableListOf<String>()

        // Increment: varName++
        Regex("""([a-zA-Z_][a-zA-Z0-9_]*)\s*\+\+""").findAll(actionCode).forEach { m ->
            val v = m.groupValues[1]
            val oldVal = (updated[v] as? Number)?.toInt() ?: 0
            updated[v] = oldVal + 1
            logs += "$v incremented to ${oldVal + 1}"
        }
        // Decrement: varName--
        Regex("""([a-zA-Z_][a-zA-Z0-9_]*)\s*--""").findAll(actionCode).forEach { m ->
            val v = m.groupValues[1]
            val oldVal = (updated[v] as? Number)?.toInt() ?: 0
            updated[v] = oldVal - 1
            logs += "$v decremented to ${oldVal - 1}"
        }
        // Toggle: varName = !varName
        Regex("""([a-zA-Z_][a-zA-Z0-9_]*)\s*=\s*!\s*\1""").findAll(actionCode).forEach { m ->
            val v = m.groupValues[1]
            val oldVal = (updated[v] as? Boolean) ?: false
            updated[v] = !oldVal
            logs += "$v toggled to ${!oldVal}"
        }
        // Numeric assignment: varName = 0
        Regex("""([a-zA-Z_][a-zA-Z0-9_]*)\s*=\s*(-?\d+)\b""").findAll(actionCode).forEach { m ->
            val v = m.groupValues[1]
            val num = m.groupValues[2].toIntOrNull() ?: 0
            updated[v] = num
            logs += "$v set to $num"
        }

        val logSummary = if (logs.isEmpty()) "onClick invoked" else logs.joinToString(", ")
        return updated to logSummary
    }
}
