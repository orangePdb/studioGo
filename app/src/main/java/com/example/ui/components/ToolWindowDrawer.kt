package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.IdeFileEntity
import com.example.data.model.BottomToolTab
import com.example.data.model.BuildOutputLine
import com.example.data.model.BuildStatus
import com.example.data.model.CodeDiagnostic
import com.example.data.model.DiagnosticSeverity
import com.example.data.model.LogLevel
import com.example.data.model.LogcatEntry
import com.example.data.model.TerminalLine
import com.example.ui.theme.AndroidEmerald
import com.example.ui.theme.BuildAmber
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StudioBlue

@Composable
fun IdeBottomToolSection(
    activeTab: BottomToolTab,
    projectFiles: List<IdeFileEntity>,
    activeFileId: Long?,
    buildStatus: BuildStatus,
    buildOutput: List<BuildOutputLine>,
    logcatEntries: List<LogcatEntry>,
    logcatFilter: LogLevel,
    logcatSearch: String,
    diagnostics: List<CodeDiagnostic>,
    terminalLines: List<TerminalLine>,
    onToggleTab: (BottomToolTab) -> Unit,
    onSelectFile: (Long, Int?) -> Unit,
    onNewFileClick: () -> Unit,
    onDeleteFile: (Long) -> Unit,
    onLogcatFilterChange: (LogLevel) -> Unit,
    onLogcatSearchChange: (String) -> Unit,
    onClearLogcat: () -> Unit,
    onExecuteTerminal: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        AnimatedVisibility(
            visible = activeTab != BottomToolTab.NONE,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(270.dp)
                    .background(MaterialTheme.colorScheme.surface),
            ) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                // Tool Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = activeTab.label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    if (activeTab == BottomToolTab.PROJECT) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable(onClick = onNewFileClick)
                                .testTag("new_file_button"),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "New File",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(14.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "New File",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    if (activeTab == BottomToolTab.LOGCAT) {
                        IconButton(
                            onClick = onClearLogcat,
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.ClearAll,
                                contentDescription = "Clear Logcat",
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    IconButton(
                        onClick = { onToggleTab(activeTab) },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Hide panel",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                when (activeTab) {
                    BottomToolTab.PROJECT -> ProjectExplorerList(
                        files = projectFiles,
                        activeFileId = activeFileId,
                        onSelectFile = { onSelectFile(it, null) },
                        onDeleteFile = onDeleteFile,
                    )
                    BottomToolTab.BUILD -> BuildOutputPanel(
                        buildStatus = buildStatus,
                        lines = buildOutput,
                    )
                    BottomToolTab.LOGCAT -> LogcatPanel(
                        entries = logcatEntries,
                        filterLevel = logcatFilter,
                        searchQuery = logcatSearch,
                        onFilterChange = onLogcatFilterChange,
                        onSearchChange = onLogcatSearchChange,
                    )
                    BottomToolTab.PROBLEMS -> ProblemsPanel(
                        diagnostics = diagnostics,
                        onDiagnosticClick = { diag -> onSelectFile(diag.fileId, diag.line) },
                    )
                    BottomToolTab.TERMINAL -> TerminalPanel(
                        lines = terminalLines,
                        onExecute = onExecuteTerminal,
                    )
                    BottomToolTab.NONE -> Unit
                }
            }
        }

        // Persistent Bottom Tool Strip
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ToolTabChip(
                tab = BottomToolTab.PROJECT,
                icon = Icons.Default.FolderOpen,
                selected = activeTab == BottomToolTab.PROJECT,
                badgeCount = projectFiles.size,
                badgeColor = StudioBlue,
                onClick = { onToggleTab(BottomToolTab.PROJECT) },
            )
            ToolTabChip(
                tab = BottomToolTab.BUILD,
                icon = Icons.Default.Build,
                selected = activeTab == BottomToolTab.BUILD,
                badgeDotColor = when (buildStatus) {
                    BuildStatus.SUCCESS -> AndroidEmerald
                    BuildStatus.FAILED -> ErrorCrimson
                    BuildStatus.BUILDING -> BuildAmber
                    BuildStatus.IDLE -> null
                },
                onClick = { onToggleTab(BottomToolTab.BUILD) },
            )
            ToolTabChip(
                tab = BottomToolTab.LOGCAT,
                icon = Icons.Default.BugReport,
                selected = activeTab == BottomToolTab.LOGCAT,
                badgeCount = logcatEntries.size.takeIf { it > 0 },
                badgeColor = AndroidEmerald,
                onClick = { onToggleTab(BottomToolTab.LOGCAT) },
            )
            val errCount = diagnostics.count { it.severity == DiagnosticSeverity.ERROR }
            val warnCount = diagnostics.count { it.severity == DiagnosticSeverity.WARNING }
            ToolTabChip(
                tab = BottomToolTab.PROBLEMS,
                icon = Icons.Default.WarningAmber,
                selected = activeTab == BottomToolTab.PROBLEMS,
                badgeCount = diagnostics.size.takeIf { it > 0 },
                badgeColor = when {
                    errCount > 0 -> ErrorCrimson
                    warnCount > 0 -> BuildAmber
                    else -> StudioBlue
                },
                onClick = { onToggleTab(BottomToolTab.PROBLEMS) },
            )
            ToolTabChip(
                tab = BottomToolTab.TERMINAL,
                icon = Icons.Default.Terminal,
                selected = activeTab == BottomToolTab.TERMINAL,
                onClick = { onToggleTab(BottomToolTab.TERMINAL) },
            )
        }
    }
}

@Composable
private fun ToolTabChip(
    tab: BottomToolTab,
    icon: ImageVector,
    selected: Boolean,
    badgeCount: Int? = null,
    badgeColor: Color = StudioBlue,
    badgeDotColor: Color? = null,
    onClick: () -> Unit,
) {
    Surface(
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        },
        shape = RoundedCornerShape(9.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .clickable(onClick = onClick)
            .testTag("tool_tab_${tab.name.lowercase()}"),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = tab.label,
                modifier = Modifier.size(15.dp),
                tint = if (selected) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = tab.label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            if (badgeDotColor != null) {
                Spacer(modifier = Modifier.width(5.dp))
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(badgeDotColor),
                )
            } else if (badgeCount != null && badgeCount > 0) {
                Spacer(modifier = Modifier.width(5.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeColor.copy(alpha = 0.22f))
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                ) {
                    Text(
                        text = badgeCount.toString(),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                    )
                }
            }
        }
    }
}

@Composable
fun ProjectExplorerList(
    files: List<IdeFileEntity>,
    activeFileId: Long?,
    onSelectFile: (Long) -> Unit,
    onDeleteFile: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val grouped = remember(files) { files.groupBy { it.folderPath } }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        grouped.forEach { (folder, folderFiles) ->
            item(key = "folder_$folder") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = BuildAmber,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = folder.ifBlank { "root" },
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            items(folderFiles, key = { it.id }) { file ->
                val isSelected = file.id == activeFileId
                val dotColor = fileBadgeColor(file.fileType)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 4.dp, top = 1.dp, bottom = 1.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                            else Color.Transparent
                        )
                        .clickable { onSelectFile(file.id) }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                        .testTag("explorer_file_${file.name}"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(dotColor),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = file.name,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.weight(1f),
                    )
                    if (files.size > 1) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete ${file.name}",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { onDeleteFile(file.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BuildOutputPanel(
    buildStatus: BuildStatus,
    lines: List<BuildOutputLine>,
) {
    if (lines.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Tap Run ▶ to execute Gradle build tasks.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(lines) { line ->
            val color = when {
                line.isError -> ErrorCrimson
                line.isSuccess -> AndroidEmerald
                else -> MaterialTheme.colorScheme.onSurface
            }
            Text(
                text = line.text,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 11.sp,
                fontWeight = if (line.isSuccess || line.isError) FontWeight.SemiBold else FontWeight.Normal,
                color = color,
            )
        }
    }
}

@Composable
private fun LogcatPanel(
    entries: List<LogcatEntry>,
    filterLevel: LogLevel,
    searchQuery: String,
    onFilterChange: (LogLevel) -> Unit,
    onSearchChange: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            LogLevel.entries.forEach { lvl ->
                val selected = lvl == filterLevel
                Surface(
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onFilterChange(lvl) },
                ) {
                    Text(
                        text = lvl.shortCode,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Filter tag/msg...", fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily),
            )
        }

        val filtered = remember(entries, filterLevel, searchQuery) {
            entries.filter { entry ->
                entry.level.ordinal >= filterLevel.ordinal &&
                    (searchQuery.isBlank() ||
                        entry.tag.contains(searchQuery, ignoreCase = true) ||
                        entry.message.contains(searchQuery, ignoreCase = true))
            }
        }

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No matching Logcat events.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                items(filtered, key = { it.id }) { entry ->
                    val lvlColor = when (entry.level) {
                        LogLevel.ERROR -> ErrorCrimson
                        LogLevel.WARN -> BuildAmber
                        LogLevel.INFO -> AndroidEmerald
                        LogLevel.DEBUG -> StudioBlue
                        LogLevel.VERBOSE -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Text(
                        text = "${entry.timestamp} ${entry.level.shortCode}/${entry.tag}: ${entry.message}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        color = lvlColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProblemsPanel(
    diagnostics: List<CodeDiagnostic>,
    onDiagnosticClick: (CodeDiagnostic) -> Unit,
) {
    if (diagnostics.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "No problems detected in workspace.",
                style = MaterialTheme.typography.bodySmall,
                color = AndroidEmerald,
            )
        }
        return
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(diagnostics) { diag ->
            val color = when (diag.severity) {
                DiagnosticSeverity.ERROR -> ErrorCrimson
                DiagnosticSeverity.WARNING -> BuildAmber
                DiagnosticSeverity.INFO -> StudioBlue
            }
            val icon = when (diag.severity) {
                DiagnosticSeverity.ERROR -> Icons.Default.ErrorOutline
                DiagnosticSeverity.WARNING -> Icons.Default.WarningAmber
                DiagnosticSeverity.INFO -> Icons.Default.Info
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.08f))
                    .clickable { onDiagnosticClick(diag) }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = diag.severity.name,
                    tint = color,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = diag.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "${diag.fileName} : line ${diag.line}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        color = color,
                    )
                }
            }
        }
    }
}

@Composable
private fun TerminalPanel(
    lines: List<TerminalLine>,
    onExecute: (String) -> Unit,
) {
    var commandInput by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items(lines) { line ->
                val color = when {
                    line.isError -> ErrorCrimson
                    line.isCommand -> AndroidEmerald
                    line.isAccent -> StudioBlue
                    else -> MaterialTheme.colorScheme.onSurface
                }
                Text(
                    text = line.text,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    color = color,
                )
            }
        }

        // Quick command chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            listOf("ls", "tree", "gradle assembleDebug", "sdk", "clear").forEach { quick ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onExecute(quick) },
                ) {
                    Text(
                        text = quick,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = commandInput,
                onValueChange = { commandInput = it },
                placeholder = {
                    Text(
                        text = "$ enter command (help, ls, gradle assembleDebug)...",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        onExecute(commandInput)
                        commandInput = ""
                    }
                ),
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily),
                modifier = Modifier
                    .weight(1f)
                    .testTag("terminal_command_input"),
            )
            Spacer(modifier = Modifier.width(6.dp))
            IconButton(
                onClick = {
                    onExecute(commandInput)
                    commandInput = ""
                },
                modifier = Modifier.testTag("terminal_send_button"),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Run command",
                    tint = AndroidEmerald,
                )
            }
        }
    }
}
