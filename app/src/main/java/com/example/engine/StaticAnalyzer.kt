package com.example.engine

import com.example.data.local.IdeFileEntity
import com.example.data.model.CodeDiagnostic
import com.example.data.model.DiagnosticSeverity
import com.example.data.model.IdeFileType

object StaticAnalyzer {

    fun analyzeProject(files: List<IdeFileEntity>): List<CodeDiagnostic> {
        val results = mutableListOf<CodeDiagnostic>()
        for (file in files) {
            results += analyzeFile(file)
        }
        return results.sortedWith(compareBy({ it.severity.ordinal }, { it.fileName }, { it.line }))
    }

    fun analyzeFile(file: IdeFileEntity): List<CodeDiagnostic> {
        return when (file.fileType) {
            IdeFileType.KOTLIN, IdeFileType.GRADLE -> analyzeKotlinOrGradle(file)
            IdeFileType.XML -> analyzeXml(file)
            IdeFileType.JSON, IdeFileType.PROPERTIES -> emptyList()
        }
    }

    private fun analyzeKotlinOrGradle(file: IdeFileEntity): List<CodeDiagnostic> {
        val diagnostics = mutableListOf<CodeDiagnostic>()
        val lines = file.content.lines()

        if (file.fileType == IdeFileType.KOTLIN &&
            lines.none { it.trimStart().startsWith("package ") }
        ) {
            diagnostics += CodeDiagnostic(
                fileId = file.id,
                fileName = file.name,
                line = 1,
                message = "Missing package declaration at top of Kotlin file.",
                severity = DiagnosticSeverity.WARNING,
            )
        }

        // Balance braces, parentheses, brackets while respecting strings & comments
        data class OpenToken(val ch: Char, val line: Int)
        val stack = ArrayDeque<OpenToken>()
        var inBlockComment = false

        lines.forEachIndexed { idx, rawLine ->
            val lineNum = idx + 1
            if (rawLine.contains("TODO", ignoreCase = false)) {
                diagnostics += CodeDiagnostic(
                    fileId = file.id,
                    fileName = file.name,
                    line = lineNum,
                    message = "TODO marker found: ${rawLine.trim().take(60)}",
                    severity = DiagnosticSeverity.INFO,
                )
            }

            var i = 0
            var inString = false
            while (i < rawLine.length) {
                val c = rawLine[i]
                if (inBlockComment) {
                    if (c == '*' && i + 1 < rawLine.length && rawLine[i + 1] == '/') {
                        inBlockComment = false
                        i += 2
                        continue
                    }
                    i++
                    continue
                }
                if (!inString && c == '/' && i + 1 < rawLine.length) {
                    if (rawLine[i + 1] == '/') break
                    if (rawLine[i + 1] == '*') {
                        inBlockComment = true
                        i += 2
                        continue
                    }
                }
                if (c == '"') {
                    if (inString && i > 0 && rawLine[i - 1] == '\\') {
                        i++
                        continue
                    }
                    inString = !inString
                    i++
                    continue
                }
                if (!inString) {
                    when (c) {
                        '{', '(', '[' -> stack.addLast(OpenToken(c, lineNum))
                        '}', ')', ']' -> {
                            val expectedOpen = when (c) {
                                '}' -> '{'
                                ')' -> '('
                                else -> '['
                            }
                            if (stack.isEmpty()) {
                                diagnostics += CodeDiagnostic(
                                    fileId = file.id,
                                    fileName = file.name,
                                    line = lineNum,
                                    message = "Unexpected closing '$c' with no matching '$expectedOpen'.",
                                    severity = DiagnosticSeverity.ERROR,
                                )
                            } else if (stack.last().ch != expectedOpen) {
                                val top = stack.removeLast()
                                diagnostics += CodeDiagnostic(
                                    fileId = file.id,
                                    fileName = file.name,
                                    line = lineNum,
                                    message = "Mismatched '$c': expected closing for '${top.ch}' opened on line ${top.line}.",
                                    severity = DiagnosticSeverity.ERROR,
                                )
                            } else {
                                stack.removeLast()
                            }
                        }
                    }
                }
                i++
            }

            if (inString) {
                diagnostics += CodeDiagnostic(
                    fileId = file.id,
                    fileName = file.name,
                    line = lineNum,
                    message = "Unclosed string literal '\"' on line $lineNum.",
                    severity = DiagnosticSeverity.ERROR,
                )
            }
        }

        while (stack.isNotEmpty()) {
            val unclosed = stack.removeLast()
            diagnostics += CodeDiagnostic(
                fileId = file.id,
                fileName = file.name,
                line = unclosed.line,
                message = "Unclosed '${unclosed.ch}' opened on line ${unclosed.line}.",
                severity = DiagnosticSeverity.ERROR,
            )
        }

        return diagnostics
    }

    private fun analyzeXml(file: IdeFileEntity): List<CodeDiagnostic> {
        val diagnostics = mutableListOf<CodeDiagnostic>()
        val text = file.content

        if (file.name.equals("AndroidManifest.xml", ignoreCase = true) &&
            !text.contains("<manifest")
        ) {
            diagnostics += CodeDiagnostic(
                fileId = file.id,
                fileName = file.name,
                line = 1,
                message = "AndroidManifest.xml must declare a root <manifest> element.",
                severity = DiagnosticSeverity.ERROR,
            )
        }

        val tagStack = ArrayDeque<Pair<String, Int>>()
        val lines = text.lines()
        val tagRegex = Regex("<(/?)([a-zA-Z0-9_.:-]+)([^>]*)>")

        lines.forEachIndexed { idx, line ->
            val lineNum = idx + 1
            for (match in tagRegex.findAll(line)) {
                val raw = match.value
                if (raw.startsWith("<?") || raw.startsWith("<!")) continue
                val isClosing = match.groupValues[1] == "/"
                val tagName = match.groupValues[2]
                val attrs = match.groupValues[3]
                val isSelfClosing = attrs.trimEnd().endsWith("/")

                if (isSelfClosing) continue

                if (!isClosing) {
                    tagStack.addLast(tagName to lineNum)
                } else {
                    if (tagStack.isEmpty()) {
                        diagnostics += CodeDiagnostic(
                            fileId = file.id,
                            fileName = file.name,
                            line = lineNum,
                            message = "Unexpected closing XML tag </$tagName>.",
                            severity = DiagnosticSeverity.ERROR,
                        )
                    } else if (tagStack.last().first != tagName) {
                        val expected = tagStack.removeLast()
                        diagnostics += CodeDiagnostic(
                            fileId = file.id,
                            fileName = file.name,
                            line = lineNum,
                            message = "Mismatched XML closing tag </$tagName>; expected </${expected.first}>.",
                            severity = DiagnosticSeverity.ERROR,
                        )
                    } else {
                        tagStack.removeLast()
                    }
                }
            }
        }

        while (tagStack.isNotEmpty()) {
            val unclosed = tagStack.removeLast()
            diagnostics += CodeDiagnostic(
                fileId = file.id,
                fileName = file.name,
                line = unclosed.second,
                message = "Unclosed XML tag <${unclosed.first}>.",
                severity = DiagnosticSeverity.ERROR,
            )
        }

        return diagnostics
    }
}
