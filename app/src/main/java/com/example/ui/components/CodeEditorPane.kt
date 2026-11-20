package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.IdeFileEntity
import com.example.data.model.CodeDiagnostic
import com.example.data.model.DiagnosticSeverity
import com.example.data.model.EditorPreferences
import com.example.engine.IdeSyntaxVisualTransformation
import com.example.ui.theme.BuildAmber
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalEditorSyntaxColors

@Composable
fun CodeEditorPane(
    activeFile: IdeFileEntity?,
    editorValue: TextFieldValue,
    diagnostics: List<CodeDiagnostic>,
    preferences: EditorPreferences,
    onValueChange: (TextFieldValue) -> Unit,
    onInsertSnippet: (String) -> Unit,
    onOpenProblems: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val syntaxColors = LocalEditorSyntaxColors.current

    if (activeFile == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(syntaxColors.editorBg),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Select a file from Project Explorer to start coding",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    val verticalScroll = rememberScrollState()
    val horizontalScroll = rememberScrollState()
    val lines = remember(editorValue.text) { editorValue.text.split('\n') }
    val diagnosticsByLine = remember(diagnostics) {
        diagnostics.groupBy { it.line }
    }
    val visualTransformation = remember(activeFile.fileType, syntaxColors) {
        IdeSyntaxVisualTransformation(activeFile.fileType, syntaxColors)
    }

    val lineHeightSp = (preferences.fontSizeSp + 7).sp
    val codeTextStyle = TextStyle(
        fontFamily = JetBrainsMonoFontFamily,
        fontSize = preferences.fontSizeSp.sp,
        lineHeight = lineHeightSp,
        color = syntaxColors.plainText,
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(syntaxColors.editorBg),
    ) {
        // Breadcrumb + Diagnostic Summary strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(syntaxColors.gutterBg)
                .padding(horizontal = 12.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = activeFile.fullPath,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            if (diagnostics.isNotEmpty()) {
                val hasError = diagnostics.any { it.severity == DiagnosticSeverity.ERROR }
                val badgeColor = if (hasError) ErrorCrimson else BuildAmber
                Surface(
                    color = badgeColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onOpenProblems),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = if (hasError) Icons.Default.ErrorOutline else Icons.Default.WarningAmber,
                            contentDescription = "Diagnostics",
                            tint = badgeColor,
                            modifier = Modifier.size(13.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${diagnostics.size}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                        )
                    }
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // Editor Body (Gutter + TextField)
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(verticalScroll),
        ) {
            if (preferences.showLineNumbers) {
                Column(
                    modifier = Modifier
                        .background(syntaxColors.gutterBg)
                        .padding(start = 6.dp, end = 8.dp, top = 10.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.End,
                ) {
                    lines.forEachIndexed { idx, _ ->
                        val lineNum = idx + 1
                        val lineDiags = diagnosticsByLine[lineNum]
                        val markerColor = when {
                            lineDiags?.any { it.severity == DiagnosticSeverity.ERROR } == true -> ErrorCrimson
                            lineDiags?.any { it.severity == DiagnosticSeverity.WARNING } == true -> BuildAmber
                            else -> null
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End,
                        ) {
                            if (markerColor != null) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(markerColor),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = lineNum.toString(),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = preferences.fontSizeSp.sp,
                                lineHeight = lineHeightSp,
                                color = markerColor ?: syntaxColors.lineNumber,
                                textAlign = TextAlign.End,
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )
            }

            val textFieldModifier = Modifier
                .weight(1f)
                .then(
                    if (preferences.wordWrap) Modifier
                    else Modifier.horizontalScroll(horizontalScroll)
                )
                .padding(horizontal = 10.dp, vertical = 10.dp)
                .testTag("code_editor_input")

            BasicTextField(
                value = editorValue,
                onValueChange = onValueChange,
                textStyle = codeTextStyle,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                visualTransformation = visualTransformation,
                modifier = textFieldModifier,
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        DeveloperAccessoryBar(onInsertSnippet = onInsertSnippet)
    }
}

@Composable
private fun DeveloperAccessoryBar(
    onInsertSnippet: (String) -> Unit,
) {
    val snippets = remember {
        listOf(
            "Tab" to "    ",
            "{}" to "{}",
            "()" to "()",
            "=" to " = ",
            "->" to " -> ",
            "@Composable" to "@Composable\n",
            "Text" to "Text(text = \"Hello\")",
            "Button" to "Button(onClick = {  }) {\n    Text(\"Action\")\n}",
            "Color" to "Color(0xFF3DDC84)",
            "Modifier" to "Modifier.padding(12.dp)",
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        snippets.forEach { (label, code) ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(7.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(7.dp))
                    .clickable { onInsertSnippet(code) }
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(7.dp),
                    )
                    .testTag("snippet_key_$label"),
            ) {
                Text(
                    text = label,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
    }
}
