package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.model.BottomToolTab
import com.example.data.model.EditorPreferences
import com.example.data.model.PreviewDeviceFrame
import com.example.data.model.WorkspaceMode
import com.example.ui.components.CodeEditorPane
import com.example.ui.components.IdeBottomToolSection
import com.example.ui.components.IdeTopToolbar
import com.example.ui.components.LivePreviewPane
import com.example.ui.components.OpenFileTabsBar
import com.example.ui.components.ProjectExplorerList
import com.example.ui.viewmodel.IdeUiState
import com.example.ui.viewmodel.IdeViewModel

@Composable
fun IdeWorkspaceScreen(
    uiState: IdeUiState,
    preferences: EditorPreferences,
    viewModel: IdeViewModel,
    modifier: Modifier = Modifier,
) {
    val project = uiState.activeProject ?: return
    val context = LocalContext.current

    BackHandler {
        if (uiState.bottomToolTab != BottomToolTab.NONE) {
            viewModel.toggleBottomToolTab(uiState.bottomToolTab)
        } else {
            viewModel.closeProject()
        }
    }

    val openFiles = remember(uiState.openTabIds, uiState.projectFiles) {
        uiState.openTabIds.mapNotNull { tabId ->
            uiState.projectFiles.firstOrNull { it.id == tabId }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        IdeTopToolbar(
            project = project,
            buildStatus = uiState.buildStatus,
            workspaceMode = uiState.workspaceMode,
            canUndo = uiState.canUndo,
            canRedo = uiState.canRedo,
            onBackClick = viewModel::closeProject,
            onRunClick = viewModel::runProject,
            onModeChange = viewModel::setWorkspaceMode,
            onUndoClick = viewModel::undo,
            onRedoClick = viewModel::redo,
            onFormatClick = viewModel::formatActiveFile,
            onShareClick = {
                val active = uiState.activeFile ?: return@IdeTopToolbar
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "${project.name}/${active.name}")
                    putExtra(Intent.EXTRA_TEXT, uiState.editorValue.text)
                }
                context.startActivity(Intent.createChooser(sendIntent, "Share ${active.name}"))
            },
            onSettingsClick = { viewModel.setShowSettingsDialog(true) },
        )

        OpenFileTabsBar(
            openFiles = openFiles,
            activeFileId = uiState.activeFileId,
            onSelectFile = { viewModel.openFile(it) },
            onCloseTab = viewModel::closeTab,
        )

        // Adaptive Workspace Body
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            val isExpandedScreen = maxWidth >= 840.dp

            Row(modifier = Modifier.fillMaxSize()) {
                // Persistent Project Sidebar on Tablets / Foldables
                if (isExpandedScreen) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .width(230.dp)
                            .fillMaxHeight(),
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = "PROJECT",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            ProjectExplorerList(
                                files = uiState.projectFiles,
                                activeFileId = uiState.activeFileId,
                                onSelectFile = { viewModel.openFile(it) },
                                onDeleteFile = viewModel::deleteFile,
                            )
                        }
                    }
                    VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }

                // Main Editor / Preview Pane(s)
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    when (uiState.workspaceMode) {
                        WorkspaceMode.CODE -> {
                            CodeEditorPane(
                                activeFile = uiState.activeFile,
                                editorValue = uiState.editorValue,
                                diagnostics = uiState.activeFileDiagnostics,
                                preferences = preferences,
                                onValueChange = viewModel::onEditorValueChange,
                                onInsertSnippet = viewModel::insertSnippet,
                                onOpenProblems = { viewModel.toggleBottomToolTab(BottomToolTab.PROBLEMS) },
                            )
                        }
                        WorkspaceMode.PREVIEW -> {
                            LivePreviewPane(
                                parsedPreview = uiState.parsedPreview,
                                previewStateMap = uiState.previewStateMap,
                                previewDarkTheme = uiState.previewDarkTheme,
                                showLayoutBounds = preferences.showLayoutBounds,
                                deviceFrame = uiState.selectedDeviceFrame,
                                onActionClick = viewModel::onPreviewActionClick,
                                onStateChange = viewModel::onPreviewStateChange,
                                onResetState = viewModel::resetPreviewState,
                                onToggleTheme = viewModel::togglePreviewTheme,
                                onToggleLayoutBounds = {
                                    viewModel.updatePreferences { it.copy(showLayoutBounds = !it.showLayoutBounds) }
                                },
                                onCycleDeviceFrame = {
                                    val next = if (uiState.selectedDeviceFrame == PreviewDeviceFrame.PIXEL_PHONE) {
                                        PreviewDeviceFrame.TABLET_COMPACT
                                    } else {
                                        PreviewDeviceFrame.PIXEL_PHONE
                                    }
                                    viewModel.setDeviceFrame(next)
                                },
                            )
                        }
                        WorkspaceMode.SPLIT -> {
                            if (isExpandedScreen) {
                                Row(modifier = Modifier.fillMaxSize()) {
                                    CodeEditorPane(
                                        activeFile = uiState.activeFile,
                                        editorValue = uiState.editorValue,
                                        diagnostics = uiState.activeFileDiagnostics,
                                        preferences = preferences,
                                        onValueChange = viewModel::onEditorValueChange,
                                        onInsertSnippet = viewModel::insertSnippet,
                                        onOpenProblems = { viewModel.toggleBottomToolTab(BottomToolTab.PROBLEMS) },
                                        modifier = Modifier.weight(1.1f).fillMaxHeight(),
                                    )
                                    VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    LivePreviewPane(
                                        parsedPreview = uiState.parsedPreview,
                                        previewStateMap = uiState.previewStateMap,
                                        previewDarkTheme = uiState.previewDarkTheme,
                                        showLayoutBounds = preferences.showLayoutBounds,
                                        deviceFrame = uiState.selectedDeviceFrame,
                                        onActionClick = viewModel::onPreviewActionClick,
                                        onStateChange = viewModel::onPreviewStateChange,
                                        onResetState = viewModel::resetPreviewState,
                                        onToggleTheme = viewModel::togglePreviewTheme,
                                        onToggleLayoutBounds = {
                                            viewModel.updatePreferences { it.copy(showLayoutBounds = !it.showLayoutBounds) }
                                        },
                                        onCycleDeviceFrame = {
                                            val next = if (uiState.selectedDeviceFrame == PreviewDeviceFrame.PIXEL_PHONE) {
                                                PreviewDeviceFrame.TABLET_COMPACT
                                            } else {
                                                PreviewDeviceFrame.PIXEL_PHONE
                                            }
                                            viewModel.setDeviceFrame(next)
                                        },
                                        modifier = Modifier.weight(0.9f).fillMaxHeight(),
                                    )
                                }
                            } else {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    CodeEditorPane(
                                        activeFile = uiState.activeFile,
                                        editorValue = uiState.editorValue,
                                        diagnostics = uiState.activeFileDiagnostics,
                                        preferences = preferences,
                                        onValueChange = viewModel::onEditorValueChange,
                                        onInsertSnippet = viewModel::insertSnippet,
                                        onOpenProblems = { viewModel.toggleBottomToolTab(BottomToolTab.PROBLEMS) },
                                        modifier = Modifier.weight(1f).fillMaxWidth(),
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                                    LivePreviewPane(
                                        parsedPreview = uiState.parsedPreview,
                                        previewStateMap = uiState.previewStateMap,
                                        previewDarkTheme = uiState.previewDarkTheme,
                                        showLayoutBounds = preferences.showLayoutBounds,
                                        deviceFrame = uiState.selectedDeviceFrame,
                                        onActionClick = viewModel::onPreviewActionClick,
                                        onStateChange = viewModel::onPreviewStateChange,
                                        onResetState = viewModel::resetPreviewState,
                                        onToggleTheme = viewModel::togglePreviewTheme,
                                        onToggleLayoutBounds = {
                                            viewModel.updatePreferences { it.copy(showLayoutBounds = !it.showLayoutBounds) }
                                        },
                                        onCycleDeviceFrame = {
                                            val next = if (uiState.selectedDeviceFrame == PreviewDeviceFrame.PIXEL_PHONE) {
                                                PreviewDeviceFrame.TABLET_COMPACT
                                            } else {
                                                PreviewDeviceFrame.PIXEL_PHONE
                                            }
                                            viewModel.setDeviceFrame(next)
                                        },
                                        modifier = Modifier.weight(0.95f).fillMaxWidth(),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        IdeBottomToolSection(
            activeTab = uiState.bottomToolTab,
            projectFiles = uiState.projectFiles,
            activeFileId = uiState.activeFileId,
            buildStatus = uiState.buildStatus,
            buildOutput = uiState.buildOutput,
            logcatEntries = uiState.logcatEntries,
            logcatFilter = uiState.logcatLevelFilter,
            logcatSearch = uiState.logcatSearchQuery,
            diagnostics = uiState.diagnostics,
            terminalLines = uiState.terminalLines,
            onToggleTab = viewModel::toggleBottomToolTab,
            onSelectFile = { fileId, line -> viewModel.openFile(fileId, line) },
            onNewFileClick = { viewModel.setShowNewFileDialog(true) },
            onDeleteFile = viewModel::deleteFile,
            onLogcatFilterChange = viewModel::setLogcatLevelFilter,
            onLogcatSearchChange = viewModel::setLogcatSearchQuery,
            onClearLogcat = viewModel::clearLogcat,
            onExecuteTerminal = viewModel::executeTerminalCommand,
        )
    }
}
